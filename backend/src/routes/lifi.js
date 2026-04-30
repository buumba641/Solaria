import express from "express";
import fetch from "node-fetch";
import { config } from "../config.js";

const router = express.Router();

/**
 * POST /api/top-up/quote
 * Body: { fromChain, toChain, fromToken, toToken, amount, fromAddress?, toAddress?, slippage? }
 */
router.post("/top-up/quote", async (req, res) => {
  const {
    fromChain,
    toChain,
    fromToken,
    toToken,
    amount,
    fromAddress,
    toAddress,
    slippage
  } = req.body ?? {};

  if (!fromChain || !toChain || !fromToken || !toToken || !amount) {
    return res.status(400).json({
      error: "Expected { fromChain, toChain, fromToken, toToken, amount }"
    });
  }

  const qs = new URLSearchParams({
    fromChain: String(fromChain),
    toChain: String(toChain),
    fromToken: String(fromToken),
    toToken: String(toToken),
    amount: String(amount),
    testnet: "true"
  });

  if (fromAddress) qs.set("fromAddress", String(fromAddress));
  if (toAddress) qs.set("toAddress", String(toAddress));
  if (slippage) qs.set("slippage", String(slippage));

  const url = `${config.lifi.baseUrl}/quote?${qs.toString()}`;

  const r = await fetch(url);
  const text = await r.text();

  if (!r.ok) return res.status(502).json({ error: "LI.FI quote failed", status: r.status, details: text });

  return res.json(JSON.parse(text));
});

/**
 * GET /api/top-up/status?txHash=<hash>
 */
router.get("/top-up/status", async (req, res) => {
  const txHash = String(req.query.txHash || "");
  if (!txHash) return res.status(400).json({ error: "txHash is required" });

  const url = `${config.lifi.baseUrl}/status?txHash=${encodeURIComponent(txHash)}`;

  const r = await fetch(url);
  const text = await r.text();
  if (!r.ok) return res.status(502).json({ error: "LI.FI status failed", status: r.status, details: text });

  return res.json(JSON.parse(text));
});

export default router;
