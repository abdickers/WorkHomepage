import test from 'node:test';
import assert from 'node:assert/strict';
import { cisaItems, phpItems, statusItems, STATUS_SOURCES, officialUrl, sortItems, collectWebWatch } from '../lib/web-watch.mts';
const now = Date.parse('2026-10-28T18:00:00Z');
test('provider incidents retain ongoing older incidents but exclude stale resolved incidents and metadata', () => {
  const incident = { id: 'abc', name: 'API unavailable', status: 'investigating', impact: 'major', updated_at: '2026-09-01T00:00:00Z' };
  const items = statusItems({ incidents: [incident, { ...incident, id: 'old', status: 'resolved' }, { ...incident, id: 'meta', name: '_system_metadata:blob' }, { ...incident, id: 'new', status: 'resolved', updated_at: '2026-10-27T00:00:00Z' }] }, STATUS_SOURCES[0], now);
  assert.deepEqual(items.map(i => i.level), ['urgent', 'resolved']);
  assert.equal(items[0].url, 'https://www.cloudflarestatus.com/incidents/abc');
});
test('PHP includes only recent security releases, with exact official release links', () => {
  const entry = (date: string, tag: string) => `<entry><title>PHP patch</title><id>http://php.net/releases/8_4_27.php</id><published>${date}</published><php:tag>${tag}</php:tag></entry>`;
  const items = phpItems(`<feed>${entry('2026-10-21', 'security')}${entry('2026-09-01', 'security')}${entry('2026-10-22', 'bugfix')}${entry('2027-01-01', 'security')}</feed>`, now);
  assert.equal(items.length, 1); assert.equal(items[0].url, 'https://php.net/releases/8_4_27.php');
});
test('CISA matches relevant newly catalogued exploits without asserting installed exposure', () => {
  const v = { cveID: 'CVE-TEST', vendorProject: 'PHP', product: 'PHP', dateAdded: '2026-10-26', vulnerabilityName: 'Memory corruption', requiredAction: 'Apply vendor mitigations' };
  const items = cisaItems({ vulnerabilities: [v, { ...v, product: 'Unrelated', vendorProject: 'Example' }, { ...v, dateAdded: '2026-09-01' }] }, now);
  assert.equal(items.length, 1); assert.equal(items[0].level, 'urgent'); assert.match(items[0].action, /Confirm whether/);
});
test('provider source links reject offsite and executable URLs', () => {
  assert.equal(officialUrl('javascript:alert(1)', 'https://www.php.net'), 'https://www.php.net');
  assert.equal(officialUrl('https://evil.test/incidents/abc', 'https://www.php.net'), 'https://www.php.net');
});
test('unknown feeds cannot be treated as a healthy empty result', () => {
  assert.throws(() => statusItems({}, STATUS_SOURCES[0])); assert.throws(() => phpItems('gateway error')); assert.throws(() => cisaItems({}));
});
test('prioritizes urgent notices before newer resolved notices', () => {
  const items = statusItems({ incidents: [{ id:'a', name:'Major outage', status:'investigating', impact:'critical', updated_at:'2026-10-26' }, { id:'b', name:'Recovered', status:'resolved', updated_at:'2026-10-27' }] }, STATUS_SOURCES[0], now);
  assert.equal(sortItems(items.reverse())[0].id, 'Cloudflare:a');
});
test('an individual feed failure preserves the other source results and exposes incomplete coverage', async () => {
  const realFetch = globalThis.fetch;
  globalThis.fetch = async (input) => {
    const url = String(input);
    if (url.includes('cloudflarestatus')) throw new Error('Unavailable');
    if (url.includes('php.net')) return new Response('<feed></feed>');
    if (url.includes('cisa.gov')) return Response.json({ vulnerabilities: [] });
    return Response.json({ incidents: [] });
  };
  try { const data = await collectWebWatch(); assert.equal(data.sources.filter(s => s.available).length, 7); assert.equal(data.sources[0].available, false); assert.deepEqual(data.items, []); }
  finally { globalThis.fetch = realFetch; }
});
