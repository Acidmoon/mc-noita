# MC-noita 实现映射建议

> 参考当前仓库 Java 代码和本归档的 Noita 机制。当前目标不是一次性实现 422 个法术，而是让底层模型能承载它们。

## 当前已有能力

当前 `src/main/java/com/mcnoita/wand/NoitaWandCaster.java` 已经实现了简化法杖：

- 从主手读取 `NoitaWandItem`。
- NBT 保存当前法力、施法延迟、充能、deck 和 draw index。
- 支持乱序 deck、施放数、法力恢复、施放延迟、充能时间。
- 支持投射物修正器叠加到少数 projectile spell。
- 支持有限次数消耗。

当前 `src/main/java/com/mcnoita/spell/NoitaSpellTemplate.java` 和 `NoitaSpellType.java` 已经能表达基础法术属性，但类型体系仍比 Noita 原版窄。

## 主要缺口

| Noita 原机制 | 当前 MC-noita 状态 | 建议 |
|---|---|---|
| `deck` / `hand` / `discarded` 三堆 | 只有 deck 和 draw index | 引入 `CastContext`，保存三堆和本次 shot 状态。 |
| `ConfigGunActionInfo c` | 以 `SpellModifiers` 直接套模板 | 改成可累计的 `CastState`，字段映射 `fireRateWait`、`reloadTime`、`spread`、`damage`、`speedMultiplier` 等。 |
| `draw_actions(n, true)` | 施放数循环抽取 | 实现解释器式抽牌，支持多重释放、修正器继续抽、包裹。 |
| 触发/定时/死亡 payload | 暂无完整模型 | 施法时预结算 `ProjectilePayload`，触发时释放。 |
| XML ProjectileComponent | Java Entity 中手写少数参数 | 建 `ProjectileSpec` 数据层，逐步迁移 XML 字段。 |
| 特殊法术 | 暂无 | 分批实现；复制/希腊字母/Wand Refresh 放在最后。 |

## 推荐底层接口

```java
interface NoitaAction {
    void execute(CastContext context);
}

final class CastContext {
    DeckState deckState;
    CastState castState;
    float mana;
    int recursionDepth;
}

final class ProjectilePayload {
    List<ResolvedProjectile> projectiles;
    CastState resolvedState;
}
```

关键点是 action 执行时能操作 deck，而 projectile entity 只接收已经结算好的结果。

## 实现顺序

1. 重构但保持现有玩法：把当前 `NoitaWandCaster` 的抽牌逻辑包装成 `CastContext`。
2. 加入 `hand` 和 `discarded`，把有限次数消耗移动到 shot 结束阶段。
3. 实现 `ACTION_TYPE_MODIFIER` 和 `ACTION_TYPE_DRAW_MANY` 的解释器。
4. 实现触发器 payload：火花弹触发、火花弹定时、失效触发黑洞这类代表法术。
5. 用 `ProjectileSpec` 扩展实体参数：寿命、速度、散射、爆炸、伤害类型。
6. 最后实现 `RESET`、`ADD_TRIGGER`、复制和希腊字母类法术。

## 测试建议

- 非乱序：槽位顺序稳定，draw index 正确推进。
- 乱序：每次充能后 deck 顺序改变，但一轮内不重复抽已抽卡。
- 法力不足：昂贵修正器失败后，后续便宜法术仍可继续尝试。
- 触发 payload：payload 在施法时扣法力，不在命中时扣。
- 链锯：同时影响施放延迟和充能时间。
- Wand Refresh：能把 hand/deck/discarded 状态按 Noita 逻辑重新组织。

