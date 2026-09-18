# MEMORY.md — AI-SecondBrain 长期项目记忆

## 项目概况

Java 17 + Spring Boot 3.1.5 后端（MyBatis-Plus / Redis / ES / Kafka / Quartz） + Vue 3 + Vite 前端 + Chrome 采集插件 + Python DeerFlow AI 服务。知识管理 + 艾宾浩斯复习系统。

## 项目约定（踩过的坑）

- **后端有两个端口，各自自洽，不要"统一"**：本地默认 profile = 8081（`application.yml`，vite 代理也是 8081）；
  Docker prod profile = 8080（`application-prod.yml`，compose 映射 8080）。首轮曾误判为端口 bug。
- **判断功能是否完成，必须以代码为准**。PRD / 需求池 / README 的状态标记普遍滞后：
  RBAC、知识广场、协作分享标「草稿/待评审」，代码早已实现；题目池标「设计中」，实际已全链路打通。
- **数据库初始化**：完整方案是 `sql/second_brain.sql`（32 张基础表）→ V7 → V8 → V9。
  V3~V6 的表已合并进 `second_brain.sql`，不需要再跑。
  `sql/complete_database_schema_verified.sql` 名不副实（只有 12 张老表），已在 compose 里弃用。
  项目无 Flyway。MySQL initdb 按文件名字典序执行，compose 里靠数字前缀 01~04 锁顺序。
- 后端 code style：Lombok 用 `@Getter/@Setter`，**禁用 `@Data`**。前端 Vue 3 Composition API + `<script setup>`。
- 分支流程：从 dev 拉取开发，推送前必须先本地编译运行通过。

## 已知未完成项（P0 已修，剩余 P1/P2）

详见 `PROJECT_COMPLETENESS_AUDIT.md`（2026-09-15 审查 + 修复双轮）。

**已修**：批量删除（后端新增 `/knowledge/batch`）、数据库 init 链路、Dashboard 两个死链
（/chat、/report 已注册路由）、Chat.vue 重复 import。

**剩余 P1/P2**：
- `ReviewReminderServiceImpl:83/97/109` 三个方法只有 log → 复习提醒链路是断的
- 导出功能后端齐全、前端零入口，Knowledge.vue:1084 只弹"开发中"
- 六处「功能开发中」占位
- ES/Kafka 未部署时 `NoOpElasticsearchService` 静默返回空结果（无声故障）
- 孤儿文件 KnowledgeGraph.vue / RagQA.vue / AsyncTaskDemo.vue 未接线，可考虑删除

## 重复实现陷阱

判断"某功能前端缺失"前，先确认是否被别的页面承接了：
- 知识图谱 → 已在「知识体系」页（KnowledgeSystem.vue）用 echarts 渲染
- 对话采集 → 已由 Capture.vue（/capture）承接，Chat.vue 是旧版
- RAG 问答 → 知识体系页已有问答框，RagQA.vue 不接线
- 唯一真正缺失过的前端功能是**学习报告**

## 安全隐患

`backend/src/main/resources/application-local.yml:33` 硬编码了一个 `sk-` 开头的 API 密钥，疑似泄露，建议轮换。
