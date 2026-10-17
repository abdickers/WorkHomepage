const artwork = {
  "quest-medallion": [256, 256],
  "elven-sundial": [256, 256],
  palantir: [384, 384],
  "quill-inkwell": [192, 192],
  "elven-compass": [256, 256],
  "ranger-sword": [900, 300],
} as const;

/** Illustrations are decoration; live values and controls remain in their widgets. */
export function StudyArt({ name, className = "" }: { name: keyof typeof artwork; className?: string }) {
  const [width, height] = artwork[name];
  // Pre-sized, local WebPs preserve the supplied alpha without an image service.
  // eslint-disable-next-line @next/next/no-img-element
  return <img
    className={`study-art ${className}`}
    src={`/art/study/${name}.webp`}
    alt=""
    aria-hidden="true"
    width={width}
    height={height}
    decoding="async"
  />;
}
