import { config } from "../config.js";

export function requireApiKey(req, res, next) {
  if (config.publicDemoMode) {
    return next();
  }

  if (!config.auth.apiKey) {
    return res.status(500).json({ error: "API key not configured" });
  }

  const apiKey = req.get("x-api-key");
  if (!apiKey || apiKey !== config.auth.apiKey) {
    return res.status(401).json({ error: "Unauthorized" });
  }

  return next();
}
