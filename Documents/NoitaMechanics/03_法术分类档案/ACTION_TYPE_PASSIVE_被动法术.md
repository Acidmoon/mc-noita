# 被动法术（ACTION_TYPE_PASSIVE）

> 生成日期：2026-06-07。共 5 项。作为法杖持有/始终存在效果，常在库存或手持时生效。

## 读取规则

- `行为摘要` 是从 `action()` 中提取的关键调用和状态改写，用来定位底层逻辑，不等于完整源码。
- 触发/定时/失效触发的投射物通常表现为 `add_projectile_trigger_hit_world`、`add_projectile_trigger_timer`、`add_projectile_trigger_death`。
- 修正器、多重释放、复制类法术经常只改写状态或抽取后续 action，不一定直接生成投射物。

## 法术列表

| # | ID | 英文名 | 中文名 | 法力 | 次数 | 关联投射物/XML | 行为摘要 |
|---:|---|---|---|---:|---:|---|---|
| 350 | `TORCH` | Torch | 火把 | 0 | 50 | `data/entities/misc/custom_cards/torch.xml` | 额外抽取: 1, true |
| 351 | `TORCH_ELECTRIC` | Electric Torch | 电子火把 | 0 | 50 | `data/entities/misc/custom_cards/torch_electric.xml` | 额外抽取: 1, true |
| 352 | `ENERGY_SHIELD` | Energy shield | 能量盾 |  |  | `data/entities/misc/custom_cards/energy_shield.xml` | 额外抽取: 1, true |
| 353 | `ENERGY_SHIELD_SECTOR` | Energy shield sector | 能量盾部分 |  |  | `data/entities/misc/custom_cards/energy_shield_sector.xml` | 额外抽取: 1, true |
| 355 | `TINY_GHOST` | Summon Tiny Ghost | 召唤迷你幽灵 | 0 |  | `data/entities/misc/custom_cards/tiny_ghost.xml` | 额外抽取: 1, true |

## 来源

- noitadata `gun_actions.lua`: https://github.com/NathanSnail/noitadata/blob/main/data/scripts/gun/gun_actions.lua
- noitadata `common.csv`: https://github.com/NathanSnail/noitadata/blob/main/data/translations/common.csv
