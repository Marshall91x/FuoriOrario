// "Import del calendario" (ADR 0011): the app posts the FIP PDF, gets back the preview. Writes nothing: the staff
// confirms the preview with the RPC import_league. The JWT is checked by the platform (verify_jwt).
import { parseCalendar } from "./parse.ts";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const preview = await parseCalendar(new Uint8Array(await req.arrayBuffer()));
    if (preview.fixtures.length === 0) throw new Error("no fixtures");
    return Response.json(preview, { headers: cors });
  } catch (_) {
    return Response.json({ error: "unreadable_calendar" }, { status: 422, headers: cors });
  }
});
