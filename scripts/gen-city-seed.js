/**
 * city 表种子数据生成脚本
 *
 * 从 DataV.GeoAtlas 行政区划树(all.json)提取所有地级市 + 4 个直辖市,
 * 生成 city_seed.sql。仅依赖 Node 内置模块,无需 npm install。
 *
 * 用法:
 *   node scripts/gen-city-seed.js
 * 输出:
 *   travel-position-server/src/main/resources/sql/city_seed.sql
 */
const https = require('https');
const fs = require('fs');
const path = require('path');

const ALL_URL = 'https://geo.datav.aliyun.com/areas_v3/bound/all.json';
// 直辖市(省级行政区,作为地级市记录)
const MUNICIPALITIES = new Set(['110000', '310000', '120000', '500000']);

const OUTPUT = path.resolve(__dirname, '..', 'travel-position-server', 'src', 'main', 'resources', 'sql', 'city_seed.sql');

function escape(str) {
  return String(str == null ? '' : str).replace(/'/g, "''");
}

https.get(ALL_URL, (res) => {
  if (res.statusCode !== 200) {
    console.error('拉取 DataV all.json 失败,HTTP ' + res.statusCode);
    process.exit(1);
  }
  let body = '';
  res.on('data', (c) => (body += c));
  res.on('end', () => {
    const arr = JSON.parse(body);

    // 省 adcode -> name 映射
    const provinceNameMap = {};
    for (const it of arr) {
      if (it.level === 'province') {
        provinceNameMap[String(it.adcode)] = it.name;
      }
    }

    const cities = [];
    for (const it of arr) {
      const adcode = String(it.adcode);
      // 普通地级市
      if (it.level === 'city' && it.lng != null && it.lat != null) {
        const provinceAdcode = String(it.parent);
        cities.push({
          adcode,
          name: it.name,
          provinceAdcode,
          provinceName: provinceNameMap[provinceAdcode] || '',
          lng: it.lng,
          lat: it.lat,
        });
      }
      // 直辖市
      if (MUNICIPALITIES.has(adcode) && it.level === 'province' && it.lng != null && it.lat != null) {
        cities.push({
          adcode,
          name: it.name,
          provinceAdcode: adcode,
          provinceName: it.name,
          lng: it.lng,
          lat: it.lat,
        });
      }
    }

    // 按 adcode 排序去重
    const unique = {};
    for (const c of cities) unique[c.adcode] = c;
    const list = Object.values(unique).sort((a, b) => a.adcode.localeCompare(b.adcode));

    let sql = '-- city 表种子数据(地级市 + 直辖市)\n';
    sql += '-- 由 scripts/gen-city-seed.js 从 DataV.GeoAtlas 生成,请勿手工编辑\n';
    sql += '-- 共 ' + list.length + ' 个城市\n\n';
    sql += 'INSERT INTO travel.city (adcode, name, province_adcode, province_name, center_lng, center_lat) VALUES\n';
    const lines = list.map((c) =>
      '  (' + c.adcode + ", '" + escape(c.name) + "', " + c.provinceAdcode + ", '" +
      escape(c.provinceName) + "', " + c.lng + ', ' + c.lat + ')'
    );
    sql += lines.join(',\n') + ';\n';

    fs.writeFileSync(OUTPUT, sql, 'utf8');
    console.log('生成 ' + list.length + ' 个城市 → ' + OUTPUT);
  });
}).on('error', (e) => {
  console.error('网络错误:', e.message);
  process.exit(1);
});
