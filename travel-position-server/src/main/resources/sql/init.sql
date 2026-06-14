-- 连接到数据库 mydb
-- psql -h 121.41.178.123 -p 5432 -U admin -d mydb

-- 创建模式
CREATE SCHEMA IF NOT EXISTS travel;

-- 设置搜索路径
SET search_path TO travel, public;

-- 用户表
CREATE TABLE IF NOT EXISTS travel.user (
  id BIGSERIAL PRIMARY KEY,
  openid VARCHAR(64) NOT NULL UNIQUE,
  unionid VARCHAR(64),
  nickname VARCHAR(64),
  avatar_url VARCHAR(255),
  phone VARCHAR(20),
  gender SMALLINT DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

-- 添加注释
COMMENT ON SCHEMA travel IS '旅游定位小程序业务模式';
COMMENT ON TABLE travel.user IS '用户表';
COMMENT ON COLUMN travel.user.id IS '用户ID';
COMMENT ON COLUMN travel.user.openid IS '微信openid';
COMMENT ON COLUMN travel.user.unionid IS '微信unionid';
COMMENT ON COLUMN travel.user.nickname IS '用户昵称';
COMMENT ON COLUMN travel.user.avatar_url IS '用户头像';
COMMENT ON COLUMN travel.user.phone IS '手机号';
COMMENT ON COLUMN travel.user.gender IS '性别 0-未知 1-男 2-女';
COMMENT ON COLUMN travel.user.create_time IS '创建时间';
COMMENT ON COLUMN travel.user.update_time IS '更新时间';
COMMENT ON COLUMN travel.user.deleted IS '逻辑删除 0-未删除 1-已删除';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_user_create_time ON travel.user(create_time);

-- 创建更新时间自动更新触发器函数
CREATE OR REPLACE FUNCTION travel.update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
DROP TRIGGER IF EXISTS update_user_modtime ON travel.user;
CREATE TRIGGER update_user_modtime
    BEFORE UPDATE ON travel.user
    FOR EACH ROW
    EXECUTE FUNCTION travel.update_modified_column();

-- ============================================================
-- 城市表(地级市元数据,300+ 行种子数据见 city_seed.sql)
-- ============================================================
CREATE TABLE IF NOT EXISTS travel.city (
  adcode          BIGINT PRIMARY KEY,
  name            VARCHAR(32) NOT NULL,
  province_adcode BIGINT NOT NULL,
  province_name   VARCHAR(32) NOT NULL,
  center_lng      DECIMAL(10,6),
  center_lat      DECIMAL(10,6),
  create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE travel.city IS '城市表(地级市元数据)';
COMMENT ON COLUMN travel.city.adcode IS '行政区划代码(与DataV一致)';
COMMENT ON COLUMN travel.city.name IS '城市名称';
COMMENT ON COLUMN travel.city.province_adcode IS '所属省adcode';
COMMENT ON COLUMN travel.city.province_name IS '所属省名称';
COMMENT ON COLUMN travel.city.center_lng IS '中心点经度';
COMMENT ON COLUMN travel.city.center_lat IS '中心点纬度';
CREATE INDEX IF NOT EXISTS idx_city_province ON travel.city(province_adcode);
CREATE INDEX IF NOT EXISTS idx_city_name ON travel.city(name);

-- ============================================================
-- 打卡足迹表
-- ============================================================
CREATE TABLE IF NOT EXISTS travel.checkin (
  id            BIGSERIAL PRIMARY KEY,
  user_id       BIGINT NOT NULL,
  city_adcode   BIGINT NOT NULL,
  lng           DECIMAL(10,6) NOT NULL,
  lat           DECIMAL(10,6) NOT NULL,
  note          VARCHAR(500),
  checkin_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted       SMALLINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_checkin_user FOREIGN KEY (user_id)     REFERENCES travel.user(id),
  CONSTRAINT fk_checkin_city FOREIGN KEY (city_adcode) REFERENCES travel.city(adcode)
);

COMMENT ON TABLE travel.checkin IS '打卡足迹表';
COMMENT ON COLUMN travel.checkin.user_id IS '用户ID';
COMMENT ON COLUMN travel.checkin.city_adcode IS '打卡城市adcode';
COMMENT ON COLUMN travel.checkin.lng IS '打卡经度';
COMMENT ON COLUMN travel.checkin.lat IS '打卡纬度';
COMMENT ON COLUMN travel.checkin.note IS '文字备注';
COMMENT ON COLUMN travel.checkin.checkin_time IS '打卡时间';
COMMENT ON COLUMN travel.checkin.deleted IS '逻辑删除 0-未删除 1-已删除';

CREATE INDEX IF NOT EXISTS idx_checkin_user_time ON travel.checkin(user_id, checkin_time);
CREATE INDEX IF NOT EXISTS idx_checkin_user_city ON travel.checkin(user_id, city_adcode);

-- checkin 表更新时间触发器(复用 update_modified_column 函数)
DROP TRIGGER IF EXISTS update_checkin_modtime ON travel.checkin;
CREATE TRIGGER update_checkin_modtime
    BEFORE UPDATE ON travel.checkin
    FOR EACH ROW
    EXECUTE FUNCTION travel.update_modified_column();

-- 种子数据:执行 city_seed.sql(由 scripts/gen-city-seed.js 生成)
-- \i city_seed.sql
