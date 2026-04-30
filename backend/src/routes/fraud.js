import express from "express";
import fetch from "node-fetch";

const router = express.Router();

// POST /api/check-fraud { tokenAddress }
router.post("/check-fraud", async (req, res) => {
  const { tokenAddress } = req.body ?? {};
  if (!tokenAddress) return res.status(400).json({ error: "tokenAddress is required" });

  let rugcheck = null;
  try {
    const r = await fetch(`https://api.rugcheck.xyz/v1/tokens/${tokenAddress}/report`);
    rugcheck = await r.json();
  } catch {
    rugcheck = { error: "rugcheck unavailable" };
  }

  const risk = rugcheck?.score ?? null;

  res.json({
    tokenAddress,
    rugcheck,
    riskLevel: risk == null ? "UNKNOWN" : risk > 500 ? "HIGH" : risk > 200 ? "MEDIUM" : "LOW",
    advice:
      risk == null
        ? "Could not fetch full report. Be cautious."
        : risk > 500
          ? "High risk flags. Avoid."
          : "No major flags detected in this basic check."
  });
});

export default router;
