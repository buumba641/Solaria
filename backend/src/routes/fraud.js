import express from "express";
import fetch from "node-fetch";
import { requireApiKey } from "../middleware/auth.js";
import { validateBody, z } from "../middleware/validate.js";

const router = express.Router();

const bodySchema = z.object({
  tokenAddress: z.string().min(1)
});

// POST /api/check-fraud { tokenAddress }
router.post("/check-fraud", requireApiKey, validateBody(bodySchema), async (req, res, next) => {
  try {
    const { tokenAddress } = req.body;

    // Basic format check: Solana addresses are 32-44 base58 chars
    if (!/^[1-9A-HJ-NP-Za-km-z]{32,44}$/.test(tokenAddress)) {
      return res.status(400).json({ error: "Invalid token address format" });
    }

    // RugCheck report (best-effort)
    let rugcheck = null;
    try {
      const r = await fetch(
        `https://api.rugcheck.xyz/v1/tokens/${encodeURIComponent(tokenAddress)}/report`
      );
      if (r.ok) {
        rugcheck = await r.json();
      } else {
        rugcheck = { error: "rugcheck returned non-OK", status: r.status };
      }
    } catch (e) {
      rugcheck = { error: "rugcheck unavailable", details: String(e) };
    }

    const risk = rugcheck?.score ?? null;

    res.json({
      tokenAddress,
      rugcheck,
      riskLevel:
        risk == null ? "UNKNOWN" : risk > 500 ? "HIGH" : risk > 200 ? "MEDIUM" : "LOW",
      advice:
        risk == null
          ? "Could not fetch full report. Be cautious."
          : risk > 500
            ? "High risk flags. Avoid."
            : "No major flags detected in this basic check."
    });
  } catch (e) {
    next(e);
  }
});

export default router;
