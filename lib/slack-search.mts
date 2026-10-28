import type { ConnectorContext } from './connector-contract.mjs';
export const SLACK_CONNECTOR = 'asdk_app_69a1d78e929881919bba0dbda1f6436d';
const PRIVATE_SEARCH = 'slack_search_public_and_private';
/** Resolve only the private message-search operation advertised by this connector. */
export function slackSearchAction(context: ConnectorContext) {
  if (context.status !== 'success') return PRIVATE_SEARCH;
  const connector = context.connectors.find(c => c.connectorId === SLACK_CONNECTOR);
  if (!connector || connector.policy !== 'enabled' || connector.tools === null) return PRIVATE_SEARCH;
  const compatible = connector.tools.filter(t => {
    const properties = t.inputSchema.properties;
    return properties && typeof properties === 'object' && !Array.isArray(properties) && 'keywords' in properties && 'filters' in properties && 'limit' in properties;
  });
  const exact = compatible.find(t => t.actionName === PRIVATE_SEARCH);
  if (exact) return exact.actionName;
  const named = compatible.filter(t => t.actionName.split(/__|[.:/]/).at(-1) === PRIVATE_SEARCH);
  if (named.length === 1) return named[0].actionName;
  // Sites may expose a qualified action name; never fabricate an invocation name.
  const described = compatible.filter(t => /search/i.test(t.description || '') && /private channels/i.test(t.description || '') && /DMs|direct messages/i.test(t.description || ''));
  return described.length === 1 ? described[0].actionName : PRIVATE_SEARCH;
}
export function slackSearchDiagnostics(context: ConnectorContext) {
  if (context.status !== 'success') return { contextStatus: context.status, actions: [] };
  const connector = context.connectors.find(c => c.connectorId === SLACK_CONNECTOR);
  return { contextStatus: context.status, policy: connector?.policy ?? 'unknown', actions: connector?.tools?.map(t => t.actionName) ?? null };
}
