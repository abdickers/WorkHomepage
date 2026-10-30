'use client';
import { useCallback, useEffect, useRef, useState } from 'react';
import type { WatchItem } from '@/lib/web-watch.mjs';
type WatchData = { checkedAt: string; items: WatchItem[]; sources: { name: string; url: string; why: string; available: boolean }[] };
function central(date: string) { return new Date(date.length === 10 ? `${date}T12:00:00Z` : date).toLocaleString('en-US', { timeZone: 'America/Chicago', month: 'short', day: 'numeric', ...(date.length > 10 ? { hour: 'numeric', minute: '2-digit' } as const : {}) }); }
function Item({ item }: { item: WatchItem }) {
  return <article className={`watch-item watch-${item.level}`}>
    <div className="watch-item-meta"><span>{item.status}</span><span>{item.source} · {central(item.date)}{item.date.length > 10 ? ' CT' : ''}</span></div>
    <h3><a href={item.url} target="_blank" rel="noreferrer">{item.title} ↗</a></h3>
    <p>{item.summary}</p>
    <details className="watch-action"><summary>What to check</summary><p>{item.action}</p></details>
  </article>;
}
export function WebWatch() {
  const [data, setData] = useState<WatchData | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [signIn, setSignIn] = useState('');
  const [filter, setFilter] = useState('All');
  const [expanded, setExpanded] = useState(false);
  const inFlight = useRef(false);
  const dialog = useRef<HTMLDialogElement>(null);
  const refresh = useCallback(async () => {
    if (inFlight.current) return;
    inFlight.current = true; setBusy(true); setSignIn('');
    try {
      const r = await fetch('/api/web-watch', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}', signal: AbortSignal.timeout(25000) });
      const body = await r.json() as WatchData & { message?: string; chatGPTSignInPath?: string };
      if (!r.ok) { if (body.chatGPTSignInPath) setSignIn(body.chatGPTSignInPath); throw new Error(body.message || 'The watch could not refresh.'); }
      setData(body); setError('');
    } catch (e) { setError(e instanceof Error ? e.message : 'The watch could not refresh.'); }
    finally { inFlight.current = false; setBusy(false); }
  }, []);
  useEffect(() => {
    void refresh();
    const timer = setInterval(() => { if (document.visibilityState === 'visible') void refresh(); }, 300000);
    const visible = () => { if (document.visibilityState === 'visible') void refresh(); };
    document.addEventListener('visibilitychange', visible);
    return () => { clearInterval(timer); document.removeEventListener('visibilitychange', visible); };
  }, [refresh]);
  const active = data?.items.filter(i => i.level !== 'resolved') || [];
  const history = data?.items.filter(i => i.level === 'resolved') || [];
  const urgent = active.filter(i => i.level === 'urgent').length;
  const missing = data?.sources.filter(s => !s.available) || [];
  const checked = data?.sources.filter(s => s.available).length || 0;
  const filtered = active.filter(i => filter === 'All' || (filter === 'Security' ? ['PHP', 'CISA'].includes(i.source) : !['PHP', 'CISA'].includes(i.source)));
  const stale = Boolean(error);
  const state = urgent ? 'urgent' : stale || missing.length ? 'uncertain' : !data ? 'loading' : active.length ? 'watching' : 'quiet';
  const summary = busy && !data ? 'Checking the horizon…' : !data ? 'Watch unavailable · retry' : !checked ? 'Feeds unavailable · retry' : `${urgent ? `${urgent} high priority` : `${active.length} open notices`}${missing.length || stale ? ' · incomplete' : ' · live'}`;
  const headline = active[0]?.title || (!data || !checked ? 'Open the watch for details' : 'No matching open notices');
  return <section className={`web-watch eye-state-${state}`} aria-label="External web watch">
    <button className="watch-launch" aria-haspopup="dialog" aria-expanded={expanded} onClick={() => { dialog.current?.showModal(); setExpanded(true); }}>
      <img className="watch-eye-art" src="/art/web-watch/sauron-eye.webp" width="72" height="72" alt=""/>
      <span className="watch-widget-copy"><span className="watch-widget-title">Eye on the Web <span aria-hidden="true">↗</span></span><span className="watch-widget-status" role="status">{summary}</span><span className="watch-widget-headline" title={headline}>{headline}</span></span>
      {urgent > 0 && <span className="watch-count" aria-label={`${urgent} high priority notices`}>{urgent}</span>}
    </button>
    <dialog ref={dialog} className={`watch-dialog eye-state-${state}`} aria-labelledby="watch-dialog-title" onClose={() => setExpanded(false)} onClick={e => { if (e.target === e.currentTarget) { const rect = e.currentTarget.getBoundingClientRect(); if (e.clientX < rect.left || e.clientX > rect.right || e.clientY < rect.top || e.clientY > rect.bottom) dialog.current?.close(); } }}>
      <div className="watch-dialog-shell">
        <header className="watch-dialog-header"><img className="watch-eye-art" src="/art/web-watch/sauron-eye.webp" width="60" height="60" alt=""/><div><span className="watch-kicker">THE WATCHTOWER</span><h2 id="watch-dialog-title">Eye on the Web</h2><p>{active.length} open · {urgent} high priority</p></div><button className="watch-icon" onClick={() => dialog.current?.close()} aria-label="Close web watch">×</button></header>
        <div className="watch-dialog-toolbar"><span>{data ? `Checked ${central(data.checkedAt)} CT` : 'No verified check yet'}<small>{checked}/8 feeds · 14 days + ongoing incidents</small></span><button className="watch-refresh" onClick={() => void refresh()} disabled={busy}>{busy ? 'Checking…' : '↻ Refresh'}</button></div>
        <div className="watch-body">
          {error && <p className="watch-error" role="alert">{error} {data && 'Previous results may be stale.'} {signIn && <a href={signIn}>Sign in to resume ↗</a>}</p>}
          {!!missing.length && <p className="watch-error">Unavailable: {missing.map(s => s.name).join(', ')}. Coverage incomplete.</p>}
          <div className="watch-filters" aria-label="Filter web notices">{['All', 'Services', 'Security'].map(f => <button key={f} aria-pressed={filter === f} onClick={() => setFilter(f)}>{f}{f === 'All' ? ` ${active.length}` : ''}</button>)}</div>
          {filtered.length ? <div className="watch-items">{filtered.map(i => <Item key={i.id} item={i}/>)}</div> : <p className="watch-empty">{busy && !data ? 'Reading official feeds…' : !data || !checked ? 'No verified results. Refresh to try again.' : `No matching open notices${missing.length || stale ? ' in the available results; coverage is incomplete.' : ' in checked feeds.'}`}</p>}
          {!!history.length && <details className="watch-history"><summary>Recently resolved · {history.length}</summary><div className="watch-items">{history.map(i => <Item key={i.id} item={i}/>)}</div></details>}
          <details className="watch-sources"><summary>Sources & scope</summary><p>Official provider incidents, PHP security releases, and newly catalogued CISA exploits matching PHP, WordPress, Apache HTTP, nginx, MySQL, PostgreSQL, OpenSSL, and our watched services. Potential relevance only: installed versions and impact on our systems are unconfirmed. History is limited to each feed. Refreshes every 5 minutes while the dashboard is open.</p><div>{data?.sources.map(s => <a key={s.name} href={s.url} target="_blank" rel="noreferrer" title={s.why}>{s.name} · {s.available ? 'checked' : 'unavailable'} ↗</a>)}</div></details>
        </div>
      </div>
    </dialog>
  </section>;
}
