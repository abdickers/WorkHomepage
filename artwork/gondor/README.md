# Work dashboard Gondor presentation

This update changes presentation only. Header, production operations, and daily tools use silver/charcoal Gondor frames; the sidebar rail replaces the former straight divider. Existing Elven, Dwarven, and oak panels retain their artwork families. Panel fills are separate inset surfaces so they stop inside the illustrated rails without clipping controls or focus outlines.

`scripts/prepare-gondor-art.py` reproduces all delivery files with Pillow. The original supplied PNGs are retained here unchanged. `manifest.json` records source hashes, native pixel crop bounds, delivery sizes, and alpha preservation. Frame corners remain fixed size; quiet rail segments stretch between them. Elven frames use eight explicit pieces rather than CSS border-image to keep all four sides visible across layout changes.

Guardian icons use the complete uncropped square canvases, delivered at 128 and 256 pixels and displayed at 104 pixels on desktop / 80 on phone. Cloudflare 2xx and confirmed zero counts use the neutral guardian. Nonzero 4xx, 5xx, and exceptions use the lit guardian. Last-confirmed stale values retain explicit stale text and dim neutral guardians. Loading/unavailable remains text, never an invented zero. Vercel has no configured monitoring metrics; its existing unavailable explanation remains, with a dim neutral guardian. Compact event diamonds and Celigo beacon data semantics are unchanged.

The flow constellation inherits the steel palette. Its compass and live count share one centered, pointer-transparent group. Existing stars, selection, links, and disclosures remain interactive.

Validation: all 19 existing Node tests, TypeScript, and whitespace checks pass. Local synthetic responses exercise nonzero/zero/stale/unavailable display states without contacting providers. Browser review covers phone and desktop, both themes, all Elven rails, Gondor frames, flow selection/disclosure, and metric icon sizing. Production source excludes the ignored QA proxy and screenshots under `outputs/`.

Authentication, connector declarations, storage bindings, provider requests, and backend code are unchanged.
