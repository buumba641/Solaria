import express from "express";
import fetch from "node-fetch";

const router = express.Router();

// GET /api/nfts/top
router.get("/nfts/top", async (_req, res) => {
  try {
    const r = await fetch("https://api-mainnet.magiceden.dev/v2/collections/popular?limit=5");
    const j = await r.json();
    res.json({ collections: j });
  } catch (e) {
    res.json({
      collections: [],
      note: "Magic Eden endpoint failed; return mock data in demo if needed.",
      error: String(e)
    });
  }
});

export default router;
