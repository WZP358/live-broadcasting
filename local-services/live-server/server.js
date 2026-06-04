const path = require('path');
const NodeMediaServer = require('node-media-server');

const rootDir = path.resolve(__dirname, '..', '..');
const mediaRoot = path.join(rootDir, '.runlogs', 'local-live-media');
const ffmpegPath = process.env.FFMPEG_PATH || 'C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe';

const config = {
  logType: 3,
  rtmp: {
    port: Number(process.env.RTMP_PORT || 1935),
    chunk_size: 60000,
    gop_cache: true,
    ping: 30,
    ping_timeout: 60,
  },
  http: {
    port: Number(process.env.HTTP_PORT || 8080),
    mediaroot: mediaRoot,
    allow_origin: '*',
  },
};

const server = new NodeMediaServer(config);
server.run();
