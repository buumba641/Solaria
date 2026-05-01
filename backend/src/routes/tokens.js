import express from "express";
import fetch from "node-fetch";
import { requireApiKey } from "../middleware/auth.js";
import { validateQuery, z } from "../middleware/validate.js";

const router = express.Router();

const querySchema = z.object({
  symbol: z.string().optional()
});

// Simple in-memory cache to avoid CoinGecko rate limits
let priceCache = { data: null, ts: 0 };
const CACHE_TTL_MS = 30_000; // 30 seconds

// GET /api/token/performance?symbol=SOL
router.get(
  "/token/performance",
  requireApiKey,
  validateQuery(querySchema),
  async (req, res, next) => {
  try {
    const symbol = String(req.query.symbol || "SOL").toUpperCase();

    if (symbol !== "SOL") {
      return res.status(400).json({ error: "Demo supports SOL only (extend as needed)" });
    }

    // Return cached data if fresh
    if (priceCache.data && Date.now() - priceCache.ts < CACHE_TTL_MS) {
      return res.json(priceCache.data);
    }

    const url = "https://api.coingecko.com/api/v3/simple/price?ids=solana&vs_currencies=usd&include_24hr_change=true";
    const r = await fetch(url);

    if (!r.ok) {
      // If rate-limited and we have stale cache, return it
      if (priceCache.data) return res.json({ ...priceCache.data, cached: true });
      return res.status(502).json({ error: "CoinGecko request failed", status: r.status });
    }

    const j = await r.json();
    const price = j?.solana?.usd ?? null;
    const change24h = j?.solana?.usd_24h_change ?? null;

    const payload = {
      symbol,
      priceUsd: price,
      change24hPercent: change24h ? Number(change24h.toFixed(2)) : null,
      note: "Extend to store last_login price in app prefs or backend DB for custom % change."
    };

    priceCache = { data: payload, ts: Date.now() };
    res.json(payload);
  } catch (e) {
    // On network error, return stale cache if available
    if (priceCache.data) return res.json({ ...priceCache.data, cached: true });
    next(e);
  }
});

export default router;
