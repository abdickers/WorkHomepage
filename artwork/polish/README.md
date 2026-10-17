# Work dashboard polish kit

Source: the user's Command Center Drive folder, 17 October 2026.

The three new originals (`raven.png`, `treasury.png`, `hourglass.png`) are kept
unchanged under `originals/`. Each is 1254 × 1254 RGBA, with alpha ranging from
0 to 255. Delivery WebPs under `public/art/polish/` are 256 × 256, retain alpha,
and use the complete original canvas. No crop, pixel erasure, or blend mode was
used. `manifest.json` records source hashes and delivery dimensions.

The 26 supplied 24px SVGs are preserved under `public/art/icons/`. Their shapes
are also inlined by `components/dashboard/realm-icon.tsx` to inherit the text
color of each control. Decorative images and icons have no pointer events or
screen-reader content. Existing service logos remain in the original pipeline.

The previously approved study art and beacon originals are reused from
`public/art/study/` and `artwork/beacons/`; this kit does not duplicate them.
The concept image is a mood reference only. Its invented data and labels were
not copied. All live application data, source notices and financial semantics
remain in their existing components.

Presentation is scoped by `data-command-realm="work"` in
`app/command-polish.css`. The Shire repository, APIs, authentication, connector
permissions, storage schemas, and data-sync code are outside this update.
