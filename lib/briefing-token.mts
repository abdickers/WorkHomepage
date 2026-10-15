const encoder = new TextEncoder();
async function signingKey(secret: string) {
  return crypto.subtle.importKey(
    "raw",
    encoder.encode(`dashboard-briefing-v1:${secret}`),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"],
  );
}
export async function signSnapshot(payload: object, secret: string) {
  const body = JSON.stringify(payload);
  const bytes = new Uint8Array(
    await crypto.subtle.sign(
      "HMAC",
      await signingKey(secret),
      encoder.encode(body),
    ),
  );
  return {
    body,
    signature: Array.from(bytes, (n) => n.toString(16).padStart(2, "0")).join(
      "",
    ),
  };
}
export async function verifySnapshot(
  body: string,
  signature: string,
  secret: string,
  userId: string,
  now = Date.now(),
) {
  if (body.length > 24000 || !/^[a-f0-9]{64}$/.test(signature))
    throw new Error("Invalid snapshot");
  const bytes = Uint8Array.from(signature.match(/../g)!, (s) =>
    parseInt(s, 16),
  );
  if (
    !(await crypto.subtle.verify(
      "HMAC",
      await signingKey(secret),
      bytes,
      encoder.encode(body),
    ))
  )
    throw new Error("Invalid snapshot");
  const data = JSON.parse(body);
  if (
    data.userId !== userId ||
    !Number.isFinite(data.expiresAt) ||
    data.expiresAt < now ||
    data.expiresAt > now + 300000 ||
    !Array.isArray(data.evidence)
  )
    throw new Error("Snapshot expired or belongs to another user");
  return data;
}
