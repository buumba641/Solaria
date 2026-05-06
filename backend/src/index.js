import express from "express";
import cors from "cors";
import rateLimit from "express-rate-limit";
import { config } from "./config.js";

import elevenlabsRouter from "./routes/elevenlabs.js";
import chatRouter from "./routes/chat.js";
import solanaRouter from "./routes/solana.js";
import tokensRouter from "./routes/tokens.js";
import fraudRouter from "./routes/fraud.js";
import nftsRouter from "./routes/nfts.js";
import lifiRouter from "./routes/lifi.js";
import paymentRouter from "./routes/payment.js";

const app = express();
app.set("trust proxy", 1);

if (config.isProd && config.publicDemoMode) {
  console.error("[FATAL] PUBLIC_DEMO_MODE must be disabled in production.");
  process.exit(1);
}

const corsOptions = config.cors.origins.length
  ? {
      origin: (origin, callback) => {
        if (!origin || config.cors.origins.includes(origin)) {
          return callback(null, true);
        }
        return callback(new Error("Not allowed by CORS"));
      }
    }
  : { origin: true };

app.use(cors(corsOptions));
app.use(express.json({ limit: "2mb" }));

const apiLimiter = rateLimit({
  windowMs: config.rateLimit.windowMs,
  max: config.rateLimit.max,
  standardHeaders: true,
  legacyHeaders: false
});

app.use("/api", apiLimiter);

// Request logging
app.use((req, _res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

app.get("/health", (_req, res) => res.json({ ok: true, name: "solaria-backend" }));

app.use("/api/elevenlabs", elevenlabsRouter);
app.use("/api", chatRouter);
app.use("/api", solanaRouter);
app.use("/api", tokensRouter);
app.use("/api", fraudRouter);
app.use("/api", nftsRouter);
app.use("/api", lifiRouter);
app.use("/api", paymentRouter);

// Handle CORS rejections explicitly
app.use((err, _req, res, next) => {
  if (err && err.message === "Not allowed by CORS") {
    return res.status(403).json({ error: "CORS blocked" });
  }
  return next(err);
});

// Global error handler — catches unhandled errors from async route handlers
app.use((err, _req, res, _next) => {
  console.error("[ERROR]", err.stack || err);
  const payload = config.isProd
    ? { error: "Internal server error" }
    : { error: "Internal server error", message: err.message };
  res.status(500).json(payload);
});

app.listen(config.port, () => {
  console.log(`Solaria backend listening on http://localhost:${config.port}`);
});
