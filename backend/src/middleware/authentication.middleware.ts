import type { NextFunction, Request, Response } from "express";
import { AuthenticationError } from "../shared/errors/authentication-error";
import { verifyAccessToken } from "../shared/utils/jwt";

export function requireAuthentication(
  request: Request,
  _response: Response,
  next: NextFunction
): void {
  const authorization = request.header("authorization");

  if (!authorization?.startsWith("Bearer ")) {
    next(new AuthenticationError());
    return;
  }

  try {
    const payload = verifyAccessToken(authorization.slice(7));
    if (!payload.sub || !payload.email) {
      throw new Error("Invalid token payload");
    }

    request.authenticatedUser = {
      id: payload.sub,
      email: payload.email
    };
    next();
  } catch {
    next(new AuthenticationError("Invalid or expired access token"));
  }
}
