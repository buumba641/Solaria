import express from "express";
import fetch from "node-fetch";
import { config } from "../config.js";
import { requireApiKey } from "../middleware/auth.js";
import { validateBody, validateQuery, z } from "../middleware/validate.js";

const router = express.Router();

const quoteSchema = z.object({
  fromChain: z.string().min(1),
  toChain: z.string().min(1),
  fromToken: z.string().min(1),
  toToken: z.string().min(1),
  amount: z.union([z.string(), z.number()]),
  fromAddress: z.string().optional(),
  toAddress: z.string().optional(),
  slippage: z.union([z.string(), z.number()]).optional()
});

const statusSchema = z.object({
  txHash: z.string().min(1)
});

/** Safely parse JSON, returning null on failure */
function safeJsonParse(text) {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

/**
 * POST /api/top-up/quote
 * Body: { fromChain, toChain, fromToken, toToken, amount, fromAddress?, toAddress?, slippage? }
 */
router.post(
  "/top-up/quote",
  requireApiKey,
  validateBody(quoteSchema),
  async (req, res, next) => {
  try {
    const { fromChain, toChain, fromToken, toToken, amount, fromAddress, toAddress, slippage } =
      req.body;

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

    if (!r.ok) {
      return res.status(502).json({ error: "LI.FI quote failed", status: r.status, details: text.slice(0, 500) });
    }

    const parsed = safeJsonParse(text);
    if (!parsed) {
      return res.status(502).json({ error: "LI.FI returned invalid JSON", raw: text.slice(0, 500) });
    }

    return res.json(parsed);
  } catch (e) {
    next(e);
  }
});

/**
 * GET /api/top-up/status?txHash=<hash>
 */
router.get(
  "/top-up/status",
  requireApiKey,
  validateQuery(statusSchema),
  async (req, res, next) => {
  try {
    const { txHash } = req.query;

    const url = `${config.lifi.baseUrl}/status?txHash=${encodeURIComponent(txHash)}`;

    const r = await fetch(url);
    const text = await r.text();

    if (!r.ok) {
      return res.status(502).json({ error: "LI.FI status failed", status: r.status, details: text.slice(0, 500) });
    }

    const parsed = safeJsonParse(text);
    if (!parsed) {
      return res.status(502).json({ error: "LI.FI returned invalid JSON", raw: text.slice(0, 500) });
    }

    return res.json(parsed);
  } catch (e) {
    next(e);
  }
});

export default router;
