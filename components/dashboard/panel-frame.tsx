/** Decorative art only. All headings, controls and data stay in the parent panel. */
export function PanelFrame({ kind = "elven" }: { kind?: "elven" | "dwarven" | "oak" | "scroll" }) {
  if (kind === "oak") {
    return <div className="realm-frame realm-oak-art" aria-hidden="true">
      {["top", "bottom", "left", "right", "top-left", "top-right", "bottom-left", "bottom-right"].map(part =>
        <span key={part} className={`oak-${part}`} />)}
    </div>;
  }
  if (kind === "scroll") {
    return <div className="realm-frame realm-scroll-art" aria-hidden="true">
      <div className="realm-scroll-paper"><span /><span /><span /></div>
      {["top", "bottom"].map(end => <div key={end} className={`realm-scroll-cap cap-${end}`}>
        <img src="/art/frames/ravens-roller-left.webp" alt="" width="180" height="176" />
        <span />
        <img src="/art/frames/ravens-roller-right.webp" alt="" width="180" height="176" />
      </div>)}
    </div>;
  }
  return <div className={`realm-frame realm-${kind}-art`} aria-hidden="true" />;
}
