"""Quick integration test for DeepFilterNet3 WebSocket server."""
import asyncio
import json
import struct
import time

HOST = "127.0.0.1"
PORT = 18766

async def test_df3_ws():
    print(f"Connecting to ws://{HOST}:{PORT}/ws ...")
    try:
        reader, writer = await asyncio.wait_for(
            asyncio.open_connection(HOST, PORT), timeout=5.0
        )
    except Exception as e:
        print(f"FAIL: Connection failed: {e}")
        return False

    # WebSocket handshake
    import base64, os
    key = base64.b64encode(os.urandom(16)).decode()
    request = (
        f"GET /ws HTTP/1.1\r\n"
        f"Host: {HOST}:{PORT}\r\n"
        f"Upgrade: websocket\r\n"
        f"Connection: Upgrade\r\n"
        f"Sec-WebSocket-Key: {key}\r\n"
        f"Sec-WebSocket-Version: 13\r\n\r\n"
    )
    writer.write(request.encode())
    await writer.drain()

    response = b""
    while b"\r\n\r\n" not in response:
        chunk = await asyncio.wait_for(reader.read(4096), timeout=5.0)
        response += chunk
        if not chunk:
            break

    if b"101" not in response:
        print(f"FAIL: Handshake failed: {response[:200]}")
        writer.close()
        return False
    print("WebSocket handshake OK")

    # Send config frame
    config = json.dumps({"type": "config", "sampleRate": 48000, "channels": 1, "chunkSamples": 480})
    def ws_send(data):
        frame = bytearray()
        frame.append(0x81)  # text
        payload = data.encode() if isinstance(data, str) else data
        length = len(payload)
        if length < 126:
            frame.append(0x80 | length)
        else:
            frame.append(0x80 | 126)
            frame.extend(struct.pack(">H", length))
        mask = os.urandom(4)
        frame.extend(mask)
        masked = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))
        frame.extend(masked)
        writer.write(bytes(frame))

    def ws_recv():
        # Read header
        pass  # simplified - just receive and check for ready

    ws_send(config)
    await writer.drain()

    # Read response
    await asyncio.sleep(2)
    raw = b""
    try:
        while True:
            chunk = await asyncio.wait_for(reader.read(4096), timeout=3.0)
            if not chunk:
                break
            raw += chunk
    except asyncio.TimeoutError:
        pass

    # Parse WebSocket frame
    if len(raw) < 2:
        print("FAIL: No response from server")
        writer.close()
        return False

    # Simple frame parse
    payload_len = raw[1] & 0x7f
    offset = 2
    if payload_len == 126:
        payload_len = struct.unpack(">H", raw[2:4])[0]
        offset = 4

    mask = raw[offset:offset+4]
    offset += 4
    payload = bytes(b ^ mask[i % 4] for i, b in enumerate(raw[offset:offset+payload_len]))
    
    try:
        msg = json.loads(payload.decode())
        print(f"Server response: {msg}")
        if msg.get("type") == "ready":
            print("SUCCESS: DeepFilterNet3 server is ready!")
            writer.close()
            return True
    except:
        print(f"Raw payload ({len(payload)} bytes): {payload[:100]}")

    writer.close()
    return False

result = asyncio.run(test_df3_ws())
print(f"\nDeepFilterNet3 WebSocket test: {'PASS' if result else 'FAIL'}")
