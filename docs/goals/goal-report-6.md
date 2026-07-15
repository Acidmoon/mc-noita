# Goal Report 6: G05 服务端执行基础设施

## 目标

G05 将服务器施法收敛到 `Validate -> Evaluate(snapshot) -> Reserve -> Commit -> Execute`，并为目录快照、多层预算、世界操作、伤害、类型化效果和跨 tick 持久任务建立可验证的服务端边界。

## 产物

- `SpellCatalogService` 持有原子不可变 snapshot；规范化 SHA-256、epoch、只覆盖已注册 ID 和同内容不递增 epoch 均已实现。`SpellCatalogResourceReloadListener` 读取受限数据包覆盖字段，并在整批解析或校验失败时保持旧 snapshot。
- `CastTransaction` 绑定玩家、手、槽位、物品状态、revision、catalog epoch/hash 和 sequence；所有 decode/evaluate/write 都在 `ItemStack` 副本上完成。预算预留、二次 binding 校验和 replacement 写入失败均保持真实法杖 NBT 不变。共享预算账本统一使用单调服务器 tick，冷却仍使用世界时间。
- `SpellBudgetManager` 覆盖 cast、owner、chunk、dimension、global、tick/window 和 in-flight 额度。已提交 reservation 可把 `PERSISTENT_JOBS` slice 转移给 child lease，避免根 cast 关闭后提前释放长期容量。
- `RootTriggerBudgetAllocator` 在 reservation 前按中央 `AUTHORITATIVE_ENTITIES` / `TRIGGER_RELEASES` ceiling 为根投射物与其冻结 Trigger 树分配 runtime budget；配置可高于 legacy 32/32，但运行时硬上限为 128。无法完整分配时在 WandState commit 前拒绝，Trigger 后代实体在实际释放位置重新申请中央额度，避免把同一实体重复记入根 cast。
- `EffectPlan` 的 Projectile、Sound、Recoil、Explosion、Teleport 和单方块视线 BREAK 均走类型化 executor。Field、泛型 Summon、区域 PLACE/REPLACE 仍显式 deferred/rejected，不会以注册节点冒充行为完成。
- `DamageProfile` 引入多通道直接伤害，旧标量稳定映射到 `PROJECTILE`；`SpellDamageService` 统一所有者归属、友伤/自伤、无敌帧和有害后续效果门控，`HealingService` 使用正值治疗。冻结 payload NBT 从 v3 经显式链迁移到 profile 的 v4，再由公共 schema v5 持续管理。
- `WorldMutationService`、`WorldQueryService` 与 `WorldMutationPolicy` 承担危险世界调用。Bomb 的 block ray 先经过完整 loaded-chunk 包络与 DDA `BLOCK_CHECKS` gate；运行时跨 chunk 查询以原子请求向每个覆盖 chunk 保守预留完整上限。即时 Explosion 在 commit 前冻结完整实体查询包络，并将每个 chunk 的完整 `ENTITY_SCANS` slice 同时计入 owner、dimension 和 global 总账。G05 尚未实现 terrain partition，因此可能跨 chunk 的 Explosion terrain 与代表性 BREAK 视线在 reservation/commit 前拒绝，而不是在写入 WandState 后静默部分执行。未绑定 identity 和缺失预算 fail closed。临时光是唯一明确的短生命周期 `UNTRACKED` 例外，且只可写空气或 light block。
- `SpellJobPersistentState`、v5 codec、`SpellJobManager`、handler registry、Overworld `PersistentState` store 和 `SpellJobServerService` 已接通。恢复只重试同时由冻结节点与 live handler 声明为幂等的 `RUNNING` step；未注册 job、损坏数据和非幂等恢复均 inert。当前没有内置长期 job handler，且一个 root cast 暂限一个持久任务节点。

## 验证

- 基线 `rtk .\gradlew.bat test --no-daemon --console=plain` 通过；随后针对 `PlanBudgetRequestFactoryTest` 的强制重跑通过，覆盖同区块 BREAK 冻结、跨 chunk BREAK 拒绝和整数爆炸边界。
- 最终 `rtk .\gradlew.bat verifyG05 --rerun-tasks --no-daemon --console=plain` 于 2026-07-15 通过：174 项 JUnit 为零失败/错误/跳过。该门禁还重新执行 GameTest 源集、真实 Fabric GameTest、架构检查、专服 smoke 与生产 remap build。
- 真实 Fabric GameTest 报告为 33/33 通过，包括预算拒绝 NBT 不变、编辑/旧 binding 拒绝、reservation 后换栈拒绝、同队 HEAL 与 hostile 拒绝，以及 Bomb 的跨 chunk seam 命中。
- `rtk git diff --check` 通过。构建仍输出既有 Gradle 9 兼容性提示和 Fabric GameTest mock-player API 的弃用警告；两者均未使门禁失败。

## 限制与后续

- 根投射物只消费已提交的 local slice；延迟 Trigger/实体世界操作必须在实际位置重新申请中央额度。这样不会把同一根投射物或 Trigger 后代重复记入根 cast，但运行时拒绝仍可能受当时的 owner、chunk、dimension、global、tick/window 压力影响。
- 即时 Teleport 已冻结其确定目标 chunk；即时 Explosion 已冻结并预留跨 chunk 实体扫描包络。跨 chunk terrain partition 仍未实现，故可能跨 chunk 的 Explosion terrain 与代表性 BREAK 视线会在事务预检中整体拒绝。后续若实现分区，必须同时增加 per-chunk 全有或全无 reservation、块候选覆盖和 GameTest。
- Field、泛型 Summon、区域 BlockMutation 和全部具体跨 tick 法术族仍属于后续 G06+ 迁移工作。任何新持久 handler 都必须追加 save/reload、owner disconnect、chunk unload、预算拒绝和非幂等恢复的 GameTest。

## 后续指导

G06 迁移具体投射物或世界法术时，必须先判断它的目标和 chunk 包络是否可在 commit 前冻结。可冻结的跨 chunk 查询应完整预留并传给 executor；不能分区的 terrain 操作应保持预检拒绝，不能退回到实体内的直接世界调用或提交后静默跳过。新 long-lived job 还必须注册精确 handler，冻结 mechanics/catalog metadata，并证明 owner、chunk、预算和恢复路径。
