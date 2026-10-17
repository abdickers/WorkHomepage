/** Supplied decoration. Values, accessible names and actions remain live HTML. */
export function PolishArt({ name, className = "" }: { name: "raven" | "treasury" | "hourglass"; className?: string }) {
  // Local, pre-sized WebP derivatives retain the original alpha and proportions.
  // eslint-disable-next-line @next/next/no-img-element
  return <img className={`polish-art ${className}`} src={`/art/polish/${name}.webp`} width={256} height={256} alt="" aria-hidden="true" decoding="async" />;
}
