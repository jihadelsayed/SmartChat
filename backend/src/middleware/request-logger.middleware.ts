import type { NextFunction, Request, Response } from "express";
import { randomUUID } from "node:crypto";
import { logger } from "../config/logger";

export function requestLogger(
  request: Request,
  response: Response,
  next: NextFunction
): void {
  const requestId = request.header("x-request-id") ?? randomUUID();
  const startedAt = performance.now();

  response.setHeader("x-request-id", requestId);

  response.on("finish", () => {
    const durationMs = Number((performance.now() - startedAt).toFixed(2));

    logger.info("HTTP request completed", {
      requestId,
      method: request.method,
      path: request.originalUrl,
      statusCode: response.statusCode,
      durationMs,
      ipAddress: request.ip,
      userAgent: request.header("user-agent") ?? null
    });
  });

  next();
}