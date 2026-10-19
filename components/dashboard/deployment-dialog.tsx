"use client";
import { useEffect, useId, useRef, type ReactNode } from "react";
import { X } from "./realm-icon";

/** Native modal supplies focus containment, Escape and inert background content. */
export function DeploymentDialog({ title, close, children }: { title: string; close: () => void; children: ReactNode }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    dialog.current?.showModal();
    return () => {
      document.body.style.overflow = overflow;
      previous?.focus();
    };
  }, []);
  return <dialog ref={dialog} className="deployment-dialog" aria-labelledby={titleId} onCancel={(event) => { event.preventDefault(); close(); }} onClick={(event) => { if (event.target === event.currentTarget) close(); }}>
    <div className="deployment-dialog-shell">
      <header className="deployment-dialog-heading"><div><span className="citadel-kicker">THE CITADEL WATCH</span><h3 id={titleId}>{title}</h3></div><button autoFocus className="icon-btn" aria-label="Close deployment details" onClick={close}><X size={20}/></button></header>
      <div className="deployment-dialog-body">{children}</div>
    </div>
  </dialog>;
}
