const illustrations = {
  overview: "overview",
  quest: "quest",
  notes: "notes",
  beacon: "beacon",
  shield: "shield",
  treasury: "treasury",
  calendar: "calendar",
  council: "council",
} as const;

export type GondorArtName = keyof typeof illustrations;

/** Illustrated decoration; labels, counts and actions remain ordinary HTML. */
export function GondorArt({ name, size = 40, className = "", alt = "" }: {
  name: GondorArtName; size?: number; className?: string; alt?: string;
}) {
  const path = `/art/royal-gondor/icons/${illustrations[name]}`;
  // Pre-sized local images retain the generated artwork's real alpha channel.
  // eslint-disable-next-line @next/next/no-img-element
  return <img
    className={`gondor-art ${className}`}
    src={`${path}-${size > 48 ? 256 : 96}.webp`}
    srcSet={`${path}-96.webp 96w, ${path}-256.webp 256w`}
    sizes={`${size}px`}
    width={size} height={size}
    alt={alt} aria-hidden={alt ? undefined : true}
    decoding="async"
  />;
}
