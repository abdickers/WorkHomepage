# Council study illustrations

The seven supplied PNGs are copied byte-for-byte to `public/art/study/originals/`
using descriptive filenames. `study-manifest.json` records source SHA-256 hashes,
actual dimensions, color mode and delivery paths. Sources are not upscaled or
described as 2K/4K masters.

| Artwork | Original dimensions | Delivery WebP |
| --- | --- | --- |
| Quest medallion | 1254 × 1254 RGBA | 256 × 256 |
| Elven sundial | 1254 × 1254 RGBA | 256 × 256 |
| Palantír | 1254 × 1254 RGBA | 384 × 384 |
| Quill and inkwell | 1254 × 1254 RGBA | 192 × 192 |
| Elven compass | 1254 × 1254 RGBA | 256 × 256 |
| Ranger sword | 2172 × 724 RGBA | 900 × 300 |
| Dark map | 1536 × 1024 RGB | 1536 × 1024 |

The six illustrations have real alpha. The map is intentionally opaque and used
only as a separate, subdued analytics background (cover/center, no tiling).
Derivatives retain aspect ratios and use Lanczos resizing, WebP quality 88,
method 6 and preserved alpha. No crops, color-keying, blend-mode removal or
dark-pixel deletion were applied. Full sword hilt and compass tips are retained.
Alpha edges were reviewed on the dashboard’s forest and light surfaces.

`StudyArt` supplies explicit dimensions, empty alt text and noninteractive
decoration. The medallion sits beside live completion text and the existing SVG
progress ring. The sundial is static decoration; current Central time is HTML.
The constellation retains its chart SVG, spokes, interactive nodes and status
logic, with the compass above live counts. The sword is above the error list.
Council artwork is shared with the expanded companion disclosure; its controls
and actions are unchanged. The quill remains outside the capture textarea.

Styling is isolated in `app/study-presentation.css`. The existing border and
beacon implementations, API/auth/data logic, hosting identity and access policy
are preserved. Long error-flow names wrap at narrow widths. No animations or
additional runtime dependencies were added.
