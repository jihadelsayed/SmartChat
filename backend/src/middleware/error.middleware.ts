import type { NextFunction, Request, Response } from "express";
import multer from "multer";
import { environment } from "../config/environment";
import { AppError } from "../shared/errors/app-error";

interface HttpError extends Error {
  status?: number;
  statusCode?: number;
  type?: string;
}

function isHttpError(error: unknown): error is HttpError {
  return error instanceof Error;
}

export function errorHandler(
  error: unknown,
  request: Request,
  response: Response,
  _next: NextFunction
): void {
  if (error instanceof multer.MulterError) {
    if (error.code === "LIMIT_FILE_SIZE") {
      response.status(413).json({
        success: false,
        error: {
          code: "ATTACHMENT_TOO_LARGE",
          message: "The attachment must not exceed 10 MB"
        }
      });

      return;
    }

    if (error.code === "LIMIT_FILE_COUNT") {
      response.status(400).json({
        success: false,
        error: {
          code: "TOO_MANY_ATTACHMENTS",
          message: "Only one attachment can be uploaded at a time"
        }
      });

      return;
    }

    response.status(400).json({
      success: false,
      error: {
        code: "ATTACHMENT_UPLOAD_ERROR",
        message: error.message
      }
    });

    return;
  }

  if (error instanceof AppError) {
    if (error.retryAfter) {
      response.setHeader("Retry-After", error.retryAfter);
    }

    response.status(error.statusCode).json({
      success: false,
      error: {
        code: error.code,
        message: error.message,
        details: error.details,
        retryable: error.retryable,
        requestId:
          error.retryable !== undefined
            ? request.requestId
            : undefined
      }
    });

    return;
  }

  if (
    isHttpError(error) &&
    (error.status === 400 || error.statusCode === 400) &&
    error.type === "entity.parse.failed"
  ) {
    response.status(400).json({
      success: false,
      error: {
        code: "INVALID_JSON",
        message: "The request body contains invalid JSON"
      }
    });

    return;
  }

  if (
    isHttpError(error) &&
    (error.status === 404 || error.statusCode === 404)
  ) {
    response.status(404).json({
      success: false,
      error: {
        code: "RESOURCE_NOT_FOUND",
        message: "The requested resource was not found"
      }
    });

    return;
  }

  console.error(error);

  response.status(500).json({
    success: false,
    error: {
      code: "INTERNAL_SERVER_ERROR",
      message:
        environment.NODE_ENV === "production"
          ? "An unexpected error occurred"
          : error instanceof Error
            ? error.message
            : "Unknown error"
    }
  });
}
