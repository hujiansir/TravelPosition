/**
 * 生成地图 marker 用的黄色圆点图标(36x36,抗锯齿,#FFC107)
 * 输出: travel-position-web/miniprogram/images/dot.png
 */
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

const out = path.resolve(__dirname, '..', 'travel-position-web', 'miniprogram', 'images', 'dot.png');

// CRC32 表
const crcTable = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();

function crc32(buf) {
  let c = 0xFFFFFFFF;
  for (let i = 0; i < buf.length; i++) c = crcTable[(c ^ buf[i]) & 0xFF] ^ (c >>> 8);
  return (c ^ 0xFFFFFFFF) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const t = Buffer.from(type, 'latin1');
  const td = Buffer.concat([t, data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
}

const u32 = (n) => { const b = Buffer.alloc(4); b.writeUInt32BE(n); return b; };
const u8 = (n) => { const b = Buffer.alloc(1); b.writeUInt8(n); return b; };

const size = 36;
const cx = (size - 1) / 2;
const cy = (size - 1) / 2;
const r = size / 2 - 2;
const raw = Buffer.alloc((size * 4 + 1) * size);
let o = 0;
for (let y = 0; y < size; y++) {
  raw[o++] = 0; // filter none
  for (let x = 0; x < size; x++) {
    const d = Math.hypot(x - cx, y - cy);
    let a;
    if (d <= r - 1) a = 255;
    else if (d <= r) a = Math.round(255 * (r - d)); // 边缘抗锯齿
    else a = 0;
    raw[o++] = 255; // R
    raw[o++] = 193; // G
    raw[o++] = 7;   // B
    raw[o++] = a;   // A
  }
}

const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
const ihdr = Buffer.concat([u32(size), u32(size), u8(8), u8(6), u8(0), u8(0), u8(0)]);
const idat = zlib.deflateSync(raw);
const png = Buffer.concat([sig, chunk('IHDR', ihdr), chunk('IDAT', idat), chunk('IEND', Buffer.alloc(0))]);

fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, png);
console.log('生成图标:', out, '(' + png.length + ' bytes)');
