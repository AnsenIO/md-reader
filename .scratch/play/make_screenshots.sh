#!/usr/bin/env bash
# Render SquadShelf store screenshots (phone 1080x2340) from the real www/ build with mock content.
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=.scratch/play

CHROME=/snap/bin/chromium
COMMON="--headless=new --no-sandbox --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --window-size=1080,2340"

# Stage mock pages at www/ ROOT so relative asset paths (styles.css, app.js, marked.min.js) resolve.
# A <style> override forces the dark theme (headless chromium defaults to light, but the app's
# designed theme is dark; prefers-color-scheme:light in styles.css would otherwise flip it).
python3 - <<'PY'
import pathlib
base = pathlib.Path("www/index.html").read_text()

dark_fix = """<style>
body { background: #0f0f1a !important; color: #e0e0e0 !important; }
.md-content h1, .md-content h2, .md-content h3 { color: #fff !important; }
.md-content p, .md-content li { color: #d0d0d0 !important; }
.md-content code { background: #1a1a2e !important; color: inherit !important; }
.md-content pre { background: #1a1a2e !important; }
.md-content th { background: #1a1a2e !important; color: #fff !important; }
.md-content td { color: #d0d0d0 !important; }
</style>"""

reader_inject = """<script>
document.addEventListener('DOMContentLoaded', () => {
  const el = document.getElementById('content-area');
  el.innerHTML = '<div class="md-content"><h2>release-notes.md</h2><h1>SquadShelf v0.1</h1>' +
   '<p>Your first release is <strong>live on Google Play</strong>. This app reads local Markdown without the cloud.</p>' +
   '<h2>Highlights</h2><ul><li>Opens .md files tapped in any file manager</li><li>Browse navigator for your Downloads folder</li><li>No account, no network needed to read</li></ul>' +
   '<blockquote>Sovereign AI means your notes stay on your device.</blockquote>' +
   '<p>Ship it: <code>npx cap sync android &amp;&amp; ./gradlew bundleRelease</code></p></div>';
  el.scrollTop = 0;
});
</script>"""

browse_inject = """<script>
document.addEventListener('DOMContentLoaded', () => {
  const el = document.getElementById('content-area');
  el.innerHTML = '<div class="nav-bar">📥 Downloads</div>' +
   '<div class="file-item"><span class="file-icon">⬅️</span><div class="file-info"><div class="file-name">🏠 Home</div></div></div>' +
   '<div class="file-item"><span class="file-icon">📁</span><div class="file-info"><div class="file-name">notes</div></div></div>' +
   '<div class="file-item"><span class="file-icon">📁</span><div class="file-info"><div class="file-name">squadshelf</div></div></div>' +
   '<div class="file-item"><span class="file-icon">📝</span><div class="file-info"><div class="file-name">release-notes.md</div><div class="file-size">4.2 KB</div></div></div>' +
   '<div class="file-item"><span class="file-icon">📝</span><div class="file-info"><div class="file-name">fabiabox-spec.md</div><div class="file-size">18.7 KB</div></div></div>' +
   '<div class="nav-hint">Tap a file to read it · tap a folder to open it</div>';
  el.scrollTop = 0;
});
</script>"""

pathlib.Path("www/shot-reader.html").write_text(base.replace("</head>", dark_fix + "\n</head>").replace("</body>", reader_inject + "\n</body>"))
pathlib.Path("www/shot-browse.html").write_text(base.replace("</head>", dark_fix + "\n</head>").replace("</body>", browse_inject + "\n</body>"))
print("mock pages staged at www/ root")
PY

python3 -m http.server 8917 --directory www >/dev/null 2>&1 &
SRV=$!
sleep 1

trap 'kill $SRV 2>/dev/null || true; rm -f www/shot-reader.html www/shot-browse.html' EXIT

$CHROME $COMMON --screenshot="$OUT/screenshot_reader.png" --virtual-time-budget=6000 "http://127.0.0.1:8917/shot-reader.html" 2>/dev/null
$CHROME $COMMON --screenshot="$OUT/screenshot_browse.png" --virtual-time-budget=6000 "http://127.0.0.1:8917/shot-browse.html" 2>/dev/null

python3 - <<'PY'
from PIL import Image, ImageStat
for p in ['screenshot_reader.png','screenshot_browse.png']:
    im = Image.open('.scratch/play/'+p).convert('L')
    st = ImageStat.Stat(im)
    # dark theme expected: mean low-ish (background #0f0f1a ~ 0x18=24), with bright text variance
    print(p, 'size=', im.size, 'mean=%.0f stddev=%.1f' % (st.mean[0], st.stddev[0]))
PY
