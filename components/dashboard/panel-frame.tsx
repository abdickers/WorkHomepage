/** Decorative art only. All headings, controls and data stay in the parent panel. */
export function PanelFrame({ kind = "elven" }: { kind?: "elven" | "dwarven" | "oak" | "scroll" | "gondor" }) {
  const surface = <span className="realm-panel-surface" aria-hidden="true" />;
  if (kind === "gondor") {
    return <>{surface}<div className="realm-frame realm-gondor-art" aria-hidden="true">
      {["top", "bottom", "left", "right", "tl", "tr", "bl", "br", "tree", "diamond"].map(part =>
        <span key={part} className={`gondor-${part}`} />)}
    </div></>;
  }
  if (kind === "oak") {
    return <>{surface}<div className="realm-frame realm-oak-art" aria-hidden="true">
      {["top", "bottom", "left", "right", "top-left", "top-right", "bottom-left", "bottom-right"].map(part =>
        <span key={part} className={`oak-${part}`} />)}
    </div></>;
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
  if (kind === "elven") {
    return <>{surface}<div className="realm-frame realm-elven-art" aria-hidden="true">
      {["top", "bottom", "left", "right", "tl", "tr", "bl", "br"].map(part =>
        <span key={part} className={`elven-${part}`} />)}
    </div></>;
  }
  return <>{surface}<div className={`realm-frame realm-${kind}-art`} aria-hidden="true" /></>;
}

/** Replaces the sidebar's former straight rule; caps never stretch. */
export function GondorPageRail() {
  return <div className="gondor-page-rail" aria-hidden="true">
    <span className="gondor-rail-top" /><span className="gondor-rail-shaft" /><span className="gondor-rail-bottom" />
  </div>;
}
