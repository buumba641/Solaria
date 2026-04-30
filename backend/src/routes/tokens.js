import express from "express";
import fetch from "node-fetch";

const router = express.Router();

// GET /api/token/performance?symbol=SOL
router.get("/token/performance", async (req, res) => {
  const symbol = String(req.query.symbol || "SOL").toUpperCase();

  if (symbol !== "SOL") return res.status(400).json({ error: "Demo supports SOL only (extend as needed)" });

  const url =
    "https://api.coingecko.com/api/v3/simple/price?ids=solana&vs_currencies=usd";

  const r = await fetch(url);
  const j = await r.json();
  const price = j?.solana?.usd;

  res.json({
    symbol,
    priceUsd: price,
    note: "Extend to store last_login price in app prefs or backend DB for % change."
  });
});

export default router;
