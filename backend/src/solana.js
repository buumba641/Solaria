import { Connection, Keypair, clusterApiUrl } from "@solana/web3.js";
import { config } from "./config.js";

export function getConnection() {
  const url =
    config.solana.rpcUrl ||
    clusterApiUrl(config.solana.cluster === "mainnet-beta" ? "mainnet-beta" : "devnet");
  return new Connection(url, "confirmed");
}

export function getOracleKeypair() {
  if (!config.oracle.keypairJson) throw new Error("Missing ORACLE_KEYPAIR_JSON");
  const secret = Uint8Array.from(JSON.parse(config.oracle.keypairJson));
  return Keypair.fromSecretKey(secret);
}
