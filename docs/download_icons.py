import urllib.request
import urllib.parse
import os
import time

base = "https://noita.wiki.gg"
out_dir = "E:/MC-noita/docs/spell_icons"
os.makedirs(out_dir, exist_ok=True)
os.makedirs(f"{out_dir}/demos", exist_ok=True)

# (raw_path, local_filename)
# raw_path uses the exact path from wiki URLs (with ?query stripped)
items = [
    ("zh/images/thumb/Timer.png/48px-Timer.png", "timer.png"),
    ("zh/images/thumb/Trigger.png/48px-Trigger.png", "trigger.png"),
    ("zh/images/thumb/Double_Trigger.png/48px-Double_Trigger.png", "double_trigger.png"),
    ("images/Inventory_Icon_action_max_uses.png", "stat_uses.png"),
    ("images/Inventory_Icon_mana_drain.png", "stat_mana.png"),
    ("images/Icon_damage_projectile.png", "stat_dmg_proj.png"),
    ("images/thumb/Inventory_Icon_damage_fire.png/20px-Inventory_Icon_damage_fire.png", "stat_dmg_fire.png"),
    ("images/thumb/Inventory_Icon_damage_electricity.png/20px-Inventory_Icon_damage_electricity.png", "stat_dmg_elec.png"),
    ("images/Inventory_Icon_damage_explosion.png", "stat_dmg_expl.png"),
    ("images/Inventory_Icon_explosion_radius.png", "stat_expl_radius.png"),
    ("images/Inventory_Icon_spread_degrees.png", "stat_spread.png"),
    ("images/Inventory_icon_speed_initial.png", "stat_speed.png"),
    ("images/Inventory_Icon_fire_rate_wait.png", "stat_cast_delay.png"),
    ("images/thumb/Inventory_Icon_gun_reload_time.png/20px-Inventory_Icon_gun_reload_time.png", "stat_reload.png"),
    ("images/Inventory_Icon_damage_critical_chance.png", "stat_crit.png"),
    ("images/Inventory_Icon_lifetime.png", "stat_lifetime.png"),
    ("images/Lifetime.png", "stat_lifetime2.png"),
    ("images/thumb/Inventory_Icon_recoil.png/20px-Inventory_Icon_recoil.png", "stat_recoil.png"),
    ("images/Inventory_Icon_speed_modifier.png", "stat_speed_mod.png"),
    ("images/Icon_damage_slice.png", "stat_dmg_slice.png"),
    ("images/Icon_damage_ice.png", "stat_dmg_ice.png"),
    ("zh/images/Demo_light_bullet.gif", "demos/spark_bolt.gif"),
    ("zh/images/Demo_spitter.gif", "demos/spitter.gif"),
    ("zh/images/Demo_air_bullet.gif", "demos/air_bullet.gif"),
    ("zh/images/Demo_slow_bullet.gif", "demos/slow_bullet.gif"),
    ("zh/images/Demo_pipe_bomb.gif", "demos/pipe_bomb.gif"),
    ("zh/images/Demo_luminous_drill.gif", "demos/luminous_drill.gif"),
    ("zh/images/Demo_disc_bullet.gif", "demos/disc_bullet.gif"),
    ("zh/images/Demo_Omega_Sawblade.gif", "demos/omega_sawblade.gif"),
    ("zh/images/Demo_rubber_ball.gif", "demos/bouncing_burst.gif"),
    ("zh/images/Demo_bubbleshot.gif", "demos/bubble_spark.gif"),
    ("zh/images/Spelldemo_black_hole_1.gif", "demos/black_hole.gif"),
    ("zh/images/Earthquake_spell_effect.gif", "demos/earthquake.gif"),
    ("zh/images/Spelldemo_plasma_cutter.gif", "demos/plasma_cutter.gif"),
    ("zh/images/Spelldemo_energy_orb.gif", "demos/energy_orb.gif"),
    ("zh/images/Spelldemo_magic_arrow.gif", "demos/magic_arrow.gif"),
    ("images/Spelldemo_black_hole_2.gif", "demos/black_hole2.gif"),
    ("zh/images/thumb/Demo_Omega_Sawblade_Orbiting.gif/500px-Demo_Omega_Sawblade_Orbiting.gif", "demos/omega_sawblade_orbiting.gif"),
    ("images/CUSTOM_CARD_BLACK_HOLE.gif", "demos/black_hole_card.gif"),
    ("images/CUSTOM_CARD_SLOW_BULLET.gif", "demos/slow_bullet_card.gif"),
]

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'image/webp,image/apng,image/*,*/*;q=0.8',
    'Referer': 'https://noita.wiki.gg/zh/wiki/法术'
}

ok = 0
fail = 0
skip = 0

for raw_path, fname in items:
    dest = os.path.join(out_dir, fname)
    if os.path.exists(dest) and os.path.getsize(dest) > 1000:
        skip += 1
        continue

    # Properly encode the path
    encoded = urllib.parse.quote(raw_path, safe='/')
    url = f"{base}/{encoded}"

    try:
        req = urllib.request.Request(url, headers=headers)
        resp = urllib.request.urlopen(req, timeout=20)
        data = resp.read()
        if len(data) > 500:
            with open(dest, 'wb') as f:
                f.write(data)
            ok += 1
            print(f"OK: {fname} ({len(data)//1024}KB)")
        else:
            fail += 1
            print(f"SMALL: {fname} ({len(data)}B)")
    except Exception as e:
        fail += 1
        print(f"FAIL: {fname} - {str(e)[:60]}")
    time.sleep(0.3)

print(f"\nDone! OK={ok}, SKIP={skip}, FAIL={fail}")
