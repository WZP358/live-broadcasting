const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const timeout = (ms, label) =>
  new Promise((_, reject) => setTimeout(() => reject(new Error(`${label} timeout`)), ms))

const testGuard = async () => {
  const bytes = Uint8Array.from([255,216,255,224,0,16,74,70,73,70,0,1,1,1,0,1,0,1,0,0,255,219,0,67,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,192,0,11,8,0,1,0,1,1,1,17,0,255,196,0,20,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,218,0,8,1,1,0,0,63,0,127,255,217])
  const formData = new FormData()
  formData.append("file", new Blob([bytes], { type: "image/jpeg" }), "bdd-safe.jpg")
  const response = await fetch("http://127.0.0.1:8300/check", { method: "POST", body: formData })
  assert(response.ok, `BDD: 视觉审核服务应返回 200，实际 ${response.status}`)
  const payload = await response.json()
  assert("status" in payload && "is_safe" in payload, "BDD: 视觉审核响应必须包含 status/is_safe")
  console.log("BDD PASS: live guard service responds")
}

const encodeWsFrame = (payload) => {
  const body = Buffer.isBuffer(payload) ? payload : Buffer.from(payload)
  const header = []
  header.push(Buffer.isBuffer(payload) ? 0x82 : 0x81)
  if (body.length < 126) {
    header.push(0x80 | body.length)
  } else if (body.length < 65536) {
    header.push(0x80 | 126, (body.length >> 8) & 255, body.length & 255)
  } else {
    throw new Error("payload too large")
  }
  const mask = Buffer.from([1, 2, 3, 4])
  const masked = Buffer.alloc(body.length)
  for (let i = 0; i < body.length; i += 1) {
    masked[i] = body[i] ^ mask[i % 4]
  }
  return Buffer.concat([Buffer.from(header), mask, masked])
}

const decodeWsFrames = (buffer) => {
  const frames = []
  let offset = 0
  while (offset + 2 <= buffer.length) {
    const opcode = buffer[offset] & 0x0f
    let length = buffer[offset + 1] & 0x7f
    let headerLength = 2
    if (length === 126) {
      if (offset + 4 > buffer.length) break
      length = buffer.readUInt16BE(offset + 2)
      headerLength = 4
    } else if (length === 127) {
      throw new Error("large websocket frames are not supported in this test")
    }
    if (offset + headerLength + length > buffer.length) break
    const payload = buffer.subarray(offset + headerLength, offset + headerLength + length)
    frames.push({ opcode, payload })
    offset += headerLength + length
  }
  return { frames, rest: buffer.subarray(offset) }
}

const testDenoise = async () => {
  const net = await import("node:net")
  const crypto = await import("node:crypto")
  const socket = net.createConnection({ host: "127.0.0.1", port: 18765 })
  await Promise.race([new Promise((resolve, reject) => socket.once("connect", resolve).once("error", reject)), timeout(10000, "denoise connect")])

  const key = crypto.randomBytes(16).toString("base64")
  socket.write(`GET /ws HTTP/1.1\r\nHost: 127.0.0.1:18765\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: ${key}\r\nSec-WebSocket-Version: 13\r\n\r\n`)
  let pending = Buffer.alloc(0)
  await Promise.race([new Promise((resolve, reject) => {
    const onData = (chunk) => {
      pending = Buffer.concat([pending, chunk])
      const headerEnd = pending.indexOf("\r\n\r\n")
      if (headerEnd >= 0) {
        const header = pending.subarray(0, headerEnd).toString()
        pending = pending.subarray(headerEnd + 4)
        socket.off("data", onData)
        header.includes("101 Switching Protocols") ? resolve() : reject(new Error(header))
      }
    }
    socket.on("data", onData)
    socket.once("error", reject)
  }), timeout(10000, "denoise websocket handshake")])

  socket.write(encodeWsFrame(JSON.stringify({ type: "config", sampleRate: 48000, channels: 1, chunkSamples: 480 })))
  const readFrame = async (label, ms) => Promise.race([new Promise((resolve, reject) => {
    const onData = (chunk) => {
      pending = Buffer.concat([pending, chunk])
      const decoded = decodeWsFrames(pending)
      pending = decoded.rest
      if (decoded.frames.length) {
        socket.off("data", onData)
        resolve(decoded.frames[0])
      }
    }
    const decoded = decodeWsFrames(pending)
    pending = decoded.rest
    if (decoded.frames.length) {
      resolve(decoded.frames[0])
      return
    }
    socket.on("data", onData)
    socket.once("error", reject)
  }), timeout(ms, label)])

  const readyFrame = await readFrame("denoise ready", 90000)
  const ready = JSON.parse(readyFrame.payload.toString())
  assert(ready.type === "ready", "BDD: 降噪服务必须返回 ready")

  const floats = new Float32Array(480)
  for (let i = 0; i < floats.length; i += 1) floats[i] = Math.sin((i / floats.length) * Math.PI * 2) * 0.05
  socket.write(encodeWsFrame(Buffer.from(floats.buffer)))
  const out = await readFrame("denoise output", 30000)
  assert(out.payload.byteLength === floats.byteLength, "BDD: 降噪服务必须返回同长度音频帧")
  socket.end()
  console.log("BDD PASS: denoise service returns enhanced audio")
}

await testGuard()
await testDenoise()
