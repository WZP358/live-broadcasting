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

    if ($script:DbPassword) {
        $mysqlArgs = @(
            "--default-character-set=utf8mb4",
            "-u$script:DbUser",
            "-p$script:DbPassword",
            "-D", $script:DbName,
            "--batch",
            "--raw",
            "--skip-column-names",
            "-e", $Sql
        )
    }

    $output = & mysql @mysqlArgs
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
    $password = '$2a$10$puULYxVheVu/sJZk7rUbvujNheV9v7afPWETHv47sjS2KAXNptTEe'

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

Ensure-Categories
Ensure-DemoPresents
Ensure-DemoUsers -Count $Rooms
Ensure-DemoRooms -Count $Rooms
Ensure-LiveInfo
Seed-Rewards
Seed-Watches
Seed-Statistics
Seed-Intimacy
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

