import express from "express";
import { requireApiKey } from "../middleware/auth.js";
import { validateBody, z } from "../middleware/validate.js";

const router = express.Router();

const chatSchema = z.object({
  message: z.string().min(1),
  walletAddress: z.string().min(1),
  conversationId: z.string().optional()
});

// POST /api/chat
router.post("/chat", requireApiKey, validateBody(chatSchema), async (req, res) => {
  const { message, conversationId } = req.body;

  res.json({
    reply: message ? `Echo: ${message}` : "Hello from Solaria.",
    audioUrl: null,
    action: null,
    conversationId: conversationId || null
  });
});

export default router;
