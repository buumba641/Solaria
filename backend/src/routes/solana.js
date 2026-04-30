import express from "express";
import {
  PublicKey,
  SystemProgram,
  Transaction,
  LAMPORTS_PER_SOL
} from "@solana/web3.js";
import { getConnection } from "../solana.js";

const router = express.Router();

/** Validate a base58 public key string. Returns PublicKey or null. */
function tryPublicKey(value) {
  try {
    return new PublicKey(value);
  } catch {
    return null;
  }
}

// GET /api/balance?wallet=<pubkey>
router.get("/balance", async (req, res, next) => {
  try {
    const wallet = String(req.query.wallet || "");
    if (!wallet) return res.status(400).json({ error: "wallet is required" });

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
router.post("/prepare-transfer", async (req, res, next) => {
  try {
    const { from, to, amountSol } = req.body ?? {};
    if (!from || !to || typeof amountSol !== "number") {
      return res.status(400).json({ error: "Expected { from, to, amountSol:number }" });
    }

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
router.post("/confirm-transfer", async (req, res, next) => {
  try {
    const { signedTxBase64 } = req.body ?? {};
    if (!signedTxBase64) return res.status(400).json({ error: "signedTxBase64 is required" });

    const conn = getConnection();
    const raw = Buffer.from(signedTxBase64, "base64");
    const sig = await conn.sendRawTransaction(raw, { skipPreflight: false });
    res.json({ signature: sig });
  } catch (e) {
    next(e);
  }
});

export default router;
