# 实用法术（ACTION_TYPE_UTILITY）

> 生成日期：2026-06-07。共 25 项。偏功能性移动、传送、工具、召唤或状态效果。

## 读取规则

- `行为摘要` 是从 `action()` 中提取的关键调用和状态改写，用来定位底层逻辑，不等于完整源码。
- 触发/定时/失效触发的投射物通常表现为 `add_projectile_trigger_hit_world`、`add_projectile_trigger_timer`、`add_projectile_trigger_death`。
- 修正器、多重释放、复制类法术经常只改写状态或抽取后续 action，不一定直接生成投射物。

## 法术列表

| # | ID | 英文名 | 中文名 | 法力 | 次数 | 关联投射物/XML | 行为摘要 |
|---:|---|---|---|---:|---:|---|---|
| 123 | `TEMPORARY_WALL` | Summon Wall | 召唤墙壁 | 40 | 20 | `data/entities/projectiles/deck/temporary_wall.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/temporary_wall.xml") \| 状态修正: c.fire_rate_wait += 40 |
| 124 | `TEMPORARY_PLATFORM` | Summon Platform | 召唤平台 | 30 | 20 | `data/entities/projectiles/deck/temporary_platform.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/temporary_platform.xml") \| 状态修正: c.fire_rate_wait += 40 |
| 127 | `LONG_DISTANCE_CAST` | Long-distance cast | 远距离施放 | 0 |  | `data/entities/projectiles/deck/long_distance_cast.xml` | 投射物/触发: add_projectile_trigger_death("data/entities/projectiles/deck/long_distance_cast.xml", 1) \| 状态修正: c.fire_rate_wait -= 5 |
| 128 | `TELEPORT_CAST` | Teleporting cast | 传送施放 | 100 |  | `data/entities/projectiles/deck/teleport_cast.xml` | 投射物/触发: add_projectile_trigger_death("data/entities/projectiles/deck/teleport_cast.xml", 1) \| 状态修正: c.fire_rate_wait += 20; c.spread_degrees += 24 |
| 129 | `SUPER_TELEPORT_CAST` | Warp cast | 传送施放 | 20 |  | `data/entities/projectiles/deck/super_teleport_cast.xml` | 投射物/触发: add_projectile_trigger_death("data/entities/projectiles/deck/super_teleport_cast.xml", 1) \| 状态修正: c.fire_rate_wait += 10; c.spread_degrees -= 6 |
| 130 | `CASTER_CAST` | Inner spell | 随心法术 | 10 |  | `data/entities/projectiles/deck/caster_cast.xml` | 额外抽取: 1, true \| 状态修正: c.spread_degrees -= 24 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/caster_cast.xml," |
| 152 | `SUMMON_WANDGHOST` | Summon Taikasauva | 召唤魔法棒 | 300 | 1 | `data/entities/projectiles/deck/wand_ghost_player.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/wand_ghost_player.xml"); add_projectile("data/entities/particles/image_emitters/wand_effect.xml") |
| 177 | `I_SHOT` | Iplicate Spell | 复制法术 | 40 | 30 | `` | 投射物/触发: add_projectile(proj) \| 额外抽取: 1, true \| 状态修正: mana -= data.mana \| 状态赋值: c.pattern_degrees = 180 |
| 178 | `Y_SHOT` | Yplicate Spell | 分叉法术 | 40 | 30 | `` | 投射物/触发: add_projectile(proj) \| 额外抽取: 1, true \| 状态修正: mana -= data.mana \| 状态赋值: c.pattern_degrees = 45 |
| 179 | `T_SHOT` | Tiplicate Spell | 上下法术 | 60 | 25 | `` | 投射物/触发: add_projectile(proj) \| 额外抽取: 1, true \| 状态修正: mana -= data.mana \| 状态赋值: c.pattern_degrees = 90 |
| 180 | `W_SHOT` | Wuplicate Spell | 三岔法术 | 70 | 20 | `` | 投射物/触发: add_projectile(proj) \| 额外抽取: 1, true \| 状态修正: mana -= data.mana \| 状态赋值: c.pattern_degrees = 20 |
| 181 | `QUAD_SHOT` | Quplicate Spell | 四重法术 | 90 | 20 | `` | 投射物/触发: add_projectile(proj) \| 额外抽取: 1, true \| 状态修正: mana -= data.mana \| 状态赋值: c.pattern_degrees = 180 |
| 182 | `PENTA_SHOT` | Peplicate Spell | 五角法术 | 110 | 20 | `` | 投射物/触发: add_projectile(proj) \| 额外抽取: 1, true \| 状态修正: mana -= data.mana \| 状态赋值: c.pattern_degrees = 180 |
| 183 | `HEXA_SHOT` | Heplicate Spell | 六角法术 | 130 | 20 | `` | 投射物/触发: add_projectile(proj) \| 额外抽取: 1, true \| 状态修正: mana -= data.mana \| 状态赋值: c.pattern_degrees = 180 |
| 195 | `BLOOD_MAGIC` | Blood magic | 血液魔法 | -100 |  | `data/entities/misc/custom_cards/blood_magic.xml` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 20; current_reload_time -= 20 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/blood_sparks.xml," |
| 196 | `MONEY_MAGIC` | Gold to Power | 黄金变力量 | 30 |  | `data/entities/misc/custom_cards/money_magic.xml` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += ( damage / 25 ); c.damage_projectile_add += ( damage / 35 ); c.damage_projectile_add += ( damage / 45 ); c.damage_projectile_add += ( damage / 55 ) \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/gold_sparks.xml," |
| 197 | `BLOOD_TO_POWER` | Blood to Power | 血液变力量 | 20 |  | `data/entities/misc/custom_cards/blood_to_power.xml` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += damage \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/blood_sparks.xml," |
| 341 | `X_RAY` | All-seeing eye | 全知之眼 | 100 | 10 | `data/entities/projectiles/deck/xray.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/xray.xml") |
| 376 | `ALL_NUKES` | Spells to nukes | 核弹置换术 | 600 | 2 | `` | 投射物/触发: add_projectile("data/entities/projectiles/deck/all_nukes.xml") \| 状态修正: c.fire_rate_wait += 100; current_reload_time += 100 |
| 377 | `ALL_DISCS` | Spells to giga sawblades | 巨型锯刃置换术 | 100 | 15 | `` | 投射物/触发: add_projectile("data/entities/projectiles/deck/all_discs.xml") \| 状态修正: c.fire_rate_wait += 50; current_reload_time += 50 |
| 378 | `ALL_ROCKETS` | Spells to magic missiles | 魔法飞弹置换术 | 100 | 10 | `` | 投射物/触发: add_projectile("data/entities/projectiles/deck/all_rockets.xml") \| 状态修正: c.fire_rate_wait += 50; current_reload_time += 50 |
| 379 | `ALL_DEATHCROSSES` | Spells to death crosses | 死亡十字置换术 | 80 | 15 | `` | 投射物/触发: add_projectile("data/entities/projectiles/deck/all_deathcrosses.xml") \| 状态修正: c.fire_rate_wait += 40; current_reload_time += 40 |
| 380 | `ALL_BLACKHOLES` | Spells to black holes | 黑洞置换术 | 200 | 10 | `` | 投射物/触发: add_projectile("data/entities/projectiles/deck/all_blackholes.xml") \| 状态修正: c.fire_rate_wait += 100; current_reload_time += 100 |
| 381 | `ALL_ACID` | Spells to acid | 酸液置换术 | 200 | 15 | `` | 投射物/触发: add_projectile("data/entities/projectiles/deck/all_acid.xml") \| 状态修正: c.fire_rate_wait += 100; current_reload_time += 100 |
| 406 | `RESET` | Wand Refresh | 魔杖刷新 | 20 |  | `` | 状态修正: current_reload_time -= 25 |

## 来源

- noitadata `gun_actions.lua`: https://github.com/NathanSnail/noitadata/blob/main/data/scripts/gun/gun_actions.lua
- noitadata `common.csv`: https://github.com/NathanSnail/noitadata/blob/main/data/translations/common.csv
