#!/usr/bin/env python3
"""Generate SquadShelf store assets (icon 512, feature graphic 1024x500) with PIL."""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.dirname(os.path.abspath(__file__))


def gradient(size, w=None, h=None):
    W = size if w is None else w
    H = size if h is None else h
    img = Image.new("RGB", (W, H))
    c1, c2 = (0x1a, 0x1a, 0x2e), (0x16, 0x21, 0x3e)
    px = img.load()
    for y in range(H):
        for x in range(W):
            t = (x + y) / (W + H - 2)
            px[x, y] = tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(3))
    return img


def rounded_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    return m


def find_font():
    cands = ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
             "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"]
    for c in cands:
        if os.path.exists(c):
            return c
    raise SystemExit("no dejavu font found")


font_path = find_font()

# ---- 512 app icon: rounded tile, open-book glyph + "S" monogram ----
S = 512
img = gradient(S)
d = ImageDraw.Draw(img, "RGBA")
cx, cy = S // 2, S // 2 + 30
w, h = 170, 120
# two pages of an open book meeting at the spine
d.polygon([(cx - 4, cy - 18), (cx - w, cy - h + 26), (cx - w, cy + h - h + 26), (cx - 4, cy + h - h + 26)], fill=(235, 240, 255))
d.polygon([(cx + 4, cy - 18), (cx + w, cy - h + 26), (cx + w, cy + h - h + 26), (cx + 4, cy + h - h + 26)], fill=(255, 255, 255))
d.line([(cx, cy - 18), (cx, cy + h - h + 26)], fill=(90, 100, 140), width=6)
for i in range(3):
    off = 26 + i * 30
    d.line([(cx - w + 22, cy - h + 26 + off), (cx - 24, cy - 18 + off)], fill=(170, 180, 215), width=5)
    d.line([(cx + w - 22, cy - h + 26 + off), (cx + 24, cy - 18 + off)], fill=(190, 197, 230), width=5)
f = ImageFont.truetype(font_path, 150)
txt = "S"
bb = d.textbbox((0, 0), txt, font=f)
tw, th = bb[2] - bb[0], bb[3] - bb[1]
d.text(((S - tw) // 2 - bb[0], 96 - bb[1]), txt, font=f, fill=(74, 144, 217))

mask = rounded_mask(S, 104)
icon = Image.new("RGBA", (S, S), (0, 0, 0, 0))
icon.paste(img, (0, 0), mask)
icon.save(os.path.join(OUT, "icon_512.png"))
print("icon saved:", icon.size)

# ---- feature graphic 1024x500 ----
W, H = 1024, 500
bg = gradient(W, h=H)
d = ImageDraw.Draw(bg)
d.rounded_rectangle([48, 96, 58, H - 96], radius=5, fill=(74, 144, 217))
f_title = ImageFont.truetype(font_path, 108)
d.text((96, 130), "SquadShelf", font=f_title, fill=(255, 255, 255))
# fit subheadline within the graphic with margin
sub = "Read Markdown. Anywhere on Android."
f_sub = ImageFont.truetype(font_path, 22)  # fallback; loop below refines upward
for sz in range(44, 20, -2):
    f_sub = ImageFont.truetype(font_path, sz)
    bb = d.textbbox((0, 0), sub, font=f_sub)
    if 98 + (bb[2] - bb[0]) <= W - 48:
        break
d.text((98, 290), sub, font=f_sub, fill=(170, 180, 215))
bg.save(os.path.join(OUT, "feature_graphic_1024x500.png"))
print("feature graphic saved:", bg.size)
