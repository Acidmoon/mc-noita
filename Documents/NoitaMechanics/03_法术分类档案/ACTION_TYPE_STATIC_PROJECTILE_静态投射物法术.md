# 静态投射物法术（ACTION_TYPE_STATIC_PROJECTILE）

> 生成日期：2026-06-07。共 45 项。生成场、区域、云、光束、十字等更偏固定或持续区域效果的实体。

## 读取规则

- `行为摘要` 是从 `action()` 中提取的关键调用和状态改写，用来定位底层逻辑，不等于完整源码。
- 触发/定时/失效触发的投射物通常表现为 `add_projectile_trigger_hit_world`、`add_projectile_trigger_timer`、`add_projectile_trigger_death`。
- 修正器、多重释放、复制类法术经常只改写状态或抽取后续 action，不一定直接生成投射物。

## 法术列表

| # | ID | 英文名 | 中文名 | 法力 | 次数 | 关联投射物/XML | 行为摘要 |
|---:|---|---|---|---:|---:|---|---|
| 20 | `BLACK_HOLE_BIG` | Giga black hole | 巨大黑洞 | 240 | 6 | `data/entities/projectiles/deck/black_hole_big.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/black_hole_big.xml") \| 状态修正: c.fire_rate_wait += 80; c.screenshake += 10 |
| 21 | `WHITE_HOLE_BIG` | Giga white hole | 巨大白洞 | 240 | 6 | `data/entities/projectiles/deck/white_hole_big.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/white_hole_big.xml") \| 状态修正: c.fire_rate_wait += 80; c.screenshake += 10 |
| 22 | `BLACK_HOLE_GIGA` | Omega Black Hole | 终结黑洞 | 500 | 6 | `data/entities/projectiles/deck/black_hole_giga.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/black_hole_giga.xml") \| 状态修正: c.fire_rate_wait += 120; c.screenshake += 40; current_reload_time += 100 |
| 23 | `WHITE_HOLE_GIGA` | Omega white hole | 终结白洞 | 500 | 6 | `data/entities/projectiles/deck/white_hole_giga.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/white_hole_giga.xml") \| 状态修正: c.fire_rate_wait += 120; c.screenshake += 40; current_reload_time += 100 |
| 60 | `BOMB_DETONATOR` | Explosive Detonator | 炸药引爆器 | 50 |  | `data/entities/projectiles/deck/bomb_detonator.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/bomb_detonator.xml") |
| 109 | `SWARM_FLY` | Summon fly swarm | 召唤苍蝇群 | 60 |  | `data/entities/projectiles/deck/swarm_fly.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/swarm_fly.xml"); add_projectile("data/entities/projectiles/deck/swarm_fly.xml"); add_projectile("data/entities/projectiles/deck/swarm_fly.xml"); add_projectile("data/entities/projectiles/deck/swarm_fly.xml") \| 状态修正: c.spread_degrees += 6.0; c.fire_rate_wait += 60; current_reload_time += 20 |
| 110 | `SWARM_FIREBUG` | Summon Firebug swarm | 召唤萤火虫群 | 70 |  | `data/entities/projectiles/deck/swarm_firebug.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/swarm_firebug.xml"); add_projectile("data/entities/projectiles/deck/swarm_firebug.xml"); add_projectile("data/entities/projectiles/deck/swarm_firebug.xml") \| 状态修正: c.spread_degrees += 12.0; c.fire_rate_wait += 60; current_reload_time += 20 |
| 111 | `SWARM_WASP` | Summon Wasp swarm | 召唤黄蜂群 | 80 |  | `data/entities/projectiles/deck/swarm_wasp.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/swarm_wasp.xml"); add_projectile("data/entities/projectiles/deck/swarm_wasp.xml"); add_projectile("data/entities/projectiles/deck/swarm_wasp.xml"); add_projectile("data/entities/projectiles/deck/swarm_wasp.xml") ... \| 状态修正: c.spread_degrees += 24.0; c.fire_rate_wait += 60; current_reload_time += 20 |
| 112 | `FRIEND_FLY` | Summon Friendly fly | 召唤友好苍蝇 | 120 |  | `data/entities/projectiles/deck/friend_fly.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/friend_fly.xml") \| 状态修正: c.spread_degrees += 24.0; c.fire_rate_wait += 80; current_reload_time += 40 |
| 120 | `WALL_HORIZONTAL` | Horizontal barrier | 水平屏障 | 70 | 80 | `data/entities/projectiles/deck/wall_horizontal.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/wall_horizontal.xml") \| 状态修正: c.fire_rate_wait += 5 |
| 121 | `WALL_VERTICAL` | Vertical barrier | 垂直屏障 | 70 | 80 | `data/entities/projectiles/deck/wall_vertical.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/wall_vertical.xml") \| 状态修正: c.fire_rate_wait += 5 |
| 122 | `WALL_SQUARE` | Square barrier | 方形屏障 | 70 | 20 | `data/entities/projectiles/deck/wall_square.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/wall_square.xml") \| 状态修正: c.fire_rate_wait += 20 |
| 125 | `PURPLE_EXPLOSION_FIELD` | Glittering field | 盛大场面 | 90 | 20 | `data/entities/projectiles/deck/purple_explosion_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/purple_explosion_field.xml") \| 状态修正: c.fire_rate_wait += 10; c.speed_multiplier -= 2 |
| 126 | `DELAYED_SPELL` | Delayed spellcast | 延迟施法 | 20 |  | `data/entities/projectiles/deck/delayed_spell.xml` | 投射物/触发: add_projectile_trigger_death("data/entities/projectiles/deck/delayed_spell.xml", 3) \| 状态修正: c.fire_rate_wait += 10 |
| 161 | `DESTRUCTION` | Destruction | 破坏 | 240 | 5 | `data/entities/projectiles/deck/destruction.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/destruction.xml") \| 状态修正: c.fire_rate_wait += 150; current_reload_time += 240 |
| 162 | `MASS_POLYMORPH` | Muodonmuutos | 墨东姆托 | 220 | 3 | `data/entities/projectiles/deck/mass_polymorph.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/mass_polymorph.xml") \| 状态修正: c.fire_rate_wait += 140; current_reload_time += 240 |
| 258 | `EXPLOSION` | Explosion | 爆炸 | 80 | 30 | `data/entities/projectiles/deck/explosion.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/explosion.xml") \| 状态修正: c.fire_rate_wait += 3; c.screenshake += 2.5 |
| 259 | `EXPLOSION_LIGHT` | Magical Explosion | 魔法爆炸 | 80 | 30 | `data/entities/projectiles/deck/explosion_light.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/explosion_light.xml") \| 状态修正: c.fire_rate_wait += 3; c.screenshake += 2.5 |
| 260 | `FIRE_BLAST` | Explosion of brimstone | 火焰爆炸 | 10 | 30 | `data/entities/projectiles/deck/fireblast.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/fireblast.xml") \| 状态修正: c.fire_rate_wait += 3; c.screenshake += 0.5 |
| 261 | `POISON_BLAST` | Explosion of poison | 毒素爆炸 | 30 | 30 | `data/entities/projectiles/deck/poison_blast.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/poison_blast.xml") \| 状态修正: c.fire_rate_wait += 3; c.screenshake += 0.5 |
| 262 | `ALCOHOL_BLAST` | Explosion of spirits | 烈酒爆炸 | 30 | 30 | `data/entities/projectiles/deck/alcohol_blast.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/alcohol_blast.xml") \| 状态修正: c.fire_rate_wait += 3; c.screenshake += 0.5 |
| 263 | `THUNDER_BLAST` | Explosion of thunder | 雷霆爆炸 | 110 | 30 | `data/entities/projectiles/deck/thunder_blast.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/thunder_blast.xml") \| 状态修正: c.fire_rate_wait += 15; c.screenshake += 3.0 |
| 264 | `BERSERK_FIELD` | Circle of fervour | 激情之环 | 30 | 15 | `data/entities/projectiles/deck/berserk_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/berserk_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 265 | `POLYMORPH_FIELD` | Circle of transmogrification | 变形之环 | 50 | 5 | `data/entities/projectiles/deck/polymorph_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/polymorph_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 266 | `CHAOS_POLYMORPH_FIELD` | Circle of unstable metamorphosis | 不稳变形之环 | 20 | 10 | `data/entities/projectiles/deck/chaos_polymorph_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/chaos_polymorph_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 267 | `ELECTROCUTION_FIELD` | Circle of thunder | 雷霆之环 | 60 | 15 | `data/entities/projectiles/deck/electrocution_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/electrocution_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 268 | `FREEZE_FIELD` | Circle of stillness | 静止之环 | 50 | 15 | `data/entities/projectiles/deck/freeze_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/freeze_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 269 | `REGENERATION_FIELD` | Circle of vigour | 活力之环 | 80 | 2 | `data/entities/projectiles/deck/regeneration_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/regeneration_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 270 | `TELEPORTATION_FIELD` | Circle of displacement | 位移之环 | 30 | 15 | `data/entities/projectiles/deck/teleportation_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/teleportation_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 271 | `LEVITATION_FIELD` | Circle of buoyancy | 浮力之环 | 10 | 15 | `data/entities/projectiles/deck/levitation_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/levitation_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 272 | `SHIELD_FIELD` | Circle of shielding | 遮蔽之环 | 20 | 10 | `data/entities/projectiles/deck/shield_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/shield_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 273 | `PROJECTILE_TRANSMUTATION_FIELD` | Projectile transmutation field | 投射物转化领域 | 120 | 6 | `data/entities/projectiles/deck/projectile_transmutation_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/projectile_transmutation_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 274 | `PROJECTILE_THUNDER_FIELD` | Projectile thunder field | 投射物雷电领域 | 140 | 6 | `data/entities/projectiles/deck/projectile_thunder_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/projectile_thunder_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 275 | `PROJECTILE_GRAVITY_FIELD` | Projectile gravity field | 投射物重力领域 | 120 | 6 | `data/entities/projectiles/deck/projectile_gravity_field.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/projectile_gravity_field.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 276 | `VACUUM_POWDER` | Powder Vacuum Field | 粉末真空场 | 40 | 20 | `data/entities/projectiles/deck/vacuum_powder.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/vacuum_powder.xml") \| 状态修正: c.fire_rate_wait += 10 |
| 277 | `VACUUM_LIQUID` | Liquid Vacuum Field | 液体真空场 | 40 | 20 | `data/entities/projectiles/deck/vacuum_liquid.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/vacuum_liquid.xml") \| 状态修正: c.fire_rate_wait += 10 |
| 278 | `VACUUM_ENTITIES` | Vacuum Field | 真空场 | 50 | 20 | `data/entities/projectiles/deck/vacuum_entities.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/vacuum_entities.xml") \| 状态修正: c.fire_rate_wait += 10 |
| 287 | `CLOUD_WATER` | Rain cloud | 雨云 | 30 | 10 | `data/entities/projectiles/deck/cloud_water.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/cloud_water.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 288 | `CLOUD_OIL` | Oil cloud | 油脂之云 | 20 | 15 | `data/entities/projectiles/deck/cloud_oil.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/cloud_oil.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 289 | `CLOUD_BLOOD` | Blood cloud | 血云 | 60 | 3 | `data/entities/projectiles/deck/cloud_blood.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/cloud_blood.xml") \| 状态修正: c.fire_rate_wait += 30 |
| 290 | `CLOUD_ACID` | Acid cloud | 酸云 | 90 | 8 | `data/entities/projectiles/deck/cloud_acid.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/cloud_acid.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 291 | `CLOUD_THUNDER` | Thundercloud | 雷云 | 90 | 5 | `data/entities/projectiles/deck/cloud_thunder.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/cloud_thunder.xml") \| 状态修正: c.fire_rate_wait += 30 |
| 372 | `RANDOM_STATIC_PROJECTILE` | Random static projectile spell | 随机静态投射物法术 | 20 |  | `` | 引擎调用: SetRandomSeed(GameGetFrameNum(); Random(1, #actions); Random(1, #actions) |
| 404 | `METEOR_RAIN` | Meteorisade |  | 225 | 2 | `data/entities/projectiles/deck/meteor_rain_meteor.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/meteor_rain.xml") \| 状态修正: c.fire_rate_wait += 100; current_reload_time += 60 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/effect_meteor_rain.xml," |
| 405 | `WORM_RAIN` | Matosade |  | 225 | 2 | `data/entities/animals/worm_big.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/worm_rain.xml") \| 状态修正: c.fire_rate_wait += 100; current_reload_time += 60 |

## 来源

- noitadata `gun_actions.lua`: https://github.com/NathanSnail/noitadata/blob/main/data/scripts/gun/gun_actions.lua
- noitadata `common.csv`: https://github.com/NathanSnail/noitadata/blob/main/data/translations/common.csv
