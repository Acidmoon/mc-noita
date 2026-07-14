# Goal Report 5: G04 Copy、递归、Divide 与 Add Trigger

## 目标

G04 将 `WandCastSession` 中简单的首/末卡 Call 升级为可表达目标查询、Draw 抑制、递归层级、状态恢复、手动次数、Divide iteration 与 Add Trigger 搜索的统一纯解释器框架。G03 的冻结 TriggerPlan、稳定 nodePath、execution identity、NBT v3 和运行时释放控制保持不变。

## 产物

- 新增 `InvocationKind`、`TargetQuery`、`InvocationPolicy`、`CallFrame`、`ActionInvocationResult`、`ExternalSpellPool` 与 `EvaluationTrace`。旧 `CallSelection` 仅保留为兼容桥，新的 evaluator 分派不再依赖它。
- `drawSuppressionDepth` 用作用域计数器实现 `dont_draw_actions`，所有退出路径在 `finally` 中恢复；嵌套 Copy 不会提前解除外层抑制。
- 递归由 CallFrame 栈拥有：正常 Draw 从 0 开始，非 recursive Call 保持层级，recursive Call 增加层级，层级 2 后只跳过新的 recursive 目标并生成结构化诊断。action、payload、projectile、entity 和 recursion 预算保持独立。
- `MutableProjectile` 暂存基础 `ProjectileDefinition`、Shot scope 与 spread sample，在 cast 末尾统一读取 scope 的最终 ShotState。Divide 后置伤害、爆炸半径和 pattern 惩罚因此能作用于同一 Shot 的既有投射物，payload Shot 仍隔离。
- Alpha、Gamma、Tau、Omega、Mu、Phi、Sigma、Zeta、Duplicate、Divide 2/3/4/10 与 Add Trigger/Timer/Expiration 已映射为显式 action。Greek 目标不支付目标 mana、不进入 Hand、不自动扣目标次数；Mu/Phi/Sigma 恢复复制过程的 mana/时序并保留 Shot State。
- Duplicate 使用开始时 Hand 快照、跳过自身、允许目标 Draw，完成后增加 20 帧 Cast Delay/Recharge，再按作用域规则 Draw 1；Always Cast Duplicate 不再整张跳过。
- Divide 使用 1-based Deck iteration、源码阈值 5/4/4/3、第一次 Draw 抑制、后续 Draw、整次手动扣一次目标次数、嵌套最大 iteration 返回和最外层 Deck 前缀弃置。预算失败返回原始 WandState。
- Add Trigger 搜索 Modifier/Passive/Other/Multicast 前缀，免费 Call 可用 Modifier，要求显式 `relatedProjectile`，手动扣目标次数。有 payload 时用目标投射物定义直接建立 G03 TriggerPlan 并预结算 payload Draw 1；无 payload 时免费 Call 目标。Timer 固定 20 Noita 帧。
- `MinecraftExternalSpellPoolAdapter` 只在服务器扫描其他快捷栏法杖，排除当前槽、空槽和未知项；Zeta 使用命名确定性 RNG，客户端不能提交候选列表。
- 注册 13 个 G04 法术物品，增加英文/中文语言键、模型与 80×80 Noita Wiki PNG 图标。目录从 315 更新为 328；下载脚本记录精确 Wiki URL、原文件名和日期，并拒绝覆盖已有纹理。
- 修复目录生成器对 hashtable 的无效属性排序，改为显式 registry ID 键排序；连续两次生成的 catalog/report SHA-256 完全一致。
- 新增 `docs/Copy调用与递归.md`，更新纯解释器、测试、路线与覆盖率文档，并新增 `verifyG04`。

## 测试与验证

- `G04InvocationSemanticsTest` 覆盖 Gamma Deck/Hand/Discard 规则、目标 mana/次数/Hand、递归 0/1/2、嵌套 Draw 抑制、Duplicate 快照/顺序/时间、Greek 状态恢复、全部 Greek 在 Trigger payload Shot 中的选择与递归、Zeta 外部池确定性、Divide 延迟冻结/嵌套 iteration/次数/弃牌/原子预算拒绝、Add Trigger 三种模式和无 payload 回退。
- 100 个 seed 的循环检查相同输入输出与 trace 完全一致、每张卡仍正好位于一个牌堆，并在预算内终止。
- Fabric GameTest 增至 27 项：新增真实法杖 Add Trigger 撞墙后通过 G03 控制器释放 frozen payload，以及嵌套 Divide 不阻塞 tick、不超过实体上限。
- 初始同步时第一次 `verifyG03` 的既有 `projectilePayloadSaveReload` 场景出现一次时序失败；未修改该 G03 运行时后，后续独立 `runGametest` 与最终全量门禁均通过，包括该场景。
- `./gradlew.bat verifyG04 --rerun-tasks --no-daemon --console=plain` 通过，21/21 Gradle 任务执行成功，真实 Fabric GameTest 27/27、专服 smoke、目录、JUnit、GameTest 源集和生产 remap build 全部成功。
- `git diff --check` 通过；纯 evaluator 包静态扫描未发现 Minecraft/Fabric/ItemStack/NBT/World/Registry 依赖。

## 保证与限制

G04 的牌堆选择、Call/Copy 收费策略、递归与 Draw 抑制属于 `faithful` 模型；Minecraft 投射物和世界效果仍沿用每个法术现有 adaptation 等级。所有 Copy/Divide/Add Trigger 计划仍必须在 WandState 提交前通过 action、projectile、payload、payload depth 和首次实体树预算。

Legacy 目录当前用目标定义中的首个 `AddProjectileAction` 作为 `relatedProjectile` 的机械定义。后续数据目录应将 related projectile 提升为独立、可验证的 catalog 字段，支持一个 action 对应多个 related projectiles，而不是继续依赖 legacy action 列表。跨玩家、区块、维度和 global time-window 的集中预算预留仍未完成。

## 对后续开发的指导

下一 Goal 应在 G04 primitive 上实现剩余 Requirement/条件/随机世界效果，而不是重新引入特殊 Call 分支。任何新 Copy 法术都必须声明 TargetQuery 与 InvocationPolicy，并在 Base Shot、Trigger payload Shot、递归边界和预算拒绝四种上下文中测试。

目录层下一步应把 `relatedProjectile`、Noita action ID、Wiki revision、验证日期和 adaptation status 迁移到校验后的数据定义，并让资源重载原子地产生新 epoch/hash。服务器预算工作应增加 per-owner、chunk、dimension 和 global reservation；G04 的单 cast 原子拒绝是其前置条件，不是替代品。
