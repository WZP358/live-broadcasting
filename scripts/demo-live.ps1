param(
    [int]$Rooms = 16,
    [int]$TickSeconds = 8,
    [switch]$Once,
    [switch]$StopOnly
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$envPath = Join-Path $root ".env"
$demoTag = "PULSELIVE_DEMO"
$userPrefix = "demo_anchor_"
$viewerPrefix = "demo_viewer_"
$adminUsername = "demo_admin"
$script:DemoPasswordHash = '$2a$10$puULYxVheVu/sJZk7rUbvujNheV9v7afPWETHv47sjS2KAXNptTEe'

function Read-DotEnv {
    param([string]$Path)
    $values = @{}
    if (!(Test-Path $Path)) {
        return $values
    }

    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (!$line -or $line.StartsWith("#") -or !$line.Contains("=")) {
            return
        }
        $idx = $line.IndexOf("=")
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim().Trim('"').Trim("'")
        $values[$key] = $value
    }
    return $values
}

function Get-JdbcDatabase {
    param([string]$JdbcUrl)
    if ($JdbcUrl -match "jdbc:mysql://[^/]+/([^?]+)") {
        return $Matches[1]
    }
    return "ant-live"
}

function Escape-Sql {
    param([AllowNull()][string]$Value)
    if ($null -eq $Value) {
        return "NULL"
    }
    return "'" + $Value.Replace("\", "\\").Replace("'", "''") + "'"
}

function Invoke-Mysql {
    param(
        [Parameter(Mandatory = $true)][string]$Sql,
        [switch]$Scalar
    )

    $mysqlArgs = @(
        "--default-character-set=utf8mb4",
        "-u$script:DbUser",
        "-D", $script:DbName,
        "--batch",
        "--raw",
        "--skip-column-names",
        "-e", $Sql
    )

    $previousMysqlPwd = [Environment]::GetEnvironmentVariable("MYSQL_PWD", "Process")
    try {
        if ($script:DbPassword) {
            $env:MYSQL_PWD = $script:DbPassword
        } else {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        }

        $output = & mysql @mysqlArgs
    } finally {
        if ($null -ne $previousMysqlPwd) {
            $env:MYSQL_PWD = $previousMysqlPwd
        } else {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        }
    }

    if ($LASTEXITCODE -ne 0) {
        throw "mysql command failed"
    }
    if ($Scalar) {
        return ($output | Select-Object -First 1)
    }
    return $output
}

function Invoke-OptionalSql {
    param([string]$Sql)
    try {
        Invoke-Mysql -Sql $Sql | Out-Null
    } catch {
        Write-Host "[demo] skipped optional SQL: $($_.Exception.Message)" -ForegroundColor DarkYellow
    }
}

function Test-Table {
    param([string]$Table)
    $sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = $(Escape-Sql $Table)"
    return [int](Invoke-Mysql -Sql $sql -Scalar) -gt 0
}

function Ensure-DemoSchema {
    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS report (
  id INT AUTO_INCREMENT PRIMARY KEY,
  reporter_id INT NOT NULL,
  target_user_id INT DEFAULT NULL,
  room_id INT DEFAULT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id VARCHAR(64) DEFAULT NULL,
  reason VARCHAR(255) NOT NULL,
  description TEXT NULL,
  status TINYINT DEFAULT 0,
  handle_result VARCHAR(500) DEFAULT '',
  handler_id INT DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  handle_time DATETIME DEFAULT NULL,
  INDEX idx_reporter (reporter_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS customer_service_ticket (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  category VARCHAR(32) NOT NULL DEFAULT 'general',
  title VARCHAR(120) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  handler_id INT DEFAULT NULL,
  reply VARCHAR(1000) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  reply_time DATETIME DEFAULT NULL,
  INDEX idx_customer_ticket_user (user_id),
  INDEX idx_customer_ticket_status (status),
  INDEX idx_customer_ticket_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS notification (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  type VARCHAR(32) NOT NULL,
  title VARCHAR(255) NOT NULL,
  content VARCHAR(500) DEFAULT '',
  related_id INT DEFAULT NULL,
  is_read TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_read (user_id, is_read),
  INDEX idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS notification_pref (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  live_start_enabled TINYINT DEFAULT 1,
  follow_enabled TINYINT DEFAULT 1,
  dnd_start VARCHAR(5) DEFAULT NULL,
  dnd_end VARCHAR(5) DEFAULT NULL,
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS guardian_subscription (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  target_user_id INT NOT NULL,
  level TINYINT NOT NULL DEFAULT 1,
  amount DECIMAL(10,2) NOT NULL,
  expire_time DATETIME NOT NULL,
  auto_renew TINYINT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_target (user_id, target_user_id),
  INDEX idx_target (target_user_id, status),
  INDEX idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS private_message (
  id INT AUTO_INCREMENT PRIMARY KEY,
  from_user_id INT NOT NULL,
  to_user_id INT NOT NULL,
  content VARCHAR(500) NOT NULL,
  is_read TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_conversation (from_user_id, to_user_id),
  INDEX idx_to_user (to_user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS message (
  id INT AUTO_INCREMENT PRIMARY KEY,
  room_id INT NOT NULL,
  from_uid INT NOT NULL,
  content TEXT,
  reply_msg_id INT DEFAULT NULL,
  status TINYINT DEFAULT 0,
  type TINYINT DEFAULT 1,
  extra VARCHAR(1000) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_room_id (room_id),
  INDEX idx_from_uid (from_uid),
  INDEX idx_status (status),
  INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS room_tag (
  id INT AUTO_INCREMENT PRIMARY KEY,
  room_id INT NOT NULL,
  tag_name VARCHAR(32) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS room_moderator (
  id INT AUTO_INCREMENT PRIMARY KEY,
  room_id INT NOT NULL,
  user_id INT NOT NULL,
  appointed_by INT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_room_user (room_id, user_id),
  INDEX idx_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS user_level (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  exp BIGINT DEFAULT 0,
  level INT DEFAULT 1,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS settlement (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  period VARCHAR(7) NOT NULL,
  gift_income DECIMAL(12,2) DEFAULT 0,
  platform_fee DECIMAL(12,2) DEFAULT 0,
  net_income DECIMAL(12,2) DEFAULT 0,
  withdrawable DECIMAL(12,2) DEFAULT 0,
  withdrawn DECIMAL(12,2) DEFAULT 0,
  status TINYINT DEFAULT 0,
  settle_time DATETIME DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_period (user_id, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS tb_wallet (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  balance DECIMAL(16,2) NOT NULL DEFAULT 0.00,
  version INT NOT NULL DEFAULT 0,
  sign VARCHAR(255) DEFAULT NULL,
  status INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tb_wallet_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null

    Invoke-Mysql -Sql @"
CREATE TABLE IF NOT EXISTS tb_wallet_log (
  id INT NOT NULL AUTO_INCREMENT,
  wallet_id INT NOT NULL,
  balance DECIMAL(16,2) NOT NULL,
  fee DECIMAL(16,2) NOT NULL,
  action_type INT NOT NULL,
  source_uuid VARCHAR(64) DEFAULT NULL,
  source_type VARCHAR(32) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tb_wallet_log_source_uuid (source_uuid),
  KEY idx_tb_wallet_log_wallet_id (wallet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@ | Out-Null
}

function Stop-DemoLive {
    $now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Invoke-OptionalSql @"
UPDATE live_info li
JOIN room r ON r.id = li.room_id
JOIN user u ON u.id = r.user_id
SET li.status = 1,
    li.end_time = COALESCE(li.end_time, '$now'),
    li.update_time = '$now'
WHERE li.status = 0
  AND u.username LIKE '$userPrefix%';
"@

    Invoke-OptionalSql @"
UPDATE room r
JOIN user u ON u.id = r.user_id
SET r.status = 0,
    r.update_time = '$now'
WHERE u.username LIKE '$userPrefix%';
"@

    Write-Host "[demo] closed demo live rooms." -ForegroundColor Green
}

function Ensure-Categories {
    $categories = @(
        @{ Name = "游戏竞技"; Sort = 1 },
        @{ Name = "学习自习"; Sort = 2 },
        @{ Name = "音乐现场"; Sort = 3 },
        @{ Name = "科技数码"; Sort = 4 },
        @{ Name = "生活分享"; Sort = 5 },
        @{ Name = "户外旅行"; Sort = 6 },
        @{ Name = "美食厨房"; Sort = 7 },
        @{ Name = "创作设计"; Sort = 8 }
    )

    foreach ($category in $categories) {
        Invoke-Mysql -Sql @"
INSERT INTO category (name, sort, disabled, is_deleted, create_time, update_time, parent_id)
SELECT $(Escape-Sql $category.Name), $($category.Sort), 0, 0, NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = $(Escape-Sql $category.Name));
"@ | Out-Null
    }
}

function Ensure-DemoPresents {
    if (!(Test-Table "present")) {
        return
    }

    $presents = @(
        @{ Name = "星光"; Icon = "https://api.iconify.design/fluent-emoji/star.svg"; Price = 6; Sort = 1 },
        @{ Name = "应援棒"; Icon = "https://api.iconify.design/fluent-emoji/glow-stick.svg"; Price = 18; Sort = 2 },
        @{ Name = "能量饮料"; Icon = "https://api.iconify.design/fluent-emoji/beverage-box.svg"; Price = 38; Sort = 3 },
        @{ Name = "冠军奖杯"; Icon = "https://api.iconify.design/fluent-emoji/trophy.svg"; Price = 88; Sort = 4 },
        @{ Name = "超级火箭"; Icon = "https://api.iconify.design/fluent-emoji/rocket.svg"; Price = 188; Sort = 5 }
    )

    foreach ($present in $presents) {
        Invoke-Mysql -Sql @"
INSERT INTO present (name, icon, price, create_time, update_time, sort, disabled)
SELECT $(Escape-Sql $present.Name), $(Escape-Sql $present.Icon), $($present.Price), NOW(), NOW(), $($present.Sort), 0
WHERE NOT EXISTS (SELECT 1 FROM present WHERE name = $(Escape-Sql $present.Name));
"@ | Out-Null
    }
}

function Ensure-DemoUsers {
    param([int]$Count)
    $password = $script:DemoPasswordHash

    for ($i = 1; $i -le $Count; $i++) {
        $username = "{0}{1:D2}" -f $userPrefix, $i
        $nickname = $script:Anchors[($i - 1) % $script:Anchors.Count]
        $avatar = $script:Avatars[($i - 1) % $script:Avatars.Count]
        $signature = $script:Signatures[($i - 1) % $script:Signatures.Count]
        $mobile = "139{0:D8}" -f (80000000 + $i)
        Invoke-Mysql -Sql @"
INSERT INTO user (username, password, avatar, signature, sex, nick_name, create_time, update_time, email, is_validated, disabled, role_id)
SELECT $(Escape-Sql $username), $(Escape-Sql $password), $(Escape-Sql $avatar), $(Escape-Sql $signature), '保密', $(Escape-Sql $nickname), NOW(), NOW(), CONCAT($(Escape-Sql $username), '@demo.local'), 1, 0, 100
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = $(Escape-Sql $username));
"@ | Out-Null
        Invoke-Mysql -Sql @"
UPDATE user
SET nick_name = $(Escape-Sql $nickname),
    avatar = $(Escape-Sql $avatar),
    signature = $(Escape-Sql $signature),
    mobile = COALESCE(mobile, $(Escape-Sql $mobile)),
    is_validated = 1,
    disabled = 0,
    update_time = NOW()
WHERE username = $(Escape-Sql $username);
"@ | Out-Null
    }

    for ($i = 1; $i -le 18; $i++) {
        $username = "{0}{1:D2}" -f $viewerPrefix, $i
        $avatar = $script:Avatars[$i % $script:Avatars.Count]
        $nickname = $script:Viewers[($i - 1) % $script:Viewers.Count]
        Invoke-Mysql -Sql @"
INSERT INTO user (username, password, avatar, signature, sex, nick_name, create_time, update_time, email, is_validated, disabled, role_id)
SELECT $(Escape-Sql $username), $(Escape-Sql $password), $(Escape-Sql $avatar), '演示观众账号，用于关注、送礼和观看历史', '保密', $(Escape-Sql $nickname), NOW(), NOW(), CONCAT($(Escape-Sql $username), '@demo.local'), 1, 0, 100
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = $(Escape-Sql $username));
"@ | Out-Null
        Invoke-Mysql -Sql @"
UPDATE user
SET nick_name = $(Escape-Sql $nickname),
    avatar = $(Escape-Sql $avatar),
    disabled = 0,
    update_time = NOW()
WHERE username = $(Escape-Sql $username);
"@ | Out-Null
    }
}

function Ensure-DemoAdmin {
    $password = $script:DemoPasswordHash
    $avatar = "https://api.dicebear.com/7.x/thumbs/svg?seed=demo-admin"

    Invoke-Mysql -Sql @"
INSERT INTO user (username, password, avatar, signature, sex, nick_name, create_time, update_time, email, is_validated, disabled, role_id)
SELECT $(Escape-Sql $adminUsername), $(Escape-Sql $password), $(Escape-Sql $avatar), '演示管理员账号，用于处理举报和客服工单', '保密', '演示管理员', NOW(), NOW(), 'demo_admin@demo.local', 1, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = $(Escape-Sql $adminUsername));
"@ | Out-Null

    Invoke-Mysql -Sql @"
UPDATE user
SET nick_name = '演示管理员',
    avatar = $(Escape-Sql $avatar),
    signature = '演示管理员账号，用于处理举报和客服工单',
    is_validated = 1,
    disabled = 0,
    role_id = 1,
    update_time = NOW()
WHERE username = $(Escape-Sql $adminUsername);
"@ | Out-Null

    if (Test-Table "user_role") {
        Invoke-OptionalSql @"
INSERT INTO user_role (user_id, role_id, create_time, update_time)
SELECT u.id, 1, NOW(), NOW()
FROM user u
WHERE u.username = $(Escape-Sql $adminUsername)
  AND NOT EXISTS (
      SELECT 1
      FROM user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 1
  );
"@
    }
}

function Ensure-DemoRooms {
    param([int]$Count)
    $categoryIds = @(Invoke-Mysql -Sql "SELECT id FROM category WHERE disabled = 0 ORDER BY sort, id" | ForEach-Object { [int]$_ })
    if ($categoryIds.Count -eq 0) {
        throw "No categories found"
    }

    for ($i = 1; $i -le $Count; $i++) {
        $username = "{0}{1:D2}" -f $userPrefix, $i
        $title = $script:Titles[($i - 1) % $script:Titles.Count]
        $intro = $script:Intros[($i - 1) % $script:Intros.Count]
        $cover = $script:Covers[($i - 1) % $script:Covers.Count]
        $categoryId = $categoryIds[($i - 1) % $categoryIds.Count]

        Invoke-Mysql -Sql @"
INSERT INTO room (name, title, cover, introduce, notice, user_id, rtmp_url, disabled, status, category_id, create_time, update_time, secret)
SELECT '$demoTag', $(Escape-Sql $title), $(Escape-Sql $cover), $(Escape-Sql $intro), '演示直播间：停止脚本后会自动下播。', u.id, 'http://play.imhtb.cn/live/', 0, 0, $categoryId, NOW(), NOW(), CONCAT('demo-', u.id)
FROM user u
WHERE u.username = $(Escape-Sql $username)
  AND NOT EXISTS (SELECT 1 FROM room r WHERE r.user_id = u.id);
"@ | Out-Null

        Invoke-Mysql -Sql @"
UPDATE room r
JOIN user u ON u.id = r.user_id
SET r.name = '$demoTag',
    r.title = $(Escape-Sql $title),
    r.cover = $(Escape-Sql $cover),
    r.introduce = $(Escape-Sql $intro),
    r.notice = '演示直播间：停止脚本后会自动下播。',
    r.category_id = $categoryId,
    r.disabled = 0,
    r.status = 1,
    r.update_time = NOW()
WHERE u.username = $(Escape-Sql $username);
"@ | Out-Null
    }
}

function Ensure-LiveInfo {
    $now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Invoke-Mysql -Sql @"
INSERT INTO live_info (start_time, end_time, room_id, user_id, create_time, update_time, status, click_count, dan_mu_count, present_count)
SELECT DATE_SUB('$now', INTERVAL MOD(r.id * 3, 90) MINUTE),
       NULL,
       r.id,
       r.user_id,
       '$now',
       '$now',
       0,
       CAST(420 + r.id * 13 AS CHAR),
       CAST(80 + r.id * 3 AS CHAR),
       CAST(18 + r.id % 12 AS CHAR)
FROM room r
JOIN user u ON u.id = r.user_id
WHERE u.username LIKE '$userPrefix%'
  AND r.status = 1
  AND NOT EXISTS (
      SELECT 1
      FROM live_info li
      WHERE li.room_id = r.id
        AND li.status = 0
  );
"@ | Out-Null
}

function Seed-Rewards {
    $viewerIds = @(Invoke-Mysql -Sql "SELECT id FROM user WHERE username LIKE '$viewerPrefix%' ORDER BY id" | ForEach-Object { [int]$_ })
    if ($viewerIds.Count -eq 0) {
        return
    }
    $presentRows = @(Invoke-Mysql -Sql "SELECT id, price FROM present WHERE disabled = 0 ORDER BY sort, id")
    if ($presentRows.Count -eq 0) {
        return
    }

    $roomRows = @(Invoke-Mysql -Sql "SELECT r.id, r.user_id FROM room r JOIN user u ON u.id = r.user_id WHERE u.username LIKE '$userPrefix%' AND r.status = 1 ORDER BY r.id")
    $idx = 0
    foreach ($row in $roomRows) {
        $parts = $row -split "`t"
        if ($parts.Count -lt 2) {
            continue
        }
        $roomId = [int]$parts[0]
        $toId = [int]$parts[1]
        for ($j = 0; $j -lt 3; $j++) {
            $fromId = $viewerIds[($idx + $j) % $viewerIds.Count]
            $presentParts = ($presentRows[($idx + $j) % $presentRows.Count]) -split "`t"
            $presentId = [int]$presentParts[0]
            $unit = [decimal]$presentParts[1]
            $num = 1 + (($idx + $j) % 5)
            $total = $unit * $num

            Invoke-Mysql -Sql @"
INSERT INTO present_reward (from_id, to_id, room_id, video_id, present_id, number, unit_price, total_price, create_time, update_time, type)
SELECT $fromId, $toId, $roomId, NULL, $presentId, $num, $unit, $total, DATE_SUB(NOW(), INTERVAL $j MINUTE), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM present_reward
    WHERE room_id = $roomId
      AND to_id = $toId
      AND create_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)
      AND from_id = $fromId
);
"@ | Out-Null
        }
        $idx++
    }
}

function Seed-Watches {
    if (!(Test-Table "watch")) {
        return
    }

    $viewerIds = @(Invoke-Mysql -Sql "SELECT id FROM user WHERE username LIKE '$viewerPrefix%' ORDER BY id" | ForEach-Object { [int]$_ })
    $roomIds = @(Invoke-Mysql -Sql "SELECT r.id FROM room r JOIN user u ON u.id = r.user_id WHERE u.username LIKE '$userPrefix%' ORDER BY r.id" | ForEach-Object { [int]$_ })
    if ($viewerIds.Count -eq 0 -or $roomIds.Count -eq 0) {
        return
    }

    for ($i = 0; $i -lt $viewerIds.Count; $i++) {
        for ($j = 0; $j -lt [Math]::Min(5, $roomIds.Count); $j++) {
            $roomId = $roomIds[($i + $j) % $roomIds.Count]
            $watchType = if ($j -lt 2) { 1 } else { 0 }
            Invoke-OptionalSql @"
INSERT INTO watch (user_id, room_id, watch_type, create_time, update_time)
SELECT $($viewerIds[$i]), $roomId, $watchType, DATE_SUB(NOW(), INTERVAL $j DAY), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM watch
    WHERE user_id = $($viewerIds[$i])
      AND room_id = $roomId
      AND watch_type = $watchType
);
"@
        }
    }
}

function Seed-Statistics {
    if (!(Test-Table "statistic_view")) {
        return
    }

    $roomRows = @(Invoke-Mysql -Sql "SELECT r.id, r.user_id FROM room r JOIN user u ON u.id = r.user_id WHERE u.username LIKE '$userPrefix%' ORDER BY r.id")
    foreach ($row in $roomRows) {
        $parts = $row -split "`t"
        if ($parts.Count -lt 2) {
            continue
        }
        $roomId = [int]$parts[0]
        $userId = [int]$parts[1]
        for ($day = 0; $day -lt 7; $day++) {
            $member = 80 + (($roomId + $day) % 60)
            $visitor = 120 + (($roomId * 2 + $day) % 90)
            $total = $member + $visitor
            Invoke-OptionalSql @"
INSERT INTO statistic_view (room_id, user_id, member_number, visitor_number, total_number, date, mark, create_time, update_time)
SELECT $roomId, $userId, $member, $visitor, $total, DATE_SUB(CURDATE(), INTERVAL $day DAY), '$demoTag', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM statistic_view
    WHERE room_id = $roomId
      AND date = DATE_SUB(CURDATE(), INTERVAL $day DAY)
      AND mark = '$demoTag'
);
"@
        }
    }
}

function Seed-Intimacy {
    if (!(Test-Table "room_intimacy_rank")) {
        return
    }

    $viewerIds = @(Invoke-Mysql -Sql "SELECT id FROM user WHERE username LIKE '$viewerPrefix%' ORDER BY id" | ForEach-Object { [int]$_ })
    $roomIds = @(Invoke-Mysql -Sql "SELECT r.id FROM room r JOIN user u ON u.id = r.user_id WHERE u.username LIKE '$userPrefix%' ORDER BY r.id" | ForEach-Object { [int]$_ })
    if ($viewerIds.Count -eq 0 -or $roomIds.Count -eq 0) {
        return
    }

    for ($i = 0; $i -lt $roomIds.Count; $i++) {
        for ($j = 0; $j -lt [Math]::Min(5, $viewerIds.Count); $j++) {
            $viewerId = $viewerIds[($i + $j) % $viewerIds.Count]
            $value = 120 + ($i * 17) + ($j * 45)
            Invoke-OptionalSql @"
INSERT INTO room_intimacy_rank (room_id, user_id, intimacy_value, create_time, update_time)
VALUES ($($roomIds[$i]), $viewerId, $value, NOW(), NOW())
ON DUPLICATE KEY UPDATE intimacy_value = VALUES(intimacy_value), update_time = NOW();
"@
        }
    }
}

function Seed-RoomTags {
    if (!(Test-Table "room_tag")) {
        return
    }

    $tagSets = @(
        @("高能互动", "排位复盘", "答辩演示"),
        @("期末冲刺", "在线答疑", "沉浸学习"),
        @("治愈音乐", "点歌台", "夜间陪伴"),
        @("校园日常", "轻松聊天", "新人友好"),
        @("代码实战", "Vue", "项目优化"),
        @("摄影构图", "后期修图", "公开课"),
        @("自习室", "番茄钟", "学习搭子"),
        @("新游试玩", "版本体验", "弹幕互动")
    )
    $roomIds = @(Invoke-Mysql -Sql "SELECT r.id FROM room r JOIN user u ON u.id = r.user_id WHERE u.username LIKE '$userPrefix%' ORDER BY r.id" | ForEach-Object { [int]$_ })
    for ($i = 0; $i -lt $roomIds.Count; $i++) {
        $tags = $tagSets[$i % $tagSets.Count]
        foreach ($tag in $tags) {
            Invoke-OptionalSql @"
INSERT INTO room_tag (room_id, tag_name, create_time)
SELECT $($roomIds[$i]), $(Escape-Sql $tag), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM room_tag
    WHERE room_id = $($roomIds[$i])
      AND tag_name = $(Escape-Sql $tag)
);
"@
        }
    }
}

function Seed-RoomModerators {
    if (!(Test-Table "room_moderator")) {
        return
    }

    $moderatorId = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_viewer_02' LIMIT 1), 0)" -Scalar)
    if ($moderatorId -le 0) {
        return
    }
    $roomRows = @(Invoke-Mysql -Sql "SELECT r.id, r.user_id FROM room r JOIN user u ON u.id = r.user_id WHERE u.username LIKE '$userPrefix%' ORDER BY r.id LIMIT 3")
    foreach ($row in $roomRows) {
        $parts = $row -split "`t"
        if ($parts.Count -lt 2) {
            continue
        }
        $roomId = [int]$parts[0]
        $anchorId = [int]$parts[1]
        Invoke-OptionalSql @"
INSERT INTO room_moderator (room_id, user_id, appointed_by, create_time)
VALUES ($roomId, $moderatorId, $anchorId, NOW())
ON DUPLICATE KEY UPDATE appointed_by = VALUES(appointed_by);
"@
    }
}

function Seed-UserLevels {
    if (!(Test-Table "user_level")) {
        return
    }

    Invoke-OptionalSql @"
INSERT INTO user_level (user_id, exp, level, update_time)
SELECT id,
       CASE
           WHEN username LIKE '$viewerPrefix%' THEN 680 + MOD(id, 9) * 120
           WHEN username LIKE '$userPrefix%' THEN 1320 + MOD(id, 12) * 180
           ELSE 500
       END,
       CASE
           WHEN username LIKE '$viewerPrefix%' THEN 3 + MOD(id, 5)
           WHEN username LIKE '$userPrefix%' THEN 6 + MOD(id, 8)
           ELSE 4
       END,
       NOW()
FROM user
WHERE username LIKE '$viewerPrefix%'
   OR username LIKE '$userPrefix%'
   OR username = $(Escape-Sql $adminUsername)
ON DUPLICATE KEY UPDATE exp = VALUES(exp), level = VALUES(level), update_time = NOW();
"@
}

function Seed-GuardianSubscriptions {
    if (!(Test-Table "guardian_subscription")) {
        return
    }

    $targetRows = @(Invoke-Mysql -Sql "SELECT id FROM user WHERE username LIKE '$userPrefix%' ORDER BY username LIMIT 4" | ForEach-Object { [int]$_ })
    $viewerRows = @(Invoke-Mysql -Sql "SELECT id FROM user WHERE username LIKE '$viewerPrefix%' ORDER BY username LIMIT 8" | ForEach-Object { [int]$_ })
    if ($targetRows.Count -eq 0 -or $viewerRows.Count -eq 0) {
        return
    }

    for ($i = 0; $i -lt $targetRows.Count; $i++) {
        for ($j = 0; $j -lt [Math]::Min(4, $viewerRows.Count); $j++) {
            $viewerId = $viewerRows[($i + $j) % $viewerRows.Count]
            $targetId = $targetRows[$i]
            if ($viewerId -eq $targetId) {
                continue
            }
            $level = 1 + (($i + $j) % 3)
            $amount = @(0, 300, 600, 1200)[$level]
            $autoRenew = if (($i + $j) % 2 -eq 0) { 1 } else { 0 }
            Invoke-OptionalSql @"
INSERT INTO guardian_subscription (user_id, target_user_id, level, amount, expire_time, auto_renew, status, create_time, update_time)
VALUES ($viewerId, $targetId, $level, $amount, DATE_ADD(NOW(), INTERVAL 25 DAY), $autoRenew, 1, DATE_SUB(NOW(), INTERVAL $j DAY), NOW())
ON DUPLICATE KEY UPDATE
    level = VALUES(level),
    amount = VALUES(amount),
    expire_time = VALUES(expire_time),
    auto_renew = VALUES(auto_renew),
    status = 1,
    update_time = NOW();
"@
        }
    }
}

function Seed-DemoMessages {
    if (!(Test-Table "message")) {
        return
    }

    $viewerIds = @(Invoke-Mysql -Sql "SELECT id FROM user WHERE username LIKE '$viewerPrefix%' ORDER BY username" | ForEach-Object { [int]$_ })
    $roomIds = @(Invoke-Mysql -Sql "SELECT r.id FROM room r JOIN user u ON u.id = r.user_id WHERE u.username LIKE '$userPrefix%' ORDER BY r.id" | ForEach-Object { [int]$_ })
    if ($viewerIds.Count -eq 0 -or $roomIds.Count -eq 0) {
        return
    }

    $messages = @(
        "这场讲得很清楚，已经关注了",
        "主播能再演示一下礼物和亲密榜吗",
        "弹幕测试：管理员可以在后台看到记录",
        "小脉助手能根据当前直播内容给建议吗",
        "这个直播间标签和推荐数据很适合答辩展示",
        "收到开播通知后点进来的，页面联动正常"
    )

    for ($i = 0; $i -lt [Math]::Min($roomIds.Count, 10); $i++) {
        for ($j = 0; $j -lt 4; $j++) {
            $viewerId = $viewerIds[($i + $j) % $viewerIds.Count]
            $text = $messages[($i + $j) % $messages.Count]
            Invoke-OptionalSql @"
INSERT INTO message (room_id, from_uid, content, reply_msg_id, status, type, extra, create_time, update_time)
SELECT $($roomIds[$i]), $viewerId, $(Escape-Sql $text), NULL, 0, 1, $(Escape-Sql '{"demoSeed":true}'), DATE_SUB(NOW(), INTERVAL ($i + $j) MINUTE), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM message
    WHERE room_id = $($roomIds[$i])
      AND from_uid = $viewerId
      AND content = $(Escape-Sql $text)
      AND extra = $(Escape-Sql '{"demoSeed":true}')
);
"@
        }
    }
}

function Seed-Reports {
    if (!(Test-Table "report")) {
        return
    }

    $viewer1 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_viewer_01' LIMIT 1), 0)" -Scalar)
    $viewer2 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_viewer_02' LIMIT 1), 0)" -Scalar)
    $roomRows = @(Invoke-Mysql -Sql "SELECT r.id, r.user_id, r.title FROM room r JOIN user u ON u.id = r.user_id WHERE u.username LIKE '$userPrefix%' ORDER BY r.id LIMIT 4")
    if ($viewer1 -le 0 -or $roomRows.Count -eq 0) {
        return
    }

    Invoke-OptionalSql @"
UPDATE report
SET status = 0,
    handle_result = '',
    handler_id = NULL,
    handle_time = NULL
WHERE description LIKE '%"demoSeed":true%';
"@

    $first = $roomRows[0] -split "`t"
    $roomId1 = [int]$first[0]
    $anchorId1 = [int]$first[1]
    $roomTitle1 = $first[2]

    Invoke-OptionalSql @"
INSERT INTO report (reporter_id, target_user_id, room_id, target_type, target_id, reason, description, status, create_time)
SELECT 0, $anchorId1, $roomId1, 'live_guard', $(Escape-Sql "demo-live-guard-$roomId1"), '疑似暴力行为', $(Escape-Sql '{"demoSeed":true,"source":"live_guard","status":"REVIEW","reason":"AI视觉审核命中风险，等待管理员裁决","violationType":"VIOLENCE","violationLabel":"暴力行为","evidenceImageUrl":"/demo-covers/game-arena.jpg","screenshotUrl":"/demo-covers/game-arena.jpg","evidence":{"frame":"demo-frame-001","confidence":0.87}}'), 0, DATE_SUB(NOW(), INTERVAL 8 MINUTE)
WHERE NOT EXISTS (
    SELECT 1 FROM report
    WHERE target_type = 'live_guard'
      AND target_id = $(Escape-Sql "demo-live-guard-$roomId1")
      AND description LIKE '%"demoSeed":true%'
);
"@

    Invoke-OptionalSql @"
INSERT INTO report (reporter_id, target_user_id, room_id, target_type, target_id, reason, description, status, create_time)
SELECT $viewer1, $anchorId1, $roomId1, 'room', $(Escape-Sql "demo-room-$roomId1"), '违规内容', $(Escape-Sql "{`"demoSeed`":true,`"summary`":`"$roomTitle1`",`"description`":`"观众认为直播间标题与实际内容不一致，提交给管理员复核。`"}"), 0, DATE_SUB(NOW(), INTERVAL 6 MINUTE)
WHERE NOT EXISTS (
    SELECT 1 FROM report
    WHERE reporter_id = $viewer1
      AND target_type = 'room'
      AND target_id = $(Escape-Sql "demo-room-$roomId1")
      AND description LIKE '%"demoSeed":true%'
);
"@

    if ($viewer2 -gt 0 -and $roomRows.Count -gt 1) {
        $second = $roomRows[1] -split "`t"
        $roomId2 = [int]$second[0]
        $anchorId2 = [int]$second[1]
        $messageId = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM message WHERE room_id = $roomId2 AND extra = $(Escape-Sql '{"demoSeed":true}') ORDER BY id DESC LIMIT 1), 0)" -Scalar)
        $targetId = if ($messageId -gt 0) { "message-$messageId" } else { "demo-message-$roomId2" }
        Invoke-OptionalSql @"
INSERT INTO report (reporter_id, target_user_id, room_id, target_type, target_id, reason, description, status, create_time)
SELECT $viewer2, $anchorId2, $roomId2, 'message', $(Escape-Sql $targetId), '刷屏骚扰', $(Escape-Sql '{"demoSeed":true,"type":"message","summary":"弹幕内容被举报，等待管理员处理","description":"用于展示弹幕举报进入审核队列。"}'), 0, DATE_SUB(NOW(), INTERVAL 4 MINUTE)
WHERE NOT EXISTS (
    SELECT 1 FROM report
    WHERE reporter_id = $viewer2
      AND target_type = 'message'
      AND target_id = $(Escape-Sql $targetId)
      AND description LIKE '%"demoSeed":true%'
);
"@
    }
}

function Seed-CustomerServiceTickets {
    if (!(Test-Table "customer_service_ticket")) {
        return
    }

    $viewer1 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_viewer_01' LIMIT 1), 0)" -Scalar)
    $viewer2 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_viewer_02' LIMIT 1), 0)" -Scalar)
    $viewer3 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_viewer_03' LIMIT 1), 0)" -Scalar)
    $anchor1 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_anchor_01' LIMIT 1), 0)" -Scalar)
    $adminId = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = $(Escape-Sql $adminUsername) LIMIT 1), 0)" -Scalar)

    if ($viewer1 -gt 0) {
        Invoke-OptionalSql @"
INSERT INTO customer_service_ticket (user_id, category, title, content, status, create_time, update_time)
SELECT $viewer1, 'live', '演示-开播提示房间信息未初始化', '点击开播时看到房间信息未初始化，希望客服协助确认直播间资料是否完整。', 0, DATE_SUB(NOW(), INTERVAL 22 MINUTE), DATE_SUB(NOW(), INTERVAL 22 MINUTE)
WHERE NOT EXISTS (
    SELECT 1 FROM customer_service_ticket
    WHERE user_id = $viewer1
      AND title = '演示-开播提示房间信息未初始化'
);
"@
        Invoke-OptionalSql @"
UPDATE customer_service_ticket
SET status = 0,
    handler_id = NULL,
    reply = '',
    reply_time = NULL,
    update_time = NOW()
WHERE user_id = $viewer1
  AND title = '演示-开播提示房间信息未初始化';
"@
    }

    if ($viewer2 -gt 0) {
        Invoke-OptionalSql @"
INSERT INTO customer_service_ticket (user_id, category, title, content, status, handler_id, reply, create_time, update_time, reply_time)
SELECT $viewer2, 'wallet', '演示-充值后余额未刷新', '完成沙箱支付后页面余额没有立刻变化，刷新后恢复正常。', 1, NULLIF($adminId, 0), '已核对支付流水，余额同步正常。如页面未刷新，请重新进入钱包页。', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 80 MINUTE), DATE_SUB(NOW(), INTERVAL 80 MINUTE)
WHERE NOT EXISTS (
    SELECT 1 FROM customer_service_ticket
    WHERE user_id = $viewer2
      AND title = '演示-充值后余额未刷新'
);
"@
    }

    if ($anchor1 -gt 0) {
        Invoke-OptionalSql @"
INSERT INTO customer_service_ticket (user_id, category, title, content, status, create_time, update_time)
SELECT $anchor1, 'appeal', '演示-直播内容误触风控申诉', '正常讲解项目时出现风险提示，希望管理员结合上下文进行复核。', 0, DATE_SUB(NOW(), INTERVAL 48 MINUTE), DATE_SUB(NOW(), INTERVAL 48 MINUTE)
WHERE NOT EXISTS (
    SELECT 1 FROM customer_service_ticket
    WHERE user_id = $anchor1
      AND title = '演示-直播内容误触风控申诉'
);
"@
    }

    if ($viewer3 -gt 0) {
        Invoke-OptionalSql @"
INSERT INTO customer_service_ticket (user_id, category, title, content, status, handler_id, reply, create_time, update_time, reply_time)
SELECT $viewer3, 'feedback', '演示-建议增加直播小助手', '希望直播间支持根据弹幕生成欢迎语和互动建议。', 2, NULLIF($adminId, 0), '建议已记录，当前版本已提供小脉 AI 助手和互动助手入口。', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 23 HOUR), DATE_SUB(NOW(), INTERVAL 23 HOUR)
WHERE NOT EXISTS (
    SELECT 1 FROM customer_service_ticket
    WHERE user_id = $viewer3
      AND title = '演示-建议增加直播小助手'
);
"@
    }
}

function Seed-Notifications {
    if (!(Test-Table "notification")) {
        return
    }

    $viewer1 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_viewer_01' LIMIT 1), 0)" -Scalar)
    $anchor1 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_anchor_01' LIMIT 1), 0)" -Scalar)
    $roomId = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT r.id FROM room r JOIN user u ON u.id = r.user_id WHERE u.username = 'demo_anchor_01' LIMIT 1), 0)" -Scalar)
    if ($viewer1 -le 0) {
        return
    }

    $notifications = @(
        @{ UserId = $viewer1; Type = "system"; Title = "演示数据已准备完成"; Content = "首页、直播间、客服、举报和消息中心均已补充演示数据。"; RelatedId = 0; IsRead = 0 },
        @{ UserId = $viewer1; Type = "live_started"; Title = "你关注的主播正在直播"; Content = "星河电竞正在进行演示直播，点击可进入直播间。"; RelatedId = $roomId; IsRead = 0 },
        @{ UserId = $viewer1; Type = "followed"; Title = "关注提醒"; Content = "你关注的主播更新了直播间公告。"; RelatedId = $roomId; IsRead = 1 },
        @{ UserId = $viewer1; Type = "system"; Title = "客服工单已受理"; Content = "你的开播问题已进入客服处理队列。"; RelatedId = 0; IsRead = 0 }
    )

    if ($anchor1 -gt 0) {
        $notifications += @(
            @{ UserId = $anchor1; Type = "system"; Title = "收益结算已生成"; Content = "本月礼物收益结算记录已生成，可在个人中心查看。"; RelatedId = 0; IsRead = 0 },
            @{ UserId = $anchor1; Type = "followed"; Title = "新增粉丝守护"; Content = "晚风同学开通了你的黄金守护。"; RelatedId = $roomId; IsRead = 0 }
        )
    }

    foreach ($item in $notifications) {
        Invoke-OptionalSql @"
INSERT INTO notification (user_id, type, title, content, related_id, is_read, create_time, update_time)
SELECT $($item.UserId), $(Escape-Sql $item.Type), $(Escape-Sql $item.Title), $(Escape-Sql $item.Content), $($item.RelatedId), $($item.IsRead), DATE_SUB(NOW(), INTERVAL MOD($($item.UserId) + $($item.RelatedId), 50) MINUTE), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM notification
    WHERE user_id = $($item.UserId)
      AND type = $(Escape-Sql $item.Type)
      AND title = $(Escape-Sql $item.Title)
);
"@
    }

    if (Test-Table "notification_pref") {
        Invoke-OptionalSql @"
INSERT INTO notification_pref (user_id, live_start_enabled, follow_enabled, dnd_start, dnd_end)
SELECT id, 1, 1, '23:30', '07:30'
FROM user
WHERE username IN ('demo_viewer_01', 'demo_anchor_01')
ON DUPLICATE KEY UPDATE live_start_enabled = 1, follow_enabled = 1;
"@
    }
}

function Seed-PrivateMessages {
    if (!(Test-Table "private_message")) {
        return
    }

    $viewer1 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_viewer_01' LIMIT 1), 0)" -Scalar)
    $anchor1 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_anchor_01' LIMIT 1), 0)" -Scalar)
    $adminId = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = $(Escape-Sql $adminUsername) LIMIT 1), 0)" -Scalar)
    if ($viewer1 -le 0 -or $anchor1 -le 0) {
        return
    }

    $messages = @(
        @{ From = $anchor1; To = $viewer1; Text = "欢迎来到演示直播间，有问题可以发弹幕或者找客服。"; Read = 0 },
        @{ From = $viewer1; To = $anchor1; Text = "我会在答辩时演示关注、送礼和举报流程。"; Read = 1 }
    )
    if ($adminId -gt 0) {
        $messages += @{ From = $adminId; To = $viewer1; Text = "你的演示客服工单已经进入处理队列。"; Read = 0 }
    }

    foreach ($item in $messages) {
        Invoke-OptionalSql @"
INSERT INTO private_message (from_user_id, to_user_id, content, is_read, create_time)
SELECT $($item.From), $($item.To), $(Escape-Sql $item.Text), $($item.Read), DATE_SUB(NOW(), INTERVAL MOD($($item.From) + $($item.To), 30) MINUTE)
WHERE NOT EXISTS (
    SELECT 1 FROM private_message
    WHERE from_user_id = $($item.From)
      AND to_user_id = $($item.To)
      AND content = $(Escape-Sql $item.Text)
);
"@
    }
}

function Seed-Wallets {
    if (!(Test-Table "tb_wallet")) {
        return
    }

    Invoke-OptionalSql @"
INSERT INTO tb_wallet (user_id, balance, version, status, create_time, update_time)
SELECT id,
       CASE
           WHEN username LIKE '$viewerPrefix%' THEN 1888.00
           WHEN username LIKE '$userPrefix%' THEN 888.00
           ELSE 0.00
       END,
       0,
       0,
       NOW(),
       NOW()
FROM user
WHERE username LIKE '$viewerPrefix%'
   OR username LIKE '$userPrefix%'
   OR username = $(Escape-Sql $adminUsername)
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    status = 0,
    update_time = NOW();
"@

    if (Test-Table "tb_wallet_log") {
        $walletRows = @(Invoke-Mysql -Sql "SELECT w.id, u.username FROM tb_wallet w JOIN user u ON u.id = w.user_id WHERE u.username IN ('demo_viewer_01', 'demo_anchor_01', 'demo_viewer_02') ORDER BY u.username")
        foreach ($row in $walletRows) {
            $parts = $row -split "`t"
            if ($parts.Count -lt 2) {
                continue
            }
            $walletId = [int]$parts[0]
            $username = $parts[1]
            $entries = @()
            if ($username -eq "demo_viewer_01") {
                $entries = @(
                    @{ Fee = 200.00; Balance = 1888.00; Type = 1; Source = "demo-wallet-recharge-01"; SourceType = "demo_recharge" },
                    @{ Fee = -88.00; Balance = 1688.00; Type = 2; Source = "demo-wallet-gift-spend-01"; SourceType = "gift_spend" }
                )
            } elseif ($username -eq "demo_anchor_01") {
                $entries = @(
                    @{ Fee = 88.00; Balance = 888.00; Type = 3; Source = "demo-wallet-gift-income-01"; SourceType = "gift_income" }
                )
            } else {
                $entries = @(
                    @{ Fee = 100.00; Balance = 1888.00; Type = 1; Source = "demo-wallet-recharge-02"; SourceType = "demo_recharge" }
                )
            }

            foreach ($entry in $entries) {
                Invoke-OptionalSql @"
INSERT INTO tb_wallet_log (wallet_id, balance, fee, action_type, source_uuid, source_type, create_time, update_time)
SELECT $walletId, $($entry.Balance), $($entry.Fee), $($entry.Type), $(Escape-Sql $entry.Source), $(Escape-Sql $entry.SourceType), DATE_SUB(NOW(), INTERVAL MOD($walletId, 9) HOUR), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_wallet_log
    WHERE source_uuid = $(Escape-Sql $entry.Source)
);
"@
            }
        }
    }
}

function Seed-Bills {
    if (!(Test-Table "bill")) {
        return
    }

    $viewer1 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_viewer_01' LIMIT 1), 0)" -Scalar)
    $anchor1 = [int](Invoke-Mysql -Sql "SELECT COALESCE((SELECT id FROM user WHERE username = 'demo_anchor_01' LIMIT 1), 0)" -Scalar)
    if ($viewer1 -le 0) {
        return
    }

    $rows = @(
        @{ User = $viewer1; Change = 200.00; Type = 0; Balance = 1888.00; Mark = "演示充值"; Order = "DEMO-BILL-RECHARGE-01" },
        @{ User = $viewer1; Change = -88.00; Type = 1; Balance = 1800.00; Mark = "演示送礼"; Order = "DEMO-BILL-GIFT-SPEND-01" }
    )
    if ($anchor1 -gt 0) {
        $rows += @{ User = $anchor1; Change = 88.00; Type = 0; Balance = 888.00; Mark = "演示礼物收入"; Order = "DEMO-BILL-GIFT-INCOME-01" }
    }

    foreach ($row in $rows) {
        Invoke-OptionalSql @"
INSERT INTO bill (user_id, bill_change, type, balance, ip, mark, create_time, update_time, order_no)
SELECT $($row.User), $($row.Change), $($row.Type), $($row.Balance), '127.0.0.1', $(Escape-Sql $row.Mark), DATE_SUB(NOW(), INTERVAL MOD($($row.User), 6) HOUR), NOW(), $(Escape-Sql $row.Order)
WHERE NOT EXISTS (
    SELECT 1 FROM bill
    WHERE order_no = $(Escape-Sql $row.Order)
);
"@
    }
}

function Seed-Settlements {
    if (!(Test-Table "settlement")) {
        return
    }

    $anchorRows = @(Invoke-Mysql -Sql "SELECT id FROM user WHERE username LIKE '$userPrefix%' ORDER BY username LIMIT 5" | ForEach-Object { [int]$_ })
    foreach ($anchorId in $anchorRows) {
        for ($i = 0; $i -lt 3; $i++) {
            $income = 680 + (($anchorId + $i) % 9) * 120
            $fee = [Math]::Round($income * 0.2, 2)
            $net = [Math]::Round($income - $fee, 2)
            $withdrawn = if ($i -eq 2) { [Math]::Round($net * 0.4, 2) } else { 0 }
            $withdrawable = [Math]::Round($net - $withdrawn, 2)
            $status = if ($i -eq 0) { 0 } elseif ($i -eq 1) { 1 } else { 2 }
            Invoke-OptionalSql @"
INSERT INTO settlement (user_id, period, gift_income, platform_fee, net_income, withdrawable, withdrawn, status, settle_time, create_time)
VALUES ($anchorId, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL $i MONTH), '%Y-%m'), $income, $fee, $net, $withdrawable, $withdrawn, $status, IF($status = 0, NULL, DATE_SUB(NOW(), INTERVAL $i MONTH)), NOW())
ON DUPLICATE KEY UPDATE
    gift_income = VALUES(gift_income),
    platform_fee = VALUES(platform_fee),
    net_income = VALUES(net_income),
    withdrawable = VALUES(withdrawable),
    withdrawn = VALUES(withdrawn),
    status = VALUES(status),
    settle_time = VALUES(settle_time);
"@
        }
    }
}

function Refresh-DemoPulse {
    $now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Invoke-Mysql -Sql @"
UPDATE live_info li
JOIN room r ON r.id = li.room_id
JOIN user u ON u.id = r.user_id
SET li.update_time = '$now',
    li.click_count = CAST(520 + r.id * 17 + SECOND(NOW()) AS CHAR),
    li.dan_mu_count = CAST(120 + r.id * 4 + MOD(SECOND(NOW()), 40) AS CHAR),
    li.present_count = CAST(28 + MOD(r.id + SECOND(NOW()), 45) AS CHAR)
WHERE li.status = 0
  AND u.username LIKE '$userPrefix%';
"@ | Out-Null

    Invoke-Mysql -Sql @"
UPDATE room r
JOIN user u ON u.id = r.user_id
SET r.status = 1,
    r.disabled = 0,
    r.update_time = '$now'
WHERE u.username LIKE '$userPrefix%';
"@ | Out-Null
}

$script:Anchors = @(
    "星河电竞", "小满自习室", "阿青游戏", "森屿厨房", "代码现场", "野火音乐",
    "橙子手作", "晨间燃脂", "像素实验室", "云端课堂", "南城电台", "北线旅行",
    "甜点一口", "夜读计划", "栗子摄影", "未来工作室", "蓝鲸数码", "山茶茶馆",
    "剪辑工坊", "竹影民谣"
)
$script:Viewers = @(
    "晚风同学", "今天不熬夜", "小白也能学", "弹幕课代表", "橘子汽水",
    "键盘观察员", "番茄钟达人", "路过看看", "一杯拿铁", "清醒一点",
    "学习搭子", "热榜巡游", "云观众01", "认真听讲", "周末快乐",
    "星光收藏家", "准时打卡", "理性发言"
)
$script:Signatures = @(
    "每天稳定直播，欢迎点进来一起交流。",
    "专注实战讲解，问题可以直接发弹幕。",
    "轻松聊天局，适合答辩演示和功能巡检。",
    "直播内容健康友好，管理员可随时抽查。",
    "正在测试推荐、搜索、礼物和榜单联动。"
)
$script:Titles = @(
    "王者上分局：边打边复盘", "Python 期末冲刺答疑", "治愈系吉他点歌台",
    "早咖啡聊天室：校园日常", "Vue 项目现场重构", "手机摄影构图公开课",
    "90 分钟沉浸自习室", "新游版本试玩体验", "AI 工具流效率工作台",
    "下班后 30 分钟拉伸", "校园歌手练习房", "十分钟快手晚餐",
    "短视频剪辑拆解课", "旅行照片修图现场", "桌游策略复盘局",
    "英语长难句精讲", "数码桌搭改造", "手账拼贴慢直播",
    "直播平台功能演示", "管理员审核流程演示"
)
$script:Intros = @(
    "用于答辩展示的模拟直播间，支持首页、搜索和排行演示。",
    "脚本运行期间保持在线，停止脚本后自动关闭这些演示直播间。",
    "含模拟观看、关注、礼物和亲密榜数据，便于完整走通业务流程。",
    "内容为健康演示文本，不依赖真实主播推流。"
)
$script:Covers = @(
    "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format%26fit=crop%26w=1200%26q=80",
    "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format%26fit=crop%26w=1200%26q=80",
    "https://images.unsplash.com/photo-1511379938547-c1f69419868d?auto=format%26fit=crop%26w=1200%26q=80",
    "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format%26fit=crop%26w=1200%26q=80",
    "https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format%26fit=crop%26w=1200%26q=80",
    "https://images.unsplash.com/photo-1452587925148-ce544e77e70d?auto=format%26fit=crop%26w=1200%26q=80",
    "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format%26fit=crop%26w=1200%26q=80",
    "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format%26fit=crop%26w=1200%26q=80"
)
$script:Avatars = @(
    "https://api.dicebear.com/7.x/thumbs/svg?seed=demo1",
    "https://api.dicebear.com/7.x/thumbs/svg?seed=demo2",
    "https://api.dicebear.com/7.x/thumbs/svg?seed=demo3",
    "https://api.dicebear.com/7.x/thumbs/svg?seed=demo4",
    "https://api.dicebear.com/7.x/thumbs/svg?seed=demo5",
    "https://api.dicebear.com/7.x/thumbs/svg?seed=demo6"
)

$envValues = Read-DotEnv $envPath
$script:DbUser = $env:DB_USERNAME
if (!$script:DbUser) { $script:DbUser = $envValues["DB_USERNAME"] }
if (!$script:DbUser) { $script:DbUser = "root" }

$script:DbPassword = $env:DB_PASSWORD
if ($null -eq $script:DbPassword -or $script:DbPassword -eq "") { $script:DbPassword = $envValues["DB_PASSWORD"] }

$dbUrl = $env:DB_URL
if (!$dbUrl) { $dbUrl = $envValues["DB_URL"] }
$script:DbName = Get-JdbcDatabase $dbUrl

if (!(Get-Command mysql -ErrorAction SilentlyContinue)) {
    throw "mysql command not found. Add MySQL bin directory to PATH first."
}
if (!(Test-Table "room") -or !(Test-Table "live_info")) {
    throw "Expected PulseLive tables were not found in database '$script:DbName'."
}

Write-Host "[demo] database: $script:DbName, rooms: $Rooms, tick: ${TickSeconds}s" -ForegroundColor Cyan
Stop-DemoLive
if ($StopOnly) {
    return
}

Ensure-DemoSchema
Ensure-Categories
Ensure-DemoPresents
Ensure-DemoUsers -Count $Rooms
Ensure-DemoAdmin
Ensure-DemoRooms -Count $Rooms
Ensure-LiveInfo
Seed-Rewards
Seed-Watches
Seed-Statistics
Seed-Intimacy
Seed-RoomTags
Seed-RoomModerators
Seed-UserLevels
Seed-GuardianSubscriptions
Seed-DemoMessages
Seed-Reports
Seed-CustomerServiceTickets
Seed-Notifications
Seed-PrivateMessages
Seed-Wallets
Seed-Bills
Seed-Settlements
Refresh-DemoPulse

Write-Host "[demo] demo live rooms are online. Open http://localhost:5173/#/home" -ForegroundColor Green

if ($Once) {
    Write-Host "[demo] -Once specified; leaving demo rooms online. Run with -StopOnly to close them." -ForegroundColor Yellow
    return
}

try {
    while ($true) {
        Start-Sleep -Seconds $TickSeconds
        Refresh-DemoPulse
        Write-Host "[demo] refreshed at $(Get-Date -Format HH:mm:ss); press Ctrl+C to close demo rooms."
    }
} finally {
    Stop-DemoLive
}

