# 材料法术（ACTION_TYPE_MATERIAL）

> 生成日期：2026-06-07。共 26 项。生成或转换材料，通常用 c.material 或特定实体/脚本实现。

## 读取规则

- `行为摘要` 是从 `action()` 中提取的关键调用和状态改写，用来定位底层逻辑，不等于完整源码。
- 触发/定时/失效触发的投射物通常表现为 `add_projectile_trigger_hit_world`、`add_projectile_trigger_timer`、`add_projectile_trigger_death`。
- 修正器、多重释放、复制类法术经常只改写状态或抽取后续 action，不一定直接生成投射物。

## 法术列表

| # | ID | 英文名 | 中文名 | 法力 | 次数 | 关联投射物/XML | 行为摘要 |
|---:|---|---|---|---:|---:|---|---|
| 116 | `SOILBALL` | Chunk of soil | 一把泥土 | 5 |  | `data/entities/projectiles/chunk_of_soil.xml` | 投射物/触发: add_projectile("data/entities/projectiles/chunk_of_soil.xml") |
| 135 | `CIRCLE_FIRE` | Circle of fire | 烈火之环 | 20 | 15 | `data/entities/projectiles/deck/circle_fire.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/circle_fire.xml") \| 状态修正: c.fire_rate_wait += 20 |
| 136 | `CIRCLE_ACID` | Circle of acid | 酸液之环 | 40 | 4 | `data/entities/projectiles/deck/circle_acid.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/circle_acid.xml") \| 状态修正: c.fire_rate_wait += 20 |
| 137 | `CIRCLE_OIL` | Circle of oil | 油液之环 | 20 | 15 | `data/entities/projectiles/deck/circle_oil.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/circle_oil.xml") \| 状态修正: c.fire_rate_wait += 20 |
| 138 | `CIRCLE_WATER` | Circle of water | 清水之环 | 20 | 15 | `data/entities/projectiles/deck/circle_water.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/circle_water.xml") \| 状态修正: c.fire_rate_wait += 20 |
| 139 | `MATERIAL_WATER` | Water | 水 | 0 |  | `data/entities/projectiles/deck/material_water.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/material_water.xml") \| 状态修正: c.fire_rate_wait -= 15; current_reload_time -= ACTION_DRAW_RELOAD_TIME_INCREASE - 10 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_apply_wet.xml," |
| 140 | `MATERIAL_OIL` | Oil | 油 | 0 |  | `data/entities/projectiles/deck/material_oil.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/material_oil.xml") \| 状态修正: c.fire_rate_wait -= 15; current_reload_time -= ACTION_DRAW_RELOAD_TIME_INCREASE - 10 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_apply_oiled.xml," |
| 141 | `MATERIAL_BLOOD` | Blood | 血液 | 0 | 250 | `data/entities/projectiles/deck/material_blood.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/material_blood.xml") \| 状态修正: c.fire_rate_wait -= 15; current_reload_time -= ACTION_DRAW_RELOAD_TIME_INCREASE - 10 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_apply_bloody.xml," |
| 142 | `MATERIAL_ACID` | Acid | 酸液 | 0 | 250 | `data/entities/projectiles/deck/material_acid.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/material_acid.xml") \| 状态修正: c.fire_rate_wait -= 15; current_reload_time -= ACTION_DRAW_RELOAD_TIME_INCREASE - 10 |
| 143 | `MATERIAL_CEMENT` | Cement | 水泥 | 0 | 250 | `data/entities/projectiles/deck/material_cement.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/material_cement.xml") \| 状态修正: c.fire_rate_wait -= 15; current_reload_time -= ACTION_DRAW_RELOAD_TIME_INCREASE - 10 |
| 153 | `TOUCH_GOLD` | Touch of Gold | 黄金之触 | 300 | 1 | `data/entities/projectiles/deck/touch_gold.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/touch_gold.xml") |
| 154 | `TOUCH_WATER` | Touch of Water | 清水之触 | 280 | 5 | `data/entities/projectiles/deck/touch_water.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/touch_water.xml") |
| 155 | `TOUCH_OIL` | Touch of Oil | 油脂之触 | 260 | 5 | `data/entities/projectiles/deck/touch_oil.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/touch_oil.xml") |
| 156 | `TOUCH_ALCOHOL` | Touch of Spirits | 烈酒之触 | 240 | 5 | `data/entities/projectiles/deck/touch_alcohol.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/touch_alcohol.xml") |
| 157 | `TOUCH_PISS` | Touch of Gold? | 黄金之触？ | 190 | 4 | `data/entities/projectiles/deck/touch_piss.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/touch_piss.xml") |
| 158 | `TOUCH_GRASS` | Touch of Grass | 草之触 | 190 | 4 | `data/entities/projectiles/deck/touch_grass.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/touch_grass.xml") |
| 159 | `TOUCH_BLOOD` | Touch of Blood | 鲜血之触 | 270 | 3 | `data/entities/projectiles/deck/touch_blood.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/touch_blood.xml") |
| 160 | `TOUCH_SMOKE` | Touch of Smoke | 烟雾之触 | 230 | 5 | `data/entities/projectiles/deck/touch_smoke.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/touch_smoke.xml") |
| 279 | `SEA_LAVA` | Sea of lava | 岩浆之海 | 140 | 3 | `data/entities/projectiles/deck/sea_lava.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/sea_lava.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 280 | `SEA_ALCOHOL` | Sea of alcohol | 酒精之海 | 140 | 3 | `data/entities/projectiles/deck/sea_alcohol.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/sea_alcohol.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 281 | `SEA_OIL` | Sea of oil | 油脂之海 | 140 | 3 | `data/entities/projectiles/deck/sea_oil.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/sea_oil.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 282 | `SEA_WATER` | Sea of water | 水之海 | 140 | 3 | `data/entities/projectiles/deck/sea_water.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/sea_water.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 283 | `SEA_SWAMP` | Summon Swamp | 召唤沼泽 | 140 | 3 | `data/entities/projectiles/deck/sea_swamp.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/sea_swamp.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 284 | `SEA_ACID` | Sea of acid | 酸液之海 | 140 | 3 | `data/entities/projectiles/deck/sea_acid.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/sea_acid.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 285 | `SEA_ACID_GAS` | Sea of flammable gas | 可燃气体之海 | 140 | 3 | `data/entities/projectiles/deck/sea_acid_gas.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/sea_acid_gas.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 286 | `SEA_MIMIC` | Sea of Mimicium | 拟态之海 | 140 | 2 | `data/entities/projectiles/deck/sea_mimic.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/sea_mimic.xml") \| 状态修正: c.fire_rate_wait += 15 |

## 来源

- noitadata `gun_actions.lua`: https://github.com/NathanSnail/noitadata/blob/main/data/scripts/gun/gun_actions.lua
- noitadata `common.csv`: https://github.com/NathanSnail/noitadata/blob/main/data/translations/common.csv
