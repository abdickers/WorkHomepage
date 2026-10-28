export type WatchItem = { id: string; source: string; title: string; url: string; date: string; level: 'urgent' | 'watch' | 'resolved'; status: string; summary: string; action: string };
export const STATUS_SOURCES = [
  { name: 'Cloudflare', url: 'https://www.cloudflarestatus.com', why: 'DNS, CDN, Workers, and hosting', action: 'Check the affected products and regions against our sites and Worker errors.' },
  { name: 'Vercel', url: 'https://www.vercel-status.com', why: 'Web deployments and hosting', action: 'Check our deployments and traffic in the affected region before retrying releases.' },
  { name: 'BigCommerce', url: 'https://status.bigcommerce.com', why: 'Storefront, checkout, and commerce APIs', action: 'Check storefront and checkout, then inspect Celigo sync errors before retrying jobs.' },
  { name: 'NetSuite', url: 'https://status.netsuite.com', why: 'ERP access and integrations', action: 'Match the affected data center and services to our account; inspect integration failures.' },
  { name: 'Celigo', url: 'https://status.celigo.com', why: 'BigCommerce–NetSuite integration', action: 'Inspect production flow jobs and errors; confirm provider recovery before replaying work.' },
  { name: 'GitHub', url: 'https://www.githubstatus.com', why: 'Source control and build pipelines', action: 'Check whether repositories, Actions, or webhooks used by our deployments are affected.' },
] as const;
export const WATCH_SOURCES = [...STATUS_SOURCES, { name: 'PHP', url: 'https://www.php.net/releases/feed.php', why: 'Security releases' }, { name: 'CISA', url: 'https://www.cisa.gov/known-exploited-vulnerabilities-catalog', why: 'Exploited vulnerabilities matching the web and database watchlist' }];
const WINDOW = 14 * 86400000;
function recent(date: string, now: number) { const d = Date.parse(date); return Number.isFinite(d) && d >= now - WINDOW && d <= now; }
export function plain(value: unknown) {
  return String(value ?? '').replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, '$1').replace(/<[^>]*>/g, ' ').replace(/&#(\d+);/g, (_, n) => Number(n) <= 0x10ffff ? String.fromCodePoint(Number(n)) : '').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;|&apos;/g, "'").replace(/\s+/g, ' ').trim().slice(0, 900);
}
export function officialUrl(value: unknown, base: string) {
  try { const u = new URL(String(value), base); const b = new URL(base); if (u.hostname.replace(/^www\./, '') !== b.hostname.replace(/^www\./, '') || !['https:', 'http:'].includes(u.protocol)) return base; u.protocol = 'https:'; return u.href; } catch { return base; }
}
export function statusItems(data: any, source: typeof STATUS_SOURCES[number], now = Date.now()): WatchItem[] {
  if (!Array.isArray(data?.incidents)) throw new Error('Unexpected incident feed');
  return data.incidents.filter((i: any) => i?.name && !/^_/.test(i.name) && i.status !== 'completed' && !['scheduled', 'in_progress', 'verifying'].includes(i.status)).flatMap((i: any) => {
    const resolved = i.status === 'resolved';
    const date = i.updated_at || i.created_at;
    if (resolved && !recent(date, now)) return [];
    if (!date || !Number.isFinite(Date.parse(date))) return [];
    return [{ id: `${source.name}:${i.id}`, source: source.name, title: plain(i.name), url: officialUrl(i.shortlink || `/incidents/${encodeURIComponent(i.id)}`, source.url), date, level: resolved ? 'resolved' : ['critical', 'major'].includes(i.impact) ? 'urgent' : 'watch', status: resolved ? 'Resolved' : plain(i.status).replace(/_/g, ' '), summary: plain(i.incident_updates?.[0]?.body || 'Provider reports an incident. Read the official update for scope.'), action: resolved ? 'If we saw errors during this window, check for failed jobs or missed work after recovery.' : source.action } satisfies WatchItem];
  });
}
export function phpItems(xml: string, now = Date.now()): WatchItem[] {
  if (!xml.includes('<feed')) throw new Error('Unexpected PHP feed');
  return [...xml.matchAll(/<entry\b[^>]*>([\s\S]*?)<\/entry>/g)].flatMap(([, entry]) => {
    const tag = (name: string) => plain(entry.match(new RegExp(`<${name}\\b[^>]*>([\\s\\S]*?)<\\/${name}>`))?.[1]);
    const date = tag('published') || tag('updated');
    if (!recent(date, now) || !/security/i.test(tag('php:tag') + ' ' + tag('title') + ' ' + tag('summary'))) return [];
    return [{ id: `PHP:${tag('id')}`, source: 'PHP', title: tag('title'), url: officialUrl(tag('id'), 'https://www.php.net'), date, level: 'watch', status: 'Security release', summary: 'PHP has published a security release. Applicability depends on the PHP branch and packages installed on our servers.', action: 'Inventory PHP versions, read the release notes, and test the matching patched package before deployment.' } satisfies WatchItem];
  });
}
// A technology watchlist, not an assertion that these products are installed.
export const TECHNOLOGY_WATCHLIST = /\b(php|wordpress|apache http|nginx|mysql|postgresql|openssl|netsuite|bigcommerce|celigo|cloudflare|vercel|github)\b/i;
export function cisaItems(data: any, now = Date.now()): WatchItem[] {
  if (!Array.isArray(data?.vulnerabilities)) throw new Error('Unexpected CISA feed');
  return data.vulnerabilities.filter((v: any) => recent(v.dateAdded, now) && TECHNOLOGY_WATCHLIST.test(`${v.vendorProject} ${v.product} ${v.vulnerabilityName}`)).map((v: any) => ({ id: `CISA:${v.cveID}`, source: 'CISA', title: `${plain(v.cveID)} · ${plain(v.vulnerabilityName)}`, url: `https://www.cisa.gov/known-exploited-vulnerabilities-catalog?search_api_fulltext=${encodeURIComponent(v.cveID)}`, date: v.dateAdded, level: 'urgent', status: 'Known exploited', summary: plain(v.shortDescription), action: `Confirm whether affected products and versions are installed. CISA guidance: ${plain(v.requiredAction)}` } satisfies WatchItem));
}
export function sortItems(items: WatchItem[]) { const rank = { urgent: 0, watch: 1, resolved: 2 }; return items.sort((a, b) => rank[a.level] - rank[b.level] || Date.parse(b.date) - Date.parse(a.date)); }
export async function readFeed(url: string, asJson = true) {
  const response = await fetch(url, { signal: AbortSignal.timeout(12000), headers: { Accept: asJson ? 'application/json' : 'application/atom+xml, application/xml' }, redirect: 'manual', cache: 'no-store' });
  if (!response.ok) { await response.body?.cancel(); throw new Error(`HTTP ${response.status}`); }
  // Bound provider payloads before decoding them in the Worker.
  const reader = response.body?.getReader(); if (!reader) throw new Error('Empty feed');
  let size = 0; const chunks: Uint8Array[] = [];
  while (true) { const { done, value } = await reader.read(); if (done) break; size += value.byteLength; if (size > 5_000_000) { await reader.cancel(); throw new Error('Feed exceeds read limit'); } chunks.push(value); }
  const bytes = new Uint8Array(size); let offset = 0; for (const c of chunks) { bytes.set(c, offset); offset += c.length; }
  const text = new TextDecoder().decode(bytes); return asJson ? JSON.parse(text) : text;
}
export async function collectWebWatch() {
  const now = Date.now();
  const jobs = [
    ...STATUS_SOURCES.map(source => ({ name: source.name, run: async () => statusItems(await readFeed(`${source.url}/api/v2/incidents.json`), source, now) })),
    { name: 'PHP', run: async () => phpItems(await readFeed('https://www.php.net/releases/feed.php', false), now) },
    { name: 'CISA', run: async () => cisaItems(await readFeed('https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json'), now) },
  ];
  const results = await Promise.allSettled(jobs.map(j => j.run()));
  const items = sortItems(results.flatMap(r => r.status === 'fulfilled' ? r.value : []));
  return { checkedAt: new Date(now).toISOString(), items, sources: WATCH_SOURCES.map(source => ({ ...source, available: results[jobs.findIndex(j => j.name === source.name)].status === 'fulfilled' })) };
}
