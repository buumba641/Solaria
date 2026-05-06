import { z } from "zod";

export function validateBody(schema) {
  return (req, res, next) => {
    const result = schema.safeParse(req.body ?? {});
    if (!result.success) {
      return res.status(400).json({ error: "Invalid request body", details: result.error.flatten() });
    }
    req.body = result.data;
    return next();
  };
}

export function validateQuery(schema) {
  return (req, res, next) => {
    const result = schema.safeParse(req.query ?? {});
    if (!result.success) {
      return res.status(400).json({ error: "Invalid query params", details: result.error.flatten() });
    }
    req.query = result.data;
    return next();
  };
}

export { z };
