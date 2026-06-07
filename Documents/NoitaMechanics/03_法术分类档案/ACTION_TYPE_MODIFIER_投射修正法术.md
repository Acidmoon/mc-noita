# 投射修正法术（ACTION_TYPE_MODIFIER）

> 生成日期：2026-06-07。共 143 项。不一定自己生成投射物，主要修改当前 ConfigGunActionInfo 状态 c，影响后续投射物。

## 读取规则

- `行为摘要` 是从 `action()` 中提取的关键调用和状态改写，用来定位底层逻辑，不等于完整源码。
- 触发/定时/失效触发的投射物通常表现为 `add_projectile_trigger_hit_world`、`add_projectile_trigger_timer`、`add_projectile_trigger_death`。
- 修正器、多重释放、复制类法术经常只改写状态或抽取后续 action，不一定直接生成投射物。

## 法术列表

| # | ID | 英文名 | 中文名 | 法力 | 次数 | 关联投射物/XML | 行为摘要 |
|---:|---|---|---|---:|---:|---|---|
| 184 | `SPREAD_REDUCE` | Reduce spread | 降低散射 | 1 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.spread_degrees -= 60.0 |
| 185 | `HEAVY_SPREAD` | Heavy spread | 沉重散射 | 2 |  | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 7; c.spread_degrees += 720; current_reload_time -= 15 |
| 186 | `RECHARGE` | Reduce recharge time | 缩减充能时间 | 12 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 10; current_reload_time -= 20 |
| 187 | `LIFETIME` | Increase lifetime | 延长存在时间 | 40 | 150 | `data/entities/misc/custom_cards/lifetime.xml` | 额外抽取: 1, true \| 状态修正: c.lifetime_add += 75; c.fire_rate_wait += 13 |
| 188 | `LIFETIME_DOWN` | Reduce lifetime | 缩减存在时间 | 10 | 150 | `data/entities/misc/custom_cards/lifetime_down.xml` | 额外抽取: 1, true \| 状态修正: c.lifetime_add -= 42; c.fire_rate_wait -= 15 |
| 189 | `NOLLA` | Nolla | 零时 | 1 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 15 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/nolla.xml," |
| 190 | `SLOW_BUT_STEADY` | Slow But Steady | 缓慢但坚定 | 0 |  | `` | 额外抽取: 1, true |
| 191 | `EXPLOSION_REMOVE` | Remove Explosion | 移除爆炸 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 15; c.explosion_radius -= 30.0; c.damage_explosion_add -= 0.8 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/explosion_remove.xml," |
| 192 | `EXPLOSION_TINY` | Concentrated Explosion | 聚爆 | 40 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 15; c.explosion_radius -= 30.0; c.damage_explosion_add += 0.8 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/explosion_tiny.xml," |
| 193 | `LASER_EMITTER_WIDER` | Plasma Beam Enhancer | 电浆束增强器 | 10 | 120 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/laser_emitter_wider.xml," |
| 194 | `MANA_REDUCE` | Add mana | 额外法力 | -30 | 150 | `data/entities/misc/custom_cards/mana_reduce.xml` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 10 |
| 199 | `QUANTUM_SPLIT` | Quantum Split | 量子分割 | 10 |  | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/quantum_split.xml," |
| 200 | `GRAVITY` | Gravity | 重力 | 1 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.gravity += 600.0 |
| 201 | `GRAVITY_ANTI` | Anti-gravity | 反重力 | 1 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.gravity -= 600.0 |
| 202 | `SINEWAVE` | Slithering path | 蛇行 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.speed_multiplier *= 2 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/sinewave.xml," |
| 203 | `CHAOTIC_ARC` | Chaotic path | 混乱之路 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.speed_multiplier *= 2 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/chaotic_arc.xml," |
| 204 | `PINGPONG_PATH` | Ping-pong path | 乒乓回弹 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.lifetime_add += 25 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/pingpong_path.xml," |
| 205 | `AVOIDING_ARC` | Avoiding arc | 规避弧度 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 10 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/avoiding_arc.xml," |
| 206 | `FLOATING_ARC` | Floating arc | 悬浮弧度 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 10 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/floating_arc.xml," |
| 207 | `FLY_DOWNWARDS` | Fly downwards | 向下飞行 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.speed_multiplier *= 1.2 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/fly_downwards.xml," |
| 208 | `FLY_UPWARDS` | Fly upwards | 向上飞行 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.speed_multiplier *= 1.2 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/fly_upwards.xml," |
| 209 | `HORIZONTAL_ARC` | Horizontal path | 水平路径 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += 0.3; c.fire_rate_wait -= 6 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/horizontal_arc.xml," |
| 210 | `LINE_ARC` | Linear arc | 线性弧 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += 0.2; c.fire_rate_wait -= 4 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/line_arc.xml," |
| 211 | `ORBIT_SHOT` | Orbiting Arc | 盘旋魔弹 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += 0.1; c.fire_rate_wait -= 6; c.lifetime_add += 25 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/spiraling_shot.xml," |
| 212 | `SPIRALING_SHOT` | Spiral Arc | 螺旋魔弹 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += 0.1; c.fire_rate_wait -= 6; c.lifetime_add += 50 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/orbit_shot.xml," |
| 213 | `PHASING_ARC` | Phasing Arc | 相位弧度 | 2 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 12; c.lifetime_add += 80; c.speed_multiplier *= 0.33; c.child_speed_multiplier *= 0.33 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/phasing_arc.xml," |
| 214 | `TRUE_ORBIT` | True Orbit | 真实环绕 | 2 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += 0.1; c.fire_rate_wait -= 20; c.lifetime_add += 80 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/true_orbit.xml," |
| 215 | `BOUNCE` | Bounce | 弹跳 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.bounces += 10 |
| 216 | `REMOVE_BOUNCE` | Remove Bounce | 移除弹跳 | 0 | 150 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/remove_bounce.xml,"; c.bounces = 0 |
| 217 | `HOMING` | Homing | 追踪 | 70 | 100 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/homing.xml,data/entities/particles/tinyspark_white.xml," |
| 218 | `ANTI_HOMING` | Anti Homing | 反追踪 | 1 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 20 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/anti_homing.xml,data/entities/particles/tinyspark_white.xml," |
| 219 | `HOMING_WAND` | Wand Homing | 魔杖追踪 | 200 | 100 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/homing_wand.xml,data/entities/particles/tinyspark_white.xml," |
| 220 | `HOMING_SHORT` | Short-range Homing | 短距离追踪 | 40 | 100 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/homing_short.xml,data/entities/particles/tinyspark_white_weak.xml," |
| 221 | `HOMING_ROTATE` | Rotate towards foes | 转向敌人 | 40 | 100 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/homing_rotate.xml,data/entities/particles/tinyspark_white.xml," |
| 222 | `HOMING_SHOOTER` | Boomerang | 回旋镖 | 10 | 100 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/homing_shooter.xml,data/entities/particles/tinyspark_white.xml," |
| 223 | `AUTOAIM` | Auto-Aim | 自动瞄准 | 25 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/autoaim.xml," |
| 224 | `HOMING_ACCELERATING` | Accelerative Homing | 加速追踪 | 60 | 100 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/homing_accelerating.xml,data/entities/particles/tinyspark_white_small.xml," |
| 225 | `HOMING_CURSOR` | Aiming Arc | 瞄准弧度 | 30 | 100 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/homing_cursor.xml,data/entities/particles/tinyspark_white.xml," |
| 226 | `HOMING_AREA` | Projectile Area Teleport | 投射物区域传送 | 60 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 8; c.spread_degrees += 6; c.speed_multiplier *= 0.75 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/homing_area.xml,data/entities/particles/tinyspark_white.xml," |
| 227 | `PIERCING_SHOT` | Piercing shot | 穿刺魔弹 | 140 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add -= 0.6 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/piercing_shot.xml,"; c.friendly_fire = true |
| 228 | `CLIPPING_SHOT` | Drilling shot | 穿凿魔弹 | 160 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 50; current_reload_time += 40 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/clipping_shot.xml," |
| 229 | `DAMAGE` | Damage Plus | 伤害增强 | 5 | 50 | `data/entities/misc/custom_cards/damage.xml` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += 0.4; c.gore_particles += 5; c.fire_rate_wait += 5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_yellow.xml," |
| 230 | `DAMAGE_RANDOM` | Random damage | 随机伤害 | 15 | 50 | `data/entities/misc/custom_cards/damage_random.xml` | 额外抽取: 1, true \| 状态修正: c.gore_particles += 5 * multiplier; c.fire_rate_wait += 5 \| 状态赋值: c.damage_projectile_add = result; c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_yellow.xml," \| 引擎调用: SetRandomSeed(GameGetFrameNum(); Random(-3, 4); Random(0, 2) |
| 231 | `BLOODLUST` | Bloodlust | 嗜血 | 2 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += 1.3; c.gore_particles += 15; c.fire_rate_wait += 8; c.spread_degrees += 6 \| 状态赋值: c.friendly_fire = true; c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_red.xml," |
| 232 | `DAMAGE_FOREVER` | Mana To Damage | 法力转伤害 | 0 | 20 | `data/entities/misc/custom_cards/damage_forever.xml` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += 0.025 * manaforspell; c.gore_particles += 15; c.fire_rate_wait += 15; current_reload_time += 10 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_red.xml," |
| 233 | `CRITICAL_HIT` | Critical Plus | 暴击增强 | 5 | 50 | `data/entities/misc/custom_cards/critical_hit.xml` | 额外抽取: 1, true \| 状态修正: c.damage_critical_chance += 15 |
| 234 | `AREA_DAMAGE` | Damage field | 伤害领域 | 30 | 100 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/area_damage.xml," |
| 235 | `SPELLS_TO_POWER` | Spells to Power | 法术变力量 | 110 | 20 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 40 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/spells_to_power.xml," |
| 236 | `ESSENCE_TO_POWER` | Essence to Power | 精华变力量 | 110 | 20 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 20 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/essence_to_power.xml," |
| 237 | `ZERO_DAMAGE` | Null shot | 归零魔弹 | 5 | 50 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 5; c.lifetime_add += 280 \| 状态赋值: c.damage_electricity_add = 0; c.damage_explosion_add = 0; c.damage_explosion = 0; c.damage_critical_chance = 0 ... |
| 238 | `HEAVY_SHOT` | Heavy Shot | 沉重一击 | 7 | 50 | `data/entities/misc/custom_cards/heavy_shot.xml` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add += 1.75; c.fire_rate_wait += 10; c.gore_particles += 10; c.speed_multiplier *= 0.3 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/heavy_shot.xml," |
| 239 | `LIGHT_SHOT` | Light shot | 轻巧魔弹 | 5 | 50 | `data/entities/misc/custom_cards/light_shot.xml` | 额外抽取: 1, true \| 状态修正: c.damage_projectile_add -= 1.0; c.explosion_radius -= 10.0; c.fire_rate_wait -= 3; c.speed_multiplier *= 7.5; c.spread_degrees -= 6 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/light_shot.xml," |
| 240 | `KNOCKBACK` | Knockback | 击退 | 5 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.knockback_force += 5 |
| 241 | `RECOIL` | Recoil | 后座力 | 5 | 150 | `` | 额外抽取: 1, true |
| 242 | `RECOIL_DAMPER` | Recoil Damper | 后座阻尼器 | 5 | 150 | `` | 额外抽取: 1, true |
| 243 | `SPEED` | Speed Up | 加速 | 3 | 100 | `data/entities/misc/custom_cards/speed.xml` | 额外抽取: 1, true \| 状态修正: c.speed_multiplier *= 2.5 |
| 244 | `ACCELERATING_SHOT` | Accelerating shot | 加速魔弹 | 20 | 50 | `data/entities/misc/custom_cards/accelerating_shot.xml` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 8; c.speed_multiplier *= 0.32 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/accelerating_shot.xml," |
| 245 | `DECELERATING_SHOT` | Decelerating shot | 减速魔弹 | 10 | 50 | `data/entities/misc/custom_cards/decelerating_shot.xml` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.speed_multiplier *= 1.68 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/decelerating_shot.xml," |
| 246 | `EXPLOSIVE_PROJECTILE` | Explosive projectile | 易爆的投射物 | 30 | 50 | `data/entities/misc/custom_cards/explosive_projectile.xml` | 额外抽取: 1, true \| 状态修正: c.explosion_radius += 15.0; c.damage_explosion_add += 0.2; c.fire_rate_wait += 40; c.speed_multiplier *= 0.75 |
| 247 | `CLUSTERMOD` | Clusterbolt | 霰爆弹 | 30 | 50 | `data/entities/misc/custom_cards/clusterbomb.xml` | 额外抽取: 1, true \| 状态修正: c.explosion_radius += 4.0; c.damage_explosion_add += 0.2; c.fire_rate_wait += 20 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/clusterbomb.xml," |
| 248 | `WATER_TO_POISON` | Water to poison | 化水为毒 | 30 | 50 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 10 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/water_to_poison.xml,data/entities/particles/tinyspark_purple.xml," |
| 249 | `BLOOD_TO_ACID` | Blood to acid | 溶血为酸 | 30 | 50 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 10 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/blood_to_acid.xml,data/entities/particles/tinyspark_red.xml," |
| 250 | `LAVA_TO_BLOOD` | Lava to blood | 熔岩化血 | 30 | 50 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 10 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/lava_to_blood.xml,data/entities/particles/tinyspark_orange.xml," |
| 251 | `LIQUID_TO_EXPLOSION` | Liquid Detonation | 液体引爆 | 40 | 50 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 20 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/liquid_to_explosion.xml,data/entities/particles/tinyspark_red.xml," |
| 252 | `TOXIC_TO_ACID` | Toxic sludge to acid | 毒性淤泥酸液化 | 50 | 50 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 10 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/toxic_to_acid.xml,data/entities/particles/tinyspark_green.xml," |
| 253 | `STATIC_TO_SAND` | Ground to sand | 化地为砂 | 70 | 8 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 60 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/static_to_sand.xml,data/entities/particles/tinyspark_yellow.xml," |
| 254 | `TRANSMUTATION` | Chaotic transmutation | 混乱转化 | 80 | 8 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 20 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/transmutation.xml,data/entities/particles/tinyspark_purple_bright.xml," |
| 255 | `RANDOM_EXPLOSION` | Chaos magic | 混沌魔法 | 120 | 30 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 40 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/random_explosion.xml,data/entities/particles/tinyspark_purple_bright.xml," |
| 256 | `NECROMANCY` | Necromancy | 死灵之术 | 20 | 50 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 10 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_necromancy.xml," |
| 257 | `LIGHT` | Light | 光 | 1 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/light.xml," |
| 292 | `ELECTRIC_CHARGE` | Electric charge | 电荷放射 | 8 | 50 | `data/entities/misc/custom_cards/electric_charge.xml` | 额外抽取: 1, true \| 状态修正: c.lightning_count += 1; c.damage_electricity_add += 0.1 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/electricity.xml," |
| 293 | `MATTER_EATER` | Matter eater | 物质吞噬者 | 120 | 10 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/matter_eater.xml," |
| 294 | `FREEZE` | Freeze charge | 冰冻放射 | 10 | 50 | `data/entities/misc/custom_cards/freeze.xml` | 额外抽取: 1, true \| 状态修正: c.damage_ice_add += 0.2 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_frozen.xml,"; c.extra_entities = c.extra_entities .. "data/entities/particles/freeze_charge.xml," |
| 295 | `HITFX_BURNING_CRITICAL_HIT` | Critical on burning | 燃烧时暴击 | 10 | 50 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_burning_critical_hit.xml," |
| 296 | `HITFX_CRITICAL_WATER` | Critical on wet (water) enemies | 潮湿（浸水）敌人暴击 | 10 | 50 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_critical_water.xml," |
| 297 | `HITFX_CRITICAL_OIL` | Critical on oiled enemies | 油污敌人暴击 | 10 | 50 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_critical_oil.xml," |
| 298 | `HITFX_CRITICAL_BLOOD` | Critical on bloody enemies | 染血敌人暴击 | 10 | 50 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_critical_blood.xml," |
| 299 | `HITFX_TOXIC_CHARM` | Charm on toxic sludge | 迷惑性毒性淤泥 | 70 | 50 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_toxic_charm.xml," |
| 300 | `HITFX_EXPLOSION_SLIME` | Explosion on slimy enemies | 粘液敌人爆炸 | 20 | 50 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_explode_slime.xml," |
| 301 | `HITFX_EXPLOSION_SLIME_GIGA` | Giant explosion on slimy enemies | 粘液敌人巨型爆炸 | 200 | 20 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_explode_slime_giga.xml,data/entities/particles/tinyspark_purple.xml," |
| 302 | `HITFX_EXPLOSION_ALCOHOL` | Explosion on drunk enemies | 醉酒敌人爆炸 | 20 | 50 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_explode_alcohol.xml," |
| 303 | `HITFX_EXPLOSION_ALCOHOL_GIGA` | Giant explosion on drunk enemies | 醉酒敌人巨型爆炸 | 200 | 20 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_explode_alcohol_giga.xml,data/entities/particles/tinyspark_orange.xml," |
| 304 | `HITFX_PETRIFY` | Petrify | 石化 | 10 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_petrify.xml," |
| 305 | `ROCKET_DOWNWARDS` | Downwards bolt bundle | 下方向集束魔弹 | 90 |  | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 25 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/rocket_downwards.xml," |
| 306 | `ROCKET_OCTAGON` | Octagonal bolt bundle | 八角形集束魔弹 | 100 |  | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 20 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/rocket_octagon.xml," |
| 307 | `FIZZLE` | Fizzle | 闪灭 | 0 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.speed_multiplier *= 1.2; c.fire_rate_wait -= 10 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/fizzle.xml," |
| 308 | `BOUNCE_EXPLOSION` | Explosive bounce | 易爆的弹跳 | 20 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.bounces += 1; c.fire_rate_wait += 25 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/bounce_explosion.xml," |
| 309 | `BOUNCE_SPARK` | Bubbly bounce | 泡泡弹跳 | 20 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.bounces += 1; c.fire_rate_wait += 8 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/bounce_spark.xml," |
| 310 | `BOUNCE_LASER` | Concentrated light bounce | 汇聚之光弹跳 | 30 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.bounces += 1; c.fire_rate_wait += 12 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/bounce_laser.xml," |
| 311 | `BOUNCE_LASER_EMITTER` | Plasma Beam Bounce | 电浆束弹跳 | 40 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.bounces += 1; c.fire_rate_wait += 12 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/bounce_laser_emitter.xml," |
| 312 | `BOUNCE_LARPA` | Larpa Bounce | 拉帕弹跳 | 80 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.bounces += 1; c.fire_rate_wait += 32 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/bounce_larpa.xml," |
| 313 | `BOUNCE_SMALL_EXPLOSION` | Sparkly bounce | 火花弹跳 | 10 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.bounces += 1; c.fire_rate_wait += 9 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/bounce_small_explosion.xml," |
| 314 | `BOUNCE_LIGHTNING` | Lightning bounce | 闪电弹跳 | 40 | 150 | `` | 额外抽取: 1, true \| 状态修正: c.bounces += 1; c.fire_rate_wait += 25 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/bounce_lightning.xml," |
| 315 | `BOUNCE_HOLE` | Vacuum bounce | 吸尘弹跳 | 60 | 20 | `` | 额外抽取: 1, true \| 状态修正: c.bounces += 1; c.fire_rate_wait += 40 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/bounce_hole.xml," |
| 316 | `FIREBALL_RAY` | Fireball thrower | 火球发射器 | 110 | 16 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/fireball_ray.xml," |
| 317 | `LIGHTNING_RAY` | Lightning thrower | 闪电发射器 | 110 | 16 | `data/entities/misc/custom_cards/electric_charge.xml` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/lightning_ray.xml," |
| 318 | `TENTACLE_RAY` | Tentacler | 触手怪 | 110 | 16 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/tentacle_ray.xml," |
| 319 | `LASER_EMITTER_RAY` | Plasma Beam Thrower | 电浆束发射器 | 110 | 16 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/laser_emitter_ray.xml," |
| 320 | `FIREBALL_RAY_LINE` | Two-way fireball thrower | 双向火球发射器 | 130 | 20 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/fireball_ray_line.xml," |
| 321 | `FIREBALL_RAY_ENEMY` | Personal fireball thrower | 专属火球发射器 | 90 | 20 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_fireball_ray_enemy.xml," |
| 322 | `LIGHTNING_RAY_ENEMY` | Personal lightning caster | 专属闪电投射器 | 90 | 20 | `data/entities/misc/custom_cards/electric_charge.xml` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_lightning_ray_enemy.xml," |
| 323 | `TENTACLE_RAY_ENEMY` | Personal tentacler | 专属触手怪 | 90 | 20 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_tentacle_ray_enemy.xml," |
| 324 | `GRAVITY_FIELD_ENEMY` | Personal gravity field | 专属重力领域 | 110 | 20 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_gravity_field_enemy.xml," |
| 325 | `CURSE` | Venomous Curse | 猛毒诅咒 | 30 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_curse.xml," |
| 326 | `CURSE_WITHER_PROJECTILE` | Weakening Curse - Projectiles | 虚弱诅咒 - 投射物 | 50 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_curse_wither_projectile.xml," |
| 327 | `CURSE_WITHER_EXPLOSION` | Weakening Curse - Explosives | 虚弱诅咒 - 爆炸 | 50 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_curse_wither_explosion.xml," |
| 328 | `CURSE_WITHER_MELEE` | Weakening Curse - Melee | 虚弱诅咒 - 近战 | 50 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_curse_wither_melee.xml," |
| 329 | `CURSE_WITHER_ELECTRICITY` | Weakening Curse - Electricity | 虚弱诅咒 - 雷电 | 50 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/hitfx_curse_wither_electricity.xml," |
| 330 | `ORBIT_DISCS` | Sawblade Orbit | 锯刃环绕 | 70 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/orbit_discs.xml," |
| 331 | `ORBIT_FIREBALLS` | Fireball Orbit | 火球环绕 | 40 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/orbit_fireballs.xml," |
| 332 | `ORBIT_NUKES` | Nuke Orbit | 核弹环绕 | 250 | 3 | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/orbit_nukes.xml," |
| 333 | `ORBIT_LASERS` | Plasma Beam Orbit | 电浆束环绕 | 100 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/orbit_lasers.xml," |
| 334 | `ORBIT_LARPA` | Orbit Larpa | 环绕拉帕 | 90 |  | `` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/orbit_larpa.xml," |
| 335 | `CHAIN_SHOT` | Chain Spell | 连锁法术 | 70 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.lifetime_add -= 30; c.damage_projectile_add -= 0.2; c.explosion_radius -= 5.0; c.spread_degrees += 10.0 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/chain_shot.xml," |
| 336 | `ARC_ELECTRIC` | Electric Arc | 电弧 | 15 | 15 | `data/entities/misc/custom_cards/arc_electric.xml` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/arc_electric.xml," |
| 337 | `ARC_FIRE` | Fire Arc | 火焰弧 | 15 | 15 | `data/entities/misc/custom_cards/arc_fire.xml` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/arc_fire.xml," |
| 338 | `ARC_GUNPOWDER` | Gunpowder Arc | 火药弧 | 15 | 15 | `data/entities/misc/custom_cards/arc_gunpowder.xml` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/arc_gunpowder.xml," |
| 339 | `ARC_POISON` | Poison Arc | 毒液弧 | 15 | 15 | `data/entities/misc/custom_cards/arc_poison.xml` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/arc_poison.xml," |
| 340 | `CRUMBLING_EARTH_PROJECTILE` | Earthquake shot | 地震魔弹 | 45 | 15 | `data/entities/misc/custom_cards/arc_poison.xml` | 额外抽取: 1, true \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/crumbling_earth_projectile.xml," |
| 342 | `UNSTABLE_GUNPOWDER` | Firecrackers | 爆竹 | 15 | 20 | `data/entities/misc/custom_cards/unstable_gunpowder.xml` | 额外抽取: 1, true \| 状态修正: c.material_amount += 10 \| 状态赋值: c.material = "gunpowder_unstable" |
| 343 | `ACID_TRAIL` | Acid trail | 酸液轨迹 | 15 | 50 | `data/entities/misc/custom_cards/acid_trail.xml` | 额外抽取: 1, true \| 状态修正: c.trail_material_amount += 5 \| 状态赋值: c.trail_material = c.trail_material .. "acid," |
| 344 | `POISON_TRAIL` | Poison trail | 毒素轨迹 | 10 | 50 | `data/entities/misc/custom_cards/poison_trail.xml` | 额外抽取: 1, true \| 状态修正: c.trail_material_amount += 9 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_apply_poison.xml,"; c.trail_material = c.trail_material .. "poison," |
| 345 | `OIL_TRAIL` | Oil trail | 油液轨迹 | 10 | 50 | `data/entities/misc/custom_cards/oil_trail.xml` | 额外抽取: 1, true \| 状态修正: c.trail_material_amount += 20 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_apply_oiled.xml,"; c.trail_material = c.trail_material .. "oil," |
| 346 | `WATER_TRAIL` | Water trail | 清水轨迹 | 10 | 50 | `data/entities/misc/custom_cards/water_trail.xml` | 额外抽取: 1, true \| 状态修正: c.trail_material_amount += 20 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_apply_wet.xml,"; c.trail_material = c.trail_material .. "water," |
| 347 | `GUNPOWDER_TRAIL` | Gunpowder trail | 火药轨迹 | 10 | 50 | `data/entities/misc/custom_cards/gunpowder_trail.xml` | 额外抽取: 1, true \| 状态修正: c.trail_material_amount += 20 \| 状态赋值: c.trail_material = c.trail_material .. "gunpowder," |
| 348 | `FIRE_TRAIL` | Fire trail | 火焰轨迹 | 10 | 50 | `data/entities/misc/custom_cards/fire_trail.xml` | 额外抽取: 1, true \| 状态修正: c.trail_material_amount += 10 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_apply_on_fire.xml,"; c.trail_material = c.trail_material .. "fire," |
| 349 | `BURN_TRAIL` | Burning trail | 燃烧轨迹 | 5 | 120 | `data/entities/misc/custom_cards/burn_trail.xml` | 额外抽取: 1, true \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_apply_on_fire.xml,"; c.extra_entities = c.extra_entities .. "data/entities/misc/burn.xml," |
| 354 | `ENERGY_SHIELD_SHOT` | Projectile energy shield | 投射物能量盾 | 5 |  | `` | 额外抽取: 1, true \| 状态修正: c.speed_multiplier *= 0.4 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/energy_shield_shot.xml," |
| 371 | `RANDOM_MODIFIER` | Random modifier spell | 随机修正法术 | 20 |  | `` | 引擎调用: SetRandomSeed(GameGetFrameNum(); Random(1, #actions); Random(1, #actions) |
| 387 | `LARPA_CHAOS` | Chaos larpa | 混沌拉帕 | 100 | 20 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 15 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/larpa_chaos.xml," |
| 388 | `LARPA_DOWNWARDS` | Downwards larpa | 下方向拉帕 | 120 | 20 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 15 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/larpa_downwards.xml," |
| 389 | `LARPA_UPWARDS` | Upwards larpa | 上方向拉帕 | 120 | 20 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 15 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/larpa_upwards.xml," |
| 390 | `LARPA_CHAOS_2` | Copy trail | 复制轨迹 | 150 | 20 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 20 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/larpa_chaos_2.xml," |
| 391 | `LARPA_DEATH` | Larpa Explosion | 拉帕爆炸 | 90 | 30 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 15 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/larpa_death.xml," |
| 413 | `COLOUR_RED` | Red Glimmer | 红色闪光 | 0 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.screenshake -= 2.5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_red.xml,data/entities/misc/colour_red.xml," |
| 414 | `COLOUR_ORANGE` | Orange Glimmer | 橙色闪光 | 0 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.screenshake -= 2.5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_red.xml,data/entities/misc/colour_orange.xml," |
| 415 | `COLOUR_GREEN` | Green Glimmer | 绿色闪光 | 0 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.screenshake -= 2.5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_red.xml,data/entities/misc/colour_green.xml," |
| 416 | `COLOUR_YELLOW` | Yellow Glimmer | 黄色闪光 | 0 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.screenshake -= 2.5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_red.xml,data/entities/misc/colour_yellow.xml," |
| 417 | `COLOUR_PURPLE` | Purple Glimmer | 紫色闪光 | 0 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.screenshake -= 2.5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_red.xml,data/entities/misc/colour_purple.xml," |
| 418 | `COLOUR_BLUE` | Blue Glimmer | 蓝色闪光 | 0 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.screenshake -= 2.5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_red.xml,data/entities/misc/colour_blue.xml," |
| 419 | `COLOUR_RAINBOW` | Rainbow Glimmer | 彩虹闪光 | 0 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.screenshake -= 2.5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/particles/tinyspark_red.xml,data/entities/misc/colour_rainbow.xml," |
| 420 | `COLOUR_INVIS` | Invisible Spell | 隐形法术 | 0 | 100 | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait -= 8; c.screenshake -= 2.5 \| 状态赋值: c.extra_entities = c.extra_entities .. "data/entities/misc/colour_invis.xml," |
| 421 | `RAINBOW_TRAIL` | Rainbow trail | 彩虹轨迹 | 0 | 50 | `data/entities/misc/custom_cards/rainbow_trail.xml` | 额外抽取: 1, true \| 状态修正: c.trail_material_amount += 20 \| 状态赋值: c.game_effect_entities = c.game_effect_entities .. "data/entities/misc/effect_rainbow_farts.xml,"; c.trail_material = c.trail_material .. "material_rainbow," |

## 来源

- noitadata `gun_actions.lua`: https://github.com/NathanSnail/noitadata/blob/main/data/scripts/gun/gun_actions.lua
- noitadata `common.csv`: https://github.com/NathanSnail/noitadata/blob/main/data/translations/common.csv
