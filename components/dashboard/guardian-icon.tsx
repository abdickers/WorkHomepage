/** Redundant status artwork; adjacent live text carries the meaning. */
export function GuardianIcon({ alert = false, unavailable = false }: { alert?: boolean; unavailable?: boolean }) {
  const name = alert && !unavailable ? "guardian-lit" : "guardian-neutral";
  return <img className="provider-metric__guardian" src={`/art/gondor/${name}-128.webp`}
    srcSet={`/art/gondor/${name}-128.webp 128w, /art/gondor/${name}-256.webp 256w`}
    sizes="(max-width: 480px) 80px, 104px" width="104" height="104"
    alt="" aria-hidden="true" data-unavailable={unavailable || undefined} decoding="async" />;
}
