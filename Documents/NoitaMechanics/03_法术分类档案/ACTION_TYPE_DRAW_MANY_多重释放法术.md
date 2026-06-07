# 多重释放法术（ACTION_TYPE_DRAW_MANY）

> 生成日期：2026-06-07。共 14 项。通过 draw_actions(n, ...) 或类似逻辑额外抽取多张法术卡。

## 读取规则

- `行为摘要` 是从 `action()` 中提取的关键调用和状态改写，用来定位底层逻辑，不等于完整源码。
- 触发/定时/失效触发的投射物通常表现为 `add_projectile_trigger_hit_world`、`add_projectile_trigger_timer`、`add_projectile_trigger_death`。
- 修正器、多重释放、复制类法术经常只改写状态或抽取后续 action，不一定直接生成投射物。

## 法术列表

| # | ID | 英文名 | 中文名 | 法力 | 次数 | 关联投射物/XML | 行为摘要 |
|---:|---|---|---|---:|---:|---|---|
| 163 | `BURST_2` | Double spell | 二重法术 | 0 | 100 | `` | 额外抽取: 2, true |
| 164 | `BURST_3` | Triple spell | 三重法术 | 2 | 100 | `` | 额外抽取: 3, true |
| 165 | `BURST_4` | Quadruple spell | 四重法术 | 5 | 100 | `` | 额外抽取: 4, true |
| 166 | `BURST_8` | Octuple spell | 八倍法术 | 30 | 100 | `` | 额外抽取: 8, true |
| 167 | `BURST_X` | Myriad Spell | 无尽法术 | 50 | 30 | `` | 额外抽取: #deck, true |
| 168 | `SCATTER_2` | Double scatter spell | 二重散射法术 | 0 | 100 | `` | 额外抽取: 2, true \| 状态修正: c.spread_degrees += 10.0 |
| 169 | `SCATTER_3` | Triple scatter spell | 三重散射法术 | 1 | 100 | `` | 额外抽取: 3, true \| 状态修正: c.spread_degrees += 20.0 |
| 170 | `SCATTER_4` | Quadruple scatter spell | 四重散射法术 | 2 | 100 | `` | 额外抽取: 4, true \| 状态修正: c.spread_degrees += 40.0 |
| 171 | `I_SHAPE` | Formation - behind your back | 阵型 - 背后 | 0 | 100 | `` | 额外抽取: 2, true \| 状态修正: c.spread_degrees -= 5.0 \| 状态赋值: c.pattern_degrees = 180 |
| 172 | `Y_SHAPE` | Formation - bifurcated | 阵型 - 分叉 | 2 | 100 | `` | 额外抽取: 2, true \| 状态修正: c.spread_degrees -= 8.0 \| 状态赋值: c.pattern_degrees = 45 |
| 173 | `T_SHAPE` | Formation - above and below | 阵型 - 上下 | 3 | 100 | `` | 额外抽取: 3, true \| 状态修正: c.spread_degrees -= 8.0 \| 状态赋值: c.pattern_degrees = 90 |
| 174 | `W_SHAPE` | Formation - trifurcated | 阵型 - 三叉 | 3 | 100 | `` | 额外抽取: 3, true \| 状态修正: c.spread_degrees -= 5.0 \| 状态赋值: c.pattern_degrees = 20 |
| 175 | `CIRCLE_SHAPE` | Formation - hexagon | 阵型 - 六边形 | 6 | 100 | `` | 额外抽取: 6, true \| 状态修正: c.spread_degrees -= 15.0 \| 状态赋值: c.pattern_degrees = 180 |
| 176 | `PENTAGRAM_SHAPE` | Formation - pentagon | 阵型 - 五边形 | 5 | 100 | `` | 额外抽取: 5, true \| 状态修正: c.spread_degrees -= 12.0 \| 状态赋值: c.pattern_degrees = 180 |

## 来源

- noitadata `gun_actions.lua`: https://github.com/NathanSnail/noitadata/blob/main/data/scripts/gun/gun_actions.lua
- noitadata `common.csv`: https://github.com/NathanSnail/noitadata/blob/main/data/translations/common.csv
