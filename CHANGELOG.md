# Changelog

## 2026-07-29

### Added

- 新增 Spring Boot 3.2 双引擎后端实现（`backend-springboot/`），包含：
  - 用户认证模块（Spring Security + JJWT，支持 Token 刷新与可撤销）
  - 视频管理模块（直传/分片上传、元数据、转码重试）
  - 互动模块（点赞/收藏/评论/关注，含嵌套评论）
  - 推荐模块（基线排序 + ItemCF 协同过滤，离线相似度 + Redis 在线召回）
  - 内容管理模块（分类/标签/举报）
  - 管理后台 API（用户管理/视频审核/数据概览/系统配置/强制下线）
  - 动态配置模块与 WebSocket（STOMP）实时推送
  - 全局异常处理与统一 API 响应格式
- 新增数据库初始化 SQL 脚本（PostgreSQL）
- 新增 Dockerfile 支持容器化部署
- 新增核心模块单元测试（AuthService、InteractionService）与集成测试（AuthController、VideoController）
- 新增 `.gitmodules` HTTPS 地址支持

### Changed

- 清理 Django backend `analytics/views.py` 遗留死代码
- 更新 BS01.md 论文补充双引擎架构描述
- 更新 README.md 补充 Spring Boot 后端文档入口

### Notes

- Spring Boot 引擎与 Django 引擎 API 契约完全一致，前端无需修改即可切换后端

### Added

- 后端新增自动审核相关模块与测试，覆盖评论审核规则和媒体审核样例。
- 管理端新增 AI 审核页面，便于集中处理审核结果。
- 移动端新增系统维护页，在 `maintenance_mode` 开启时统一跳转展示。

### Changed

- Web、管理端、移动端统一接入系统事件驱动的配置热更新。
- 移动端在 `App onShow` 时主动补拉全局配置，避免后台切回前台后配置滞后。
- 前端推荐流/关注流/精选流的热刷新逻辑补齐，配置变更后可按页面类型触发刷新或重载。
- 根仓库文档更新，补充本次发布涉及的配置同步、维护模式和审核能力说明。

### Notes

- 当前主仓库标签建议从本次提交开始使用日期版标签，便于和子模块同步记录。
