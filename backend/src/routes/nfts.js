import express from "express";
import fetch from "node-fetch";

const router = express.Router();

// Fallback mock data for when Magic Eden is unavailable during demo
const MOCK_COLLECTIONS = [
  { name: "Mad Lads", symbol: "mad_lads", floorPrice: 120, volumeAll: 980000 },
  { name: "Tensorians", symbol: "tensorians", floorPrice: 45, volumeAll: 320000 },
  { name: "Claynosaurz", symbol: "claynosaurz", floorPrice: 32, volumeAll: 290000 },
  { name: "Famous Fox Federation", symbol: "famous_fox_federation", floorPrice: 18, volumeAll: 150000 },
  { name: "Okay Bears", symbol: "okay_bears", floorPrice: 12, volumeAll: 140000 }
];

// GET /api/nfts/top
router.get("/nfts/top", async (_req, res, next) => {
  try {
    const r = await fetch("https://api-mainnet.magiceden.dev/v2/collections/popular?limit=5", {
      headers: { "Accept": "application/json" },
      signal: AbortSignal.timeout(5000) // 5s timeout
    });

    if (!r.ok) {
      console.warn(`Magic Eden returned ${r.status}, using mock data`);
      return res.json({ collections: MOCK_COLLECTIONS, source: "mock" });
    }

    const j = await r.json();
    res.json({ collections: j, source: "magiceden" });
  } catch (e) {
    console.warn("Magic Eden endpoint failed, using mock data:", String(e));
    res.json({ collections: MOCK_COLLECTIONS, source: "mock" });
  }
});

export default router;
