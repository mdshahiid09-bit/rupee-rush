// supabase/functions/verify-task-install/index.ts
// Secure server-side validation that an app was installed strictly after started_at

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
      },
    });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Missing authorization" }), { status: 401 });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: { user }, error: userError } = await supabase.auth.getUser();
    if (userError || !user) {
      return new Response(JSON.stringify({ error: "Invalid token" }), { status: 401 });
    }

    const { claim_id, package_name, installed_timestamp_ms } = await req.json();

    // Fetch claim
    const { data: claim, error: claimError } = await supabase
      .from("claims")
      .select("id, user_id, status, started_at, offers(package_name)")
      .eq("id", claim_id)
      .eq("user_id", user.id)
      .single();

    if (claimError || !claim) {
      return new Response(JSON.stringify({ success: false, message: "Claim not found" }), { status: 404 });
    }

    const startedAtMs = new Date(claim.started_at).getTime();
    if (installed_timestamp_ms <= startedAtMs) {
      // Installed before startedAt - suspicious / pre-installed
      return new Response(JSON.stringify({
        success: false,
        flagged: true,
        message: "Pre-installed apps do not count. Install must happen after task start."
      }), { status: 400 });
    }

    // Update claim to pending status
    const { error: updateError } = await supabase
      .from("claims")
      .update({
        status: "pending",
        installed_at: new Date(installed_timestamp_ms).toISOString(),
      })
      .eq("id", claim_id);

    if (updateError) {
      return new Response(JSON.stringify({ success: false, message: updateError.message }), { status: 500 });
    }

    return new Response(JSON.stringify({
      success: true,
      status: "pending",
      message: "Installation verified. Task is now pending admin approval."
    }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      status: 200
    });
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : "Internal error";
    return new Response(JSON.stringify({ error: errorMsg }), { status: 500 });
  }
});
