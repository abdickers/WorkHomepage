# Illustrated Gondor icons

Created with built-in image generation, 2026-10-18. Silver, charcoal, ivory and restrained sapphire form one consistent inventory-art family. Every source was generated individually with real transparent alpha; only proportional resizing and WebP conversion were used for delivery.

| Name | Role |
| --- | --- |
| overview | White City citadel, navigation and overview |
| quest | Crossed royal swords, priorities and Jira rows |
| notes | Quill and inkwell, field notes and correspondence rows |
| shield | White Tree and seven stars, hero emblem and Registry |
| beacon | Limestone signal tower, production heading and navigation |
| treasury | Silver strongbox and coins, invoices and treasury |
| calendar | Silver astrolabe, agenda and day dial |
| council | Sapphire seeing orb in a silver Gondor pedestal |

Originals: `artwork/royal-gondor/icons/<name>-original.png`.
Delivery: `public/art/royal-gondor/icons/<name>-96.webp` and `<name>-256.webp`; shield also has a 512px derivative.

Exact generation prompts and source provenance: [navigation artwork](ASSETS-NAV.md) and [emblems](ASSETS-EMBLEMS.md).

## Integration and verification

`GondorArt` supplies pre-sized illustrations with transparent silhouettes and responsive image selection. Existing text labels, counts, link targets, feed parsing, storage, connection settings and provider actions are retained. Navigation now has explicit accessible names when labels are hidden on narrow screens.

The current theme selects one scenery variable used by the page, hero surface and full-scene link: daylight White City for Dawn, moonlit royal court for Dusk. Frame surfaces retain their existing insets.

Verified at 1440px desktop and 375px phone widths: all illustrations loaded, no horizontal page overflow, hero shield clear of the title, theme switching through both entry points, and Dawn retained after reload. Synthetic local feed rows were used for visual QA without accessing providers. TypeScript and the 23 existing Node checks passed.
