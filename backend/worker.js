/**
 * Cloudflare Worker / Serverless Proxy for Gemini API
 * 
 * Instructions:
 * 1. Deploy this script to Cloudflare Workers (https://workers.cloudflare.com) - 100% Free Tier (100k requests/day).
 * 2. In Cloudflare Worker Settings -> Variables -> Environment Variables, add:
 *    GEMINI_API_KEY = <your-new-gemini-api-key>
 * 3. In your KMP mobile app, replace direct generativelanguage.googleapis.com calls with:
 *    https://<your-worker-subdomain>.workers.dev/api/summary?book=Genesis&chapter=1
 */

export default {
  async fetch(request, env) {
    // Handle CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type",
        },
      });
    }

    const url = new URL(request.url);

    if (url.pathname === "/api/summary") {
      if (request.method !== "POST") {
        return new Response(JSON.stringify({ error: "Method not allowed" }), { status: 405 });
      }

      try {
        const body = await request.json();
        const geminiApiKey = env.GEMINI_API_KEY;

        if (!geminiApiKey) {
          return new Response(JSON.stringify({ error: "Server API key missing" }), { status: 500 });
        }

        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${geminiApiKey}`;

        const response = await fetch(geminiUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });

        const data = await response.json();

        return new Response(JSON.stringify(data), {
          status: response.status,
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*",
          },
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500 });
      }
    }

    return new Response(JSON.stringify({ status: "BibleBible Proxy Operational" }), { status: 200 });
  },
};
