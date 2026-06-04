const WebSocket = require('ws');

const TOKEN = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJxYV90ZXN0MSIsImp0aSI6IjEwMDIyIiwidXNlcm5hbWUiOiLmtYvor5XkuIDlj7ciLCJpYXQiOjE3ODA0NzczNjcsImV4cCI6MTc4MDU2Mzc2N30.RLAlS89yJIHXeKJr8cIxhoCyUPUQFI2pVeY_TfcA0Uw';
const ROOM_ID = 20;
const WS_URL = 'ws://localhost:9000/ws/browser-live?token=' + encodeURIComponent(TOKEN);

console.log('连接信令服务器:', WS_URL);
const ws = new WebSocket(WS_URL);

ws.on('open', () => {
  console.log('✅ 信令 WebSocket 已连接');
  ws.send(JSON.stringify({ type: 'join', roomId: ROOM_ID, role: 'BROADCASTER' }));
  console.log('📡 已发送开播消息, roomId=' + ROOM_ID + ', role=BROADCASTER');
});

ws.on('message', (data) => {
  console.log('📩 收到:', data.toString().substring(0, 200));
});

ws.on('error', (err) => {
  console.error('❌ WebSocket 错误:', err.message);
});

ws.on('close', (code, reason) => {
  console.log('🔌 WebSocket 关闭, code=' + code);
});

// 保持连接 5 分钟
setTimeout(() => {
  console.log('⏰ 5分钟到, 关闭直播');
  ws.send(JSON.stringify({ type: 'leave' }));
  setTimeout(() => ws.close(), 1000);
}, 5 * 60 * 1000);
