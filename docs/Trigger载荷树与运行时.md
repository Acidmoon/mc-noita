# Trigger 载荷树与运行时（G03）

## 目标与边界

G03 将 Trigger 的后续法术变成一次施法时就完成的冻结树。它的目标
是保留 Noita Trigger 的可观察付费与 Shot State 作用域，同时使 Minecraft
实体运行时只响应碰撞、计时和终止事件。

行为规格参考：Noita Wiki 的 [Trigger](https://noita.wiki.gg/wiki/Trigger)
（本项目记录 revision `84174`，2026-07-13）。Minecraft 的 20 TPS 实体
碰撞使该映射属于 `equivalent`：Timer 与碰撞以服务器 tick 顺序运行，
不是原游戏逐帧像素碰撞的逐像素复现。

G03 不实现 Greek、Duplicate、Divide By 或 Add Trigger 的完整目标搜索
策略；那些 Copy/Call 规则保留给后续 Goal。

## 数据流

```text
WandCastSession（纯 Java）
  DrawRequest.payload -> 扣法力、Hand/Discard、次数结算
  ShotState.EMPTY -> 独立 payload Shot State
  TriggerPlan / PayloadPlan / ProjectilePlan（冻结 nodePath）
       |
       | ResolvedCast: catalog epoch/hash
       v
MinecraftEffectExecutor（服务器提交后）
  executionId + catalog 元数据 -> NoitaProjectilePayload NBT 适配层
       |
       v
SparkBoltProjectileEntity / BombEntity
  TriggerPayloadController.accept(event) -> ReleaseDecision
       |
       v
TriggerPayloadSpawner（逐节点、隔离失败地生成冻结子实体）
```

`spell.plan` 是唯一的纯计划模型。`NoitaProjectilePayload`、
`NoitaTriggerPlan` 和 `NoitaPayloadPlan` 仅是 NBT/实体适配器：它们保存
已经冻结的机械数值，绝不按法术 ID 查询 `SpellCatalog`。

## 计划模型

- `ProjectilePlan.nodePath` 是静态计划路径，例如 `root/0`。
- `TriggerPlan` 包含模式、Timer 延迟、payload 深度、释放策略和一个
  `PayloadPlan`。
- `PayloadPlan` 是一个独立 payload shot，可包含多个投射物，例如多重
  施放后的所有子投射物。
- `TriggerMode.EXPIRATION` 是内部终止语义；旧 `DEATH` 仍被显式解析并
  归一化为 `EXPIRATION`。

`WandCastSession.beginTrigger` 在进入 payload 前保存外层 `ShotState`，
用 `ShotState.EMPTY` 求值 payload，离开后恢复外层。因此 payload 的
伤害、速度、散射、寿命和 Cast Delay 不泄漏到外层，payload Recharge
仍写入全局 recharge 累计器。Payload 的 `DrawRequest` 使用同一 Deck、
Hand、Discard、mana 与次数事务，所以未命中不退款，多次 Piercing 命中
也不重复扣费。

## 运行时状态机

`TriggerPayloadController` 是 Spark Bolt 与 Bomb 共用的服务端控制器。
它只接受三类事件：

- `COLLISION`：有效方块或实体碰撞。Hit 与 Timer 可以释放；同一
  `CollisionKey` 不会因 `onEntityHit` 和 `onCollision` 回调链重复释放。
- `TIMER_EXPIRED`：Timer 到期一次性释放。Timer 可在此前的多个有效
  Piercing 碰撞中已经释放过。
- `TERMINATED`：仅 `NATURAL_EXPIRY`、`TERMINAL_COLLISION`、`KILLED`
  可以释放 Expiration 载荷。`UNLOAD`、`INVALID_DATA`、`ADMIN_CLEANUP`
  绝不释放。

状态持久化 `releaseSequence`、`timerElapsedTicks`、`timerExpired`、
`expirationReleased`、`inert`、最近一个有界 `CollisionKey` 与剩余运行预算。
Timer 不依赖实体 `age`，所以存档重载会继续原有延迟而不是重新计时。控制器在返回
`ReleaseDecision` 前先写入内存状态和预算，随后才生成子实体。因此某个
兄弟节点生成失败不会让整个事件重试，也不会重复已成功的兄弟节点。

非 Piercing 碰撞按 `COLLISION -> TERMINATED -> discard` 顺序运行；实体
在 `super.tick()` 后若已移除，会直接返回，避免同 tick 的 Timer 再次释放。
`SparkBoltProjectileEntity` 的自然寿命、特殊碰撞、Summon/Fizzle/静态
Blast、投射物领域销毁、显式爆炸和 `KILLED` 移除都走命名终止路径。Bomb
保存 owner UUID 并在服务器运行时解析；扫掠方块/实体接触与落地 MINE 的
近距实体接触均先提交有效 `COLLISION`，所以正常重载不会丢失子载荷的
所有者或把自然 fuse 误写成 Hit。

## 身份与预算

每个接受的 cast 在 `MinecraftEffectExecutor` 中得到服务器权威
`executionId`。每个计划节点还携带 catalog epoch/hash；扇出与重复
释放的实体从静态路径派生实例路径：

```text
root/0/release/2/entity/1
```

这使诊断、CollisionKey 和正常 save/reload 状态不因同一计划的多个实体
而冲突。

纯求值分别限制 action、projectile、payload node、payload depth 和递归
深度，并在提交牌堆、法力和冷却前计算每棵树至少完整释放一次的物理实体
与事件占用。默认每次 cast 最多 32 个实体与 32 个 release event；例如
`root + 32 payload` 会在求值期拒绝，而不会先扣费再在命中时吞掉载荷。
运行时的 `TriggerRuntimeBudget` 再限制合法 Piercing 重复释放。一次释放后，
控制器先扣除本事件成本，为每个子树保留其首次嵌套释放所需的最低余额，
再把剩余容量拆分给仍然存活的父实体和子实体；父子不会持有同一份余额，
也不会获得新的默认预算。预算耗尽时节点变为 inert 并停止后续释放，服务端
按执行 ID、节点、所有者、维度、模式、序号和预算类型做有界限频诊断。

这是树内、正常服务器生命周期的上限。跨玩家、区块、维度和全局时间窗
的集中预算管理仍属于后续服务器预算工作。

## NBT 与兼容性

Schema v3 的冻结 payload 保存：

- 执行 UUID、实例 nodePath、catalog epoch/hash；
- 完整 `TriggerPlan` / `PayloadPlan` 树和所有机械参数；
- Timer 延迟、释放策略、payload 深度；
- 实体的 `TriggerRuntimeState`（含 Timer 已过 tick）和剩余预算；
- Bomb 的 owner UUID（零 UUID 表示显式无主）。

解码使用共享 `DecodeContext`，限制 16 层语义载荷深度、128 个逻辑
Projectile 节点、32 个直接子项、64 个结构列表项、512 字符字符串、128 KiB payload NBT、
136 层/16,384 节点 entity NBT 和 256 KiB complete entity NBT。计划
bookkeeping compound 受独立的结构 NBT 限制，不会挤占逻辑 projectile
节点上限。v3 的每个 frozen node 必须有完整机械字段、身份、TriggerPlan
与 runtime budget；v3 根节点也要求所有后代继续使用 v3，并与根共享非零 execution
UUID 和 catalog epoch/hash。实体 v3 还必须有 FrozenPayload 和 TriggerRuntimeState，
不得退回 legacy/fresh 状态。任一子节点的未来或降级 schema、非法枚举、重复路径、
不一致的身份、非法 item path 或损坏字段都会拒绝整棵树，实体安全移除而不释放。
执行器可达的数值另有硬上限，例如爆炸半径 32、trail light 16 和 bounce 32。
v2 的 `DEATH` 显式迁移为 `EXPIRATION`；旧
`TriggerPayloadReleased=true` 保守迁移为 inert，优先避免重放，即使这会
停止旧 Piercing Trigger 的后续释放。

v3 实体只写 `FrozenPayload` 与 `TriggerRuntimeState` 这一份权威状态，
不再额外写无法表达 payload shot 边界的扁平 `TriggerPayloads` 投影。这样
接近 128 KiB 的合法冻结树不会在实体 NBT 中被完整复制一次而越过 256 KiB 上限。
旧 in-memory 构造器在服务器实体创建边界会被递归赋予新的 execution UUID、
catalog 标识和唯一后代路径，随后才能按 v3 保存；v3 解码不接受全零 legacy UUID。
纯求值也会在 `WandState` 提交前拒绝超过 64 项的 modifier effect，避免产生
只能在实体首次存档后被解码器拒绝的已付费计划。

G03 的保证范围是正常 save/unload/reload 生命周期中的 at-most-once 最终
释放。进程在子实体生成后、父实体状态落盘前崩溃仍可能重复；要承诺
exactly-once 需要持久执行账本与事务，不在本 Goal 范围内。

## 测试与扩展

- `G03TriggerPlanTest`：cast 时支付、Shot State/时序隔离、嵌套路径与
  node/depth 预算。
- `TriggerPayloadControllerTest`：Hit 去重与 Piercing 多次碰撞、Timer
  碰撞加到期、Expiration 一次性、运行时预算拆分。
- `FrozenTriggerPayloadNbtTest`：最大深度/宽树往返、损坏子树、v3 缺字段、
  重复路径、混合 schema/identity、未来 schema、数值/路径安全、`DEATH`
  迁移、64 项 modifier 列表、legacy runtime 路径绑定和运行状态往返。
- Fabric GameTest：真实 Spark/Bomb 方块与实体 Hit、连续 Piercing Hit、
  嵌套 Trigger、MINE 近距 Hit、0/1 tick Timer、同 tick Piercing 碰撞加
  Timer 到期、Timer 到期前后 NBT 重载、natural/KILLED Expiration、legacy
  runtime identity 重载，以及模拟 `UNLOADED_TO_CHUNK` 后重载再终止。

新增投射物应只把有效碰撞、Timer 与命名终止事件交给控制器。不得从实体
回调访问牌堆、法术目录或玩家法力；新增 world effect 必须在
`TriggerPayloadSpawner` 之后使用冻结 plan 参数执行，并遵守后续世界级
预算与权限策略。
