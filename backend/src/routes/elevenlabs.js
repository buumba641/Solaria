import express from "express";
import fetch from "node-fetch";
import { config } from "../config.js";

const router = express.Router();

/**
 * POST /api/elevenlabs/signed-url
 * Returns a short-lived signed websocket URL to connect to the ElevenLabs Conversational AI agent.
 */
router.post("/signed-url", async (_req, res, next) => {
  if (!config.elevenlabs.apiKey || !config.elevenlabs.agentId) {
    return res.status(400).json({ error: "Missing ELEVENLABS_API_KEY or ELEVENLABS_AGENT_ID" });
  }

  try {
    const response = await fetch("https://api.elevenlabs.io/v1/convai/signed-url", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "xi-api-key": config.elevenlabs.apiKey
      },
      body: JSON.stringify({ agent_id: config.elevenlabs.agentId })
    });

    if (!response.ok) {
      const text = await response.text();
      return res.status(502).json({ error: "ElevenLabs error", status: response.status, details: text });
    }

    const data = await response.json();
    return res.json(data);
  } catch (e) {
    next(e);
  }
});

export default router;
