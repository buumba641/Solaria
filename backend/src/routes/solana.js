import express from "express";
import {
  PublicKey,
  SystemProgram,
  Transaction,
  LAMPORTS_PER_SOL
} from "@solana/web3.js";
import { getConnection } from "../solana.js";

const router = express.Router();

// GET /api/balance?wallet=<pubkey>
router.get("/balance", async (req, res) => {
  const wallet = String(req.query.wallet || "");
  if (!wallet) return res.status(400).json({ error: "wallet is required" });

  const conn = getConnection();
  const pubkey = new PublicKey(wallet);

  const lamports = await conn.getBalance(pubkey, "confirmed");
  res.json({ wallet, sol: lamports / LAMPORTS_PER_SOL, lamports });
});

// POST /api/prepare-transfer { from, to, amountSol }
router.post("/prepare-transfer", async (req, res) => {
  const { from, to, amountSol } = req.body ?? {};
  if (!from || !to || typeof amountSol !== "number") {
    return res.status(400).json({ error: "Expected { from, to, amountSol:number }" });
  }

  const conn = getConnection();
  const fromPk = new PublicKey(from);
  const toPk = new PublicKey(to);

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
});

// POST /api/confirm-transfer { signedTxBase64 }
router.post("/confirm-transfer", async (req, res) => {
  const { signedTxBase64 } = req.body ?? {};
  if (!signedTxBase64) return res.status(400).json({ error: "signedTxBase64 is required" });

  const conn = getConnection();
  const raw = Buffer.from(signedTxBase64, "base64");
  const sig = await conn.sendRawTransaction(raw, { skipPreflight: false });
  res.json({ signature: sig });
});

export default router;
