# Work dashboard presentation assets

The four user-supplied beacon originals are retained byte-for-byte in `beacons/`.
They are 1024 × 1536 RGBA PNGs. `beacon-manifest.json` records their hashes and
the 320 × 480 WebP delivery derivatives in `public/art/beacons/`.
Derivatives use Lanczos resampling, WebP quality 88, method 6, with alpha retained.
No color keying, dark-pixel removal, cropping, or image generation was applied.

Existing count semantics are preserved: positive running counts use gold,
positive queued counts use blue, and positive error counts use orange. Zero or
unknown counts use the unlit tower, distinguished by their live HTML values.
The returned-flow roster uses unlit artwork because it is a count of returned
flows, not an active job or alert. Existing stale/error messages remain visible.

`public/art/frames/` reuses the approved artwork derivatives from
`abdickers/HomePage` (`assets/redesign` and `assets/expansion`). The Elven sheet
is 1080 × 1080 with 360-pixel slices; the Dwarven sheet uses 320-pixel slices.
These prepared sheets have quiet rails and transparent centers. Display corners
stay fixed at 42–64 CSS pixels; the rails repeat rather than stretching ornaments.
Oak corner crops retain their source aspect ratios; quiet edge strips repeat.
The Ravens scroll uses separate roller ends, repeatable roller center and paper
strips. No live text or controls are baked into the artwork.

All decoration is noninteractive and hidden from assistive technology. Existing
heading, form, disclosure, link, feed, authentication and storage behavior remains
in the original components. The hero and hosting/access manifest are unchanged.
