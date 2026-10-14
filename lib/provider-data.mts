/** Read MCP JSON blocks without treating prose or an error as empty success. */
export function providerData(value: unknown): any {
  if (value == null) throw new Error("No provider result");
  if (typeof value === "string") return providerData(JSON.parse(value));
  if (Array.isArray(value)) return value;
  if (typeof value !== "object")
    throw new Error("Unrecognized provider response");
  const obj = value as Record<string, any>;
  if (obj.isError === true) throw new Error("Provider returned an error");
  if (obj.structuredContent !== undefined)
    return providerData(obj.structuredContent);
  if (Array.isArray(obj.content)) {
    for (const block of obj.content) {
      if (block.type === "text" && typeof block.text === "string") {
        try {
          return providerData(JSON.parse(block.text));
        } catch {}
      }
    }
    throw new Error("Provider returned no structured data");
  }
  if (obj.result && typeof obj.result === "object")
    return providerData(obj.result);
  return obj;
}
export function records(value: unknown, key = "items"): any[] {
  const obj = providerData(value);
  if (Array.isArray(obj)) return obj;
  if (!Array.isArray(obj[key]))
    throw new Error(`Provider response is missing ${key}`);
  return obj[key];
}
export function maskLog(text: unknown): string {
  return String(text ?? "")
    .slice(0, 1600)
    .replace(
      /([?&](?:code|state|token|key|secret|access_token)=)[^\s&]+/gi,
      "$1[redacted]",
    )
    .replace(/Bearer\s+[\w.\-]+/gi, "Bearer [redacted]");
}
