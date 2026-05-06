import express from "express";
import fetch from "node-fetch";
import { requireApiKey } from "../middleware/auth.js";
import { validateQuery, z } from "../middleware/validate.js";

const router = express.Router();

const querySchema = z.object({
  symbol: z.string().optional(),
  days: z.coerce.number().int().min(1).max(30).optional()
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
    const days = req.query.days ?? 7;

    if (symbol !== "SOL") {
      return res.status(400).json({ error: "Demo supports SOL only (extend as needed)" });
    }

    // Return cached data if fresh
    if (priceCache.data && Date.now() - priceCache.ts < CACHE_TTL_MS) {
      return res.json(priceCache.data);
    }

    const priceUrl = "https://api.coingecko.com/api/v3/simple/price?ids=solana&vs_currencies=usd&include_24hr_change=true";
    const r = await fetch(priceUrl);

    if (!r.ok) {
      // If rate-limited and we have stale cache, return it
      if (priceCache.data) return res.json({ ...priceCache.data, cached: true });
      return res.status(502).json({ error: "CoinGecko request failed", status: r.status });
    }

    const j = await r.json();
    const price = j?.solana?.usd ?? null;
    const change24h = j?.solana?.usd_24h_change ?? null;

    let history = null;
    if (days >= 2) {
      try {
        const chartUrl = `https://api.coingecko.com/api/v3/coins/solana/market_chart?vs_currency=usd&days=${days}`;
        const chartResp = await fetch(chartUrl);
        if (chartResp.ok) {
          const chartJson = await chartResp.json();
          const prices = Array.isArray(chartJson?.prices) ? chartJson.prices : [];
          history = prices.map((point) => ({
            timestamp: point[0],
            price: point[1]
          }));
        }
      } catch {
        history = null;
      }
    }

    const payload = {
      symbol,
      mint: null,
      price,
      change24h: change24h ? Number(change24h.toFixed(2)) : null,
      volume24h: null,
      marketCap: null,
      history
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
