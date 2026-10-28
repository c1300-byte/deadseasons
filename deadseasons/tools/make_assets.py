"""
Generates every asset and data file for Dead Seasons: textures, sounds, models, blockstates, lang,
recipes, loot tables, damage types, worldgen, spawns and the truck interior dimension.

Run from the project root:   python tools/make_assets.py
Requires: Pillow, numpy, scipy, and ffmpeg (with libvorbis) on PATH for sounds.
"""
import json
import os
import random
import shutil
import subprocess
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from sprites import PALETTE, S, ARMOR  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "main", "resources")
MOD = "deadseasons"
A = os.path.join(ROOT, "assets", MOD)
D = os.path.join(ROOT, "data", MOD)
DMC = os.path.join(ROOT, "data", "minecraft")
DFORGE = os.path.join(ROOT, "data", "forge")

rng = random.Random(1234)


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def save(img, *parts):
    path = os.path.join(A, "textures", *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def clamp(v):
    return max(0, min(255, int(v)))


def jitter(c, amt=10, r=None):
    r = r or rng
    j = r.randint(-amt, amt)
    return (clamp(c[0] + j), clamp(c[1] + j), clamp(c[2] + j), 255 if len(c) < 4 else c[3])


def hexc(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


# =============================================================== item sprites

def sprite(rows, palette=PALETTE, shade=True):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows[:16]):
        row = row.ljust(16, ".")[:16]
        for x, ch in enumerate(row):
            if ch == "." or ch not in palette:
                continue
            c = palette[ch]
            px[x, y] = (c[0], c[1], c[2], 255)
    return img


def make_items():
    for name, rows in S.items():
        save(sprite(rows), "item", name + ".png")

    armor_colors = {
        "military_helmet": ("helmet", 0x5A6B2F, 0x47552A, 0x3D4A22),
        "military_vest": ("chest", 0x5A6B2F, 0x47552A, 0x6E5A3A),
        "military_pants": ("legs", 0x5A6B2F, 0x47552A, 0x3A2A1A),
        "military_boots": ("boots", 0x3A3A3A, 0x2A2A2A, 0x1A1A1A),
        "riot_helmet": ("helmet", 0x22252C, 0x16181D, 0x3D5AA8),
        "riot_armor": ("chest", 0x22252C, 0x16181D, 0x3D5AA8),
        "riot_leggings": ("legs", 0x22252C, 0x16181D, 0x3D5AA8),
        "riot_boots": ("boots", 0x22252C, 0x16181D, 0x111111),
        "winter_hat": ("beanie", 0xB02A2A, 0x8A1F1F, 0xEEEEEE),
        "winter_coat": ("chest", 0x2C3E6B, 0x1F2C4D, 0xD9C9A8),
        "snow_pants": ("legs", 0x5A5F66, 0x44484E, 0x2C3E6B),
        "snow_boots": ("boots", 0x6B4423, 0x4A2F17, 0xD9C9A8),
        "sun_hat": ("sun_hat", 0xD8C07A, 0xB8A060, 0x8B5A2B),
        "light_shirt": ("chest", 0xF0F0E8, 0xD0D0C8, 0x3FB0C8),
        "cargo_shorts": ("shorts", 0xB8A070, 0x9A8458, 0x8B7A50),
        "sneakers": ("boots", 0xF0F0F0, 0xCCCCCC, 0xB02A2A),
        "gas_mask": ("gas_mask", 0x444444, 0x333333, 0x555555),
    }
    for name, (tmpl, a, sh, x) in armor_colors.items():
        pal = dict(PALETTE)
        pal["A"] = hexc(a)
        pal["a"] = hexc(sh)
        pal["X"] = hexc(x)
        save(sprite(ARMOR[tmpl], pal), "item", name + ".png")

    # Door item
    door = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = door.load()
    for y in range(1, 16):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (1, 15)
            c = (40, 44, 50) if edge else (95, 102, 112)
            if 3 <= y <= 6 and 6 <= x <= 9:
                c = (120, 170, 200)
            if y == 9 and x == 10:
                c = (200, 180, 60)
            p[x, y] = c + (255,)
    save(door, "item", "reinforced_door.png")


# =============================================================== block textures

def noise_tex(base, amt=12, seed=0, size=16):
    r = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = jitter(base, amt, r)
    return img


def steel_plate(seed=1, base=(92, 98, 108)):
    img = noise_tex(base, 6, seed)
    px = img.load()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = (50, 54, 60, 255)
        px[i, 7] = (70, 75, 82, 255)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13), (2, 9), (13, 9), (7, 2), (7, 13)):
        px[x, y] = (150, 156, 165, 255)
        px[x + 1, y + 1] = (45, 48, 52, 255)
    return img


def planks_barricade(stage, seed=0):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    r = random.Random(seed + stage * 17)
    boards = [(1, 4), (6, 9), (11, 14)]
    removed = {0: [], 1: [], 2: [1], 3: [0, 2]}[stage]
    for bi, (y1, y2) in enumerate(boards):
        if bi in removed:
            continue
        for y in range(y1, y2 + 1):
            for x in range(16):
                c = (150, 110, 62) if y not in (y1, y2) else (110, 78, 42)
                px[x, y] = jitter(c, 10, r)
        for nx in (2, 13):
            px[nx, (y1 + y2) // 2] = (70, 70, 74, 255)
    # diagonal brace
    if stage < 3:
        for i in range(16):
            for w in (0, 1):
                x = i
                y = 15 - i + w
                if 0 <= y < 16:
                    px[x, y] = jitter((128, 92, 50), 8, r)
    # cracks
    for _ in range(stage * 6):
        x, y = r.randint(0, 15), r.randint(0, 15)
        if px[x, y][3] > 0:
            px[x, y] = (40, 28, 16, 255)
    return img


def metal_barricade(stage, seed=0):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    r = random.Random(seed + 99 + stage)
    for y in range(16):
        for x in range(16):
            if y in (0, 15) or x in (0, 15):
                px[x, y] = (60, 62, 66, 255)
            elif y % 5 != 0:
                c = (130, 134, 140) if x % 2 == 0 else (105, 108, 114)
                px[x, y] = jitter(c, 6, r)
    holes = stage * 5
    for _ in range(holes):
        x, y = r.randint(2, 13), r.randint(2, 13)
        px[x, y] = (0, 0, 0, 0)
        px[x + 1, y] = (0, 0, 0, 0)
    for _ in range(stage * 8):
        x, y = r.randint(1, 14), r.randint(1, 14)
        if px[x, y][3]:
            px[x, y] = (140, 80, 40, 255)  # rust
    return img


def make_blocks():
    b = lambda img, name: save(img, "block", name + ".png")

    b(steel_plate(3), "reinforced_wall")
    top = steel_plate(5, (88, 95, 104))
    tp = top.load()
    for y in range(3, 9):
        for x in range(4, 12):
            tp[x, y] = (110, 160, 190, 255) if (x + y) % 5 else (170, 210, 230, 255)
    b(top, "reinforced_door_top")
    bottom = steel_plate(6, (88, 95, 104))
    bp = bottom.load()
    bp[12, 2] = (200, 180, 60, 255)
    bp[12, 3] = (200, 180, 60, 255)
    b(bottom, "reinforced_door_bottom")

    for s in range(4):
        b(planks_barricade(s), f"wooden_barricade_{s}")
        b(metal_barricade(s), f"metal_barricade_{s}")

    # barbed wire (cross model)
    wire = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wp = wire.load()
    for x in range(16):
        for (cy, amp, ph) in ((4, 2.5, 0.0), (11, 2.5, 1.5)):
            y = int(round(cy + amp * np.sin(x * 0.9 + ph)))
            wp[x, max(0, min(15, y))] = (150, 152, 158, 255)
            if x % 4 == 1:
                for dy in (-1, 1):
                    yy = max(0, min(15, y + dy))
                    wp[x, yy] = (110, 112, 118, 255)
    b(wire, "barbed_wire")

    plate = noise_tex((70, 72, 76), 8, 21)
    pp = plate.load()
    for i in range(16):
        pp[i, 0] = pp[i, 15] = pp[0, i] = pp[15, i] = (45, 46, 50, 255)
    b(plate, "spike_trap_base")
    spike = noise_tex((150, 152, 158), 10, 22)
    sp = spike.load()
    for y in range(4):
        for x in range(16):
            sp[x, y] = (200, 202, 208, 255)
    b(spike, "spike")

    mine = noise_tex((78, 92, 46), 8, 31)
    mp = mine.load()
    for y in range(16):
        for x in range(16):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 6:
                mp[x, y] = (60, 60, 60, 255)
    mp[7, 7] = mp[8, 8] = (190, 40, 40, 255)
    b(mine, "landmine")

    b(noise_tex((212, 175, 55), 14, 41), "alarm_bell")
    b(noise_tex((70, 60, 50), 8, 42), "alarm_bell_post")

    flood = Image.new("RGBA", (16, 16))
    fp = flood.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if x in (0, 15) or y in (0, 15):
                fp[x, y] = (50, 52, 58, 255)
            else:
                v = 255 - int(d * 9)
                fp[x, y] = (v, v, clamp(v - 30), 255)
    b(flood, "floodlight_front")
    b(steel_plate(51, (60, 64, 70)), "floodlight_side")

    b(steel_plate(61, (70, 76, 84)), "turret_base")
    tt = noise_tex((50, 54, 60), 6, 62)
    ttp = tt.load()
    ttp[7, 7] = ttp[8, 8] = (200, 40, 40, 255)
    b(tt, "turret_top")
    b(noise_tex((30, 30, 34), 4, 63), "turret_barrel")

    grow = Image.new("RGBA", (16, 16))
    gp = grow.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                gp[x, y] = (40, 40, 44, 255)
            else:
                v = 200 + ((x * 7 + y * 3) % 40)
                gp[x, y] = (clamp(v), clamp(v * 0.55), 255, 255)
    b(grow, "grow_lamp")

    barrel_side = noise_tex((120, 86, 48), 10, 71)
    bsp = barrel_side.load()
    for x in range(16):
        bsp[x, 3] = bsp[x, 12] = (70, 70, 74, 255)
    b(barrel_side, "rain_collector_side")
    b(noise_tex((100, 72, 40), 8, 72), "rain_collector_inside")
    water = noise_tex((50, 90, 200), 10, 73)
    b(water, "rain_water")

    radio = noise_tex((60, 62, 58), 6, 81)
    rp = radio.load()
    for y in range(3, 12):
        for x in range(2, 8):
            rp[x, y] = (30, 30, 30, 255) if (x + y) % 2 else (45, 45, 45, 255)
    for x in range(9, 14):
        rp[x, 4] = (220, 190, 80, 255)
    rp[11, 8] = rp[12, 8] = (180, 180, 180, 255)
    rp[11, 9] = rp[12, 9] = (120, 120, 120, 255)
    b(radio, "radio_front")
    b(noise_tex((60, 62, 58), 6, 82), "radio_side")

    crate = noise_tex((88, 104, 56), 10, 91)
    cp = crate.load()
    for i in range(16):
        cp[i, 0] = cp[i, 15] = cp[0, i] = cp[15, i] = (60, 70, 38, 255)
        cp[i, i] = (66, 78, 42, 255)
    for x in range(5, 11):
        cp[x, 7] = cp[x, 8] = (230, 230, 230, 255)
    b(crate, "supply_crate_side")
    ctop = noise_tex((88, 104, 56), 10, 92)
    ctp = ctop.load()
    for i in range(16):
        ctp[i, 0] = ctp[i, 15] = ctp[0, i] = ctp[15, i] = (60, 70, 38, 255)
    for x in range(6, 10):
        for y in range(4, 12):
            ctp[x, y] = (230, 230, 230, 255)
    for y in range(6, 10):
        for x in range(4, 12):
            ctp[x, y] = (230, 230, 230, 255)
    b(ctop, "supply_crate_top")

    blood = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    blp = blood.load()
    r = random.Random(101)
    for _ in range(5):
        cx, cy, rad = r.randint(3, 12), r.randint(3, 12), r.uniform(1.5, 3.5)
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 < rad * rad:
                    blp[x, y] = jitter((120, 10, 10), 15, r)
    for _ in range(14):
        blp[r.randint(0, 15), r.randint(0, 15)] = (100, 8, 8, 255)
    b(blood, "blood_splatter")

    tw = noise_tex((226, 226, 220), 4, 111)
    twp = tw.load()
    for y in range(16):
        twp[0, y] = twp[15, y] = (170, 170, 165, 255)
    for x in range(16):
        twp[x, 5] = twp[x, 10] = (200, 200, 195, 255)
    b(tw, "truck_wall")
    tf = Image.new("RGBA", (16, 16))
    tfp = tf.load()
    for y in range(16):
        for x in range(16):
            tfp[x, y] = (150, 156, 164, 255) if (x + 2 * y) % 6 in (0, 1) else (110, 116, 124, 255)
    b(tf, "truck_floor")
    tl = Image.new("RGBA", (16, 16))
    tlp = tl.load()
    for y in range(16):
        for x in range(16):
            tlp[x, y] = (180, 180, 175, 255) if x in (0, 15) or y in (0, 15) else (255, 252, 230, 255)
    b(tl, "truck_light")
    te = noise_tex((140, 146, 152), 4, 121)
    tep = te.load()
    for y in range(0, 16, 2):
        for x in range(16):
            tep[x, y] = (100, 104, 110, 255)
    tep[7, 13] = tep[8, 13] = (230, 190, 60, 255)
    b(te, "truck_exit")


# =============================================================== humanoid skins (64x64)

def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def paint(img, rect, fn):
    px = img.load()
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = fn(x, y, w, h)
            if c is not None:
                px[x0 + x, y0 + y] = c


PARTS_CLASSIC = {
    "head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4),
    "rarm": (40, 16, 4, 12, 4), "rleg": (0, 16, 4, 12, 4),
}
PARTS_PLAYER_LEFT = {"larm": (32, 48, 4, 12, 4), "lleg": (16, 48, 4, 12, 4)}


def skin(spec, seed, player_layout=False):
    r = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    sk = spec["skin"]
    shirt = spec["shirt"]
    pants = spec["pants"]
    shoes = spec.get("shoes", (40, 36, 32))
    hair = spec.get("hair")
    blood = spec.get("blood", 0.0)
    rips = spec.get("rips", 0.0)
    sleeves = spec.get("sleeves", 4)
    camo = spec.get("camo")
    spots = spec.get("spots")

    def cloth(base):
        def f(x, y, w, h):
            if camo and r.random() < 0.3:
                return jitter(camo, 8, r)
            if r.random() < blood:
                return jitter((110, 12, 12), 15, r)
            if r.random() < rips:
                return jitter(sk, 10, r)
            return jitter(base, 8, r)
        return f

    def flesh(x, y, w, h):
        if spots and r.random() < 0.12:
            return jitter(spots, 12, r)
        if r.random() < blood * 0.6:
            return jitter((110, 12, 12), 15, r)
        return jitter(sk, 9, r)

    # Head
    hf = faces(*PARTS_CLASSIC["head"])
    for name, rect in hf.items():
        if name == "top":
            paint(img, rect, (lambda x, y, w, h: jitter(hair, 8, r)) if hair else flesh)
        elif name == "bottom":
            paint(img, rect, flesh)
        elif name == "front":
            def face(x, y, w, h):
                if hair and y == 0:
                    return jitter(hair, 8, r)
                if y == 3 and x in (1, 2, 5, 6):
                    return spec.get("eye_socket", (30, 20, 20, 255))
                if y == 4 and x in (1, 6):
                    return spec.get("eye_socket", (30, 20, 20, 255))
                if y == 4 and x in (2, 5):
                    return spec.get("eye", (200, 40, 30, 255))
                mouth = spec.get("mouth", "line")
                if mouth == "scream" and 5 <= y <= 7 and 2 <= x <= 5:
                    return (20, 5, 5, 255)
                if mouth == "line" and y == 6 and 2 <= x <= 5:
                    return (60, 25, 25, 255) if x % 2 else (200, 190, 170, 255)
                if mouth == "drool" and y in (6, 7) and 2 <= x <= 5:
                    return (120, 220, 60, 255) if y == 7 and x in (3, 4) else (40, 60, 20, 255)
                return flesh(x, y, w, h)
            paint(img, rect, face)
        else:
            paint(img, rect, lambda x, y, w, h: jitter(hair, 8, r) if hair and y <= (5 if name == "back" else 2) else flesh(x, y, w, h))
    # Body
    for rect in faces(*PARTS_CLASSIC["body"]).values():
        paint(img, rect, cloth(shirt))
    # Arms
    limbs = [PARTS_CLASSIC["rarm"]] + ([PARTS_PLAYER_LEFT["larm"]] if player_layout else [])
    for uv in limbs:
        for name, rect in faces(*uv).items():
            paint(img, rect, lambda x, y, w, h, name=name: cloth(shirt)(x, y, w, h)
                  if (name == "top" or y < sleeves) and name != "bottom" else flesh(x, y, w, h))
    # Legs
    legs = [PARTS_CLASSIC["rleg"]] + ([PARTS_PLAYER_LEFT["lleg"]] if player_layout else [])
    for uv in legs:
        for name, rect in faces(*uv).items():
            paint(img, rect, lambda x, y, w, h, name=name: jitter(shoes, 6, r)
                  if name == "bottom" or y >= 10 else cloth(pants)(x, y, w, h))
    # Hat layer extras (helmets etc.)
    if spec.get("hat"):
        hat = spec["hat"]
        for name, rect in faces(32, 0, 8, 8, 8).items():
            if name == "bottom":
                continue
            paint(img, rect, lambda x, y, w, h, name=name: jitter(hat, 6, r)
                  if name == "top" or y <= (1 if name == "front" else 3) else None)
    return img


ZOMBIES = {
    "walker": dict(skin=(98, 138, 84), shirt=(52, 88, 150), pants=(50, 56, 90), hair=(40, 30, 24), blood=0.08, rips=0.1),
    "runner": dict(skin=(120, 150, 92), shirt=(170, 40, 36), pants=(70, 70, 76), hair=(20, 20, 20), blood=0.12, rips=0.12, sleeves=1,
                   eye=(255, 200, 40, 255)),
    "brute": dict(skin=(80, 112, 64), shirt=(90, 70, 50), pants=(60, 50, 40), hair=None, blood=0.15, rips=0.3,
                  eye=(255, 60, 20, 255)),
    "screamer": dict(skin=(168, 180, 150), shirt=(200, 200, 196), pants=(120, 120, 130), hair=(200, 200, 200), blood=0.05,
                     mouth="scream", eye=(240, 240, 240, 255)),
    "soldier_zombie": dict(skin=(98, 132, 80), shirt=(90, 104, 50), pants=(80, 92, 44), shoes=(30, 30, 30), hair=(50, 40, 30),
                           camo=(60, 72, 36), blood=0.08, rips=0.05),
    "riot_zombie": dict(skin=(98, 132, 80), shirt=(30, 32, 38), pants=(26, 28, 34), shoes=(18, 18, 20), hair=(30, 30, 30),
                        blood=0.06, rips=0.03, sleeves=12),
    "spitter": dict(skin=(110, 150, 60), shirt=(80, 100, 60), pants=(60, 70, 50), hair=None, blood=0.0, rips=0.25,
                    mouth="drool", eye=(170, 255, 60, 255), spots=(150, 210, 70)),
    "bloater": dict(skin=(130, 150, 96), shirt=(150, 140, 120), pants=(90, 80, 70), hair=None, blood=0.05, rips=0.45,
                    spots=(150, 100, 150), eye=(220, 220, 80, 255)),
    "crawler": dict(skin=(90, 112, 70), shirt=(90, 90, 90), pants=(60, 60, 60), hair=(30, 25, 20), blood=0.2, rips=0.3),
    "frostbitten": dict(skin=(150, 184, 204), shirt=(90, 110, 140), pants=(70, 80, 100), hair=(230, 240, 250), blood=0.0,
                        rips=0.1, eye=(230, 250, 255, 255), spots=(230, 245, 255)),
    "abomination": dict(skin=(140, 60, 60), shirt=(100, 30, 30), pants=(70, 30, 30), hair=None, blood=0.35, rips=0.5,
                        spots=(220, 210, 190), eye=(255, 230, 0, 255)),
}

SURVIVORS = [
    dict(skin=(224, 172, 132), shirt=(70, 110, 60), pants=(60, 64, 90), hair=(60, 40, 20), eye=(60, 90, 140, 255),
         eye_socket=(250, 250, 250, 255)),
    dict(skin=(180, 120, 80), shirt=(150, 60, 50), pants=(50, 50, 60), hair=(20, 15, 10), eye=(70, 50, 30, 255),
         eye_socket=(250, 250, 250, 255)),
    dict(skin=(240, 200, 170), shirt=(200, 180, 120), pants=(90, 70, 50), hair=(200, 160, 80), eye=(80, 130, 60, 255),
         eye_socket=(250, 250, 250, 255)),
    dict(skin=(120, 80, 50), shirt=(60, 60, 70), pants=(40, 40, 50), hair=(15, 10, 10), eye=(50, 30, 20, 255),
         eye_socket=(250, 250, 250, 255)),
    dict(skin=(210, 160, 120), shirt=(90, 104, 50), pants=(80, 92, 44), hair=(120, 60, 30), eye=(90, 120, 160, 255),
         eye_socket=(250, 250, 250, 255), camo=(60, 72, 36)),
    dict(skin=(230, 185, 150), shirt=(40, 80, 130), pants=(70, 70, 76), hair=(90, 30, 20), eye=(60, 100, 60, 255),
         eye_socket=(250, 250, 250, 255)),
]
TRADER = dict(skin=(200, 150, 110), shirt=(110, 90, 60), pants=(70, 60, 45), hair=(90, 90, 90), eye=(50, 80, 50, 255),
              eye_socket=(250, 250, 250, 255), hat=(60, 90, 60))


def make_entities():
    for i, (name, spec) in enumerate(ZOMBIES.items()):
        save(skin(spec, 1000 + i), "entity", name + ".png")
    for i, spec in enumerate(SURVIVORS):
        save(skin(spec, 2000 + i, player_layout=True), "entity", f"survivor_{i}.png")
    save(skin(TRADER, 3000, player_layout=True), "entity", "trader.png")
    save(truck_texture(), "entity", "box_truck.png")


def truck_texture():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    r = random.Random(77)

    def solid(c, amt=6):
        return lambda x, y, w, h: jitter(c, amt, r)

    # cargo box 40x36x54 at (0,0)
    cargo = faces(0, 0, 40, 36, 54)
    white = (222, 222, 216)
    for name, rect in cargo.items():
        if name in ("right", "left"):
            def side(x, y, w, h):
                if y in (0, h - 1) or x in (0, w - 1):
                    return (150, 150, 146, 255)
                if 14 <= y <= 19:
                    return jitter((176, 42, 42), 5, r)  # red stripe
                if r.random() < 0.04:
                    return jitter((130, 90, 50), 10, r)  # rust / dirt
                return jitter(white, 5, r)
            paint(img, rect, side)
        elif name == "back":
            def back(x, y, w, h):
                if x in (0, w - 1) or y in (0, h - 1):
                    return (120, 120, 116, 255)
                if y % 4 == 0:
                    return (170, 170, 166, 255)  # roll-up door slats
                if y == h - 6 and 17 <= x <= 22:
                    return (60, 60, 60, 255)  # handle
                return jitter((200, 200, 194), 4, r)
            paint(img, rect, back)
        else:
            paint(img, rect, solid(white, 4))
    # cab 38x26x33 at (0,90)
    cab = faces(0, 90, 38, 26, 33)
    red = (176, 42, 42)
    for name, rect in cab.items():
        if name == "front":
            def front(x, y, w, h):
                if 2 <= y <= 13 and 3 <= x <= w - 4:
                    return None if not (x == w // 2) else (40, 40, 40, 255)  # windshield (see-through)
                if y >= h - 6 and (4 <= x <= 8 or w - 9 <= x <= w - 5):
                    return (250, 240, 170, 255)  # headlights
                if y >= h - 8 and 12 <= x <= w - 13:
                    return (60, 62, 66, 255) if y % 2 else (90, 92, 96, 255)  # grille
                return jitter(red, 6, r)
            paint(img, rect, front)
        elif name in ("left", "right"):
            def cabside(x, y, w, h):
                if 2 <= y <= 11 and 3 <= x <= w - 4:
                    return None  # side windows
                if y == 16 and w // 2 - 3 <= x <= w // 2:
                    return (40, 40, 40, 255)  # door handle
                return jitter(red, 6, r)
            paint(img, rect, cabside)
        elif name == "top":
            paint(img, rect, solid((150, 36, 36), 5))
        else:
            paint(img, rect, solid((140, 34, 34), 5))
    # chassis 40x4x88 at (0,150)
    for rect in faces(0, 150, 40, 4, 88).values():
        paint(img, rect, solid((45, 45, 48), 5))
    # bumper 42x4x2 at (144,90)
    for rect in faces(144, 90, 42, 4, 2).values():
        paint(img, rect, solid((150, 152, 158), 6))
    # wheel 4x12x12 at (144,100)
    for name, rect in faces(144, 100, 4, 12, 12).items():
        if name in ("left", "right"):
            def hub(x, y, w, h):
                d = ((x - 5.5) ** 2 + (y - 5.5) ** 2) ** 0.5
                if d < 2.2:
                    return (170, 172, 176, 255)
                if d < 3.2:
                    return (90, 92, 96, 255)
                return jitter((24, 24, 26), 4, r)
            paint(img, rect, hub)
        else:
            paint(img, rect, lambda x, y, w, h: (34, 34, 36, 255) if (y % 3) else (20, 20, 22, 255))
    return img


def make_armor_layers():
    """64x32 armor textures. layer_1: helmet (head), chestplate (body+arms), boots (legs). layer_2: leggings."""
    specs = {
        "military": dict(helmet=(90, 107, 47), chest=(90, 107, 47), legs=(84, 98, 46), boots=(40, 40, 40), camo=(60, 72, 36),
                         chest_arms=False),
        "riot": dict(helmet=(34, 37, 44), chest=(34, 37, 44), legs=(30, 32, 38), boots=(18, 18, 20), visor=(61, 90, 168),
                     chest_arms=True),
        "winter": dict(helmet=(176, 42, 42), chest=(44, 62, 107), legs=(90, 95, 102), boots=(107, 68, 35), fur=(217, 201, 168),
                       chest_arms=True),
        "summer": dict(helmet=(216, 192, 122), chest=(240, 240, 232), legs=(184, 160, 112), boots=(240, 240, 240),
                       pattern=(63, 176, 200), chest_arms=True, shorts=True, brim=True),
        "gas_mask": dict(mask=(60, 60, 60), filter=(120, 124, 130), lens=(106, 166, 240)),
    }
    for name, s in specs.items():
        r = random.Random(hash(name) & 0xFFFF)
        l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

        def col(c):
            if s.get("camo") and r.random() < 0.3:
                return jitter(s["camo"], 6, r)
            if s.get("pattern") and r.random() < 0.15:
                return jitter(s["pattern"], 6, r)
            return jitter(c, 6, r)

        head = faces(0, 0, 8, 8, 8)
        if name == "gas_mask":
            for fname, rect in head.items():
                if fname == "front":
                    def mask(x, y, w, h):
                        if y == 3 and x in (1, 2, 5, 6):
                            return s["lens"] + (255,)
                        if y >= 5 and 2 <= x <= 5:
                            return jitter(s["filter"], 6, r)
                        if y >= 3:
                            return jitter(s["mask"], 6, r)
                        return None
                    paint(l1, rect, mask)
                elif fname in ("left", "right"):
                    paint(l1, rect, lambda x, y, w, h: (40, 40, 40, 255) if y == 4 else None)
                elif fname == "back":
                    paint(l1, rect, lambda x, y, w, h: (40, 40, 40, 255) if y == 4 else None)
        else:
            for fname, rect in head.items():
                if fname == "bottom":
                    continue
                if fname == "top":
                    paint(l1, rect, lambda x, y, w, h: col(s["helmet"]))
                elif fname == "front":
                    def front(x, y, w, h):
                        if s.get("visor") and 2 <= y <= 4:
                            return jitter(s["visor"], 6, r)
                        if y <= 1:
                            return col(s["helmet"])
                        return None
                    paint(l1, rect, front)
                else:
                    paint(l1, rect, lambda x, y, w, h: col(s["helmet"]) if y <= (4 if fname == "back" else 3) else None)
            # chest
            for fname, rect in faces(16, 16, 8, 12, 4).items():
                def chest(x, y, w, h, fname=fname):
                    if s.get("fur") and y <= 1:
                        return jitter(s["fur"], 6, r)
                    if name == "military" and fname == "front" and 5 <= y <= 7 and x in (1, 2, 5, 6):
                        return (110, 90, 58, 255)  # pouches
                    return col(s["chest"])
                paint(l1, rect, chest)
            if s.get("chest_arms"):
                for fname, rect in faces(40, 16, 4, 12, 4).items():
                    limit = 4 if name == "summer" else 12
                    paint(l1, rect, lambda x, y, w, h: col(s["chest"]) if y < limit or fname == "top" else None)
            # boots (layer 1 legs)
            for fname, rect in faces(0, 16, 4, 12, 4).items():
                paint(l1, rect, lambda x, y, w, h, fname=fname: jitter(s["boots"], 5, r) if y >= 8 or fname == "bottom" else None)
            # leggings (layer 2)
            for fname, rect in faces(16, 16, 8, 12, 4).items():
                paint(l2, rect, lambda x, y, w, h: col(s["legs"]) if y >= 9 else None)
            for fname, rect in faces(0, 16, 4, 12, 4).items():
                limit = 6 if s.get("shorts") else 12
                paint(l2, rect, lambda x, y, w, h, fname=fname: col(s["legs"]) if (y < limit and fname != "bottom") or fname == "top" else None)
        save(l1, "models", "armor", f"{name}_layer_1.png")
        save(l2, "models", "armor", f"{name}_layer_2.png")


def make_effect_icon():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = img.load()
    for y in range(18):
        for x in range(18):
            d = ((x - 8.5) ** 2 + (y - 8.5) ** 2) ** 0.5
            if d < 8:
                px[x, y] = (90, 130, 40, 255) if d < 6.5 else (40, 60, 20, 255)
    for (x, y) in [(8, 4), (9, 4), (5, 10), (6, 11), (12, 10), (11, 11), (8, 8), (9, 9), (8, 9), (9, 8)]:
        px[x, y] = (20, 30, 10, 255)
    save(img, "mob_effect", "infected.png")


# =============================================================== sounds

def make_sounds():
    if shutil.which("ffmpeg") is None:
        print("ffmpeg not found - skipping sounds")
        return
    from scipy.io import wavfile
    sr = 44100
    out = os.path.join(A, "sounds")
    os.makedirs(out, exist_ok=True)
    tmp = os.path.join(os.path.dirname(__file__), "_tmp_wav")
    os.makedirs(tmp, exist_ok=True)
    nr = np.random.default_rng(5)

    def env(n, attack=0.002, decay=0.15):
        t = np.arange(n) / sr
        a = np.clip(t / attack, 0, 1)
        return a * np.exp(-t / decay)

    def lowpass(x, alpha):
        y = np.zeros_like(x)
        acc = 0.0
        for i in range(len(x)):
            acc += alpha * (x[i] - acc)
            y[i] = acc
        return y

    def shot(length, decay, body_hz, crack, bass):
        n = int(sr * length)
        t = np.arange(n) / sr
        noise = nr.standard_normal(n)
        crack_part = noise * env(n, 0.0005, decay * 0.25) * crack
        body = lowpass(noise, 0.08) * env(n, 0.001, decay) * 3.0
        thump = np.sin(2 * np.pi * body_hz * t * np.exp(-t * 6)) * env(n, 0.001, decay * 0.6) * bass
        return crack_part + body + thump

    def click(n_clicks, gap):
        n = int(sr * (0.05 + gap * n_clicks))
        sig = np.zeros(n)
        for c in range(n_clicks):
            start = int(c * gap * sr)
            m = int(0.03 * sr)
            t = np.arange(m) / sr
            burst = nr.standard_normal(m) * np.exp(-t / 0.004) + np.sin(2 * np.pi * 2800 * t) * np.exp(-t / 0.01) * 0.6
            sig[start:start + m] += burst[: max(0, min(m, n - start))]
        return sig

    def scream():
        n = int(sr * 1.6)
        t = np.arange(n) / sr
        f = 700 + 500 * np.sin(np.pi * t / 1.6) + 40 * np.sin(2 * np.pi * 7 * t)
        phase = 2 * np.pi * np.cumsum(f) / sr
        saw = 2 * ((phase / (2 * np.pi)) % 1) - 1
        sig = np.tanh(3 * (saw + 0.5 * np.sin(phase * 1.5) + 0.3 * nr.standard_normal(n)))
        a = np.clip(t / 0.08, 0, 1) * np.clip((1.6 - t) / 0.4, 0, 1)
        return sig * a

    def plane():
        n = int(sr * 7)
        t = np.arange(n) / sr
        brown = np.cumsum(nr.standard_normal(n))
        brown = brown - lowpass(brown, 0.001)
        brown /= np.max(np.abs(brown)) + 1e-9
        f = 90 * (1 + 0.08 * np.tanh((t - 3.5) * 1.2) * -1)
        phase = 2 * np.pi * np.cumsum(f) / sr
        hum = np.sin(phase) + 0.5 * np.sin(phase * 2) + 0.25 * np.sin(phase * 3)
        a = np.exp(-((t - 3.5) / 2.0) ** 2)
        return (hum * 0.6 + brown * 0.8) * a

    def engine():
        n = int(sr * 2.0)
        t = np.arange(n) / sr
        f = 32
        pulses = np.sin(2 * np.pi * f * t) ** 15
        rumble = lowpass(nr.standard_normal(n), 0.02) * 4
        sig = pulses * 1.2 + rumble + 0.4 * np.sin(2 * np.pi * f * 2 * t)
        a = np.clip(t / 0.1, 0, 1) * np.clip((2.0 - t) / 0.3, 0, 1)
        return sig * a

    def horn():
        n = int(sr * 3.5)
        t = np.arange(n) / sr
        sig = np.zeros(n)
        for det in (55, 55.6, 82.4):
            ph = 2 * np.pi * det * t
            sig += 2 * ((ph / (2 * np.pi)) % 1) - 1
        sig = lowpass(sig, 0.05)
        a = np.clip(t / 1.2, 0, 1) * np.clip((3.5 - t) / 1.0, 0, 1)
        return sig * a

    sounds = {
        "pistol_shot": shot(0.45, 0.12, 120, 1.0, 1.0),
        "rifle_shot": shot(0.8, 0.2, 90, 1.4, 1.4),
        "shotgun_shot": shot(0.9, 0.25, 70, 1.1, 2.0),
        "smg_shot": shot(0.22, 0.06, 140, 0.9, 0.7),
        "gun_reload": click(3, 0.18),
        "gun_empty": click(1, 0.1),
        "screamer_scream": scream(),
        "airdrop_plane": plane(),
        "truck_engine": engine(),
        "blood_moon": horn(),
    }
    for name, sig in sounds.items():
        sig = sig / (np.max(np.abs(sig)) + 1e-9) * 0.9
        wav = os.path.join(tmp, name + ".wav")
        wavfile.write(wav, sr, (sig * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "4",
                        os.path.join(out, name + ".ogg")], check=True)
    shutil.rmtree(tmp)

    sj = {}
    for name in sounds:
        sj[name] = {"subtitle": f"subtitles.{MOD}.{name}", "sounds": [{"name": f"{MOD}:{name}", "stream": name == "airdrop_plane"}]}
    wj(os.path.join(A, "sounds.json"), sj)


# =============================================================== models & blockstates

# Guns are drawn side-on with the barrel pointing right. These angles are based on the vanilla crossbow
# (whose sprite points up-left) rotated a further 135 degrees, then turned 90 degrees right on the Y axis after in-game testing, so the barrel points forward in your hands.
GUN_DISPLAY = {
    "thirdperson_righthand": {"rotation": [-90, 90, 75], "translation": [2, 0.1, -3], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-90, -90, 165], "translation": [2, 0.1, -3], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [-90, 90, 80], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [-90, -90, 170], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}

GUNS = ["pistol", "revolver", "smg", "shotgun", "hunting_rifle", "assault_rifle"]
MELEE = ["baseball_bat", "spiked_bat", "machete", "crowbar", "fire_axe", "katana", "sledgehammer"]
FLAT_ITEMS = ["handgun_ammo", "shotgun_shells", "rifle_ammo", "gun_parts", "scrap_metal", "cloth", "gas_can", "acid_glob",
              "clean_water", "soda_can", "canned_beans", "canned_peaches", "mre", "energy_bar", "cooked_flesh", "bandage",
              "medkit", "antibiotics", "painkillers", "box_truck_spawner",
              "military_helmet", "military_vest", "military_pants", "military_boots",
              "riot_helmet", "riot_armor", "riot_leggings", "riot_boots",
              "winter_hat", "winter_coat", "snow_pants", "snow_boots",
              "sun_hat", "light_shirt", "cargo_shorts", "sneakers", "gas_mask", "reinforced_door"]
EGGS = ["walker", "runner", "brute", "screamer", "soldier_zombie", "riot_zombie", "spitter", "bloater", "crawler",
        "frostbitten", "abomination", "survivor", "survivor_trader"]


def bm(name, obj):
    wj(os.path.join(A, "models", "block", name + ".json"), obj)


def im(name, obj):
    wj(os.path.join(A, "models", "item", name + ".json"), obj)


def bs(name, obj):
    wj(os.path.join(A, "blockstates", name + ".json"), obj)


def t(name):
    return f"{MOD}:block/{name}"


def box(frm, to, tex, faces_=("north", "south", "east", "west", "up", "down"), uv=None):
    f = {}
    for fc in faces_:
        e = {"texture": tex}
        if uv:
            e["uv"] = uv
        f[fc] = e
    return {"from": frm, "to": to, "faces": f}


def make_models():
    for g in GUNS:
        im(g, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{g}"}, "display": GUN_DISPLAY})
    for m in MELEE:
        im(m, {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/{m}"}})
    for i in FLAT_ITEMS:
        im(i, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{i}"}})
    for e in EGGS:
        im(e + "_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"})

    # Simple cubes
    for name in ("reinforced_wall", "grow_lamp", "truck_wall", "truck_floor", "truck_light", "truck_exit"):
        bm(name, {"parent": "minecraft:block/cube_all", "textures": {"all": t(name)}})
        bs(name, {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
    bm("floodlight", {"parent": "minecraft:block/cube_column", "textures": {"end": t("floodlight_front"), "side": t("floodlight_side")}})
    bs("floodlight", {"variants": {"": {"model": f"{MOD}:block/floodlight"}}})
    bm("supply_crate", {"parent": "minecraft:block/cube_bottom_top",
                        "textures": {"top": t("supply_crate_top"), "bottom": t("supply_crate_top"), "side": t("supply_crate_side")}})
    bs("supply_crate", {"variants": {"": {"model": f"{MOD}:block/supply_crate"}}})

    # Barbed wire
    bm("barbed_wire", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": t("barbed_wire")}})
    bs("barbed_wire", {"variants": {"": {"model": f"{MOD}:block/barbed_wire"}}})

    # Spike trap
    elems = [box([0, 0, 0], [16, 1, 16], "#base")]
    for sx in (2, 7, 12):
        for sz in (2, 7, 12):
            elems.append(box([sx, 1, sz], [sx + 2, 7, sz + 2], "#spike", ("north", "south", "east", "west", "up")))
    bm("spike_trap", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                      "textures": {"base": t("spike_trap_base"), "spike": t("spike"), "particle": t("spike_trap_base")},
                      "elements": elems})
    bs("spike_trap", {"variants": {"": {"model": f"{MOD}:block/spike_trap"}}})

    # Landmine
    bm("landmine", {"parent": "minecraft:block/block", "textures": {"all": t("landmine"), "particle": t("landmine")},
                    "elements": [box([3, 0, 3], [13, 2, 13], "#all"), box([7, 2, 7], [9, 3, 9], "#all")]})
    bs("landmine", {"variants": {"": {"model": f"{MOD}:block/landmine"}}})

    # Alarm bell
    bm("alarm_bell", {"parent": "minecraft:block/block",
                      "textures": {"bell": t("alarm_bell"), "post": t("alarm_bell_post"), "particle": t("alarm_bell")},
                      "elements": [box([3, 0, 3], [13, 2, 13], "#post"), box([7, 2, 7], [9, 5, 9], "#post"),
                                   box([4, 5, 4], [12, 12, 12], "#bell"), box([6, 12, 6], [10, 14, 10], "#bell")]})
    bs("alarm_bell", {"variants": {"powered=false": {"model": f"{MOD}:block/alarm_bell"},
                                   "powered=true": {"model": f"{MOD}:block/alarm_bell"}}})

    # Turret
    bm("auto_turret", {"parent": "minecraft:block/block",
                       "textures": {"base": t("turret_base"), "top": t("turret_top"), "barrel": t("turret_barrel"), "particle": t("turret_base")},
                       "elements": [box([2, 0, 2], [14, 7, 14], "#base"), box([4, 7, 4], [12, 13, 12], "#top"),
                                    box([7, 9, 0], [9, 11, 4], "#barrel"), box([7, 9, 12], [9, 11, 16], "#barrel"),
                                    box([0, 9, 7], [4, 11, 9], "#barrel"), box([12, 9, 7], [16, 11, 9], "#barrel")]})
    bs("auto_turret", {"variants": {"": {"model": f"{MOD}:block/auto_turret"}}})

    # Rain collector (4 water levels)
    for lvl in range(4):
        el = [box([1, 0, 1], [15, 1, 15], "#inside"),
              box([1, 0, 1], [15, 12, 2], "#side"), box([1, 0, 14], [15, 12, 15], "#side"),
              box([1, 0, 2], [2, 12, 14], "#side"), box([14, 0, 2], [15, 12, 14], "#side")]
        if lvl > 0:
            h = 1 + lvl * 3
            el.append(box([2, h - 0.5, 2], [14, h, 14], "#water", ("up",)))
        bm(f"rain_collector_{lvl}", {"parent": "minecraft:block/block",
                                     "textures": {"side": t("rain_collector_side"), "inside": t("rain_collector_inside"),
                                                  "water": t("rain_water"), "particle": t("rain_collector_side")},
                                     "elements": el})
    bs("rain_collector", {"variants": {f"level={l}": {"model": f"{MOD}:block/rain_collector_{l}"} for l in range(4)}})

    # Radio (facing)
    bm("radio", {"parent": "minecraft:block/block",
                 "textures": {"front": t("radio_front"), "side": t("radio_side"), "particle": t("radio_side")},
                 "elements": [
                     {"from": [2, 0, 4], "to": [14, 9, 12], "faces": {
                         "north": {"texture": "#front"}, "south": {"texture": "#side"}, "east": {"texture": "#side"},
                         "west": {"texture": "#side"}, "up": {"texture": "#side"}, "down": {"texture": "#side"}}},
                     box([11, 9, 9], [12, 16, 10], "#side")]})
    bs("radio", {"variants": {f"facing={f}": {"model": f"{MOD}:block/radio", "y": y}
                              for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})

    # Blood splatter (carpet)
    bm("blood_splatter", {"parent": "minecraft:block/carpet", "render_type": "minecraft:cutout",
                          "textures": {"wool": t("blood_splatter"), "particle": t("blood_splatter")}})
    bs("blood_splatter", {"variants": {"": {"model": f"{MOD}:block/blood_splatter"}}})

    # Barricades: damage 0-7 mapped to 4 visual stages
    for name, max_hits in (("wooden_barricade", 4), ("metal_barricade", 8)):
        for s in range(4):
            bm(f"{name}_{s}", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout",
                               "textures": {"all": t(f"{name}_{s}")}})
        bs(name, {"variants": {f"damage={d}": {"model": f"{MOD}:block/{name}_{min(3, d * 4 // max_hits)}"} for d in range(8)}})

    # Reinforced door
    for part in ("bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open",
                 "top_left", "top_left_open", "top_right", "top_right_open"):
        bm(f"reinforced_door_{part}", {"parent": f"minecraft:block/door_{part}", "render_type": "minecraft:cutout",
                                       "textures": {"bottom": t("reinforced_door_bottom"), "top": t("reinforced_door_top")}})
    variants = {}
    base_rot = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, rot in base_rot.items():
        for half in ("lower", "upper"):
            for hinge in ("left", "right"):
                for open_ in (False, True):
                    part = ("bottom" if half == "lower" else "top") + "_" + hinge + ("_open" if open_ else "")
                    y = rot
                    if open_:
                        y = (rot + (90 if hinge == "left" else 270)) % 360
                    v = {"model": f"{MOD}:block/reinforced_door_{part}"}
                    if y:
                        v["y"] = y
                    variants[f"facing={facing},half={half},hinge={hinge},open={'true' if open_ else 'false'}"] = v
    bs("reinforced_door", {"variants": variants})

    # Block items
    for name in ("reinforced_wall", "grow_lamp", "floodlight", "supply_crate", "spike_trap", "landmine", "alarm_bell",
                 "auto_turret", "radio", "blood_splatter"):
        im(name, {"parent": f"{MOD}:block/{name}"})
    im("rain_collector", {"parent": f"{MOD}:block/rain_collector_0"})
    im("wooden_barricade", {"parent": f"{MOD}:block/wooden_barricade_0"})
    im("metal_barricade", {"parent": f"{MOD}:block/metal_barricade_0"})
    im("barbed_wire", {"parent": "minecraft:item/generated", "textures": {"layer0": t("barbed_wire")}})


# =============================================================== lang

def make_lang():
    L = {
        "itemGroup.deadseasons": "Dead Seasons",
        "key.categories.deadseasons": "Dead Seasons",
        "key.deadseasons.reload": "Reload Gun",
        "container.deadseasons.supply_crate": "Supply Crate",
        "effect.deadseasons.infected": "Zombie Infection",
        # items
        "item.deadseasons.handgun_ammo": "Handgun Ammo", "item.deadseasons.shotgun_shells": "Shotgun Shells",
        "item.deadseasons.rifle_ammo": "Rifle Ammo", "item.deadseasons.gun_parts": "Gun Parts",
        "item.deadseasons.scrap_metal": "Scrap Metal", "item.deadseasons.cloth": "Cloth", "item.deadseasons.gas_can": "Gas Can",
        "item.deadseasons.acid_glob": "Acid Glob",
        "item.deadseasons.pistol": "Pistol", "item.deadseasons.revolver": "Revolver", "item.deadseasons.smg": "SMG",
        "item.deadseasons.shotgun": "Pump Shotgun", "item.deadseasons.hunting_rifle": "Hunting Rifle",
        "item.deadseasons.assault_rifle": "Assault Rifle",
        "item.deadseasons.baseball_bat": "Baseball Bat", "item.deadseasons.spiked_bat": "Spiked Bat",
        "item.deadseasons.machete": "Machete", "item.deadseasons.crowbar": "Crowbar", "item.deadseasons.fire_axe": "Fire Axe",
        "item.deadseasons.katana": "Katana", "item.deadseasons.sledgehammer": "Sledgehammer",
        "item.deadseasons.military_helmet": "Military Helmet", "item.deadseasons.military_vest": "Military Vest",
        "item.deadseasons.military_pants": "Military Pants", "item.deadseasons.military_boots": "Military Boots",
        "item.deadseasons.riot_helmet": "Riot Helmet", "item.deadseasons.riot_armor": "Riot Armor",
        "item.deadseasons.riot_leggings": "Riot Leggings", "item.deadseasons.riot_boots": "Riot Boots",
        "item.deadseasons.winter_hat": "Winter Hat", "item.deadseasons.winter_coat": "Winter Coat",
        "item.deadseasons.snow_pants": "Snow Pants", "item.deadseasons.snow_boots": "Snow Boots",
        "item.deadseasons.sun_hat": "Sun Hat", "item.deadseasons.light_shirt": "Light Shirt",
        "item.deadseasons.cargo_shorts": "Cargo Shorts", "item.deadseasons.sneakers": "Sneakers",
        "item.deadseasons.gas_mask": "Gas Mask",
        "item.deadseasons.clean_water": "Clean Water", "item.deadseasons.soda_can": "Soda",
        "item.deadseasons.canned_beans": "Canned Beans", "item.deadseasons.canned_peaches": "Canned Peaches",
        "item.deadseasons.mre": "MRE", "item.deadseasons.energy_bar": "Energy Bar", "item.deadseasons.cooked_flesh": "Boiled Flesh",
        "item.deadseasons.bandage": "Bandage", "item.deadseasons.medkit": "Medkit", "item.deadseasons.antibiotics": "Antibiotics",
        "item.deadseasons.painkillers": "Painkillers", "item.deadseasons.box_truck_spawner": "Box Truck (Creative)",
        # blocks
        "block.deadseasons.barbed_wire": "Barbed Wire", "block.deadseasons.spike_trap": "Spike Trap",
        "block.deadseasons.reinforced_wall": "Reinforced Wall", "block.deadseasons.reinforced_door": "Reinforced Door",
        "block.deadseasons.wooden_barricade": "Wooden Barricade", "block.deadseasons.metal_barricade": "Metal Barricade",
        "block.deadseasons.landmine": "Landmine", "block.deadseasons.alarm_bell": "Alarm Bell",
        "block.deadseasons.floodlight": "Floodlight", "block.deadseasons.auto_turret": "Auto Turret",
        "block.deadseasons.grow_lamp": "Grow Lamp", "block.deadseasons.rain_collector": "Rain Collector",
        "block.deadseasons.radio": "Emergency Radio", "block.deadseasons.supply_crate": "Supply Crate",
        "block.deadseasons.blood_splatter": "Blood Splatter",
        "block.deadseasons.truck_wall": "Truck Wall", "block.deadseasons.truck_floor": "Truck Floor",
        "block.deadseasons.truck_light": "Truck Light", "block.deadseasons.truck_exit": "Cargo Door",
        # entities
        "entity.deadseasons.walker": "Walker", "entity.deadseasons.runner": "Runner", "entity.deadseasons.brute": "Brute",
        "entity.deadseasons.screamer": "Screamer", "entity.deadseasons.soldier_zombie": "Soldier Zombie",
        "entity.deadseasons.riot_zombie": "Riot Zombie", "entity.deadseasons.spitter": "Spitter",
        "entity.deadseasons.bloater": "Bloater", "entity.deadseasons.crawler": "Crawler",
        "entity.deadseasons.frostbitten": "Frostbitten", "entity.deadseasons.abomination": "Abomination",
        "entity.deadseasons.acid_spit": "Acid Spit", "entity.deadseasons.survivor": "Survivor",
        "entity.deadseasons.survivor_trader": "Trader", "entity.deadseasons.box_truck": "Box Truck",
        # seasons / modes / directions
        "season.deadseasons.summer": "Summer", "season.deadseasons.autumn": "Autumn",
        "season.deadseasons.winter": "Winter", "season.deadseasons.spring": "Spring",
        "mode.deadseasons.follow": "Follow", "mode.deadseasons.guard": "Guard", "mode.deadseasons.farm": "Farm",
        "mode.deadseasons.wander": "Wander",
        "direction.deadseasons.north": "north", "direction.deadseasons.south": "south",
        "direction.deadseasons.east": "east", "direction.deadseasons.west": "west",
        # tooltips
        "tooltip.deadseasons.warmth": "Warmth: +%s", "tooltip.deadseasons.cooling": "Cooling: +%s",
        "tooltip.deadseasons.gas_mask": "Blocks poison and nausea (Bloaters, Spitters)",
        "tooltip.deadseasons.thirst": "Thirst +%s", "tooltip.deadseasons.heal": "Heals %s hearts",
        "tooltip.deadseasons.cures_infection": "Cures zombie infection",
        "tooltip.deadseasons.bandage": "Stops bleeding and patches you up",
        "tooltip.deadseasons.gun_ammo": "Ammo: %s/%s (%s)", "tooltip.deadseasons.gun_damage": "Damage: %s",
        "tooltip.deadseasons.gun_auto": "Automatic - hold right-click", "tooltip.deadseasons.gun_semi": "Semi-automatic",
        "tooltip.deadseasons.gun_durability": "Condition: %s/%s (repair with Gun Parts in an anvil)",
        "tooltip.deadseasons.gun_help": "R or Sneak + Right-click to reload. Gunshots attract zombies!",
        "tooltip.deadseasons.truck_spawner": "Places a box truck. In survival, find one in a ruined town.",
        # messages
        "message.deadseasons.no_ammo": "Out of %s!", "message.deadseasons.thirsty": "You're getting thirsty...",
        "message.deadseasons.dirty_water": "That water was dirty... boil it next time.",
        "message.deadseasons.cold": "You're freezing! Find shelter, a fire, or warmer clothes.",
        "message.deadseasons.hot": "You're overheating! Get into shade, water, or lighter clothes.",
        "message.deadseasons.infected": "You've been infected! You have %s days to take Antibiotics.",
        "message.deadseasons.infection_reminder": "Infection: %s days left. Find Antibiotics!",
        "message.deadseasons.cured": "The infection is gone. You'll live.",
        "message.deadseasons.not_infected": "You're not infected.",
        "message.deadseasons.survivor_joined": "%s has joined your group!",
        "message.deadseasons.survivor_unsure": "%s isn't sure about you yet...",
        "message.deadseasons.survivor_hint": "Offer food to win this survivor's trust.",
        "message.deadseasons.survivor_armed": "%s takes the weapon.",
        "message.deadseasons.survivor_dismissed": "%s leaves your group.",
        "message.deadseasons.survivor_mode": "%s: %s",
        "message.deadseasons.barricade": "Barricade: %s/%s",
        "message.deadseasons.turret_ammo": "Turret ammo: %s/%s",
        "message.deadseasons.alarm": "ALARM! Hostiles near %s, %s, %s",
        "message.deadseasons.truck_full": "The tank is full.",
        "message.deadseasons.truck_fuel": "Fuel: %s%%",
        "message.deadseasons.truck_repair": "Truck: %s/%s",
        "message.deadseasons.truck_drive": "W/S to drive, A/D to steer, Sneak to get out. Sneak + right-click to enter the cargo area.",
        "message.deadseasons.truck_inside": "You're in the cargo area. Use the cargo door to get out.",
        "message.deadseasons.truck_nomad": "On the move - hordes will have a harder time finding you.",
        "message.deadseasons.no_sleep_blood_moon": "You can't sleep during a Blood Moon!",
        # events
        "event.deadseasons.blood_moon_warning": "The sky is turning red... a BLOOD MOON rises tonight. Fortify your base!",
        "event.deadseasons.blood_moon_start": "THE BLOOD MOON HAS RISEN. They're coming.",
        "event.deadseasons.blood_moon_end": "The Blood Moon has passed. You survived.",
        "event.deadseasons.boss": "Something massive is coming...",
        "event.deadseasons.fog": "A thick fog rolls in. Stay close to the light.",
        "event.deadseasons.airdrop": "SUPPLY DROP spotted at %s, %s, %s! Follow the smoke.",
        "event.deadseasons.herd": "A massive herd is moving in from the %s!",
        "event.deadseasons.raid": "A horde has tracked you down! Defend yourself!",
        "event.deadseasons.raid_avoided": "You hear a horde in the distance, but it loses your trail.",
        "event.deadseasons.season_start": "%s has arrived.",
        "event.deadseasons.season_tip.summer": "Long days and heat. Stock water, build defenses and prepare for the cold.",
        "event.deadseasons.season_tip.autumn": "Crops grow slower and animals grow scarce. Harvest and preserve food.",
        "event.deadseasons.season_tip.winter": "Bitter cold. Outdoor crops won't grow - use glass greenhouses or Grow Lamps. Wear winter clothes.",
        "event.deadseasons.season_tip.spring": "The thaw begins. Plant your fields and restock.",
        # radio
        "radio.deadseasons.header": "[Emergency Broadcast]",
        "radio.deadseasons.day": "Day %s - %s (%s days left)",
        "radio.deadseasons.blood_moon": "Next Blood Moon in %s days.",
        "radio.deadseasons.blood_moon_tonight": "WARNING: Blood Moon tonight!",
        "radio.deadseasons.airdrop": "Last supply drop: %s, %s, %s",
        "radio.deadseasons.no_airdrop": "No recent supply drops.",
        "radio.deadseasons.raid_soon": "Horde activity is increasing near you. Expect a raid soon.",
        "radio.deadseasons.herd_soon": "A large herd is on the move in this region.",
        # hud
        "hud.deadseasons.day": "Day %s - %s (%sd left)",
        "hud.deadseasons.blood_moon": "BLOOD MOON",
        "hud.deadseasons.blood_moon_tonight": "Blood Moon tonight",
        "hud.deadseasons.blood_moon_in": "Blood Moon in %s days",
        "hud.deadseasons.fog": "Fog Night",
        "hud.deadseasons.infected": "Infected - %s days left",
        "hud.deadseasons.truck": "Fuel %s%%  |  Truck %s/%s",
        "hud.deadseasons.temp.freezing": "Freezing", "hud.deadseasons.temp.cold": "Cold",
        "hud.deadseasons.temp.comfortable": "Comfortable", "hud.deadseasons.temp.hot": "Hot",
        "hud.deadseasons.temp.overheating": "Overheating",
        # commands
        "command.deadseasons.status": "Day %s - %s (%s days left). Blood Moon in %s days. Next airdrop in ~%s days.",
        "command.deadseasons.season_set": "Season set to %s.",
        # deaths
        "death.attack.deadseasons.bullet": "%1$s was shot",
        "death.attack.deadseasons.bullet.player": "%1$s was shot by %2$s",
        "death.attack.deadseasons.infection": "%1$s succumbed to the infection",
        "death.attack.deadseasons.infection.player": "%1$s succumbed to the infection",
        "death.attack.deadseasons.dehydration": "%1$s died of thirst",
        "death.attack.deadseasons.dehydration.player": "%1$s died of thirst",
        "death.attack.deadseasons.hypothermia": "%1$s froze to death",
        "death.attack.deadseasons.hypothermia.player": "%1$s froze to death",
        "death.attack.deadseasons.heatstroke": "%1$s died of heatstroke",
        "death.attack.deadseasons.heatstroke.player": "%1$s died of heatstroke",
        "death.attack.deadseasons.roadkill": "%1$s was run over",
        "death.attack.deadseasons.roadkill.player": "%1$s was run over by %2$s",
    }
    for e in EGGS:
        L[f"item.deadseasons.{e}_spawn_egg"] = L.get(f"entity.deadseasons.{e}", e.title()) + " Spawn Egg"
    for s in ("pistol_shot", "rifle_shot", "shotgun_shot", "smg_shot", "gun_reload", "gun_empty", "screamer_scream",
              "airdrop_plane", "truck_engine", "blood_moon"):
        L[f"subtitles.deadseasons.{s}"] = {
            "pistol_shot": "Gunshot", "rifle_shot": "Rifle shot", "shotgun_shot": "Shotgun blast", "smg_shot": "Gunfire",
            "gun_reload": "Gun reloads", "gun_empty": "Gun clicks empty", "screamer_scream": "Screamer shrieks",
            "airdrop_plane": "Plane flies over", "truck_engine": "Engine rumbles", "blood_moon": "Ominous horn"}[s]
    wj(os.path.join(A, "lang", "en_us.json"), L)


# =============================================================== data: recipes

def shaped(name, pattern, key, result, count=1):
    k = {}
    for ch, v in key.items():
        k[ch] = {"tag": v[1:]} if v.startswith("#") else {"item": v}
    wj(os.path.join(D, "recipes", name + ".json"),
       {"type": "minecraft:crafting_shaped", "pattern": pattern, "key": k, "result": {"item": result, "count": count}})


def shapeless(name, items, result, count=1):
    ing = [{"tag": i[1:]} if i.startswith("#") else {"item": i} for i in items]
    wj(os.path.join(D, "recipes", name + ".json"),
       {"type": "minecraft:crafting_shapeless", "ingredients": ing, "result": {"item": result, "count": count}})


def cook(name, ingredient, result, xp=0.1, time=200, kinds=("smelting", "campfire_cooking", "smoking")):
    for kind in kinds:
        t_ = time if kind == "smelting" else (time // 2 if kind == "smoking" else time * 3)
        wj(os.path.join(D, "recipes", f"{name}_from_{kind}.json"),
           {"type": f"minecraft:{kind}", "ingredient": ingredient, "result": result, "experience": xp, "cookingtime": t_})


def make_recipes():
    m = lambda n: f"{MOD}:{n}"
    I, C, P = "minecraft:iron_ingot", "minecraft:copper_ingot", m("gun_parts")
    PL = "#minecraft:planks"
    shapeless("handgun_ammo", ["minecraft:copper_ingot", "minecraft:gunpowder", "minecraft:iron_nugget", "minecraft:iron_nugget"], m("handgun_ammo"), 16)
    shapeless("shotgun_shells", ["minecraft:paper", "minecraft:gunpowder", "minecraft:gunpowder", "minecraft:iron_nugget", "minecraft:red_dye"], m("shotgun_shells"), 8)
    shapeless("rifle_ammo", [C, C, "minecraft:gunpowder", "minecraft:gunpowder", I], m("rifle_ammo"), 12)
    shaped("gun_parts", ["ICI", "CRC", "ICI"], {"I": I, "C": C, "R": "minecraft:redstone"}, P, 2)
    shapeless("iron_from_scrap", [m("scrap_metal")] * 4, "minecraft:iron_ingot")
    shapeless("cloth", ["minecraft:string"] * 3, m("cloth"), 2)
    shapeless("cloth_from_wool", ["#minecraft:wool"], m("cloth"), 3)
    shaped("gas_can", ["ICI", "CBC", "ICI"], {"I": I, "C": "#minecraft:coals", "B": "minecraft:bucket"}, m("gas_can"))

    shaped("pistol", ["III", " PW"], {"I": I, "P": P, "W": PL}, m("pistol"))
    shaped("revolver", ["III", "PPW"], {"I": I, "P": P, "W": PL}, m("revolver"))
    shaped("smg", ["III", "PPI", " W "], {"I": I, "P": P, "W": PL}, m("smg"))
    shaped("shotgun", ["III", "PPW", "  W"], {"I": I, "P": P, "W": PL}, m("shotgun"))
    shaped("hunting_rifle", ["GII", "PPW", " WW"], {"G": "minecraft:glass", "I": I, "P": P, "W": PL}, m("hunting_rifle"))
    shaped("assault_rifle", ["III", "PPP", " DI"], {"I": I, "P": P, "D": "minecraft:diamond"}, m("assault_rifle"))

    shaped("baseball_bat", ["  P", " P ", "S  "], {"P": PL, "S": "minecraft:stick"}, m("baseball_bat"))
    shapeless("spiked_bat", [m("baseball_bat")] + ["minecraft:iron_nugget"] * 4, m("spiked_bat"))
    shaped("machete", ["  I", " I ", "L  "], {"I": I, "L": "minecraft:leather"}, m("machete"))
    shaped("crowbar", ["  I", " I ", "II "], {"I": I}, m("crowbar"))
    shaped("fire_axe", ["IIR", "IS ", " S "], {"I": I, "R": "minecraft:red_dye", "S": "minecraft:stick"}, m("fire_axe"))
    shaped("katana", ["  I", " D ", "L  "], {"I": I, "D": "minecraft:diamond", "L": "minecraft:leather"}, m("katana"))
    shaped("sledgehammer", ["III", "ISI", " S "], {"I": I, "S": "minecraft:stick"}, m("sledgehammer"))

    G, CL = "minecraft:green_dye", m("cloth")
    shaped("military_helmet", ["IGI", "I I"], {"I": I, "G": G}, m("military_helmet"))
    shaped("military_vest", ["C C", "IGI", "ICI"], {"I": I, "G": G, "C": CL}, m("military_vest"))
    shaped("military_pants", ["IGI", "C C", "I I"], {"I": I, "G": G, "C": CL}, m("military_pants"))
    shaped("military_boots", ["C C", "I I"], {"I": I, "C": CL}, m("military_boots"))
    B = "minecraft:black_dye"
    shaped("riot_helmet", ["IBI", "IGI"], {"I": I, "B": B, "G": "minecraft:glass_pane"}, m("riot_helmet"))
    shaped("riot_armor", ["I I", "IBI", "III"], {"I": I, "B": B}, m("riot_armor"))
    shaped("riot_leggings", ["IBI", "I I", "I I"], {"I": I, "B": B}, m("riot_leggings"))
    shaped("riot_boots", ["I I", "B B"], {"I": I, "B": B}, m("riot_boots"))
    W = "#minecraft:wool"
    shaped("winter_hat", ["RRR", "R R"], {"R": "minecraft:red_wool"}, m("winter_hat"))
    shaped("winter_coat", ["C C", "WCW", "WCW"], {"C": CL, "W": W}, m("winter_coat"))
    shaped("snow_pants", ["WCW", "W W", "W W"], {"C": CL, "W": W}, m("snow_pants"))
    shaped("snow_boots", ["L L", "W W"], {"L": "minecraft:leather", "W": W}, m("snow_boots"))
    shaped("sun_hat", ["WWW", "W W"], {"W": "minecraft:wheat"}, m("sun_hat"))
    shaped("light_shirt", ["C C", "CCC", "CCC"], {"C": CL}, m("light_shirt"))
    shaped("cargo_shorts", ["CCC", "C C"], {"C": CL}, m("cargo_shorts"))
    shaped("sneakers", ["C C", "W W"], {"C": CL, "W": "minecraft:white_wool"}, m("sneakers"))
    shaped("gas_mask", ["LLL", "GCG", " I "], {"L": "minecraft:leather", "G": "minecraft:glass_pane", "C": "minecraft:charcoal", "I": I}, m("gas_mask"))

    water_nbt = {"type": "forge:nbt", "item": "minecraft:potion", "nbt": "{Potion:\"minecraft:water\"}"}
    cook("clean_water", water_nbt, m("clean_water"), 0.1, 160, ("smelting", "campfire_cooking"))
    wj(os.path.join(D, "recipes", "clean_water_filtered.json"),
       {"type": "minecraft:crafting_shapeless",
        "ingredients": [water_nbt, {"item": "minecraft:charcoal"}, {"item": "minecraft:paper"}],
        "result": {"item": m("clean_water")}})
    cook("cooked_flesh", {"item": "minecraft:rotten_flesh"}, m("cooked_flesh"), 0.1, 200)
    shapeless("bandage", [CL, CL], m("bandage"), 2)
    shaped("medkit", ["ICI", "BHB", "ICI"], {"I": "minecraft:iron_nugget", "C": "minecraft:red_dye", "B": m("bandage"),
                                              "H": "minecraft:honey_bottle"}, m("medkit"))
    shapeless("antibiotics", ["minecraft:glass_bottle", "minecraft:red_mushroom", "minecraft:brown_mushroom",
                              "minecraft:fermented_spider_eye", "minecraft:sugar"], m("antibiotics"))
    shapeless("painkillers", ["minecraft:sugar", "minecraft:bone_meal", "minecraft:paper"], m("painkillers"), 2)
    shapeless("energy_bar", ["minecraft:wheat", "minecraft:honey_bottle", "minecraft:sugar"], m("energy_bar"), 2)

    N = "minecraft:iron_nugget"
    shaped("barbed_wire", ["N N", " N ", "N N"], {"N": N}, m("barbed_wire"), 4)
    shaped("spike_trap", ["N N", "III"], {"N": N, "I": I}, m("spike_trap"), 2)
    shaped("reinforced_wall", ["ISI", "SOS", "ISI"], {"I": I, "S": "minecraft:stone_bricks", "O": "minecraft:obsidian"}, m("reinforced_wall"), 4)
    shaped("reinforced_door", ["II", "IO", "II"], {"I": I, "O": "minecraft:obsidian"}, m("reinforced_door"))
    shaped("wooden_barricade", ["S S", "PPP", "S S"], {"S": "minecraft:stick", "P": PL}, m("wooden_barricade"), 2)
    shaped("metal_barricade", ["M M", "MIM", "M M"], {"M": m("scrap_metal"), "I": I}, m("metal_barricade"), 2)
    shaped("landmine", ["P", "T", "I"], {"P": "minecraft:stone_pressure_plate", "T": "minecraft:tnt", "I": I}, m("landmine"), 2)
    shaped("alarm_bell", [" S ", "CRC", "CCC"], {"S": "minecraft:stick", "C": C, "R": "minecraft:redstone"}, m("alarm_bell"))
    shaped("floodlight", ["IGI", "GTG", "IRI"], {"I": I, "G": "minecraft:glass", "T": "minecraft:torch", "R": "minecraft:redstone"}, m("floodlight"))
    shaped("auto_turret", ["IPI", "RDR", "III"], {"I": I, "P": P, "R": "minecraft:redstone", "D": "minecraft:dispenser"}, m("auto_turret"))
    shaped("grow_lamp", ["GPG", "GTG", "GRG"], {"G": "minecraft:glass", "P": "minecraft:purple_dye", "T": "minecraft:torch",
                                                 "R": "minecraft:redstone"}, m("grow_lamp"))
    shaped("rain_collector", ["P P", "P P", "PBP"], {"P": PL, "B": "minecraft:bucket"}, m("rain_collector"))
    shaped("radio", [" S ", "ICI", "IRI"], {"S": "minecraft:stick", "I": I, "C": C, "R": "minecraft:redstone"}, m("radio"))
    shaped("supply_crate", ["PNP", "PCP", "PNP"], {"P": PL, "N": N, "C": "minecraft:chest"}, m("supply_crate"))
    shapeless("blood_splatter", ["minecraft:red_dye", "minecraft:red_dye", "minecraft:slime_ball"], m("blood_splatter"), 4)


# =============================================================== data: loot tables

def item_entry(name, weight=1, mn=1, mx=1, extra=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    funcs = []
    if mx > 1 or mn != 1:
        funcs.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": mn, "max": mx}})
    if extra:
        funcs.extend(extra)
    if funcs:
        e["functions"] = funcs
    return e


def chest_table(name, rolls, entries):
    wj(os.path.join(D, "loot_tables", "chests", name + ".json"),
       {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
                                              "entries": entries}]})


def make_loot():
    m = lambda n: f"{MOD}:{n}"
    blocks_self = ["barbed_wire", "spike_trap", "reinforced_wall", "wooden_barricade", "metal_barricade", "landmine",
                   "alarm_bell", "floodlight", "auto_turret", "grow_lamp", "rain_collector", "radio", "supply_crate"]
    for b in blocks_self:
        wj(os.path.join(D, "loot_tables", "blocks", b + ".json"),
           {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": m(b)}],
                                                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    wj(os.path.join(D, "loot_tables", "blocks", "reinforced_door.json"),
       {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": m("reinforced_door"),
                                                                        "conditions": [{"condition": "minecraft:block_state_property",
                                                                                        "block": m("reinforced_door"),
                                                                                        "properties": {"half": "lower"}}]}]}]})
    wj(os.path.join(D, "loot_tables", "blocks", "blood_splatter.json"), {"type": "minecraft:block", "pools": []})

    def ent(name, pools):
        wj(os.path.join(D, "loot_tables", "entities", name + ".json"), {"type": "minecraft:entity", "pools": pools})

    def pool(entry_name, mn, mx, chance=None, looting=True):
        p = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": entry_name, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": mn, "max": mx}}]}]}
        if looting:
            p["entries"][0]["functions"].append({"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
        if chance is not None:
            p["conditions"] = [{"condition": "minecraft:killed_by_player"}, {"condition": "minecraft:random_chance", "chance": chance}]
        return p

    flesh = pool("minecraft:rotten_flesh", 0, 2)
    ent("walker", [flesh, pool(m("cloth"), 1, 1, 0.1)])
    ent("runner", [flesh, pool(m("energy_bar"), 1, 1, 0.05)])
    ent("brute", [pool("minecraft:rotten_flesh", 2, 4), pool(m("scrap_metal"), 1, 2, 0.5)])
    ent("screamer", [flesh, pool(m("painkillers"), 1, 1, 0.1)])
    ent("soldier_zombie", [flesh, pool(m("handgun_ammo"), 2, 8, 0.5), pool(m("rifle_ammo"), 1, 4, 0.25),
                           pool(m("gun_parts"), 1, 1, 0.05), pool(m("mre"), 1, 1, 0.1)])
    ent("riot_zombie", [flesh, pool(m("shotgun_shells"), 1, 4, 0.35), pool(m("scrap_metal"), 1, 2, 0.4)])
    ent("spitter", [pool(m("acid_glob"), 0, 2), pool("minecraft:slime_ball", 1, 1, 0.15)])
    ent("bloater", [flesh, pool("minecraft:gunpowder", 1, 2, 0.5)])
    ent("crawler", [flesh, pool(m("cloth"), 1, 2, 0.2)])
    ent("frostbitten", [flesh, pool("minecraft:ice", 1, 1, 0.2), pool("minecraft:snowball", 1, 3, 0.4)])
    ent("abomination", [pool("minecraft:rotten_flesh", 4, 8), pool("minecraft:diamond", 1, 3, 1.0),
                        pool(m("gun_parts"), 2, 4, 1.0), pool(m("antibiotics"), 1, 2, 1.0), pool(m("medkit"), 1, 1, 1.0),
                        pool(m("rifle_ammo"), 10, 20, 1.0)])
    ent("survivor", [])
    ent("survivor_trader", [pool("minecraft:emerald", 1, 3)])
    ent("box_truck", [])

    # Chests
    chest_table("house", (3, 6), [
        item_entry(m("canned_beans"), 10, 1, 2), item_entry(m("soda_can"), 8, 1, 2), item_entry(m("clean_water"), 8, 1, 2),
        item_entry(m("cloth"), 10, 1, 4), item_entry(m("bandage"), 8, 1, 2), item_entry("minecraft:bread", 8, 1, 3),
        item_entry("minecraft:string", 6, 1, 4), item_entry("minecraft:wheat_seeds", 6, 2, 6), item_entry("minecraft:carrot", 5, 1, 3),
        item_entry(m("baseball_bat"), 3), item_entry(m("painkillers"), 4, 1, 2), item_entry(m("handgun_ammo"), 3, 2, 6),
        item_entry(m("winter_hat"), 2), item_entry(m("light_shirt"), 2), item_entry("minecraft:torch", 6, 2, 6),
        item_entry(m("antibiotics"), 1), item_entry("minecraft:emerald", 3, 1, 3)])
    chest_table("store", (3, 6), [
        item_entry(m("canned_beans"), 12, 1, 3), item_entry(m("canned_peaches"), 10, 1, 3), item_entry(m("soda_can"), 12, 1, 3),
        item_entry(m("clean_water"), 12, 1, 3), item_entry(m("energy_bar"), 8, 1, 3), item_entry(m("mre"), 3, 1, 1),
        item_entry("minecraft:sugar", 4, 1, 4), item_entry("minecraft:paper", 4, 1, 4), item_entry(m("cloth"), 4, 1, 3),
        item_entry("minecraft:emerald", 4, 1, 4)])
    chest_table("police", (3, 5), [
        item_entry(m("handgun_ammo"), 12, 4, 16), item_entry(m("shotgun_shells"), 8, 2, 8), item_entry(m("pistol"), 4),
        item_entry(m("revolver"), 2), item_entry(m("shotgun"), 2), item_entry(m("riot_helmet"), 2), item_entry(m("riot_armor"), 1),
        item_entry(m("riot_boots"), 2), item_entry(m("bandage"), 6, 1, 2), item_entry(m("gun_parts"), 3, 1, 2),
        item_entry(m("crowbar"), 3), item_entry("minecraft:emerald", 4, 1, 4)])
    chest_table("clinic", (3, 6), [
        item_entry(m("bandage"), 14, 1, 4), item_entry(m("medkit"), 5), item_entry(m("antibiotics"), 6, 1, 2),
        item_entry(m("painkillers"), 10, 1, 3), item_entry(m("clean_water"), 8, 1, 2), item_entry(m("gas_mask"), 2),
        item_entry("minecraft:glass_bottle", 6, 1, 3), item_entry("minecraft:honey_bottle", 3)])
    chest_table("gas_station", (3, 5), [
        item_entry(m("gas_can"), 8, 1, 2), item_entry(m("soda_can"), 10, 1, 3), item_entry(m("energy_bar"), 8, 1, 3),
        item_entry(m("scrap_metal"), 8, 1, 4), item_entry(m("crowbar"), 3), item_entry(m("fire_axe"), 2),
        item_entry("minecraft:coal", 6, 2, 6), item_entry(m("cargo_shorts"), 2), item_entry(m("sun_hat"), 2)])
    chest_table("military", (4, 7), [
        item_entry(m("rifle_ammo"), 12, 4, 16), item_entry(m("handgun_ammo"), 10, 4, 16), item_entry(m("hunting_rifle"), 2),
        item_entry(m("assault_rifle"), 1), item_entry(m("smg"), 2), item_entry(m("military_helmet"), 4),
        item_entry(m("military_vest"), 3), item_entry(m("military_pants"), 3), item_entry(m("military_boots"), 4),
        item_entry(m("mre"), 10, 1, 3), item_entry(m("medkit"), 4), item_entry(m("landmine"), 4, 1, 3),
        item_entry(m("barbed_wire"), 6, 2, 8), item_entry(m("gun_parts"), 6, 1, 3), item_entry(m("gas_mask"), 3)])
    chest_table("airdrop", (6, 9), [
        item_entry(m("antibiotics"), 8, 1, 2), item_entry(m("medkit"), 8, 1, 2), item_entry(m("mre"), 12, 2, 4),
        item_entry(m("clean_water"), 12, 2, 4), item_entry(m("handgun_ammo"), 10, 8, 24), item_entry(m("rifle_ammo"), 8, 6, 18),
        item_entry(m("shotgun_shells"), 8, 4, 12), item_entry(m("gun_parts"), 8, 1, 3), item_entry(m("gas_can"), 6, 1, 2),
        item_entry(m("pistol"), 3), item_entry(m("shotgun"), 2), item_entry(m("hunting_rifle"), 2), item_entry(m("smg"), 2),
        item_entry(m("assault_rifle"), 1), item_entry(m("winter_coat"), 3), item_entry(m("military_vest"), 2),
        item_entry(m("landmine"), 3, 1, 2), item_entry("minecraft:golden_apple", 1)])
    chest_table("settlement", (2, 5), [
        item_entry("minecraft:wheat_seeds", 10, 2, 8), item_entry("minecraft:carrot", 8, 1, 4), item_entry("minecraft:potato", 8, 1, 4),
        item_entry("minecraft:bread", 8, 1, 3), item_entry(m("clean_water"), 8, 1, 2), item_entry(m("cloth"), 6, 1, 4),
        item_entry("minecraft:emerald", 4, 1, 3)])


# =============================================================== data: damage, worldgen, spawns, dimension

def make_worldgen():
    for name, exh in (("bullet", 0.1), ("infection", 0.0), ("dehydration", 0.0), ("hypothermia", 0.0),
                      ("heatstroke", 0.0), ("roadkill", 0.1)):
        wj(os.path.join(D, "damage_type", name + ".json"),
           {"exhaustion": exh, "message_id": f"{MOD}.{name}", "scaling": "when_caused_by_living_non_player"})
    survival = [f"{MOD}:{n}" for n in ("infection", "dehydration", "hypothermia", "heatstroke")]
    for tag in ("bypasses_armor", "no_knockback", "bypasses_enchantments"):
        wj(os.path.join(DMC, "tags", "damage_type", tag + ".json"), {"replace": False, "values": survival})
    wj(os.path.join(DMC, "tags", "damage_type", "bypasses_effects.json"), {"replace": False, "values": [f"{MOD}:infection"]})
    wj(os.path.join(DMC, "tags", "damage_type", "bypasses_resistance.json"), {"replace": False, "values": [f"{MOD}:infection"]})
    wj(os.path.join(DMC, "tags", "damage_type", "is_projectile.json"), {"replace": False, "values": [f"{MOD}:bullet"]})

    # Towns
    for name, settle in (("ruined_town", False), ("survivor_settlement", True)):
        wj(os.path.join(D, "worldgen", "structure", name + ".json"),
           {"type": f"{MOD}:town", "biomes": f"#{MOD}:has_town", "step": "surface_structures", "spawn_overrides": {},
            "terrain_adaptation": "none", "settlement": settle})
    wj(os.path.join(D, "worldgen", "structure_set", "towns.json"),
       {"structures": [{"structure": f"{MOD}:ruined_town", "weight": 3}, {"structure": f"{MOD}:survivor_settlement", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 28, "separation": 10, "salt": 1928374651}})
    wj(os.path.join(D, "tags", "worldgen", "biome", "has_town.json"),
       {"replace": False, "values": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:snowy_plains",
                                     "minecraft:savanna", "minecraft:desert", "minecraft:forest", "minecraft:birch_forest",
                                     "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:meadow", "minecraft:flower_forest"]})

    # Natural zombie spawns (added alongside vanilla mobs)
    sp = lambda t_, w, a, b: {"type": f"{MOD}:{t_}", "weight": w, "minCount": a, "maxCount": b}
    wj(os.path.join(D, "forge", "biome_modifier", "zombie_spawns.json"),
       {"type": "forge:add_spawns", "biomes": "#minecraft:is_overworld", "spawners": [
           sp("walker", 90, 2, 4), sp("runner", 25, 1, 3), sp("screamer", 8, 1, 1), sp("soldier_zombie", 12, 1, 2),
           sp("riot_zombie", 7, 1, 1), sp("spitter", 10, 1, 2), sp("bloater", 8, 1, 1), sp("crawler", 20, 1, 3),
           sp("brute", 5, 1, 1), sp("frostbitten", 25, 1, 3)]})

    # Truck interior dimension
    wj(os.path.join(D, "dimension_type", "truck_interior.json"), {
        "ultrawarm": False, "natural": False, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": True,
        "has_raids": False, "has_skylight": False, "has_ceiling": True, "coordinate_scale": 1.0, "ambient_light": 0.0,
        "logical_height": 256, "effects": "minecraft:overworld", "infiniburn": "#minecraft:infiniburn_overworld",
        "min_y": 0, "height": 256, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0})
    wj(os.path.join(D, "dimension", "truck_interior.json"), {
        "type": f"{MOD}:truck_interior",
        "generator": {"type": "minecraft:flat", "settings": {"biome": "minecraft:the_void", "layers": [],
                                                             "lakes": False, "features": False, "structure_overrides": []}}})


def main():
    # Clean generated folders so stale files never linger
    for sub in ("textures", "models", "blockstates", "sounds"):
        p = os.path.join(A, sub)
        if os.path.isdir(p):
            shutil.rmtree(p)
    for sub in ("recipes", "loot_tables", "damage_type", "worldgen", "forge", "dimension", "dimension_type", "tags"):
        p = os.path.join(D, sub)
        if os.path.isdir(p):
            shutil.rmtree(p)
    if os.path.isdir(DMC):
        shutil.rmtree(DMC)

    make_items()
    make_blocks()
    make_entities()
    make_armor_layers()
    make_effect_icon()
    make_models()
    make_lang()
    make_recipes()
    make_loot()
    make_worldgen()
    make_sounds()
    print("Assets generated in", ROOT)


if __name__ == "__main__":
    main()
