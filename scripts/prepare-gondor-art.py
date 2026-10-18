"""Reproduce measured Gondor slices and uncropped guardian delivery icons."""
from pathlib import Path
import hashlib
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'artwork/gondor'
OUT = ROOT / 'public/art/gondor'
SLICES = {
    'gondor-header-frame.png': {
        'header-top-left': (9, 90, 154, 235),
        'header-top-right': (2017, 90, 2162, 235),
        'header-bottom-left': (9, 530, 154, 675),
        'header-bottom-right': (2017, 530, 2162, 675),
        'header-top': (400, 90, 800, 140),
        'header-bottom': (400, 625, 800, 675),
        'header-left': (9, 400, 79, 500),
        'header-right': (2092, 400, 2162, 500),
        'header-tree': (970, 0, 1202, 160),
        'header-diamond': (1028, 610, 1144, 698),
    },
    'gondor-left-rail.png': {
        'page-rail-top': (256, 0, 466, 420),
        'page-rail-shaft': (310, 420, 414, 800),
        'page-rail-bottom': (295, 1880, 430, 2140),
    },
}
OUT.mkdir(parents=True, exist_ok=True)
manifest = {'coordinates': '[left, top, right, bottom), native source pixels',
            'sources': [], 'delivery': []}
for path in SOURCE.glob('*.png'):
    im = Image.open(path).convert('RGBA')
    manifest['sources'].append({'path': path.name, 'size': list(im.size),
        'alphaBounds': list(im.getchannel('A').getbbox()),
        'sha256': hashlib.sha256(path.read_bytes()).hexdigest()})
    if path.name in SLICES:
        for name, box in SLICES[path.name].items():
            crop = im.crop(box)
            dest = OUT / (name + '.webp')
            crop.save(dest, 'WEBP', lossless=True, exact=True, method=6)
            assert Image.open(dest).getchannel('A').tobytes() == crop.getchannel('A').tobytes()
            manifest['delivery'].append({'path': dest.name, 'source': path.name, 'bounds': box, 'lossless': True})
    else:
        for size in [128, 256]:
            scaled = im.resize((size, size), Image.Resampling.LANCZOS)
            dest = OUT / (path.stem + '-' + str(size) + '.webp')
            scaled.save(dest, 'WEBP', quality=90, method=6, exact=True)
            assert Image.open(dest).getchannel('A').tobytes() == scaled.getchannel('A').tobytes()
            manifest['delivery'].append({'path': dest.name, 'source': path.name, 'size': [size, size], 'uncropped': True})
print('Prepared', len(manifest['delivery']), 'delivery assets; alpha verified.')

# Explicit corner/rail layers avoid border-image sides disappearing on resize.
elven_source = ROOT / 'public/art/frames/elven-panel-frame-nine-slice.webp'
elven = Image.open(elven_source).convert('RGBA')
manifest['sources'].append({'path': str(elven_source.relative_to(ROOT)).replace('\\', '/'),
    'size': list(elven.size), 'alphaBounds': list(elven.getchannel('A').getbbox()),
    'sha256': hashlib.sha256(elven_source.read_bytes()).hexdigest()})
boxes = {'tl': (0,0,360,360), 'top': (360,0,720,360), 'tr': (720,0,1080,360),
         'left': (0,360,360,720), 'right': (720,360,1080,720),
         'bl': (0,720,360,1080), 'bottom': (360,720,720,1080), 'br': (720,720,1080,1080)}
for name, box in boxes.items():
    crop = elven.crop(box)
    path = ROOT / ('public/art/frames/elven-' + name + '.webp')
    crop.save(path, 'WEBP', lossless=True, exact=True, method=6)
    assert Image.open(path).getchannel('A').tobytes() == crop.getchannel('A').tobytes()
    manifest['delivery'].append({'path': str(path.relative_to(ROOT)).replace('\\', '/'),
        'source': str(elven_source.relative_to(ROOT)).replace('\\', '/'), 'bounds': box, 'lossless': True})
(SOURCE / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
print('Prepared eight explicit Elven corner/rail layers; alpha verified.')
