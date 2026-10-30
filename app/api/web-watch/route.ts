import { dailyOwner, privateHeaders } from '@/lib/daily-server';
import { collectWebWatch } from '@/lib/web-watch.mjs';
export async function POST(request: Request) {
  const auth = await dailyOwner(request);
  if (auth.error) return auth.error;
  return Response.json(await collectWebWatch(), { headers: privateHeaders });
}
