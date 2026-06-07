# 投射物实体 XML 组件

> `gun_actions.lua` 决定“卡牌做什么”，实体 XML 决定“投射物在世界里如何运动、碰撞、伤害和消失”。

## 代表性组件

| 组件 | 作用 |
|---|---|
| `VelocityComponent` | 重力、空气阻力、质量等运动参数。 |
| `ProjectileComponent` | 速度范围、寿命、碰撞、伤害、爆炸、友伤、穿透、摄像机震动等核心投射物参数。 |
| `config_explosion` | 爆炸半径、破坏力、粒子、物理推力、是否伤害生物等。 |
| `damage_by_type` | 切割、治疗、火焰、冰冻等类型伤害。 |
| `CellEaterComponent` | 吞噬材料，黑洞等挖掘/清空效果依赖它。 |
| `LuaComponent` | 每帧执行额外 Lua，例如黑洞引力。 |
| `TeleportProjectileComponent` | 传送投射物的特殊组件。 |
| `LifetimeComponent` | 非标准投射物或召唤物的寿命。 |

## 代表样本

| XML | 对应法术 | 关键行为 |
|---|---|---|
| `deck/light_bullet.xml` | 火花弹 | 速度 750-850，寿命 40 帧，投射物伤害，微小爆炸和粒子尾迹。 |
| `deck/chainsaw.xml` | 链锯 | 寿命 1 帧，切割伤害，挖掘小范围材料；卡牌 action 还会清零施放延迟并降低充能。 |
| `deck/black_hole.xml` | 黑洞 | 不与世界普通碰撞，寿命 120 帧，`CellEaterComponent` 吞噬材料，`LuaComponent` 每帧执行引力脚本。 |
| `deck/heal_bullet.xml` | 治疗魔弹 | `damage_by_type healing` 为负值，带友伤/碰撞设置；表现为治疗而非普通伤害。 |
| `deck/teleport_projectile.xml` | 传送魔弹 | 有 `TeleportProjectileComponent`，碰撞后触发传送效果。 |
| `bomb.xml` | 炸弹 | 高爆炸破坏，有限次数卡牌，实体本身负责爆炸表现。 |

## 与 action 的关系

同一个 XML 可以被多个 action 复用。例如普通火花弹、触发火花弹、定时火花弹都关联 `light_bullet.xml`；差异不在 XML，而在 action 调用的是 `add_projectile()`、`add_projectile_trigger_hit_world()` 还是 `add_projectile_trigger_timer()`。

因此移植到 Minecraft 时，建议把法术拆成两层：

- `SpellAction`：抽牌、法力、修正器、触发器、payload。
- `ProjectileSpec`：实体速度、寿命、伤害、碰撞、爆炸、粒子和特殊组件。

这样同一个 ProjectileSpec 可以被不同触发包装器复用。

