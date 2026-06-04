/**
 * simulate-broadcast.js
 *
 * 模拟 N 个用户同时开摄像头直播，使首页展示多个直播中的房间。
 *
 * 工作原理：
 *   1. 为每个 userId 生成 JWT（与后端 JwtUtil.create() 算法一致）
 *   2. 通过 REST API 检查房间是否存在，不存在则注册用户（注册时自动创建房间）
 *   3. 连接 Netty WebSocket，发送 { type: "join", role: "BROADCASTER" } 标记开播
 *   4. 保持连接并定期发送心跳，维持直播状态
 *
 * 用法：
 *   node scripts/simulate-broadcast.js [数量=30]
 *
 * 前置条件：
 *   npm install ws jsonwebtoken
 *
 * 停止：Ctrl+C 关闭所有连接，房间状态由 Netty 断连回调自动设为停播。
 */

const WebSocket = require('ws');
const jwt = require('jsonwebtoken');
const http = require('http');

// ====== 配置 ======
const COUNT = Math.max(1, Math.min(100, parseInt(process.argv[2]) || 30));
const HOST = process.env.LIVE_HOST || 'localhost';
const HTTP_PORT = parseInt(process.env.LIVE_HTTP_PORT || '9000');
const WS_PORT = parseInt(process.env.LIVE_WS_PORT || '10022');

// 后端 JwtUtil.SECRET（jjwt signWith / setSigningKey(String) 会 base64 解码该值得到原始字节）
const JWT_SECRET_BASE64 = '+/UElOVEVILUFOVC1MSVZF';
const JWT_KEY = Buffer.from(JWT_SECRET_BASE64, 'base64');

// 起始 roomId（可为 null，脚本会自动探测）
const ROOM_ID_OFFSET = parseInt(process.env.ROOM_ID_OFFSET || '0');

// ====== JWT 工具（对齐后端 JwtUtil.create(Integer userId)） ======
function createJwt(userId) {
  const token = jwt.sign({}, JWT_KEY, {
    jwtid: String(userId),
    algorithm: 'HS256',
    expiresIn: '24h',
  });
  return 'Bearer ' + token;
}

// ====== HTTP 请求（Node 内置 http，零依赖） ======
function apiRequest(method, path, token, body) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, `http://${HOST}:${HTTP_PORT}`);
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = token;

    const options = {
      method,
      hostname: HOST,
      port: HTTP_PORT,
      path: url.pathname + url.search,
      headers,
      timeout: 10000,
    };

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data) });
        } catch {
          resolve({ status: res.statusCode, body: data });
        }
      });
    });

    req.on('timeout', () => {
      req.destroy();
      reject(new Error(`Request timeout: ${method} ${path}`));
    });
    req.on('error', reject);

    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

// ====== 用户注册 ======
async function registerUser(userId) {
  const username = `testsim${userId}`;
  const nickname = `模拟主播${userId}`;
  const password = 'test123456';

  try {
    const { status, body } = await apiRequest('POST', '/api/v1/user/register', null, {
      username,
      nickname,
      password,
      passwordConfirm: password,
    });
    if (status === 200 && body && body.code === 0) {
      console.log(`  [注册] 用户 ${username} (id=${userId}) 创建成功`);
      return true;
    }
    // code != 0 可能是用户名已存在
    if (body && body.code !== 0) {
      console.log(`  [注册] 用户 ${username} 可能已存在: ${body.msg}`);
      return true; // 已存在也算成功
    }
    return false;
  } catch (e) {
    console.log(`  [注册] 请求失败: ${e.message}`);
    return false;
  }
}

// ====== 获取或创建房间 ======
async function ensureRoom(userId, token) {
  // 先尝试获取已有房间
  try {
    const { status, body } = await apiRequest('GET', '/api/v1/room/setting/info', token);
    if (status === 200 && body && body.code === 0 && body.data && body.data.id) {
      return body.data.id;
    }
    console.log(`  用户 ${userId} 无房间（status=${status}），尝试注册...`);
  } catch (e) {
    console.log(`  获取房间信息失败: ${e.message}，尝试注册...`);
  }

  // 注册用户（会自动创建房间）
  const ok = await registerUser(userId);
  if (!ok) {
    throw new Error(`无法注册用户 ${userId}`);
  }

  // 再次尝试获取房间
  try {
    const { status, body } = await apiRequest('GET', '/api/v1/room/setting/info', token);
    if (status === 200 && body && body.code === 0 && body.data && body.data.id) {
      return body.data.id;
    }
  } catch (e) {
    // ignore
  }

  throw new Error(`注册后仍无法获取用户 ${userId} 的房间`);
}

// ====== 建立一条 WebSocket 并发送开播指令 ======
function startBroadcast(userId, roomId, token) {
  return new Promise((resolve, reject) => {
    const wsUrl = `ws://${HOST}:${WS_PORT}?token=${encodeURIComponent(token)}`;
    const ws = new WebSocket(wsUrl);

    const timeout = setTimeout(() => {
      ws.close();
      reject(new Error(`WebSocket 连接超时`));
    }, 8000);

    ws.on('open', () => {
      clearTimeout(timeout);
      // 发送 join 消息，角色为 BROADCASTER，触发 markLiveStarted
      ws.send(JSON.stringify({ type: 'join', roomId, role: 'BROADCASTER' }));
    });

    ws.on('message', (data) => {
      try {
        const msg = JSON.parse(data.toString());
        if (msg.type === 'joined') {
          clearTimeout(timeout);
          resolve(ws);
        }
      } catch {
        // ignore
      }
    });

    ws.on('error', (err) => {
      clearTimeout(timeout);
      reject(err);
    });
  });
}

// ====== 主流程 ======
async function main() {
  console.log('='.repeat(56));
  console.log(`  模拟开播脚本`);
  console.log(`  目标: ${COUNT} 个直播间同时开播`);
  console.log(`  后端: http://${HOST}:${HTTP_PORT}  ws://${HOST}:${WS_PORT}`);
  console.log('='.repeat(56));

  const connections = [];
  let successCount = 0;
  let failCount = 0;

  for (let i = 0; i < COUNT; i++) {
    const userId = ROOM_ID_OFFSET > 0 ? ROOM_ID_OFFSET + i : i + 1;
    const token = createJwt(userId);
    const idx = i + 1;

    try {
      // 1. 确保房间存在
      const roomId = await ensureRoom(userId, token);

      // 2. 建立 WebSocket 并开播
      const ws = await startBroadcast(userId, roomId, token);

      // 3. 心跳保活
      const heartbeat = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'heartbeat' }));
        }
      }, 30000);

      ws.on('close', (code) => {
        clearInterval(heartbeat);
        if (code !== 1000) {
          console.log(`  [${idx}] 连接断开 (code=${code})`);
        }
      });

      ws.on('error', () => {
        clearInterval(heartbeat);
      });

      connections.push({ userId, roomId, ws, heartbeat });
      successCount++;
      console.log(`  [${idx}/${COUNT}] OK  userId=${userId}  roomId=${roomId}  ✓`);

      // 错峰连接，避免瞬间压力
      await sleep(150);
    } catch (err) {
      failCount++;
      console.log(`  [${idx}/${COUNT}] FAIL  userId=${userId}  ✗  ${err.message}`);
    }
  }

  console.log('-'.repeat(56));
  console.log(`  完成: 成功 ${successCount} / 失败 ${failCount}`);
  console.log(`  已开播房间数: ${successCount}`);
  console.log(`  按 Ctrl+C 停止所有直播并退出`);
  console.log('-'.repeat(56));

  // 优雅退出
  let cleaning = false;
  const cleanup = () => {
    if (cleaning) return;
    cleaning = true;
    console.log('\n正在停止所有直播...');
    for (const conn of connections) {
      clearInterval(conn.heartbeat);
      try {
        conn.ws.close(1000);
      } catch {
        // ignore
      }
    }
    console.log(`已停止 ${connections.length} 个连接。`);
    process.exit(0);
  };

  process.on('SIGINT', cleanup);
  process.on('SIGTERM', cleanup);
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

main().catch((err) => {
  console.error('脚本异常退出:', err);
  process.exit(1);
});
