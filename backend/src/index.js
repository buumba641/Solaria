import express from "express";
import cors from "cors";
import { config } from "./config.js";

import elevenlabsRouter from "./routes/elevenlabs.js";
import solanaRouter from "./routes/solana.js";
import tokensRouter from "./routes/tokens.js";
import fraudRouter from "./routes/fraud.js";
import nftsRouter from "./routes/nfts.js";
import lifiRouter from "./routes/lifi.js";
import paymentRouter from "./routes/payment.js";

const app = express();
app.use(cors());
app.use(express.json({ limit: "2mb" }));

// Request logging
app.use((req, _res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

app.get("/health", (_req, res) => res.json({ ok: true, name: "solaria-backend" }));

app.use("/api/elevenlabs", elevenlabsRouter);
app.use("/api", solanaRouter);
app.use("/api", tokensRouter);
app.use("/api", fraudRouter);
app.use("/api", nftsRouter);
app.use("/api", lifiRouter);
app.use("/api", paymentRouter);

// Global error handler — catches unhandled errors from async route handlers
app.use((err, _req, res, _next) => {
  console.error("[ERROR]", err.stack || err);
  res.status(500).json({ error: "Internal server error", message: err.message });
});

app.listen(config.port, () => {
  console.log(`Solaria backend listening on http://localhost:${config.port}`);
});
