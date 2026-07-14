# Copy 调用与递归（G04）

## 责任与边界

G04 把 Draw 与直接调用彻底分开。普通 Draw 仍由 `WandCastSession.draw` 负责法力检查、次数检查、Hand 插入和最终 Hand-to-Discard 次数结算；Call、Copy、Divide 与 Add Trigger 先通过 `TargetQuery` 选择目标，再由 `InvocationPolicy` 明确是否允许目标 Draw、是否检查目标法力/次数、是否进入 Hand，以及是否恢复全局状态。

纯解释器输入新增服务器生成的 `ExternalSpellPool`。它只含其他快捷栏法杖的稳定法术 ID；客户端网络包不能提交或覆盖 Zeta 候选。`MinecraftExternalSpellPoolAdapter` 是唯一读取玩家物品栏和 Registry 的边界，`wand.eval`、`spell.action` 与 `spell.plan` 继续不依赖 Minecraft。

## 调用模型

- `InvocationKind` 区分 `DRAW`、`CALL`、`COPY`、`DIVIDE` 与 `ADD_TRIGGER`。
- `CallFrame` 保存调用者、目标、递归层级、Divide iteration、Draw 抑制深度、Shot scope、payload `nodePath` 和 trace path。
- `TargetQuery` 声明 Deck/Hand/Discard/External 来源、方向、类别筛选、排除项和数量上限。Alpha、Gamma 与 Tau 不再由扩张的枚举和特殊分支表达。
- `InvocationPolicy` 默认让目标不支付 mana、不进入 Hand、不自动扣次数。筛选 Greek Copy 还会恢复复制过程中的 mana、Cast Delay 和 Recharge，但保留 Shot State 与投射物计划。
- `ActionInvocationResult` 向外传播嵌套 Divide 的最大 iteration，避免用全局临时整数猜测弃牌范围。

正常 Draw 的 frame 从递归层级 0 开始。调用非 recursive 目标保持层级；调用 recursive 目标增加 1；当前层级已经为 2 时跳过新的 recursive 目标并写入 `RECURSIVE_CALL_LIMIT`。递归层级、Trigger payload depth、Divide iteration、action budget 与实体预算是互相独立的计数器。

## Draw 抑制与状态恢复

`dont_draw_actions` 映射为 `drawSuppressionDepth` 计数器。禁止 Draw 的调用进入时递增，退出时在 `finally` 中递减；嵌套 Copy 不会提前解除外层抑制。所有显式 `DrawAction` 在执行前检查计数器，投射物生成、Shot State 修改和 TriggerPlan 构造仍可执行。

Mu、Phi 和 Sigma 的目标快照来自 Discard、Hand、Deck，分别筛选 Projectile Modifier、Projectile 与 Static Projectile。复制过程的 mana 和时序被恢复，复制产生的 Shot State 修正与计划保留；Mu/Sigma 最后 Action Draw 1，Phi 不 Draw。Omega 排除 Discard/Deck 中的 Wand Refresh，并只调用 Hand 中非 recursive action。

## Shot scope 与延迟冻结

每个 Base Shot 和 Trigger payload Shot 都持有一个 `MutableShotScope`。`MutableProjectile` 仅保存基础 `ProjectileDefinition`、scope 引用、稳定 `nodePath` 和确定性 spread sample；`ProjectilePlan` 在整个 cast 结束时读取该 scope 的最终 `ShotState`。

因此 Divide 在调用目标后施加的伤害、爆炸半径和 `patternDegrees` 会统一作用于同一 Shot 中已经加入计划的投射物。Payload 使用独立 scope，G03 的 TriggerPlan、payload 深度、execution ID 和运行时结构没有改变。

## 具体法术

- Alpha：Discard 第一张、Hand 第一张、Deck 第一张。
- Gamma：Deck 最后一张，Deck 空时 Hand 最后一张；不回退 Discard。
- Tau：执行前快照 Deck 前两张并依次 Call。
- Zeta：用命名 RNG 从 `ExternalSpellPool` 选择一个目标，禁止目标 Draw，然后 Action Draw 1。
- Duplicate：快照开始时的 Hand 数量，跳过自身，允许目标 Draw；完成后增加 20 帧 Cast Delay、20 帧 Recharge，再 Action Draw 1。Always Cast 仍执行复制体，但永久卡的末尾单 Draw 被抑制。
- Divide 2/3/4/10：使用 1-based `Deck[iteration]`，阈值分别为 5/4/4/3；第一次复制禁止 Draw，后续复制允许 Draw；目标 mana 不支付，有限次数整次手动扣一次。最外层按返回的最大 iteration 弃置 Deck 前缀。伤害惩罚分别为 -0.2/-0.4/-0.6/-1.5，爆炸半径惩罚为 -5/-10/-20/-40，发射 pattern 设为 5 度。
- Add Trigger/Timer/Expiration：扫描 Deck 前缀，免费调用可用 Modifier 并禁止其 Draw，跳过 Passive/Other/Multicast，要求目标具有显式 `relatedProjectile`。有合法 payload 时直接冻结目标投射物并用 G03 payload Draw 1 预结算；没有 payload 时免费 Call 目标。Timer 固定 20 Noita 帧。

## Trace、预算与失败

`ResolvedCast.trace` 提供稳定 `EvaluationTrace`，记录来源/目标、牌堆、递归层级、iteration、Draw 策略、`nodePath`、action budget 和跳过原因。`toJson()` 只输出稳定值，不写入运行时投射物 NBT。

action、projectile、payload、payload depth、首次实体树和递归预算仍在 WandState 提交前统一验证。预算异常返回原始 WandState 和空计划，因此 Copy/Divide/Add Trigger 不会留下部分 mana、次数或弃牌提交。

## Minecraft 适配与资源

这些调用 primitive 属于 `faithful` 的牌堆/调用模型；实际投射物世界行为继续采用各法术现有 adaptation 等级。新增 13 个法术物品后目录为 328 项。图标于 2026-07-14 从 Noita Wiki 下载，脚本为 `tools/download-g04-icons.ps1`，来源均为 `https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20...png`，原始文件名保留在脚本映射中。

当前限制：Add Trigger 使用目录中首个 `AddProjectileAction` 作为 `relatedProjectile` 的机械定义；未来数据目录应把 related projectile 解析为独立、可验证的投射物定义，而不是依赖 legacy action 列表。跨玩家/区块/维度/global tick 的集中预算预留仍属于后续服务器预算工作。
