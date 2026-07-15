# Catalog 快照与资源重载

## 责任

`com.mcnoita.catalog.SpellCatalogService` 是服务端施法使用的唯一目录快照入口。它持有一个原子 `CatalogSnapshot`，其中包装纯 `SpellCatalog` 的 epoch、SHA-256 和不可变 definitions。`ResolvedCast` 已经把该 epoch/hash 冻结进结果和后续持久化投射物；执行阶段不得按法术 ID 重新读取当前目录。

`LegacySpellCatalogAdapter` 仍是 Registry 到纯 definitions 的兼容边界。它的 `createDefinitions()` 在所有内置 Item 注册完成后返回不可变映射；`createCatalog()` 保留给旧测试和兼容调用方，但不再使用 JVM `Map.hashCode()`。

## 启动与顺序

`MCNoita.onInitialize()` 在 `ModItems.register()` 后调用 `SpellCatalogService.initializeFromLegacy()`。首个快照使用 epoch `0`。`NoitaWandCaster` 在每次求值时读取当前快照的纯目录，因此不会重新扫描 Item Registry。

服务在建立快照时按完整 registry ID 的字典序固定 definitions 顺序。该顺序也是 `SpellCatalog.candidates()` 的候选顺序，必须稳定，否则相同随机 seed 可能选择不同法术。

## 原子重载契约

`SpellCatalogService.reload(Map<String, SpellDefinition>)` 接受覆盖项而非新的 Item 注册：

1. 校验覆盖项的 key 与 definition ID 一致，且每个 ID 都来自启动时捕获的内置 ID 集合。
2. 每次重载都从启动时冻结的 built-in definitions 重建局部 map，再应用本轮完整 override 集合，重新校验并计算规范化 SHA-256；删除文件或字段会回退到 built-in 值，绝不从前一 snapshot 继承。
3. 内容哈希相同则保留原 `CatalogSnapshot` 和 epoch。
4. 内容不同才构造 epoch 加一的新快照，并以一次原子引用写入发布。
5. 校验失败时不替换当前快照，调用方可从 `ReloadResult.errors()` 取得原因。

规范化哈希覆盖 `SpellDefinition`、全部密封 `SpellAction`、投射物、Shot Modifier、时序、Trigger、TargetQuery 和所有影响求值的集合/列表字段。目录 ID、TargetQuery 的集合字段会排序；语义有序的 action、effect 和 source 列表保持原顺序。

`SpellCatalogResourceReloadListener` 已在 `SpellCatalogService.initializeFromLegacy()` 后注册为 Fabric `SERVER_DATA` listener。它读取所有数据包中的
`data/<namespace>/spell_overrides/<name>.json`，先解析整批资源，再只用一轮
`SpellCatalogService.reload(...)` 发布。任一 JSON、字段或 ID 错误都会记录诊断并保留旧 snapshot；listener 不会注册 Item，也不会在失败时发布半个目录。

G05 的 override 语法刻意很小：每个文件必须包含完整的 `id`，并且只能覆盖
`mana_cost`、`recursive`、`use_consumption_policy` 和 `related_projectile`。例如：

```json
{
  "id": "mc-noita:spark_bolt",
  "mana_cost": 8,
  "recursive": false,
  "use_consumption_policy": "WHEN_PROJECTILE_SHOT",
  "related_projectile": "spark_bolt"
}
```

分类和完整 action tree 仍从启动快照冻结。它们需要带 field-path 诊断的完整、版本化 action codec 后才可开放；在此之前 listener 明确拒绝 `actions` 等未知字段，避免不完整 JSON 改变纯 evaluator 行为。

## 测试

`SpellCatalogServiceTest` 覆盖插入顺序无关的稳定 SHA-256、有效覆盖的原子 epoch/hash 切换、未知 ID 保留旧快照、删除 override 回退 built-in，以及相同内容重载不增加 epoch。`SpellCatalogOverrideDecoderTest` 覆盖安全字段覆盖、冻结 action/category 保留、未知字段、非整数法力和 ID 不匹配拒绝。
