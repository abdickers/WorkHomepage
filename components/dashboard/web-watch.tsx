'use client';
import { useCallback, useEffect, useId, useRef, useState } from 'react';
import type { WatchItem } from '@/lib/web-watch.mjs';
type WatchData = { checkedAt: string; items: WatchItem[]; sources: { name: string; url: string; why: string; available: boolean }[] };
function central(date: string) { return new Date(date.length === 10 ? `${date}T12:00:00Z` : date).toLocaleString('en-US', { timeZone: 'America/Chicago', month: 'short', day: 'numeric', ...(date.length > 10 ? { hour: 'numeric', minute: '2-digit' } as const : {}) }); }
function SauronEye() {
  const id = useId().replace(/:/g, '');
  return <svg className="sauron-eye" viewBox="0 0 240 160" aria-hidden="true"><defs><radialGradient id={id}><stop stopColor="#fff2b0"/><stop offset=".36" stopColor="#ffbd38"/><stop offset=".7" stopColor="#f34e15"/><stop offset="1" stopColor="#6f1608"/></radialGradient></defs><path className="eye-tower" d="M88 160 98 93 82 35 108 61 120 88 132 61 158 35 142 93 152 160Z"/><path className="eye-flame outer" d="M20 77 39 65 31 43 58 53 61 23 85 45 99 12 116 35 134 7 145 39 177 23 174 52 208 42 198 64 224 77 201 96 209 116 180 108 169 138 145 118 126 148 111 122 80 140 75 113 42 120 46 97Z"/><path className="eye-lid" d="M28 78Q120 1 212 78Q120 151 28 78Z" fill={`url(#${id})`}/><path className="eye-iris" d="M120 31Q142 76 120 125Q99 78 120 31Z"/><path d="M31 78Q120 19 209 78M31 78Q120 139 209 78" fill="none" stroke="#ffcf67" strokeWidth="2" opacity=".8"/></svg>;
}
function Item({ item }: { item: WatchItem }) { return <article className={`watch-item watch-${item.level}`}><div className="watch-item-meta"><span>{item.status}</span><span>{item.source} · {central(item.date)}{item.date.length > 10 ? ' CT' : ''}</span></div><h3><a href={item.url} target="_blank" rel="noreferrer">{item.title} <span aria-hidden="true">↗</span></a></h3><p>{item.summary}</p><p className="watch-action"><strong>Next check:</strong> {item.action}</p></article>; }
export function WebWatch() {
  const [data, setData] = useState<WatchData | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [signIn, setSignIn] = useState('');
  const [filter, setFilter] = useState('All');
  const [showAll, setShowAll] = useState(false);
  const inFlight = useRef(false);
  const refresh = useCallback(async () => {
    if (inFlight.current) return; inFlight.current = true; setBusy(true); setSignIn('');
    try {
      const r = await fetch('/api/web-watch', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}', signal: AbortSignal.timeout(25000) });
      const body = await r.json() as WatchData & { message?: string; chatGPTSignInPath?: string };
      if (!r.ok) { if (body.chatGPTSignInPath) setSignIn(body.chatGPTSignInPath); throw new Error(body.message || 'The watch could not refresh.'); }
      setData(body); setError('');
    } catch (e) { setError(e instanceof Error ? e.message : 'The watch could not refresh.'); }
    finally { inFlight.current = false; setBusy(false); }
  }, []);
  useEffect(() => { void refresh(); const timer = setInterval(() => { if (document.visibilityState === 'visible') void refresh(); }, 300000); const visible = () => { if (document.visibilityState === 'visible') void refresh(); }; document.addEventListener('visibilitychange', visible); return () => { clearInterval(timer); document.removeEventListener('visibilitychange', visible); }; }, [refresh]);
  const active = data?.items.filter(i => i.level !== 'resolved') || [];
  const history = data?.items.filter(i => i.level === 'resolved') || [];
  const urgent = active.filter(i => i.level === 'urgent').length;
  const missing = data?.sources.filter(s => !s.available) || [];
  const filtered = active.filter(i => filter === 'All' || (filter === 'Security' ? ['PHP', 'CISA'].includes(i.source) : !['PHP', 'CISA'].includes(i.source)));
  const state = error || missing.length ? 'uncertain' : !data ? 'loading' : urgent ? 'urgent' : active.length ? 'watching' : 'quiet';
  return <section className={`web-watch eye-state-${state}`} aria-labelledby="web-watch-title">
    <header className="watch-header"><div className="eye-emblem"><SauronEye/></div><div className="watch-heading"><span className="watch-kicker">THE WATCHTOWER · EXTERNAL INTELLIGENCE</span><h2 id="web-watch-title">Eye on the Web</h2><p>Service disruptions. Security threats. The things that deserve your attention.</p><div className="watch-signal" role="status">{busy ? 'The Eye is searching…' : error ? 'Watch interrupted · previous results may be stale' : !data ? 'The watch has not checked in' : `${active.length} open ${active.length === 1 ? 'notice' : 'notices'}${urgent ? ` · ${urgent} high priority` : ''}${missing.length ? ' · coverage incomplete' : active.length ? ' · review applicability' : ' · no matching notices in checked feeds'}`}</div></div><button className="watch-refresh" onClick={() => void refresh()} disabled={busy}>{busy ? 'Checking…' : 'Refresh watch'}</button></header>
    <div className="watch-body"><div className="watch-context"><span>Live official feeds · 14-day lookback + ongoing incidents</span><span>{data ? `Checked ${central(data.checkedAt)} CT` : 'Awaiting first check'} · refreshes every 5 min while open</span></div>
      <p className="watch-scope">Potential relevance to our stack; impact on our systems is unconfirmed. Security watchlist: PHP, WordPress, Apache HTTP, nginx, MySQL, PostgreSQL, OpenSSL, and the services below.</p>
      {error && <p className="watch-error" role="alert">{error} {signIn && <a href={signIn}>Sign in to resume the watch ↗</a>}</p>}
      {!!missing.length && <p className="watch-error">Could not check: {missing.map(s => s.name).join(', ')}. Their status is unknown; open the source links below.</p>}
      <div className="watch-filters" aria-label="Filter web notices">{['All', 'Service interruptions', 'Security'].map(f => <button key={f} aria-pressed={filter === f} onClick={() => { setFilter(f); setShowAll(false); }}>{f}</button>)}</div>
      {filtered.length ? <><div className="watch-items">{filtered.slice(0, showAll ? undefined : 3).map(i => <Item key={i.id} item={i}/>)}</div>{filtered.length > 3 && <button className="watch-more" onClick={() => setShowAll(!showAll)}>{showAll ? 'Show fewer notices' : `View all ${filtered.length} notices`}</button>}</> : <p className="watch-empty">{busy && !data ? 'Reading the official dispatches…' : !data ? 'No verified results yet. Refresh to try again.' : `No matching open notices${missing.length || error ? ' in the available results. Coverage is incomplete.' : ' in the checked feeds. This is not a health check of our systems.'}`}</p>}
      {!!history.length && <details className="watch-history"><summary>Recently resolved · {history.length} provider notices</summary><div className="watch-items">{history.map(i => <Item key={i.id} item={i}/>)}</div></details>}
      <details className="watch-sources"><summary>Sources & coverage</summary><p>A focused watch of these feeds, not a search of the entire web. PHP security releases and matching newly added CISA exploited vulnerabilities cover the past 14 days. Provider history is limited to the incidents returned by each feed. No installed-version inventory is available. This watch runs while the dashboard is open.</p><div>{data?.sources.map(s => <a key={s.name} href={s.url} target="_blank" rel="noreferrer" title={s.why}>{s.name} · {s.available ? 'checked' : 'unavailable'} ↗</a>)}</div></details>
    </div>
  </section>;
}
