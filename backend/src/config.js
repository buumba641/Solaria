import dotenv from "dotenv";
dotenv.config();

export const config = {
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
  }
};
