// supabase/functions/app-update-check/index.ts
// Handles private GitHub Releases check securely without exposing GitHub token to APK

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";

interface RequestBody {
  current_version: string;
  platform?: string;
}

serve(async (req) => {
  // CORS headers
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
    const { current_version } = (await req.json()) as RequestBody;

    // GitHub secrets from environment
    const githubToken = Deno.env.get("GITHUB_RELEASE_TOKEN");
    const repoOwner = Deno.env.get("GITHUB_REPO_OWNER") || "rupeerush";
    const repoName = Deno.env.get("GITHUB_REPO_NAME") || "rupee-rush-android";
    const minSupportedVersion = Deno.env.get("MIN_SUPPORTED_VERSION") || "1.0.0";

    const headers: Record<string, string> = {
      "Accept": "application/vnd.github.v3+json",
      "User-Agent": "RupeeRush-Updater/1.0",
    };

    if (githubToken) {
      headers["Authorization"] = `Bearer ${githubToken}`;
    }

    const githubApiUrl = `https://api.github.com/repos/${repoOwner}/${repoName}/releases`;
    const response = await fetch(githubApiUrl, { headers });

    if (!response.ok) {
      // Fallback response if GitHub API is unreachable or repo not yet published
      return new Response(
        JSON.stringify({
          has_update: false,
          latest_version: current_version || "1.0.0",
          is_forced: false,
          notes: "Rupee Rush is up to date.",
          apk_url: null,
          sha256: null,
        }),
        {
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*",
          },
          status: 200,
        }
      );
    }

    const releases = await response.json();
    // Filter only normal (non-draft, non-prerelease) releases
    const normalReleases = Array.isArray(releases)
      ? releases.filter((r) => !r.draft && !r.prerelease)
      : [];

    if (normalReleases.length === 0) {
      return new Response(
        JSON.stringify({
          has_update: false,
          latest_version: current_version,
          is_forced: false,
          notes: "No new release available.",
          apk_url: null,
          sha256: null,
        }),
        { headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" } }
      );
    }

    const latest = normalReleases[0];
    const latestVersion = latest.tag_name?.replace(/^v/, "") || "1.0.0";
    const apkAsset = latest.assets?.find((a: { name: string; browser_download_url: string }) =>
      a.name.endsWith(".apk")
    );
    const shaAsset = latest.assets?.find((a: { name: string; browser_download_url: string }) =>
      a.name.endsWith(".sha256") || a.name.endsWith(".txt")
    );

    let sha256 = "";
    if (shaAsset && githubToken) {
      try {
        const shaRes = await fetch(shaAsset.browser_download_url, { headers });
        if (shaRes.ok) {
          const shaText = await shaRes.text();
          sha256 = shaText.trim().split(" ")[0];
        }
      } catch (_) {}
    }

    const hasUpdate = latestVersion !== current_version;
    const isForced = compareVersions(current_version, minSupportedVersion) < 0;

    return new Response(
      JSON.stringify({
        has_update: hasUpdate,
        latest_version: latestVersion,
        current_version: current_version,
        is_forced: isForced,
        notes: latest.body || "Bug fixes and performance improvements.",
        apk_url: apkAsset ? apkAsset.browser_download_url : null,
        sha256: sha256 || null,
        published_at: latest.published_at,
      }),
      {
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
        status: 200,
      }
    );
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : "Internal Server Error";
    return new Response(JSON.stringify({ error: errorMsg }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      status: 500,
    });
  }
});

function compareVersions(v1: string, v2: string): number {
  if (!v1 || !v2) return 0;
  const p1 = v1.split(".").map(Number);
  const p2 = v2.split(".").map(Number);
  for (let i = 0; i < Math.max(p1.length, p2.length); i++) {
    const num1 = p1[i] || 0;
    const num2 = p2[i] || 0;
    if (num1 > num2) return 1;
    if (num1 < num2) return -1;
  }
  return 0;
}
