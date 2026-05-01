import express from "express";
import {
  PublicKey,
  SystemProgram,
  Transaction,
  LAMPORTS_PER_SOL
} from "@solana/web3.js";
import { getConnection } from "../solana.js";
import { requireApiKey } from "../middleware/auth.js";
import { validateBody, validateQuery, z } from "../middleware/validate.js";

const router = express.Router();

const balanceQuerySchema = z.object({
  wallet: z.string().min(1)
});

const prepareBodySchema = z.object({
  from: z.string().min(1),
  to: z.string().min(1),
  amountSol: z.number().positive()
});

const confirmBodySchema = z.object({
  signedTxBase64: z.string().min(1),
  commitment: z.enum(["processed", "confirmed", "finalized"]).optional()
});

const confirmQuerySchema = z.object({
  wait: z.enum(["true", "false"]).optional()
});

/** Validate a base58 public key string. Returns PublicKey or null. */
function tryPublicKey(value) {
  try {
    return new PublicKey(value);
  } catch {
    return null;
  }
}

// GET /api/balance?wallet=<pubkey>
router.get("/balance", validateQuery(balanceQuerySchema), async (req, res, next) => {
  try {
    const { wallet } = req.query;

    const pubkey = tryPublicKey(wallet);
    if (!pubkey) return res.status(400).json({ error: "Invalid wallet address" });

    const conn = getConnection();
    const lamports = await conn.getBalance(pubkey, "confirmed");
    res.json({ wallet, sol: lamports / LAMPORTS_PER_SOL, lamports });
  } catch (e) {
    next(e);
  }
});

// POST /api/prepare-transfer { from, to, amountSol }
router.post(
  "/prepare-transfer",
  requireApiKey,
  validateBody(prepareBodySchema),
  async (req, res, next) => {
  try {
    const { from, to, amountSol } = req.body;

    const fromPk = tryPublicKey(from);
    if (!fromPk) return res.status(400).json({ error: "Invalid 'from' address" });

    const toPk = tryPublicKey(to);
    if (!toPk) return res.status(400).json({ error: "Invalid 'to' address" });

    if (amountSol <= 0) {
      return res.status(400).json({ error: "amountSol must be positive" });
    }

    const conn = getConnection();

    const ix = SystemProgram.transfer({
      fromPubkey: fromPk,
      toPubkey: toPk,
      lamports: Math.round(amountSol * LAMPORTS_PER_SOL)
    });

    const { blockhash, lastValidBlockHeight } = await conn.getLatestBlockhash("confirmed");

    const tx = new Transaction({
      feePayer: fromPk,
      recentBlockhash: blockhash
    }).add(ix);

    // Simulate to catch errors before the user signs
    const sim = await conn.simulateTransaction(tx, { sigVerify: false });
    if (sim.value.err) {
      return res.status(400).json({ error: "Simulation failed", details: sim.value.err });
    }

    const fee = await conn.getFeeForMessage(tx.compileMessage(), "confirmed");
    const txBase64 = tx.serialize({ requireAllSignatures: false }).toString("base64");

    res.json({
      txBase64,
      feeLamports: fee.value ?? null,
      lastValidBlockHeight
    });
  } catch (e) {
    next(e);
  }
});

// POST /api/confirm-transfer { signedTxBase64 }
router.post(
  "/confirm-transfer",
  requireApiKey,
  validateQuery(confirmQuerySchema),
  validateBody(confirmBodySchema),
  async (req, res, next) => {
  try {
    const { signedTxBase64, commitment } = req.body;
    const wait = req.query.wait === "true";

    const conn = getConnection();
    const raw = Buffer.from(signedTxBase64, "base64");
    const tx = Transaction.from(raw);
    const sig = await conn.sendRawTransaction(raw, { skipPreflight: false });
    if (!wait) return res.json({ signature: sig });

    const latest = await conn.getLatestBlockhash(commitment || "confirmed");
    const confirmation = await conn.confirmTransaction(
      {
        signature: sig,
        blockhash: tx.recentBlockhash || latest.blockhash,
        lastValidBlockHeight: latest.lastValidBlockHeight
      },
      commitment || "confirmed"
    );
    res.json({ signature: sig, confirmation });
  } catch (e) {
    next(e);
  }
});

export default router;
