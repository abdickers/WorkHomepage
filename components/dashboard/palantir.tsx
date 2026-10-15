"use client";
import { useState, useRef } from "react";
import { ArrowUpRight, Copy, Check, MessageSquare, Send } from "lucide-react";
import { BrandLogo } from "./brand-logo";
import { IncidentBriefing } from "./incident-briefing";
export function Palantir() {
  const [prompt, setPrompt] = useState("");
  const [copied, setCopied] = useState(false);
  const [blocked, setBlocked] = useState(false);
  const companionRef = useRef<Window | null>(null);
  function open() {
    const url = prompt.trim()
      ? `https://chatgpt.com/?q=${encodeURIComponent(prompt.trim())}`
      : "https://chatgpt.com/";
    let companion = companionRef.current;
    try {
      if (companion && !companion.closed) {
        companion.location.href = url;
      } else {
        companion = window.open(
          url,
          "adam-work-palantir",
          "popup=yes,width=720,height=1000,resizable=yes,scrollbars=yes",
        );
      }
    } catch {
      companion = window.open(
        url,
        "adam-work-palantir",
        "popup=yes,width=720,height=1000,resizable=yes,scrollbars=yes",
      );
    }
    companionRef.current = companion;
    setBlocked(!companion);
    try {
      if (companion) companion.opener = null;
    } catch {}
    companion?.focus();
  }
  async function copy() {
    try {
      await navigator.clipboard.writeText(prompt);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      setBlocked(true);
    }
  }
  return (
    <section className="panel palantir">
      <div className="panel-heading">
        <div>
          <MessageSquare size={18} />
          <h3>The Council</h3>
        </div>
        <span className="tag">CLAUDE · ON DEMAND</span>
      </div>
      <IncidentBriefing />
      <details className="chatgpt-companion">
        <summary>The Palantír · Open ChatGPT companion</summary>
        <div className="orb-scene" aria-hidden="true">
          <div className="palantir-orb">
            <span>✧</span>
          </div>
          <div className="orb-plinth" />
        </div>
        <h4>A little counsel. A clearer path.</h4>
        <p className="realm-account">Use dickersona@76fireworks.com in ChatGPT’s account switcher. Companion windows share your browser login.</p>
        <p className="palantir-description">
          Bring a question, untangle a problem, or turn a rough thought into a
          plan.
        </p>
        <div className="prompt-presets">
          {[
            "Help me prioritize my day.",
            "Help me troubleshoot a Celigo sync failure.",
            "Help me write a clear message.",
          ].map((p, i) => (
            <button key={p} onClick={() => setPrompt(p)}>
              {["Plan my day", "Investigate a sync", "Find the words"][i]}
            </button>
          ))}
        </div>
        <textarea
          aria-label="Message for ChatGPT"
          value={prompt}
          maxLength={8000}
          onChange={(e) => setPrompt(e.target.value)}
          placeholder="What’s on your mind, Adam?"
        />
        <div className="palantir-actions">
          <button
            className="icon-btn"
            aria-label="Copy ChatGPT prompt"
            disabled={!prompt.trim()}
            onClick={copy}
          >
            {copied ? <Check size={16} /> : <Copy size={16} />}
          </button>
          <button className="gold-button" onClick={open}>
            <BrandLogo name="ChatGPT" />
            Open ChatGPT
            <ArrowUpRight size={15} />
          </button>
        </div>
        {blocked && (
          <p className="connection-message">
            If your browser blocked the window,{" "}
            <a href="https://chatgpt.com/" target="_blank" rel="noreferrer">
              open ChatGPT here
            </a>{" "}
            and paste your prompt.
          </p>
        )}
        <div className="panel-foot">
          <span>Work account · dickersona@76fireworks.com</span>
        </div>
      </details>
    </section>
  );
}
