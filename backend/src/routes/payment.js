import express from "express";
import {
  PublicKey,
  Transaction,
  TransactionInstruction,
  SystemProgram
} from "@solana/web3.js";
import { getConnection, getOracleKeypair } from "../solana.js";
import { config } from "../config.js";
import crypto from "crypto";
import rateLimit from "express-rate-limit";
import { requireApiKey } from "../middleware/auth.js";
import { validateBody, z } from "../middleware/validate.js";

const router = express.Router();
const sensitiveLimiter = rateLimit({
  windowMs: config.rateLimit.windowMs,
  max: config.rateLimit.sensitiveMax,
  standardHeaders: true,
  legacyHeaders: false
});

const paymentLinkSchema = z.object({
  merchant: z.string().min(1),
  amount: z.number().positive(),
  description: z.string().max(140).optional()
});

const settleSchema = z.object({
  intentId: z.union([z.string(), z.number()]),
  merchantAddress: z.string().min(1),
  sourceChainTx: z.string().min(1),
  proofHash: z.string().optional()
});

/** Validate a base58 public key string. Returns PublicKey or null. */
function tryPublicKey(value) {
  try {
    return new PublicKey(value);
  } catch {
    return null;
  }
}

/**
 * Derive the PaymentIntent PDA address.
 * Seeds: [b"intent", merchant_pubkey, intent_id_le_bytes]
 */
function deriveIntentPda(programId, merchantPubkey, intentId) {
  const intentIdBuffer = Buffer.alloc(8);
  intentIdBuffer.writeBigUInt64LE(BigInt(intentId));

  return PublicKey.findProgramAddressSync(
    [Buffer.from("intent"), merchantPubkey.toBuffer(), intentIdBuffer],
    programId
  );
}

/**
 * Derive the Config PDA address.
 * Seeds: [b"config"]
 */
function deriveConfigPda(programId) {
  return PublicKey.findProgramAddressSync(
    [Buffer.from("config")],
    programId
  );
}

/**
 * Build the Anchor instruction discriminator for a given method name.
 * Anchor uses sha256("global:<method_name>")[0..8].
 */
function anchorDiscriminator(methodName) {
  const hash = crypto.createHash("sha256").update(`global:${methodName}`).digest();
  return hash.subarray(0, 8);
}

/**
 * POST /api/payment-link
 * Body: { merchant, amount, description }
 *
 * Creates a payment link by building a transaction that calls
 * create_payment_intent on the Solaria Anchor program.
 * Returns the intent PDA address and the unsigned transaction for the merchant to sign.
 */
router.post(
  "/payment-link",
  requireApiKey,
  validateBody(paymentLinkSchema),
  async (req, res, next) => {
  try {
    const { merchant, amount, description } = req.body;

    const merchantPk = tryPublicKey(merchant);
    if (!merchantPk) return res.status(400).json({ error: "Invalid merchant address" });

    if (typeof amount !== "number" || amount <= 0) {
      return res.status(400).json({ error: "amount must be a positive number (lamports)" });
    }

    const desc = String(description || "Payment").slice(0, 140);

    if (!config.solana.programId) {
      return res.status(500).json({ error: "SOLARIA_PROGRAM_ID not configured" });
    }

    const programId = tryPublicKey(config.solana.programId);
    if (!programId) return res.status(500).json({ error: "Invalid SOLARIA_PROGRAM_ID in config" });

    // Generate a unique intent ID from timestamp + short random suffix
    const intentId = Date.now() * 1000 + crypto.randomInt(0, 1000);

    // Derive PDA
    const [intentPda, _bump] = deriveIntentPda(programId, merchantPk, intentId);
    const conn = getConnection();

    // Build the Anchor instruction data:
    // discriminator (8 bytes) + amount (u64 LE) + description (borsh string: 4-byte len + utf8) + intent_id (u64 LE)
    const discriminator = anchorDiscriminator("create_payment_intent");
    const amountBuf = Buffer.alloc(8);
    amountBuf.writeBigUInt64LE(BigInt(amount));

    const descBytes = Buffer.from(desc, "utf8");
    const descLenBuf = Buffer.alloc(4);
    descLenBuf.writeUInt32LE(descBytes.length);

    const intentIdBuf = Buffer.alloc(8);
    intentIdBuf.writeBigUInt64LE(BigInt(intentId));

    const ixData = Buffer.concat([discriminator, amountBuf, descLenBuf, descBytes, intentIdBuf]);

    const ix = new TransactionInstruction({
      programId,
      keys: [
        { pubkey: merchantPk, isSigner: true, isWritable: true },   // merchant
        { pubkey: intentPda, isSigner: false, isWritable: true },    // payment_intent PDA
        { pubkey: SystemProgram.programId, isSigner: false, isWritable: false } // system_program
      ],
      data: ixData
    });

    const { blockhash, lastValidBlockHeight } = await conn.getLatestBlockhash("confirmed");

    const tx = new Transaction({
      feePayer: merchantPk,
      recentBlockhash: blockhash
    }).add(ix);

    const txBase64 = tx.serialize({ requireAllSignatures: false }).toString("base64");

    res.json({
      intentId,
      intentPda: intentPda.toBase58(),
      txBase64,
      lastValidBlockHeight,
      description: desc,
      amount,
      note: "Sign this transaction on-device via Mobile Wallet Adapter, then POST to /api/confirm-transfer"
    });
  } catch (e) {
    next(e);
  }
});

/**
 * POST /api/settle-intent
 * Body: { intentId, merchantAddress, sourceChainTx, proofHash }
 *
 * Called by the backend oracle after verifying a cross-chain payment.
 * Builds, signs (with oracle key), and broadcasts the settle_payment instruction.
 */
router.post(
  "/settle-intent",
  requireApiKey,
  sensitiveLimiter,
  validateBody(settleSchema),
  async (req, res, next) => {
  try {
    const { intentId, merchantAddress, sourceChainTx, proofHash } = req.body;

    const normalizedIntentId = Number(intentId);
    if (!Number.isFinite(normalizedIntentId)) {
      return res.status(400).json({ error: "intentId must be a number" });
    }

    const merchantPk = tryPublicKey(merchantAddress);
    if (!merchantPk) return res.status(400).json({ error: "Invalid merchantAddress" });

    if (!config.solana.programId) {
      return res.status(500).json({ error: "SOLARIA_PROGRAM_ID not configured" });
    }

    const programId = tryPublicKey(config.solana.programId);
    if (!programId) return res.status(500).json({ error: "Invalid SOLARIA_PROGRAM_ID in config" });

    let oracleKeypair;
    try {
      oracleKeypair = getOracleKeypair();
    } catch (e) {
      return res.status(500).json({ error: "Oracle keypair not configured", details: e.message });
    }

    const [intentPda] = deriveIntentPda(programId, merchantPk, normalizedIntentId);
    const [configPda] = deriveConfigPda(programId);

    const conn = getConnection();

    // Build settle_payment instruction data:
    // discriminator + intent_id (u64 LE) + source_chain_tx (borsh string) + proof_hash ([u8; 32])
    const discriminator = anchorDiscriminator("settle_payment");
    const intentIdBuf = Buffer.alloc(8);
    intentIdBuf.writeBigUInt64LE(BigInt(normalizedIntentId));

    const srcTxBytes = Buffer.from(String(sourceChainTx).slice(0, 200), "utf8");
    const srcTxLenBuf = Buffer.alloc(4);
    srcTxLenBuf.writeUInt32LE(srcTxBytes.length);

    // proof_hash: 32 bytes, hex-encoded or zero-filled
    let proofHashBuf;
    if (proofHash && typeof proofHash === "string" && proofHash.length === 64) {
      proofHashBuf = Buffer.from(proofHash, "hex");
    } else {
      proofHashBuf = Buffer.alloc(32);
    }

    const ixData = Buffer.concat([discriminator, intentIdBuf, srcTxLenBuf, srcTxBytes, proofHashBuf]);

    const ix = new TransactionInstruction({
      programId,
      keys: [
        { pubkey: oracleKeypair.publicKey, isSigner: true, isWritable: false }, // oracle
        { pubkey: intentPda, isSigner: false, isWritable: true },               // payment_intent PDA
        { pubkey: configPda, isSigner: false, isWritable: false }               // config PDA
      ],
      data: ixData
    });

    const { blockhash, lastValidBlockHeight } = await conn.getLatestBlockhash("confirmed");

    const tx = new Transaction({
      feePayer: oracleKeypair.publicKey,
      recentBlockhash: blockhash
    }).add(ix);

    tx.sign(oracleKeypair);
    const sig = await conn.sendRawTransaction(tx.serialize(), { skipPreflight: false });

    res.json({
      signature: sig,
      intentId: normalizedIntentId,
      intentPda: intentPda.toBase58(),
      status: "settled"
    });
  } catch (e) {
    next(e);
  }
});

export default router;
