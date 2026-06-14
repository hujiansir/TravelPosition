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

