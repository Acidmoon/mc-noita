# 其他_特殊法术（ACTION_TYPE_OTHER）

> 生成日期：2026-06-07。共 42 项。执行随机、复制、刷牌、希腊字母、终结类等特殊逻辑。

## 读取规则

- `行为摘要` 是从 `action()` 中提取的关键调用和状态改写，用来定位底层逻辑，不等于完整源码。
- 触发/定时/失效触发的投射物通常表现为 `add_projectile_trigger_hit_world`、`add_projectile_trigger_timer`、`add_projectile_trigger_death`。
- 修正器、多重释放、复制类法术经常只改写状态或抽取后续 action，不一定直接生成投射物。

## 法术列表

| # | ID | 英文名 | 中文名 | 法力 | 次数 | 关联投射物/XML | 行为摘要 |
|---:|---|---|---|---:|---:|---|---|
| 198 | `DUPLICATE` | Spell duplication | 法术复制 | 250 |  | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 20; current_reload_time += 20 |
| 356 | `OCARINA_A` | Ocarina - note A | 陶笛 - 音符 A | 1 |  | `data/entities/projectiles/deck/ocarina/ocarina_a.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/ocarina/ocarina_a.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 357 | `OCARINA_B` | Ocarina - note B | 陶笛 - 音符 B | 1 |  | `data/entities/projectiles/deck/ocarina/ocarina_b.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/ocarina/ocarina_b.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 358 | `OCARINA_C` | Ocarina - note C | 陶笛 - 音符 C | 1 |  | `data/entities/projectiles/deck/ocarina/ocarina_c.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/ocarina/ocarina_c.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 359 | `OCARINA_D` | Ocarina - note D | 陶笛 - 音符 D | 1 |  | `data/entities/projectiles/deck/ocarina/ocarina_d.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/ocarina/ocarina_d.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 360 | `OCARINA_E` | Ocarina - note E | 陶笛 - 音符 E | 1 |  | `data/entities/projectiles/deck/ocarina/ocarina_e.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/ocarina/ocarina_e.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 361 | `OCARINA_F` | Ocarina - note F | 陶笛 - 音符 F | 1 |  | `data/entities/projectiles/deck/ocarina/ocarina_f.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/ocarina/ocarina_f.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 362 | `OCARINA_GSHARP` | Ocarina - note G# | 陶笛 - 音符 G# | 1 |  | `data/entities/projectiles/deck/ocarina/ocarina_gsharp.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/ocarina/ocarina_gsharp.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 363 | `OCARINA_A2` | Ocarina - note A2 | 陶笛 - 音符 A2 | 1 |  | `data/entities/projectiles/deck/ocarina/ocarina_a2.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/ocarina/ocarina_a2.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 364 | `KANTELE_A` | Kantele - note A | 齐特琴 - 音符 A | 1 |  | `data/entities/projectiles/deck/kantele/kantele_a.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/kantele/kantele_a.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 365 | `KANTELE_D` | Kantele - note D | 齐特琴 - 音符 D | 1 |  | `data/entities/projectiles/deck/kantele/kantele_d.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/kantele/kantele_d.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 366 | `KANTELE_DIS` | Kantele - note D# | 齐特琴 - 音符 D# | 1 |  | `data/entities/projectiles/deck/kantele/kantele_dis.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/kantele/kantele_dis.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 367 | `KANTELE_E` | Kantele - note E | 齐特琴 - 音符 E | 1 |  | `data/entities/projectiles/deck/kantele/kantele_e.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/kantele/kantele_e.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 368 | `KANTELE_G` | Kantele - note G | 齐特琴 - 音符 G | 1 |  | `data/entities/projectiles/deck/kantele/kantele_g.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/kantele/kantele_g.xml") \| 状态修正: c.fire_rate_wait += 15 |
| 369 | `RANDOM_SPELL` | Random spell | 随机法术 | 5 |  | `` | 引擎调用: SetRandomSeed(GameGetFrameNum(); Random(1, #actions); Random(1, #actions) |
| 373 | `DRAW_RANDOM` | Copy random spell | 复制随机法术 | 20 |  | `` | 引擎调用: SetRandomSeed(GameGetFrameNum(); Random(1, datasize) |
| 374 | `DRAW_RANDOM_X3` | Copy random spell thrice | 三次复制随机法术 | 50 |  | `` | 引擎调用: SetRandomSeed(GameGetFrameNum(); Random(1, datasize) |
| 375 | `DRAW_3_RANDOM` | Copy three random spells | 复制三个随机法术 | 40 |  | `` | 引擎调用: SetRandomSeed(GameGetFrameNum(); Random(1, datasize) |
| 382 | `ALL_SPELLS` | The end of everything | 万物之终结 | 600 | 1 | `` | 状态修正: c.fire_rate_wait += 100; current_reload_time += 100 \| 引擎调用: EntityLoad("data/entities/projectiles/deck/all_spells_loader.xml", x, y) |
| 383 | `SUMMON_PORTAL` | Summon portal | 召唤传送门 | 50 | 7 | `data/entities/misc/custom_cards/summon_portal.xml` | 投射物/触发: add_projectile("data/entities/projectiles/deck/summon_portal.xml") \| 状态修正: c.fire_rate_wait += 80 |
| 384 | `ADD_TRIGGER` | Add trigger | 追加触发 | 10 | 50 | `` | 投射物/触发: add_projectile_trigger_hit_world(target, 1) |
| 385 | `ADD_TIMER` | Add timer | 追加计时 | 20 | 50 | `` | 投射物/触发: add_projectile_trigger_timer(target, 20, 1) |
| 386 | `ADD_DEATH_TRIGGER` | Add expiration trigger | 追加失效触发 | 20 | 50 | `` | 投射物/触发: add_projectile_trigger_death(target, 1) |
| 392 | `ALPHA` | Alpha | 初始 | 40 |  | `` | 状态修正: c.fire_rate_wait += 15 |
| 393 | `GAMMA` | Gamma | 伽马校正 | 40 |  | `` | 状态修正: c.fire_rate_wait += 15 |
| 394 | `TAU` | Tau | 陶 | 90 |  | `` | 状态修正: c.fire_rate_wait += 35 |
| 395 | `OMEGA` | Omega | 终结 | 320 |  | `` | 状态修正: c.fire_rate_wait += 50 |
| 396 | `MU` | Mu | 谬 | 120 |  | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 50 |
| 397 | `PHI` | Phi | 斐 | 120 |  | `` | 状态修正: c.fire_rate_wait += 50 |
| 398 | `SIGMA` | Sigma | 西格玛 | 120 |  | `` | 额外抽取: 1, true \| 状态修正: c.fire_rate_wait += 30 |
| 399 | `ZETA` | Zeta | 泽塔 | 10 |  | `` | 额外抽取: 1, true \| 引擎调用: SetRandomSeed(x + GameGetFrameNum(); Random(1, #options) |
| 400 | `DIVIDE_2` | Divide by 2 | 一分为二 | 35 |  | `` | 状态修正: c.fire_rate_wait += 20; c.damage_projectile_add -= 0.2; c.explosion_radius -= 5.0 \| 状态赋值: c.pattern_degrees = 5 |
| 401 | `DIVIDE_3` | Divide by 3 | 一分为三 | 50 |  | `` | 状态修正: c.fire_rate_wait += 35; c.damage_projectile_add -= 0.4; c.explosion_radius -= 10.0 \| 状态赋值: c.pattern_degrees = 5 |
| 402 | `DIVIDE_4` | Divide by 4 | 一分为四 | 70 |  | `` | 状态修正: c.fire_rate_wait += 50; c.damage_projectile_add -= 0.6; c.explosion_radius -= 20.0 \| 状态赋值: c.pattern_degrees = 5 |
| 403 | `DIVIDE_10` | Divide by 10 | 一分为十 | 200 | 5 | `` | 状态修正: c.fire_rate_wait += 80; c.damage_projectile_add -= 1.5; c.explosion_radius -= 40.0; current_reload_time += 20 \| 状态赋值: c.pattern_degrees = 5 |
| 407 | `IF_ENEMY` | Requirement - Enemies | 要求 - 敌人数量 | 0 |  | `` | 额外抽取: 1, true |
| 408 | `IF_PROJECTILE` | Requirement - Projectile Spells | 要求 - 投射物法术数量 | 0 |  | `` | 额外抽取: 1, true |
| 409 | `IF_HP` | Requirement - Low Health | 要求 - 生命值低下 | 0 |  | `` | 额外抽取: 1, true |
| 410 | `IF_HALF` | Requirement - Every Other | 要求 - 每次 | 0 |  | `` | 额外抽取: 1, true |
| 411 | `IF_END` | Requirement - Endpoint | 要求 - 终点 | 0 |  | `` | 额外抽取: 1, true |
| 412 | `IF_ELSE` | Requirement - Otherwise | 要求 - 否则 | 0 |  | `` | 额外抽取: 1, true |
| 422 | `CESSATION` | Cessation | 不复存在 | 0 | 25 | `data/entities/misc/custom_cards/rainbow_trail.xml` | 状态修正: c.fire_rate_wait += 600; current_reload_time += 600 \| 引擎调用: StartReload(current_reload_time) |

## 来源

- noitadata `gun_actions.lua`: https://github.com/NathanSnail/noitadata/blob/main/data/scripts/gun/gun_actions.lua
- noitadata `common.csv`: https://github.com/NathanSnail/noitadata/blob/main/data/translations/common.csv
