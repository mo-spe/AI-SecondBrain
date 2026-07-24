# 学习游戏化与参与感 — PRD

> 日期：2026-07-23
> 状态：已实现
> 来源：docs/需求池.md #4

---

## 1. Executive Summary

**问题**: AI-SecondBrain 目前缺乏激励用户持续学习的机制。用户孤军奋战地复习卡片、创建知识节点，但没有任何奖励、认可或社交比较来驱动日活和留存。现有的 `/review/streak-days` 接口虽然计算了连续复习天数，但没有持久化或激励这个数据。

**方案**: 在平台全范围增加游戏化层，包含：

- **综合积分制**：复习活动、知识创建、连续打卡、正确率的加权积分
- **成就徽章体系**：18 个成就，5 大分类（复习/知识/打卡/正确率/精通），铜/银/金/铂金四档
- **排行榜**：日/周/月/总榜 + 按知识标签分领域排行
- **每日签到**：独立于复习活动的签到机制，含连续签到倍数加成
- **补签卡机制**：每月免费 2 张，成就和打卡里程碑可额外获得
- **等级系统**：50 级经验值成长曲线

**成功标准**:

1. 上线 30 天内 DAU 提升 30%
2. 平均连续学习天数提升 50%
3. 复习卡片完成率提升 25%
4. 60% 活跃用户在 14 天内解锁至少 5 个成就
5. 排行榜参与率（查看排行）达到 DAU 的 40%

---

## 2. User Experience & Functionality

### 2.1 用户画像

| 画像         | 描述                          | 核心需求           |
| ------------ | ----------------------------- | ------------------ |
| 学习者小王   | 大学生，每天复习 20+ 卡片备考 | 看进度，和同学竞争 |
| 知识达人老张 | 知识工作者，创建大量节点      | 知识贡献得到认可   |
| 新手上路小李 | 新用户，需要养成习惯的动力    | 引导式成就入门     |

### 2.2 用户故事

**US-1: 工作台游戏化组件**

- 作为用户，我想在工作台看到我的积分、等级、连续打卡和下一个成就
- AC: 侧边栏显示等级徽章、XP 进度条、总积分、连续打卡火焰图标、下一个待解锁成就；点击任意元素跳转完整成就页

**US-2: 每日签到**

- 作为用户，我想每日签到获取奖励积分
- AC: 签到按钮在工作台和复习页可见；已签到后显示"已签到"并禁用；积分 = 5 × 连续倍数（≥7天 x2，≥30天 x3）；展示 90 天热力图

**US-3: 成就解锁**

- 作为用户，我想在达成里程碑时解锁成就，获得庆祝动画
- AC: 解锁时弹出 Toast（图标 + 名称 + 积分）；成就页展示全部 18 个成就（分类 Tab + 全部/已解锁/未解锁筛选）；点击查看详情；未解锁的显示进度条

**US-4: 排行榜**

- 作为用户，我想看到自己与他人的排名比较
- AC: 周期选择器（日/周/月/总榜）；领域下拉筛选；展示 Top 100 + 当前用户在底部固定行；前三名金银铜徽章；当前用户行高亮

**US-5: 补签卡**

- 作为错过一天复习的用户，我想用补签卡保留连续记录
- AC: 昨日无复习活动时 Dashboard 显示补签按钮；确认弹窗显示剩余卡数；使用后插入补签记录、扣减卡片、恢复连续天数

### 2.3 非目标 (MVP 排除)

- 不做 WebSocket 实时排行榜推送（用轮询）
- 不做自定义头像框或主页装饰
- 不做积分商城/兑换（积分纯为荣誉指标）
- 不做组队/公会系统
- 不做负向惩罚（积分只增不减，连续天数只记录最大值）
- 不做成就名称多语言（MVP 仅中文）

---

## 3. Technical Specifications

### 3.1 架构总览

```
Vue 3 + Element Plus + Pinia (Frontend)
  |
  | HTTP REST (JWT via Authorization header)
  |
Spring Boot 3.1.5 + MyBatis-Plus 3.5.3.1 (Backend)
  |
  +-- GamificationController (新建, 7 个端点)
  +-- GamificationService (新建, 核心业务逻辑)
  +-- GamificationScheduler (新建, @Scheduled 定时快照)
  +-- 6 Mappers + 6 Entities (新建)
  +-- 2 Tag Entities + 2 Tag Mappers (前置依赖)
  |
  +-- 接入点 (修改已有 Service):
        ReviewCardServiceImpl.submitReviewResult()  → awardReviewPoints()
        KnowledgeServiceImpl.createNode()            → awardCreatePoints()
        AuthController.login()                       → recordLoginActivity()
```

### 3.2 数据库设计

**迁移文件**: `sql/V6__add_gamification_tables.sql`

#### user_gamification — 用户游戏化状态

| 列                           | 类型          | 说明             |
| ---------------------------- | ------------- | ---------------- |
| id                           | BIGINT PK     | 主键             |
| user_id                      | BIGINT UNIQUE | 用户 ID          |
| total_points                 | BIGINT        | 累计总积分       |
| current_points               | BIGINT        | 当前可用积分     |
| level                        | INT           | 等级 1-50        |
| experience                   | BIGINT        | 当前等级经验值   |
| experience_to_next_level     | BIGINT        | 升级所需经验     |
| current_streak               | INT           | 当前连续签到天数 |
| max_streak                   | INT           | 历史最长连续天数 |
| last_check_in_date           | DATE          | 最后签到日期     |
| last_review_date             | DATE          | 最后复习日期     |
| makeup_cards_remaining       | INT           | 剩余补签卡数     |
| makeup_cards_used_this_month | INT           | 本月已用补签卡   |
| total_review_count           | INT           | 累计复习次数     |
| total_correct_count          | INT           | 累计正确次数     |
| total_node_count             | INT           | 累计创建节点数   |
| create_time / update_time    | DATETIME      | 时间戳           |

> **设计决策**: 独立表而非扩展 `user` 表。user 表是核心认证表应保持精简，游戏化是可插拔模块，分离避免耦合。

#### daily_check_in — 签到记录

| 列                             | 类型         | 说明         |
| ------------------------------ | ------------ | ------------ |
| id                             | BIGINT PK    | 主键         |
| user_id                        | BIGINT       | 用户 ID      |
| check_in_date                  | DATE         | 签到日期     |
| points_earned                  | INT          | 获得积分     |
| streak_bonus_multiplier        | DECIMAL(3,1) | 连续加成倍数 |
| is_makeup                      | TINYINT      | 是否补签     |
| create_time                    | DATETIME     | 签到时间     |
| UNIQUE(user_id, check_in_date) |              | 防重         |

#### achievement — 成就定义（种子数据）

| 列                 | 类型               | 说明                                     |
| ------------------ | ------------------ | ---------------------------------------- |
| id                 | BIGINT PK          | 主键                                     |
| code               | VARCHAR(50) UNIQUE | 成就代码                                 |
| name               | VARCHAR(100)       | 成就名称                                 |
| description        | VARCHAR(500)       | 描述                                     |
| icon               | VARCHAR(100)       | Element Plus 图标名                      |
| category           | VARCHAR(30)        | review/knowledge/streak/accuracy/mastery |
| tier               | VARCHAR(20)        | bronze/silver/gold/platinum              |
| trigger_type       | VARCHAR(50)        | 触发类型                                 |
| trigger_value      | INT                | 触发阈值                                 |
| points_reward      | INT                | 解锁积分奖励                             |
| makeup_card_reward | INT                | 解锁补签卡奖励                           |
| sort_order         | INT                | 排序                                     |

#### user_achievement — 用户成就记录

| 列                              | 类型      | 说明       |
| ------------------------------- | --------- | ---------- |
| id                              | BIGINT PK | 主键       |
| user_id                         | BIGINT    | 用户 ID    |
| achievement_id                  | BIGINT    | 成就 ID    |
| unlocked_at                     | DATETIME  | 解锁时间   |
| notified                        | TINYINT   | 是否已通知 |
| UNIQUE(user_id, achievement_id) |           | 防重       |

#### points_log — 积分流水

| 列           | 类型         | 说明                                                      |
| ------------ | ------------ | --------------------------------------------------------- |
| id           | BIGINT PK    | 主键                                                      |
| user_id      | BIGINT       | 用户 ID                                                   |
| points       | INT          | 积分变动（正=获得，负=消耗）                              |
| type         | VARCHAR(30)  | review/create/checkin/achievement/streak_bonus/makeup_use |
| description  | VARCHAR(200) | 描述                                                      |
| reference_id | BIGINT       | 关联 ID                                                   |
| create_time  | DATETIME     | 创建时间                                                  |

#### leaderboard_snapshot — 排行榜快照

| 列                                             | 类型        | 说明                     |
| ---------------------------------------------- | ----------- | ------------------------ |
| id                                             | BIGINT PK   | 主键                     |
| user_id                                        | BIGINT      | 用户 ID                  |
| period                                         | VARCHAR(10) | daily/weekly/monthly/all |
| domain                                         | VARCHAR(50) | 领域标签名或 "all"       |
| rank_position                                  | INT         | 排名                     |
| score                                          | BIGINT      | 当期综合得分             |
| snapshot_date                                  | DATE        | 快照日期                 |
| UNIQUE(user_id, period, domain, snapshot_date) |             | 防重                     |

### 3.3 积分公式

**复习积分** (在 `submitReviewResult` 时触发):

```
reviewPoints = (10 基础 + (5 正确奖励)) × 难度系数
难度系数: 1-2 → 1.0, 3-4 → 1.5, 5 → 2.0
```

- 正确回答困难卡片(难度5): 30 分
- 正确回答普通卡片: 15 分
- 错误回答普通卡片: 10 分

**知识创建积分** (在 `createNode` 时触发):

```
createPoints = 20
```

**签到积分**:

```
checkinPoints = 5 × streakMultiplier
连续 < 7天: 1.0
连续 7-29天: 2.0
连续 ≥ 30天: 3.0
```

**排行榜综合分** (用于排名计算):

```
compositeScore = (复习次数 × 10) × 0.40
               + (节点创建数 × 20) × 0.25
               + (当前连续天数 × 5) × 0.20
               + (正确率 × 0.5) × 0.15
```

权重理由: 复习是核心行为(40%)，知识创建体现知识深度(25%)，连续打卡体现坚持(20%)，正确率体现质量(15%)。

**等级成长**:

```
升级所需经验 = level × 150 + 100
Level 1 → 2: 250 XP
Level 25 → 26: 3850 XP
Level 49 → 50: 7450 XP
```

### 3.4 成就目录 (18 个种子数据)

| Code          | 名称     | 分类      | 等级     | 阈值    | 积分 | 补签卡 |
| ------------- | -------- | --------- | -------- | ------- | ---- | ------ |
| first_review  | 初出茅庐 | review    | bronze   | 1次     | 50   | -      |
| review_10     | 温故知新 | review    | bronze   | 10次    | 100  | -      |
| review_50     | 学而不厌 | review    | silver   | 50次    | 200  | -      |
| review_100    | 博览群书 | review    | gold     | 100次   | 500  | 1      |
| review_500    | 学问思辨 | review    | gold     | 500次   | 1000 | 2      |
| review_1000   | 博闻强识 | review    | platinum | 1000次  | 2000 | 3      |
| first_node    | 知识萌芽 | knowledge | bronze   | 1个     | 50   | -      |
| node_10       | 知识积累 | knowledge | bronze   | 10个    | 100  | -      |
| node_50       | 学识渊博 | knowledge | silver   | 50个    | 200  | -      |
| node_100      | 知识大师 | knowledge | gold     | 100个   | 500  | 1      |
| streak_3      | 三天打鱼 | streak    | bronze   | 3天     | 50   | -      |
| streak_7      | 持之以恒 | streak    | silver   | 7天     | 150  | 1      |
| streak_30     | 坚如磐石 | streak    | gold     | 30天    | 500  | 2      |
| streak_100    | 水滴石穿 | streak    | platinum | 100天   | 1500 | 5      |
| accuracy_80   | 小有成就 | accuracy  | bronze   | 80%     | 100  | -      |
| accuracy_90   | 出类拔萃 | accuracy  | silver   | 90%     | 300  | -      |
| accuracy_95   | 炉火纯青 | accuracy  | gold     | 95%     | 500  | 1      |
| mastery_first | 融会贯通 | mastery   | silver   | 1个节点 | 200  | -      |

> accuracy 成就需 total_review_count ≥ 50 才触发评估，避免新手运气。
> mastery_first 在任一 knowledge_node.masteryLevel ≥ 4 时触发。

### 3.5 API 设计

**Controller**: `backend/.../controller/GamificationController.java`

| Method | Path                                              | 说明                                               |
| ------ | ------------------------------------------------- | -------------------------------------------------- |
| GET    | `/gamification/profile`                           | 用户游戏化概览（积分/等级/连续/补签卡/下一个成就） |
| POST   | `/gamification/check-in`                          | 每日签到                                           |
| GET    | `/gamification/achievements?category=&filter=`    | 成就列表（含进度）                                 |
| GET    | `/gamification/leaderboard?period=&domain=&size=` | 排行榜                                             |
| POST   | `/gamification/makeup`                            | 使用补签卡                                         |
| GET    | `/gamification/points-log?current=&size=`         | 积分流水                                           |
| GET    | `/gamification/streak-calendar?months=3`          | 签到热力图数据                                     |

### 3.6 定时任务

```java
// 排行榜快照 — 每15分钟（6:00-23:00）
@Scheduled(cron = "0 */15 6-23 * * *")
public void generateLeaderboardSnapshots() { ... }

// 每月补签卡重置 — 每月1号 00:00
@Scheduled(cron = "0 0 0 1 * *")
public void resetMonthlyMakeupCards() { ... }
```

## 4. Risks & Roadmap

### 4.1 分阶段交付

| 阶段                   | 内容                                                                                       | 估时 |
| ---------------------- | ------------------------------------------------------------------------------------------ | ---- |
| **Phase 1: 基础**      | V6 迁移 + 8 Entity + 8 Mapper + GamificationService 积分奖励 + ReviewCard/Knowledge 接入点 | 1 周 |
| **Phase 2: 签到+补签** | check-in/makeup/streak-calendar API + Tag CRUD 前置 + 前端签到组件 + Dashboard 小组件      | 1 周 |
| **Phase 3: 成就**      | 成就评估引擎 + 成就 API + AchievementToast + Achievements.vue 页面                         | 1 周 |
| **Phase 4: 排行榜**    | GamificationScheduler + 排行榜 API + Leaderboard.vue 页面                                  | 1 周 |
| **Phase 5: 打磨**      | 积分流水页 + 动效优化 + 性能优化(Redis 缓存) + 无障碍审计                                  | 1 周 |

### 4.2 技术风险

| 风险           | 影响                            | 缓解措施                                                                       |
| -------------- | ------------------------------- | ------------------------------------------------------------------------------ |
| 排行榜快照性能 | 用户量大时查询慢                | 增量生成 + Redis Sorted Set 备选；每期只保留 Top 1000                          |
| Tag CRUD 阻塞  | 领域排行榜依赖标签系统          | Phase 2 优先完成最小标签 CRUD                                                  |
| 积分通胀       | 总榜被早期用户霸占              | 日/周/月榜按期重置；总榜只计近 365 天                                          |
| 接入点耦合     | 游戏化代码影响核心复习/知识流程 | try-catch 包裹；积分计算失败绝不影响主流程                                     |
| 并发签到       | 用户同时签到 + 复习导致重复     | MySQL UNIQUE(user_id, check_in_date) 兜底；捕获 DuplicateKeyException 优雅处理 |
| 补签卡滥用     | 用户囤卡或利用月度重置          | 硬上限最多 5 张；每周限用 2 次；审计日志                                       |

---

## 5. Verification

1. **积分入账**: 提交复习答案 → 检查 points_log 表有 review 类型记录 → user_gamification.total_points 增加
2. **签到**: 点击签到 → daily_check_in 插入记录 → user_gamification.current_streak 更新 → 再次点击提示"已签到"
3. **成就解锁**: 达到 review_count=1 → user_achievement 插入 first_review → 前端弹出 Toast
4. **补签卡**: 昨日无签到 → 使用补签卡 → daily_check_in 插入 yesterday 的 is_makeup=1 记录 → streak 不中断
5. **排行榜**: 多名用户有活动 → 等待快照生成 → GET leaderboard 返回排名 → 当前用户排名正确
6. **等级成长**: 累计积分 → experience 增长 → 达到阈值自动升级 → level 字段更新
