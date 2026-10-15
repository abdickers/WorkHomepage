import { AsyncLocalStorage } from "node:async_hooks";

type RuntimeConfig = { ANTHROPIC_API_KEY?: string; ANTHROPIC_MODEL?: string };
const runtime = new AsyncLocalStorage<RuntimeConfig>();
export function withRuntimeConfig<T>(config: RuntimeConfig, run: () => T): T {
  return runtime.run(config, run);
}
/** Server-only, request-scoped runtime secrets; never pass this object to a client. */
export function getRuntimeConfig(): RuntimeConfig {
  return runtime.getStore() ?? {};
}
