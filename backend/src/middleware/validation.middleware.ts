import type { NextFunction, Request, RequestHandler, Response } from "express";
import type { ZodType } from "zod";
import { ValidationError } from "../shared/errors/validation-error";

interface ValidatedRequestData {
  body?: unknown;
  params?: Record<string, string>;
}

export function validate(schema: ZodType): RequestHandler {
  return (request: Request, _response: Response, next: NextFunction): void => {
    const result = schema.safeParse({
      body: request.body,
      params: request.params,
      query: request.query,
      headers: request.headers
    });

    if (!result.success) {
      next(new ValidationError(result.error.flatten()));
      return;
    }

    const validatedRequest = result.data as ValidatedRequestData;

    if (validatedRequest.body !== undefined) {
      request.body = validatedRequest.body;
    }

    if (validatedRequest.params !== undefined) {
      request.params = validatedRequest.params;
    }

    next();
  };
}
