# 旅游打卡点亮城市版图小程序 — 设计文档

> 创建日期：2026-06-14
> 状态：已通过 brainstorming 评审，待编写实现计划
> 适用范围：MVP 首版（个人版图记录）

---

## 1. 背景与目标

做一个微信小程序，让用户在旅游时通过**手动打卡**记录到访的地级市，并在地图上**点亮**其「城市版图」，形成个人旅行足迹。

- **核心价值**：以「点亮中国地级市版图」为成就感驱动，沉淀旅行足迹。
- **MVP 目标**：最快上线一个可用的「个人版图 + 打卡 + 足迹」闭环，验证核心体验。
- **非目标（留待后续迭代）**：社交（海报分享 / 全国排行榜 / 好友 PK）、图片游记、离线打卡、景点 POI 打卡、GPS 自动点亮。

## 2. 现有框架基线（设计前提）

设计必须与现有代码框架保持一致，复用已有基础设施。

### 前端 `travel-position-web`（TypeScript 小程序）
- 标准 `miniprogram-ts-quickstart` 脚手架，appid `wx2f2926f00942112e`，glass-easel 组件框架。
- 已有：`utils/request.ts`（统一请求，对接后端 `Result`）、`config/index.ts`（baseUrl `http://localhost:8080/api`）、`api/wx-auth.ts`（登录/手机号）、`app.ts`（onLaunch 自动 `wxLogin`）。
- 页面仅脚手架示例（index/logs）。

### 后端 `travel-position-server`（Spring Boot 3.2 + Java 21）
- 技术栈：MyBatis-Plus 3.5.5、PostgreSQL（schema `travel`）、Redis、weixin-java-miniapp 4.6.0、Hutool。
- 已有：`Result` 统一响应、`BusinessException` + `GlobalExceptionHandler`、`RedisUtil`、`User` 实体/Service/Mapper、`WxAuthController`（登录 + 手机号）。
- **缺口**：`WxAuthController.login` 中 TODO 了「按 openid 查/建用户 + 生成 token」；无鉴权拦截；MinIO、RabbitMQ 中间件就绪但未接入。

### 中间件
Redis / PostgreSQL / MinIO / RabbitMQ（均在 `121.41.178.123`）。MVP 仅用到 PostgreSQL + Redis。

## 3. 关键设计决策（含理由）

| # | 决策点 | 选择 | 理由 |
|---|---|---|---|
| D1 | 版图粒度 | **地级市级（300+）** | 主流旅行打卡颗粒度，「走遍中国」目标感与难度平衡；DataV 取市级边界 GeoJSON。 |
| D2 | 打卡机制 | **手动点击打卡** | 实现简单、可控、省电，不依赖后台持续定位。 |
| D3 | 打卡内容 | **轻量：城市+时间+经纬度+文字备注** | 不存图片，MVP 不启用 MinIO，后端最轻，快速上线。 |
| D4 | 重复打卡 | **记足迹（单表派生点亮）** | 每次打卡留一条 checkin 记录，城市点亮状态由 `SELECT DISTINCT city_adcode` 派生，数据模型最简。 |
| D5 | MVP 范围 | **纯个人版图**（无分享/排行/社交） | 最快上线验证核心体验；社交后续迭代。 |
| D6 | 城市判定 | **方案 A：后端调用腾讯位置服务逆地理编码** | key 全程在后端（安全）；腾讯 adcode 与 DataV 天然对齐；可 Redis 缓存降配额；不引入 PostGIS 重依赖。 |
| D7 | 鉴权 | **Spring Security + JWT** | 用户明确要求引入安全框架；规范、可扩展。 |
| D8 | DataV.GeoAtlas | **前端直连拉取 GeoJSON** | 大文件不走后端带宽；后端只持 `city` 元数据表。 |
| D9 | 前端地图 | **小程序原生 `<map>` 组件 + getLocation** | 原生组件本就基于腾讯地图，前端无需申请 key；逆地理在后端。 |

## 4. 整体架构

```
[微信小程序前端 TS]               [Spring Boot 后端 Java21]          [外部/存储]

app.ts (onLaunch 自动 wxLogin)    WxAuthController
  └─ 登录拿 JWT                      └─ POST /wx/login
                                         code → openid → 查/建 User → 签 JWT

pages/                            SecurityConfig (Spring Security)
 ├─ index    版图主页                ├─ JwtAuthenticationFilter 校验 token
 │   └─ <map> + DataV polygon 覆盖   ├─ 放行 /wx/login,/wx/phone,/error
 ├─ checkin  打卡页                  └─ 401/403 → 统一 Result JSON
 ├─ footprint 足迹页
 └─ my       个人页                CheckinController
                                    ├─ POST /checkin        打卡
api/                                ├─ GET  /checkin/lit     已点亮城市+统计
 ├─ wx-auth.ts / checkin.ts          ├─ GET  /checkin/list    足迹分页
 ├─ city.ts                          └─ DELETE /checkin/{id}  软删
 └─ utils/geo.ts (拉取/缓存DataV)
                                   CityController
utils/                               └─ GET /city/by-adcode   城市元数据
 ├─ request.ts (+Authorization头)
 └─ ...                           TencentMapService  逆地理编码(lat,lng→adcode)
                                     └─ Redis 缓存 "geo:{lng6},{lat6}"→adcode

                                          ↓
                                   PostgreSQL(schema=travel): user / city / checkin
                                   Redis: geocode 缓存（版图聚合缓存后续按需）
                                   腾讯位置服务 WebService: 逆地理编码(后端持有key)
                                   DataV.GeoAtlas: 市级边界GeoJSON(前端直连)
```

**职责要点**：
- DataV GeoJSON 由**前端直连**拉取（小程序后台配置 request 合法域名 `geo.datav.aliyun.com`），后端不代理。
- 腾讯位置服务 key **只在后端** `application.yml`，前端不持有。
- 「腾讯地图」= 前端原生 `<map>` 组件 + 后端位置服务 API，与用户「腾讯地图 SDK」选型一致。

## 5. 数据模型（PostgreSQL，schema `travel`）

复用现有 `user` 表（不改）。新增 `city`、`checkin` 两表。

### 5.1 `city` 城市元数据表（300+ 地级市种子数据）
```sql
CREATE TABLE travel.city (
  adcode          BIGINT PRIMARY KEY,            -- 6位行政区划代码,与DataV adcode一致
  name            VARCHAR(32) NOT NULL,          -- "杭州市"
  province_adcode BIGINT NOT NULL,               -- 所属省adcode
  province_name   VARCHAR(32) NOT NULL,          -- "浙江省"
  center_lng      DECIMAL(10,6),                 -- 中心点(地图定位/标注)
  center_lat      DECIMAL(10,6),
  create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 5.2 `checkin` 打卡足迹表
```sql
CREATE TABLE travel.checkin (
  id            BIGSERIAL PRIMARY KEY,
  user_id       BIGINT NOT NULL,               -- → user.id
  city_adcode   BIGINT NOT NULL,               -- → city.adcode (逆地理编码结果)
  lng           DECIMAL(10,6) NOT NULL,        -- 打卡GPS经度
  lat           DECIMAL(10,6) NOT NULL,        -- 打卡GPS纬度
  note          VARCHAR(500),                  -- 文字备注(可空)
  checkin_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted       SMALLINT NOT NULL DEFAULT 0,   -- 逻辑删除(沿用user表风格)
  CONSTRAINT fk_checkin_user FOREIGN KEY (user_id)     REFERENCES travel.user(id),
  CONSTRAINT fk_checkin_city FOREIGN KEY (city_adcode) REFERENCES travel.city(adcode)
);
CREATE INDEX idx_checkin_user_time ON travel.checkin(user_id, checkin_time);
CREATE INDEX idx_checkin_user_city ON travel.checkin(user_id, city_adcode);
-- 复用现有 update_modified_column() 触发器风格,为 checkin 表加同名触发器
```

### 5.3 点亮状态派生（不建冗余表）
```sql
SELECT DISTINCT city_adcode FROM travel.checkin WHERE user_id = ? AND deleted = 0;
```
单人打卡量天然有限（百级内），`(user_id, city_adcode)` 索引下足够快；后续需要再考虑 Redis 聚合缓存。

## 6. 后端设计

### 6.1 鉴权（Spring Security + JWT）
- pom 新增 `spring-boot-starter-security`。
- `JwtProperties`：`@ConfigurationProperties("jwt")`，含 `secret`、`expireSeconds`（默认 7 天），写入 `application.yml`。
- `JwtUtil`：签发/解析/校验；payload = `{userId, openid, iat, exp}`；用 Hutool `JWTUtil`。
- `JwtAuthenticationFilter`（继承 `OncePerRequestFilter`）：取 `Authorization: Bearer xxx` → 校验签名+过期 → 构建 `UsernamePasswordAuthenticationToken(userId, null, authorities)` → 塞入 `SecurityContextHolder`；`finally` 清理。
- `SecurityConfig`：`csrf(disable)` + `sessionManagement(STATELESS)`；放行 `/wx/login`、`/wx/phone`、`/error`，其余 `authenticated()`；`addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`；401/403 由 `AuthenticationEntryPoint`/`AccessDeniedHandler` 输出 `Result.error(401/403, msg)`。
- `UserContext.getUserId()`：从 `SecurityContextHolder` 取 principal（userId）。
- 续期策略（MVP）：固定 7 天有效期，过期前端重新 `wx.login` 换新 token；不做 refresh token、不做黑名单。

### 6.2 接口清单

**鉴权类（免登录）**

| 方法 | 路径 | 入参 | 出参 |
|---|---|---|---|
| POST | `/wx/login` | `{code}` | `{token, openid, userInfo}`（补全 token） |
| POST | `/wx/phone` | `{code}` | 手机号信息（已有） |

**打卡类（需登录）**

| 方法 | 路径 | 入参 | 出参 | 说明 |
|---|---|---|---|---|
| POST | `/checkin` | `{lng, lat, note?}` | `{checkinId, cityAdcode, cityName, provinceName, isFirstLit}` | 核心打卡 |
| GET | `/checkin/lit` | — | `{litCityCount, litProvinceCount, cities:[{cityAdcode,cityName,provinceName,firstCheckinTime}]}` | 已点亮城市+统计，供版图渲染 |
| GET | `/checkin/list` | `page,size` | `{total, records:[{id,cityAdcode,cityName,lng,lat,note,checkinTime}]}` | 足迹时间线分页 |
| DELETE | `/checkin/{id}` | — | — | 软删一条 |

**城市类（需登录）**

| 方法 | 路径 | 入参 | 出参 |
|---|---|---|---|
| GET | `/city/by-adcode` | `adcode` | `{adcode,name,provinceName,centerLng,centerLat}` |

> 版图 GeoJSON 不经后端，前端直连 DataV。

### 6.3 `POST /checkin` 核心流程
```
1. 校验 lng/lat 范围、note 长度(≤500)
2. cityAdcode = tencentMapService.reverseGeocode(lat, lng)
       ├─ Redis 查 "geo:{lng量化6位},{lat量化6位}" 命中则直接返回 adcode
       └─ 未命中 → 调腾讯位置服务 /ws/geocoder/v1/?location=lat,lng&key=...
            解析 ad_info.adcode(区县级) → 地级市 adcode 换算 → 写 Redis(TTL 30天)
3. city = cityService.getByAdcode(cityAdcode)
       不存在 → BusinessException("当前定位未匹配到地级市")  [港澳台/边界外等]
4. isFirstLit = (该用户在该 city_adcode 下历史 checkin 数 == 0)
5. 写 checkin 记录(user_id, city_adcode, lng, lat, note, checkin_time)
6. 返回 {checkinId, cityAdcode, cityName, provinceName, isFirstLit}
```

**地级市 adcode 换算策略**（行政区划码 `省2 + 市2 + 县2`）：
- 主路径：取腾讯返回区县级 adcode 前 4 位 + `"00"` → 查 `city` 表（如 `330106`→`330100` 杭州）。
- 回退路径：换算结果不在表（直辖市 `110000`/`310000`/`120000`/`500000`、省直辖县级市、港澳台）→ 用腾讯 `address_component.city` 名（直辖市时取 province 名）反查 `city.name`。
- 匹配不到时返回明确业务错误，不静默兜底。

### 6.4 新增类清单（沿用 `com.travel` 分层）
```
common/   UserContext.java, UserPrincipal 概念
config/   SecurityConfig.java(新增), JwtProperties.java(新增), WebConfig.java(保留现有CORS配置,鉴权移交SecurityConfig)
util/     JwtUtil.java
dto/      CheckinRequest, CheckinVO, LitCityVO, CheckinRecordVO, CityVO
entity/   City.java, Checkin.java
mapper/   CityMapper, CheckinMapper            (extends BaseMapper)
service/  CityService(+Impl), CheckinService(+Impl), TencentMapService
          UserService(补 loginOrRegister)
controller/ CheckinController, CityController
```

## 7. 前端设计

### 7.1 页面结构（tabBar 三页 + 打卡子页）
```
pages/
├─ index/     版图主页(tabBar)   ── <map> + DataV polygon 覆盖,顶部点亮统计,右下"+"打卡入口
├─ footprint/ 足迹页(tabBar)     ── 按时间倒序的打卡时间线(城市/省/时间/备注)
├─ my/        个人页(tabBar)     ── 头像昵称(来自user) + 点亮统计 + 关于
└─ checkin/   打卡子页(非tab)    ── 从首页"+"进入,显示定位坐标/地址,备注输入,打卡按钮
```

### 7.2 版图渲染（`pages/index`）
- 用小程序原生 **`<map>` 组件**，前端无需腾讯 key。
- **polygon 覆盖**：从 DataV 拉市级边界 GeoJSON → 转成 `<map>` 的 `polygons`（`[{points:[{latitude,longitude}…], fillColor, strokeColor}]`）；处理多环 `Polygon` 与 `MultiPolygon`。
  - 已点亮：`fillColor: #FFC107`（亮橙）+ 描边
  - 未点亮：`fillColor: rgba(0,0,0,0)` 透明 + 浅灰描边
- **加载策略**（避免一次渲染 300+ 市卡顿）：
  1. 启动加载全国**省级**轮廓 `100000_full.json` 作底图；
  2. **分省懒加载市级**：只拉「当前定位省」+「已点亮省」的市级边界；
  3. 优先用简化版边界、缓存到 `wx.storage` 并按 DataV 版本号失效；
  4. 已点亮城市另加 `markers`（首次打卡点），点击弹窗显示首次打卡时间。
- 小程序后台需把 `https://geo.datav.aliyun.com` 加入 **request 合法域名**。

### 7.3 打卡流程（`pages/checkin`）
```
1. 进入 → wx.getLocation({type:'gcj02'}) 取 lat/lng（gcj02 与腾讯/DataV 一致）
        app.json 需声明 permission.scope.userLocation + requiredPrivateInfos:["getLocation"]
2. 用户填备注(≤500) → 点「打卡」
3. POST /checkin {lng,lat,note}（Authorization 头自动带）
4. 成功 → isFirstLit? 「🎉 首次点亮 XX市！」 : 「已在 XX市 留下足迹」→ 返回首页刷新版图
5. 失败 → 按后端 message 提示（如"当前定位未匹配到地级市"）
```

### 7.4 `app.json` 改动
```jsonc
{
  "pages": ["pages/index/index","pages/checkin/checkin","pages/footprint/footprint","pages/my/my","pages/logs/logs"],
  "tabBar": { "list": [/* 版图/足迹/我的 */] },
  "permission": { "scope.userLocation": { "desc": "用于旅游打卡定位所在城市" } },
  "requiredPrivateInfos": ["getLocation"]
  // 其余 window/componentFramework/lazyCodeLoading 保留
}
```

### 7.5 `utils/request.ts` 改动
- 请求头自动加 `Authorization: Bearer ${wx.getStorageSync('token')}`；
- 响应 `code===401` → 清 token → 调 `app.wxLogin()` 重新登录。

### 7.6 新增前端文件
```
miniprogram/
├─ app.json/app.ts [改:tabBar/permission/request.ts改造]
├─ api/checkin.ts [新增:create/lit/list/remove]
├─ api/city.ts    [新增:getByAdcode]
├─ utils/geo.ts   [新增:DataV 加载/缓存/GeoJSON→polygon 转换]
└─ pages/{checkin,footprint,my}/ [新增三页]
```

## 8. 端到端数据流

**① 启动登录**：`app.onLaunch` → `wx.login` 取 code → `POST /wx/login` → 后端换 openid → 查/建 `User` → 签 JWT → 前端存 `token/openid`。

**② 版图首页加载**（并行）：`GET /checkin/lit` 拿点亮集合+统计；`geo.ts` 读/拉省级轮廓渲染底图；按点亮省份集合懒加载市级 GeoJSON；合成 polygons（点亮亮橙/未点亮透明）。

**③ 打卡**：`checkin` 页 `wx.getLocation(gcj02)` → 填备注 → `POST /checkin {lng,lat,note}` → 后端逆地理(Redis)→校验 city→判 `isFirstLit`→写库 → 前端 Toast，按需补拉该省 GeoJSON，本地增量点亮该市并刷新统计。

**④ 足迹页**：`GET /checkin/list?page&size` 渲染时间线，触底分页；`DELETE /checkin/{id}` 软删，返回首页时 `lit` 重新派生（删该市唯一打卡则该市取消点亮）。

## 9. 错误处理

沿用 `BusinessException` + `GlobalExceptionHandler`。

| 场景 | 码 | 处理 |
|---|---|---|
| 未登录 / token 失效 | 401 | `AuthenticationEntryPoint` → `Result(401)`；前端清 token 重新 `wx.login` |
| 权限不足 | 403 | `AccessDeniedHandler` → `Result(403)` |
| 参数非法（lng/lat 越界、note>500） | 400 | `@Valid` + 现有校验异常处理器 |
| 逆地理匹配不到城市（海外/港澳台未覆盖/边界点） | 业务 500 | `BusinessException("当前定位未匹配到地级市")`；前端友好提示 |
| 腾讯逆地理 API 失败/超时 | 业务 500 | 重试 1 次 → 失败抛异常，**绝不写入错误 adcode** |
| DataV GeoJSON 拉取失败 | 前端 | 重试 + 骨架兜底，不影响打卡主流程 |
| 定位权限拒绝 | 前端 | 引导 `wx.openSetting` 开启授权 |

原则：快速失败、错误带上下文、不静默吞异常；外部依赖（腾讯 API）失败不得污染业务数据。

## 10. 测试策略

**后端**（JUnit5 + Mockito）：
- 单元：`TencentMapService`（mock HTTP，验证 Redis 命中/未命中、adcode 换算 `330106→330100`、直辖市回退）；`CheckinService`（`isFirstLit` 逻辑、城市不存在抛异常）；`JwtUtil`（签发/解析/过期/伪造）。
- 集成（`@SpringBootTest` + Testcontainers PostgreSQL）：`/checkin` 端到端（mock 腾讯响应，验入库+返回）；`SecurityConfig`（无 token/过期 token → 401、有效 token → 通过）。
- 种子：`init.sql` 含 `city` 表 300+ 行种子数据 + `checkin` 表 DDL。

**前端**（MVP 以真机/模拟器手动验证为主）：
- 关键链路：登录 → 版图加载 → 打卡点亮 → 足迹列表 → 删除。
- 真机验证定位精度（模拟器定位可能不准）。

**验收口径**：可视产物以小程序真机/模拟器最终表现为准，不以「接口通 / 文件已生成」代替验收。

## 11. 实施依赖与风险

- **依赖**：
  - 腾讯位置服务 key（需申请，配额内）；后端 `application.yml` 配置。
  - DataV.GeoAtlas 数据稳定性（公共服务，需兜底重试）。
  - 小程序后台配置 request 合法域名 `geo.datav.aliyun.com`、`location` 相关 privacy。
  - `city` 表 300+ 行种子数据准备（从 DataV 元数据导出）。
- **风险**：
  - 腾讯逆地理 adcode 换算在直辖市/省直辖县级市的边界情况 → 用名称回退 + 充分单测覆盖。
  - 全国市级 GeoJSON 体积 → 分省懒加载 + storage 缓存缓解。
  - 小程序 map 组件 polygon 数量上限 → 分省渲染、只渲染可视区相关省。
  - 坐标系统一致性：小程序 `getLocation(type:'gcj02')`、腾讯地图、DataV.GeoAtlas 边界均应为 GCJ02（火星坐标），理论上无需转换；实现期需二次确认 DataV 边界坐标体系，避免出现版图偏移。

## 12. 后续迭代（非本期）

- 海报分享（生成「我的中国版图」图）。
- 全国排行榜 / 好友 PK。
- 图片游记（启用 MinIO）。
- 离线打卡 / 弱网缓存。
- 景点 POI 打卡、GPS 自动点亮。
