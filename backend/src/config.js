import dotenv from "dotenv";
import { fileURLToPath } from "url";
import path from "path";

// Resolve .env relative to the backend root, not CWD
const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.resolve(__dirname, "../.env") });

export const config = {
  isProd: process.env.NODE_ENV === "production",
  publicDemoMode: process.env.PUBLIC_DEMO_MODE === "true",
  port: Number(process.env.PORT ?? 8787),

  solana: {
    cluster: process.env.SOLANA_CLUSTER ?? "devnet",
    rpcUrl: process.env.SOLANA_RPC_URL || "",
    programId: process.env.SOLARIA_PROGRAM_ID || ""
  },

  oracle: {
    keypairJson: process.env.ORACLE_KEYPAIR_JSON || ""
  },

  elevenlabs: {
    apiKey: process.env.ELEVENLABS_API_KEY || "",
    agentId: process.env.ELEVENLABS_AGENT_ID || ""
  },

  lifi: {
    baseUrl: process.env.LIFI_BASE_URL || "https://li.quest/v1"
  },

  auth: {
    apiKey: process.env.ADMIN_API_KEY || ""
  },

  cors: {
    origins: (process.env.CORS_ORIGINS || "")
      .split(",")
      .map((value) => value.trim())
      .filter(Boolean)
  },

  rateLimit: {
    windowMs: Number(process.env.RATE_LIMIT_WINDOW_MS ?? 15 * 60 * 1000),
    max: Number(process.env.RATE_LIMIT_MAX ?? 100),
    sensitiveMax: Number(process.env.SENSITIVE_RATE_LIMIT_MAX ?? 20)
  }
};
