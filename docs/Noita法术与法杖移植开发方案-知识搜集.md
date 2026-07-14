# Noita 法术与法杖移植开发方案

> 搜集时间：2026-07-13  
> 目标版本：Minecraft Java Edition 1.20.4 + Fabric + Java 17  
> 权威口径：Noita Wiki 作为本项目首要行为规格；非官方数据镜像只补充 Wiki 未说明的实现字段  
> 审查状态：已完成事实准确性、来源权威性、逻辑一致性三线审查并修订

---

## 概述

本项目真正要移植的不是 422 个彼此孤立的技能，而是一个能组合这些法术的“法杖解释器”。Noita Wiki 将法杖描述为 Deck、Hand、Discard 三堆卡牌驱动的施法系统：法术通过 Draw 被调用，共同修改 Shot State；触发器建立隔离的载荷 Shot State；修正器、多重释放、包裹、刷新、复制和递归共同决定一次施法的结果。只有先把这些底层语义做对，逐个登记法术才不会把错误复制 422 次。

Minecraft 不具备 Noita 的 2D 像素材料模拟和 60 FPS 法术执行环境，因此本方案采用“组合语义忠实、世界效果等价、差异显式记录”的原则。法杖抽牌、法力、时序、触发载荷、复制与递归必须尽量忠实；液体、粉末、像素破坏、导电、冻结、变形等效果通过方块、实体、状态效果和有预算的区域算法适配。

当前仓库已经具备一个实现原型，而不是空项目：`ModItems.java` 声明了 328 个法术物品，其中包含 Wiki 列出的全部 122 个 Projectile、45 个 Static Projectile、143 个 Projectile Modifier，以及 18 个 Multicast/Utility/Other 法术；`NoitaWandCaster` 已有纯 Deck/Hand/Discard 求值、冻结 Trigger payload，以及 G04 Greek/Divide/Add Trigger 调用 primitive。注册数量仍不等于行为完成度，覆盖率清单继续区分 approximate 与 verified。

---

## 一、权威机制结论

### 1.1 法术规模与分类

Noita Wiki 当前列出 422 个法术，分为 8 类：

| 类型 | 数量 | 核心作用 | 对实现的意义 |
|---|---:|---|---|
| Projectile | 122 | 生成运动投射物 | 需要投射物规格和世界执行器 |
| Static Projectile | 45 | 生成领域、云、静态结构等 | 不能只当低速 Projectile |
| Projectile Modifier | 143 | 修改当前 Shot State，通常继续 Draw | 必须共享状态，不能只包装下一枚投射物 |
| Material | 26 | 生成、转换或移除材料 | 进入 MC 材料适配层 |
| Utility | 25 | 传送、照明、召唤、编辑施法状态等 | 需要独立动作策略 |
| Other | 42 | 复制、递归、条件、特殊规则 | 解释器难点，最后分批完成 |
| Multicast | 14 | 增加 Draw | 是解释器操作，不是额外弹丸数量 |
| Passive | 5 | 持有或装入法杖时持续生效 | 需要生命周期和装备状态管理 |

类型只是行为提示，不是实现接口的充分定义。Wiki 明确指出类型边界会模糊，所有类型都可能生成投射物、修改投射物、生成材料或执行脚本效果。因此 Java 中不能用一个 `switch (spellType)` 承担全部行为；类型用于分类、复制规则和使用次数规则，具体动作由动作定义决定。

### 1.2 Deck、Hand、Discard 与 Draw

- Deck 保存可抽取法术；非乱序法杖按槽位顺序，乱序法杖只在指定时机洗牌。
- Hand 保存本次 cast 已正常抽取并调用的法术，防止同一张卡在同一 cast 中被普通抽牌重复使用。
- Discard 保存已施放或因法力/次数不足被跳过的法术；cast 结束后 Hand 通常进入 Discard。
- Draw 是从 Deck 取牌、校验次数与法力、扣除法力、移入 Hand、调用 action 的完整过程。
- 法力或次数不足时，卡直接进入 Discard，并尝试后续卡；但 Deck 最后一张失败卡仍可能消耗一次 Draw。
- 法杖固有的 Spells/Cast 提供初始 Draw，但它本身不能触发 Wrap；法术 action 发起的 Draw 可以在 Deck 为空时 Wrap。

### 1.3 Shot State，而不是“修正下一张卡”

Wiki 明确纠正了一个常见误解：法术不会直接修改其他法术，法术 action 修改的是当前 Shot State。一个 Shot State 保存伤害向量、暴击、散射、速度、寿命、施放延迟、投射物树和附加效果等。

这条规则直接决定解释器结构。例如 `Double Spell -> Damage Plus -> Spark Bolt -> Spark Bolt` 中，两个 Spark Bolt 共享同一 Shot State，因而两者都获得 Damage Plus。若实现为“修正器只包裹递归调用的下一枚投射物，然后恢复旧状态”，第二枚 Spark Bolt 会错误地失去增伤。

每次普通法杖 cast 创建 Base Shot State。每个 Trigger/Timer/Expiration（Lua API 中也称 death trigger）载荷创建独立的 Trigger Shot State。载荷中的伤害、速度、散射和施放延迟与外部隔离，但法力和充能时间等全局值不完全隔离。

### 1.4 Wrap、Reload 与时序

- 法术 Draw 遇到空 Deck 时，把 Discard 移回 Deck 并排序/洗牌，这叫 Wrap；该情况会要求本次 cast 结束后 Reload。
- 同一 cast 中首次有效的 Wand Refresh 会 Wrap，且不因这次 reset 本身强制 Reload；同一 cast 中第二次调用不再执行 reset，并会强制 Recharge。
- 初始 Spells/Cast 最后一抽恰好清空 Deck 会触发 Reload，但不会 Wrap。
- Cast Delay 与 Recharge Time 同时倒计时，实际等待较大者，不是两者相加。
- 载荷内 Cast Delay 不计入真实 cast 间隔；载荷内 Recharge Time 修改仍可影响整根法杖。
- Noita 以 60 帧/秒表达许多数值，Minecraft 为 20 tick/秒。应先保存规范化秒值或 Noita 帧值，再由唯一换算器转换，禁止在各实体内散落 `/ 60`、`* 20`。

### 1.5 Trigger、Timer 与 Death Trigger

- Trigger：碰到实体或地形时释放载荷。
- Timer：碰撞时按普通 Trigger 释放；若投射物仍存活，到时限会再释放一次并终止 Timer。带 Piercing 的 Timer 在到期前可因多次命中重复释放。
- Expiration/Death Trigger：投射物自然过期或被其他机制销毁时释放，最多一次。
- 载荷在施法时通过 Draw 选定、校验并支付法力，不是在命中时再次抽牌或收费。
- 普通触发载荷初始 Draw 为 1；Double Trigger 和 Delayed Spellcast 等有更高初始 Draw。
- 载荷可在自己的作用域内 Wrap，并拥有独立 Shot State。

### 1.6 复制、调用与递归

正常 Draw 会检查法力和次数并把卡放入 Hand；Copy/Call 直接调用目标 action，通常不自动检查目标的法力和剩余次数，也不把目标加入 Hand。不同复制法术可额外实现自己的规则，不能用一个统一“复制物品栈”替代。

Wiki 给出的递归规则为：普通 Draw 的递归层级为 0；调用非 recursive action 不增加层级；调用 recursive action 增加 1；达到递归上限后不得再调用 recursive action；原版递归上限为 2。实现还必须加入独立的总步骤预算，因为合法的 Multicast、Divide By、Trigger 与复制组合即使不突破递归层级，也可能制造极大的动作树。

### 1.7 使用次数与 Always Cast

- 次数耗尽的法术会被跳过且不消耗法力。
- 正常 Draw 时并非立刻扣次数；次数通常在 Hand 正常移入 Discard 时处理。Utility/Other 按其类别规则处理；其他有限次数卡只有在同一 cast 中有 Projectile、Static Projectile 或 Material 类型卡通过正常 Draw 进入 Hand 时才尝试扣次。非标准 Copy/Call 生成世界效果不等同于正常入 Hand。
- Copy/Call 通常不会触发普通 Hand 弃牌扣次流程，但个别复制动作会手动扣次。
- Always Cast 位于普通队列之外，每次 cast 加在前面；通常法力消耗视为 0，有限次数视为无限。
- Always Cast 与 Trigger/Timer 存在特殊行为，必须以 Wiki 例子建立测试，不能只设置 `permanent=true` 后复用普通 Draw。

---

## 二、项目现状与差距

### 2.1 已有基础

| 能力 | 当前实现 | 可保留价值 |
|---|---|---|
| Fabric 工程 | 1.20.4、Yarn、Loader 0.15.11、Fabric API 0.97.1、Java 17 | 版本基线正确 |
| 法杖物品与 GUI | `NoitaWandItem`、ScreenHandler、客户端 Screen/HUD | 可继续演进 |
| 简化施法状态 | NBT 保存 mana、deck、discard、延迟和充能时间 | 可迁移到版本化状态对象 |
| 投射物 | 通用投射物实体和爆炸实体 | 可作为第一批执行器 |
| 触发载荷 | `NoitaProjectilePayload` 支持嵌套 NBT | 可迁移为不可变执行计划 |
| 法术资源 | 大量模型、纹理、双语文本已经存在 | 可直接复用；仅检查路径、尺寸、缺失和重复 |
| 历史资料 | `Documents/NoitaMechanics` 含 422 项索引与 Lua/XML 缓存 | 适合补充 Wiki 未说明字段 |

### 2.2 关键缺口

1. `ModItems.java` 超过 3300 行，注册、数据、分类和行为入口耦合；继续手写 107 个法术会迅速失控。
2. `NoitaWandCaster.CastContext` 是私有内部解释器，世界副作用与抽牌混在一起，无法做纯单元测试。
3. 当前 Modifier 通过临时 `CastState` 包裹递归 Draw 后恢复，不能保证同一 Shot State 的后续多重释放共享修正。
4. `NoitaProjectilePayload` 已能递归保存嵌套投射物 payload tree，但节点类型单一，尚不足以表达材料、Utility、静态场、条件和延迟动作等异构计划节点；递归 NBT 解码也没有深度、节点数和字节上限。
5. Alpha、Gamma、Duplicate 只覆盖复制系统的很小部分，且还没有逐条对齐 Wiki 的目标搜索、Draw 开关、次数与递归规则。
6. 328 个已注册法术物品不等于 328 个完成行为；许多世界效果仍使用通用实体近似。
7. 伤害目前偏向单一数值，需要独立的 Projectile、Explosion、Fire、Electricity、Drill、Slice、Ice、Healing、Radioactive/Toxic、Poison、Curse、Holy 等语义层。
8. 没有 `src/test` 或 GameTest，缺乏能保护复杂组合语义的回归网。
9. 大范围实体扫描、方块修改、弹丸复制、递归和持久实体存在服务器拒绝服务风险。
10. 当前 trigger 求值会恢复载荷内的 `rechargeTimeSeconds`，错误隔离了应为全局的 Recharge 修改；投射物的单一 `triggerPayloadReleased` 又无法表达 Piercing Trigger/Timer 的重复命中语义。
11. 当前复制系统用统一 `copiedActionDepth >= 2` 限制所有复制，没有根据目标 action 的 `recursive` 属性决定是否增加层级。
12. 现有法术定义没有可靠的 canonical `noita_id` 字段，诸如 Wiki `MANA_REDUCE` 与 Java `ADD_MANA` 的别名必须显式映射。
13. 当前仓库含大量未提交资源；任何重构都必须先生成清单并避免覆盖用户资产。

---

## 三、第一性原理目标与非目标

### 3.1 必须保留的核心体验

1. 法术顺序、Draw、Multicast、Modifier、Trigger 和 Wrap 能产生可推理的组合结果。
2. 法杖的 shuffle、Spells/Cast、cast delay、recharge、mana、capacity、spread、always cast、speed multiplier 均有真实作用。
3. 同一套法术在单人和专用服务器上由服务器得出相同结果。
4. 法术数据可以追溯到 Wiki 页面，并明确记录 MC 适配差异。
5. 玩家能从 UI 看懂法杖当前属性、法力、冷却、法术次数和危险配置。

### 3.2 明确不追求逐像素复制

1. 不重造 Noita 的 2D Falling Everything Engine。
2. 不承诺 60 次/秒的每次射击都生成独立 MC 实体；高频效果允许合并为射线、批次或周期伤害，但总法力、DPS 和视觉节奏需可解释。
3. 不允许默认绕过领地保护、`mobGriefing` 或服务器配置进行大规模破坏。
4. 本项目限定为朋友间非商业使用，允许直接从 Noita 官网或 Noita Wiki 下载图标、纹理、法杖图、参考文本和其他所需资源，不把版权归属或再许可审计作为开发门禁。

---

## 四、目标架构

### 4.1 四层结构

```text
BuiltIn Spell Catalog + Wiki Source Metadata
                  |
                  v
Pure Wand Evaluator -- deterministic seed --> ResolvedCast / EffectPlan
                  |                              |
                  |                              v
                  +----------------------> Server Effect Executors
                                                 |
                                                 v
                                      Client Visual/Audio Projection
```

#### A. 定义层

- `SpellDefinition`：ID、类型、法力、次数、tier/tag、动作列表、来源、适配等级、Wiki revision ID、目标 Noita 版本和数据证据版本。
- `WandDefinition`：shuffle、spellsPerCast、castDelay、recharge、manaMax、manaCharge、capacity、spread、alwaysCast、speedMultiplier。
- `ActionDefinition`：不是按类型猜行为，而是显式动作，如 `draw`、`modify_shot`、`add_projectile`、`begin_trigger`、`call_spell`、`discard`、`refresh_wand`、`spawn_material`。
- 内置法术 ID 在注册期固定；可重载 JSON 只能覆盖已注册 ID 的平衡与行为，不能在资源重载时动态添加 Minecraft Item。
- 每次成功重载生成不可变 catalog snapshot 与 `catalogEpoch/hash`。施法时把机械参数冻结到 EffectPlan；在途或已持久化实体不能在重载后按 ID 重新解释伤害和载荷。非法重载整体拒绝并保留上一份有效 snapshot。

#### B. 纯解释器层

- `WandCastSession`：Deck、Hand、Discard、法力、全局充能修正、递归层、确定性 RNG、预算。
- `ShotState`：不可变或受控可变的当前 shot 累加器。
- `ProjectileTree`：普通投射物、trigger/timer/death 子树、延迟/条件节点。
- `ResolvedCast`：不引用 `World`、`Entity`、NBT、ItemStack 或客户端类；只包含要执行的结果和下一法杖状态。NBT、Registry 和 ItemStack 转换属于 persistence/adapter 层。
- 同一输入状态、同一 seed 必须得到相同输出，便于单测、重放和网络调试。

#### C. 服务器执行层

- 把 `ResolvedCast` 转成实体、射线、区域场、方块变换、状态效果和声音事件。
- 一次施法在服务器主线程按 `validate -> evaluate -> reserve budgets -> commit WandState -> execute EffectPlan` 顺序完成；用手、槽位、ItemStack 状态哈希/修订号阻止 GUI 编辑、换手和连续包之间的竞态。
- 服务器校验玩家持有法杖、法力、冷却、槽位版本和执行预算；客户端只发送带协议版本和序号的施法意图。
- 持久实体保存 `schemaVersion`、`catalogEpoch/hash`、执行 UUID、所有者 UUID、必要的已解析参数、载荷树和已释放标记；不得保存客户端计算的权威结果。
- 解码采用有深度、节点数和字节上限的迭代或受控过程。普通保存/卸载路径保证 one-shot payload 最多释放一次；若要覆盖进程崩溃窗口，还需持久执行账本和去重事务，不能仅靠一个布尔值宣称 exactly-once。

#### D. 客户端表现层

- 渲染、粒子、音效、HUD、法杖编辑 GUI 只读取服务器同步状态。
- 高频法术优先用粒子/射线批处理，不为视觉效果额外生成服务器实体。
- 所有客户端包接收器都要容忍未知版本、缺失实体和乱序包。

### 4.2 推荐包结构

```text
com.mcnoita
  catalog/       内置 ID、定义加载、Codec、来源元数据
  wand/model/    WandDefinition、WandState、DeckState
  wand/eval/     纯解释器、Draw、Wrap、Reload、预算
  spell/action/  各类 Action 与复制/递归规则
  spell/plan/    ShotState、ProjectileTree、ResolvedCast、EffectPlan
  spell/exec/    服务端世界执行器
  spell/damage/  自定义伤害类型与换算
  spell/material/MC 材料映射与区域算法
  network/       1.20.4 旧式 Fabric networking
  client/        HUD、Screen、Renderer、粒子、音效
  datagen/       模型、语言、配方、标签和验证报告
```

### 4.3 数据来源与可追溯性

每个法术定义至少保存：

```json
{
  "id": "mc-noita:spark_bolt",
  "noita_id": "LIGHT_BULLET",
  "type": "projectile",
  "source": {
    "wiki": "https://noita.wiki.gg/wiki/Spark_Bolt",
    "wiki_revision_id": 0,
    "verified_on": "2026-07-13",
    "noita_version": "待记录",
    "implementation_evidence": "固定版本或 commit，不能只写浮动 main"
  },
  "adaptation": {
    "level": "equivalent",
    "notes": "Noita 像素速度映射为 MC blocks/second"
  }
}
```

`wiki_revision_id` 必须填实际 revision，示例中的 `0` 表示尚未核验，不能进入 verified 状态。`adaptation.level` 建议使用：`faithful`（语义可直接复现）、`equivalent`（MC 等价效果）、`approximate`（明显近似）、`deferred`（尚未实现）。构建时分别输出 registered、implemented、verified、deferred 数量，禁止把有模型和纹理但使用占位行为的法术标成完成。

---

## 五、Minecraft 适配规则

| Noita 机制 | MC 适配 | 必须保持 | 允许变化 |
|---|---|---|---|
| 60 FPS 时间 | 统一换算到 20 TPS，必要时保留分数累计器 | 真实秒数、相对快慢 | 视觉帧级细节 |
| 高频法杖 | 一 tick 内合并弹丸/射线批次 | 法力、平均 DPS、散射统计 | 实体数量 |
| 像素材料 | 方块标签、原版流体、区域云、短寿命显示实体 | 材料类别与交互意图 | 单像素流动 |
| 材料耐久/硬度 | block tag + hardness/resistance + 法术挖掘等级 | 强弱关系 | 精确像素 ray energy |
| 液体导电 | 有界 BFS/邻域采样连接水与金属 | 可传导和伤害风险 | 传播形状 |
| 火/冰/毒 | 原版燃烧、冻结、状态效果 + 自定义伤害 | 伤害类型与联动 | 持续时间可平衡 |
| Healing 负伤害 | 服务端 heal，保留友伤/命中所有者规则 | 可用于恢复生命 | 负数 DamageSource 实现细节 |
| 黑洞/白洞 | 吸引/排斥场 + 受保护的方块处理 | 方向、范围、危险度 | 地形像素吞噬 |
| 传送 | 射线/投射物落点 + 碰撞安全搜索 | 距离和触发方式 | 禁止卡墙/越界 |
| 变形 | allowlist 实体替换或限时伪装 | 随机/混乱/稳定差别 | 不支持的 Boss 免疫 |
| 召唤 | 标记所有者的受限实体 | 生命周期、阵营 | 数量和 AI 简化 |
| 地形破坏 | 配置、权限、领地 API 钩子、方块预算 | 强弱层级 | 默认破坏半径可降低 |

建议第一版单次施法默认预算：最多解释 2048 个 action 节点、生成 128 个逻辑弹丸计划、落地 32 个服务器实体、检查 4096 个方块、修改 512 个方块；嵌套触发深度 16、复制递归层级 2。该数值只是启动基线，必须用阶段 4 基准测试校准。

预算不能只按玩家计数。服务器需要中央 `SpellBudgetManager` 同时维护 per-cast、per-owner、per-chunk、per-dimension 和 global 的 tick/时间窗预算，并限制 NBT bytes/depth/nodes、跨 tick 场任务、离线所有者/召唤物、网络 bytes/packets 和客户端视觉事件。先对完整 EffectPlan 做预算预留，再原子提交法力、次数、冷却和 WandState；如果无法完整预留，则在解释阶段按确定规则生成已标记截断的计划或整体拒绝，禁止提交后随机丢弃半棵树。预算应可配置，但不可突破硬上限；超限要记录限频日志并给出可理解的诊断。

---

## 六、分阶段实施路线

### 阶段 0：冻结基线与建立测试夹具

目标：知道当前能做什么，并为重构建立安全网。

- 记录当前 328 个注册法术、模型、纹理、语言键和行为覆盖率。
- 把测试分为三类：characterization 只记录现状和已知错误，不作为长期期望；Wiki golden 定义目标语义；regression 防止已修复问题复发。
- 为现有 starter wand、Spark Bolt、Bomb、Double Spell、Damage Plus、Trigger、Wand Refresh、Alpha、Gamma 建立带“现状/目标”标签的夹具，不能用快照锁死已知错误。
- 在 `build.gradle` 接入测试 source set、JUnit、选定的属性测试库、Fabric GameTest run/task 和 CI 命令；从本阶段开始加入 dedicated-server class-loading smoke test。
- 给 C2S 施法协议加入版本、序号、状态哈希与服务器速率限制，避免先固化不安全的空包协议。
- 给 NBT 增加 schema version，并准备旧状态迁移策略。
- 输出 `docs/实现覆盖率.md`，区分 registered、visual-only、approximate、verified。

验收：`test`、`build` 与 dedicated-server smoke task 通过；资源清单无未说明的孤儿和缺失；至少 10 个 characterization/golden 测试可重复运行。

### 阶段 1：抽离纯法杖解释器

目标：世界中不生成任何实体也能算出一次 cast。

- 从 `NoitaWandCaster` 抽出 `WandState`、`DeckState`、`WandCastSession`、`ShotState` 和 `ResolvedCast`。
- 把时间统一为 Noita frame 或 duration 类型，在边界统一换算。
- 用确定性 RNG 处理 shuffle、spread、random spell。
- 实现 action step budget 和结构化诊断。
- 暂时用适配器把旧硬编码法术喂给新解释器，保持现有游戏可用。

验收：解释器不 import `net.minecraft.world.*`、`net.minecraft.client.*` 或实体类；相同输入/seed 输出完全一致。

### 阶段 2：完成基础法杖语义

目标：Deck/Hand/Discard、Draw、Wrap、Reload、mana、uses、always cast 与时间结算对齐 Wiki。

- 区分 `initialDraw` 与 `actionDraw(canWrap=true)`。
- 精确处理失败卡重试、末卡失败、Hand 弃牌时扣次数。
- 修改器持续累加到整个 Shot State，不在递归返回时错误恢复。
- Cast Delay 与 Recharge Time 并行倒计时。
- 按 Wiki 实现 shuffle 时机、同一 cast 首次有效 Wand Refresh 不因 reset 强制 reload、第二次调用强制 recharge 的差异。
- Always Cast 单独建模法力、次数与 trigger 特例。

验收：下文“Wiki 黄金测试”第 1 至 6、11、12 项通过；Trigger、Copy 和 recursion 的第 7 至 10 项留给阶段 3，但阶段 2 必须已有明确的 pending fixture。

### 阶段 3：触发树、复制与递归

目标：组合系统能表达 Noita 后期法杖。

- `ProjectileTree` 支持 hit、timer、death 载荷和嵌套载荷。
- Trigger Shot State 与 Base Shot State 隔离；全局 mana/recharge 正确共享。
- 实现 Call/Copy、type-filter copy、Divide、Add Trigger 搜索/Discard 等解释器 primitive，并用 Alpha、Gamma、Tau、Mu、Sigma、Zeta、Phi、Omega、Duplicate、Divide By、Add Trigger 的代表 fixture 验证；本阶段不要求把所有对应 Item 定义宣称为完成，完整 catalog 迁移计入阶段 7。
- 区分 Draw 与 Call；复制目标通常不扣目标 mana/uses，不进 Hand。
- 同时执行递归层级限制、action 总预算、payload 节点预算。

验收：黄金测试第 7 至 10 项以及嵌套 Trigger、Divide By、预算截断通过；复杂组合能以 JSON 调试树展示每张卡的来源、Draw/Call、Shot State、catalog epoch 和截断原因。

### 阶段 4：服务器执行器与伤害系统

目标：解释结果安全地落地到 MC 世界。

- 建立 Projectile/Explosion/Fire/Electricity/Drill/Slice/Ice/Healing/Poison/Curse/Holy 伤害通道。
- 把通用投射物巨类拆为运动组件、碰撞组件、命中效果和载荷释放器。
- 支持 chunk unload/reload、caster 离线、实体序列化、重复触发幂等。
- 所有破坏和友伤经过服务器配置与权限策略。
- 网络只传意图与视觉必要数据，所有伤害、法力、冷却和随机结果由服务器决定。
- 建立第一组性能基准和 per-cast/per-tick 预算仪表；从下一阶段开始，每迁移一个法术族都执行 MSPT、实体数、区块更新和包流量门禁。

验收：专用服务器启动；恶意客户端不能凭包生成法术或伪造法力；触发载荷重载世界后最多释放一次。

### 阶段 5：迁移 Projectile 与 Static Projectile

目标：把现有 167 个投射物从“已登记”推进到“已验证”。

- 按运动族群分批：直线、重力、弹跳、回旋/路径、制导、射线、爆炸物、场/云、召唤物。
- 每族先做 2 至 5 个代表法术，再迁移同族数据。
- 每个法术对照 Wiki 记录伤害类型、速度、寿命、友伤、穿透、爆炸、触发方式和适配差异。
- 对 Nuke、Black Hole、Omega Sawblade 等高风险法术增加专门预算和配置。

验收：每个 `verified` 法术至少有数据验证测试和一个行为测试；不能用纹理存在替代行为验收。

### 阶段 6：迁移 143 个 Projectile Modifier

目标：修正器能组合，而不只是单独演示。

- 分成数值型、轨迹型、制导型、命中型、材质轨迹型、复制/发射型、颜色/视觉型。
- 数值型尽量数据驱动；复杂型使用小型 action executor，避免一个无限增长的枚举 switch。
- 每个修正器至少与单发、Multicast、Trigger payload 三种上下文测试；不适用者写明原因。

验收：关键组合顺序与 Wiki 一致；修正器对共享 Shot State 的传播有成组测试。

### 阶段 7：补齐当前 catalog 差集（本次审查时为 107 个）

优先级建议：

1. 13 个剩余 Multicast：解释器收益高、世界执行成本低。
2. 24 个剩余 Utility：完善传送、视野、召唤和编辑能力。
3. 26 个 Material：先建立材料族映射，再逐个接入。
4. 5 个 Passive：完善持有/装载生命周期。
5. 39 个剩余 Other：条件、复制、Requirement、Divide By 等按风险分批；阶段 3 的 primitive/fixture 不自动等于这些具体定义已经 verified。

验收：catalog manifest 动态计算总数和差集，不把 107 写成永久常量；每个法术都关联来源、定义、executor、测试 ID 与适配说明，并分别报告 registered、implemented、verified、deferred 数量。正式完成口径不允许用 `registered=422` 掩盖 deferred；`approximate` 必须有玩家可见说明。

### 阶段 8：法杖获取、成长与编辑体验

- 依据 Wiki 的 spell tier 和 wand tier 建立战利品/结构/生物群系映射，而不是一次性把 422 个法术塞进创造栏之外的随机池。
- 设计法杖生成器，保持 shuffle 与高属性之间的权衡。
- GUI 显示法杖属性、Always Cast、法术次数、法力、预计 cast tree 和危险预算。
- 加入配置界面：地形破坏、友伤、最大实体/方块预算、解锁方式和管理员黑名单。
- Datagen 负责模型、语言、配方、标签和覆盖率报告，生成文件必须可复现。

验收：新世界中法术有渐进获取路径；服务端可禁用破坏性法术而不破坏存档。

### 阶段 9：性能、多人和朋友间交付

- 建立基准法杖：高速单发、深层 trigger、Divide By、群体制导、大范围材料转换、Nuke。
- 在 20 TPS 目标下压测 1、4、16 名玩家并记录 MSPT、实体数、包速率和区块修改数。
- 加入采样性能日志、超限诊断和管理员命令。
- 执行 dedicated server、断线重连、维度切换、区块卸载、存档升级测试。
- 检查所有纹理、名称、Wiki 摘录和数据是否齐全、可加载、无重复；资源可直接从 Noita 官网或 Wiki 补齐，不执行版权归属或许可证审计。

验收：供朋友使用的 JAR 为 Loom remap 产物；不存在客户端类加载到服务端、无限递归或未限流方块扫描；资源来源至少保留下载 URL，便于损坏后重新获取。

---

## 七、Wiki 黄金测试与对抗性测试

### 7.1 组合语义黄金测试

1. 非乱序法杖从左到右；乱序只在首次使用、reload、wrap、Wand Refresh 等规定时机洗牌。
2. Spells/Cast=2、三枚 Spark Bolt：第一次 2 枚，第二次 1 枚；法杖固有 Draw 不 wrap。
3. Double Spell、Damage Plus、两枚 Spark Bolt：两枚都获得 Damage Plus。
4. 法力不足卡直接进 Discard，继续尝试后卡；最后一张失败卡的 Draw 语义单测。
5. 法术 Draw 在空 Deck 上 wrap 并标记 reload；同一 cast 首次有效 Wand Refresh wrap 且不因该 reset 强制 reload，第二次调用不再 reset 并强制 recharge。
6. Cast Delay 与 Recharge 同时开始，实际等待 `max(delay, recharge)`。
7. Trigger 载荷在施法求值时扣法力；这是由 Draw/载荷预结算模型推出并应通过实机 fixture 验证。未触发不再次结算或退款；Piercing Trigger/Timer 多次释放时不重复收费。
8. Trigger payload 的伤害/速度/散射不污染 Base Shot State；payload cast delay 被忽略，recharge 修改仍生效。
9. Gamma 调用目标 action，但不自动支付目标 mana、扣目标次数或加入 Hand。
10. recursive action 达到层级 2 后停止继续调用 recursive action。
11. Always Cast 通常零法力、无限次数，并位于普通块前。
12. 有限次数 modifier 只有在 Wiki 规定的 Hand 正常弃牌条件满足时才扣次。

### 7.2 属性与模糊测试

- 卡牌守恒：除 Always Cast 临时副本、次数耗尽移除和显式动作外，槽位 ID 在 Deck/Hand/Discard 中恰好出现一次。
- 法力守恒：没有负法力 action 时 mana 不增加；每次正常 Draw 最多扣一次。
- 确定性：同定义、同状态、同 seed、同配置产生相同 `ResolvedCast`。
- 终止性：任意随机法术序列都在 recursion、step、payload、projectile 四个预算内结束。
- 序列化往返：纯领域对象先通过 persistence adapter 转换，再做 NBT/Codec 往返；未知字段可安全忽略或迁移；深度、节点数、字节数超限在分配大对象前拒绝。
- 资源重载：缺失、重复、非法枚举、NaN/Infinity、负寿命、未知引用都给出带 ID 的错误，不让世界半初始化。

### 7.3 世界与安全测试

- 伪造 C2S 包、重复施法包、过期槽位版本、旁观者/死亡玩家施法均被拒绝。
- caster 退出、换维度、死亡、投射物 chunk unload 后载荷不重复释放；进程崩溃恢复的保证范围单独记录，不能把普通 NBT 布尔值描述为 exactly-once。
- protected block、无法破坏方块、领地、出生点、`mobGriefing=false` 按策略生效。
- 递归/Divide By/复制链超限可见地截断，不抛 OOM、不阻塞 server tick。
- 友伤、治疗弹命中自己、反射、穿透、多次碰撞均有幂等和所有者测试。
- 客户端没有该模组时由 Fabric 依赖拒绝连接；版本不一致时给出明确提示。

---

## 八、代码对抗性审查清单

每次实现或迁移法术必须回答：

1. 这是正常 Draw、Call、Copy、Discard 还是直接操作牌堆？是否错误复用了另一个概念？
2. 修改属于当前 Shot State、Trigger Shot State 还是全局 cast 值？退出作用域时哪些值应恢复？
3. 该动作能否 wrap？wrap 是否触发 reload？shuffle 是否发生？
4. 法力何时扣、次数何时扣、Always Cast 是否例外？
5. recursion level 与 total action budget 是否都受控？
6. 同一 payload 能否因碰撞、计时、死亡、存档重载重复释放？
7. 世界查询是否限制半径、实体数、方块数、区块加载和每 tick 工作量？
8. 是否会从 common/server 代码引用 `net.minecraft.client.*`？
9. 网络输入是否全部由服务器重新验证？客户端是否能提交伤害、法力或随机结果？
10. NBT/JSON 缺失、旧版本、未知 ID、非法数值会发生什么？
11. 是否破坏已有物品 ID、存档 NBT 或用户未提交资源？迁移策略是什么？
12. 该法术是 faithful、equivalent、approximate 还是 deferred？Wiki URL、验证日期和差异说明是否齐全？
13. 施法是否遵循 validate/evaluate/reserve/commit/execute 事务？换手、GUI 编辑、重复包和 catalog reload 能否制造竞态？
14. 预算是否同时覆盖 cast、owner、chunk、dimension、global、NBT 和网络，而非只限制单个玩家？

---

## 九、信息来源

### A 级：本项目选定的首要行为规格来源

- [Spells](https://noita.wiki.gg/wiki/Spells) — 422 个法术、8 类、属性与次数/法力/时序规则。
- [Wands](https://noita.wiki.gg/wiki/Wands) — shuffle、Spells/Cast、Cast Delay、Recharge、Mana、Capacity、Spread、Always Cast、Speed Multiplier。
- [Expert Guide: Draw](https://noita.wiki.gg/wiki/Expert_Guide:_Draw) — Deck/Hand/Discard、Draw、Shot State、Wrap 与 Reload。
- [Expert Guide: Calling and Recursion](https://noita.wiki.gg/wiki/Expert_Guide:_Calling_and_Recursion) — Draw、Call、Copy 与递归层级 2。
- [Expert Guide: Discards from the Hand](https://noita.wiki.gg/wiki/Expert_Guide:_Discards_from_the_Hand) — 次数扣减、Always Cast 临时副本、Wand Refresh 例外。
- [Trigger](https://noita.wiki.gg/wiki/Trigger) — hit/timer/death 条件、载荷选择和 Shot State 隔离。
- [Add Trigger](https://noita.wiki.gg/wiki/Add_Trigger) — 目标搜索、免费 modifier、失败语义和时序影响。
- [Greek Spells](https://noita.wiki.gg/wiki/Greek_Spells) — Alpha、Gamma、Tau、Mu、Sigma、Zeta、Phi、Omega 的目标与 Draw 特征。
- [Spell Duplication](https://noita.wiki.gg/wiki/Spell_Duplication) — 复制 Hand 与复制后继续 Draw。
- [Damage Types](https://noita.wiki.gg/wiki/Damage_Types) — Noita 伤害类型及抗性/状态交互。
- [Materials](https://noita.wiki.gg/wiki/Materials) — 固体、普通/魔法液体、气体、粉末、有机物/其他材料与耐久/硬度。
- [Wand and Spell Tiers](https://noita.wiki.gg/wiki/Wand_and_Spell_Tiers) — tier 与区域法术池。

这些页面是社区 Wiki，不是 Nolla Games 的官方引擎规范。2026-07-13 核验到的关键 revision ID 应进入 spell metadata：Spells `84025`、Wands `84538`、Expert Guide: Draw `83853`、Calling and Recursion `84517`、Discards from the Hand `83045`、Trigger `84174`、Add Trigger `83880`、Damage Types `84814`、Wand and Spell Tiers `83457`。`Greek Spells` 只作为总览；`Spell Duplication` 有 stub 标记，`Materials` 有 cleanup 标记，相关完整行为必须由固定版本实现证据和测试补强。

### B 级：版本固定的实现证据

- 本地 `Documents/NoitaMechanics/_source_cache/noitadata/` 中的 `gun.lua`、`gun_actions.lua`、投射物 XML 与翻译 CSV。
- 本地 `Documents/NoitaMechanics/00_来源与方法/法术动作索引_422项.csv`。

当前本地缓存未记录 `NathanSnail/noitadata` 的来源 commit。为保证数值和行为可复现，后续补充证据时应记录目标 Noita 版本、镜像 commit 和派生过程。当 B 级来源与 Wiki 的玩家可观察行为冲突时，本项目默认以 Wiki 行为为准；若确认 Wiki 有误，必须在开发文档中记录具体页面/revision、数据版本、复现步骤和采用 datamine 行为的理由。

### 非商业资源获取策略

- 本项目仅供朋友间非商业游玩，可直接从 [Noita 官网](https://noitagame.com/)、[Noita 官方 Modding 页面](https://noitagame.com/modding/) 或 [Noita Wiki](https://noita.wiki.gg/) 下载所需图标、纹理、截图、法杖图、参考文本及其他资源。
- 不要求执行版权归属、许可证兼容性、署名证明或逐文件权利审计，也不因这些事项阻断开发、构建或朋友间分享。
- 只需为下载资源保留简单的 `source_url`、原始文件名和下载日期，目的仅是方便更新、去重与重新下载，不是版权审计。
- 下载脚本必须限制在明确的官网/Wiki URL，写入前检查目标路径，不能覆盖用户已经修改的本地资源。

---

## 十、争议与不确定性

1. Wiki 是社区维护的机制权威，不是 Noita 闭源引擎规范；极端递归、帧顺序和 C++ 内部边界仍可能只能通过 datamine 与游戏实验确认。
2. 422 是 Wiki 当前法术目录数量；后续游戏或 Wiki 更新可能改变，构建覆盖率应绑定核对日期而不是永久写死。
3. MC 的 20 TPS、3D 方块世界、实体碰撞和服务端权限模型决定了部分效果只能等价适配，不能声称逐像素还原。
4. 当前范围明确为朋友间非商业使用，因此资源获取不设版权审计或发布阻断门槛；官网/Wiki 资源可直接使用。
5. 若未来目标变为公开发布或商业化，应由用户另行决定是否重新引入资源审查流程；当前开发代理不得自行扩大该流程。

---

## 十一、立即执行的下一批任务

1. 先提交/备份当前大量未提交的法术资源，建立可回退基线。
2. 新建纯 Java `wand/eval` 模块和上述 12 个 Wiki 黄金测试，不先增加第 316 个法术。
3. 修复共享 Shot State 语义，使 `Double Spell + Damage Plus + 2x Spark Bolt` 两枚弹丸都继承增伤。
4. 把法杖固有 Draw 与 action Draw 分离，锁定 wrap/reload 差异。
5. 给载荷树、复制、方块操作和实体生成加入硬预算。
6. 生成当前 328 个法术的覆盖率报告，确定哪些只是视觉/数值占位。
7. 建立轻量资源来源表并直接补齐缺失图标，只记录 URL、原始文件名和下载日期，不做版权归属检测。
8. 为 catalog snapshot、施法事务、NBT 载荷和多层预算先写架构决策记录，再开始大规模迁移。
