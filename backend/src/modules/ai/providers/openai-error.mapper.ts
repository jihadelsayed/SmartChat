import {
  APIConnectionError,
  APIConnectionTimeoutError,
  APIError,
  AuthenticationError as OpenAiAuthenticationError,
  BadRequestError,
  NotFoundError as OpenAiNotFoundError,
  PermissionDeniedError,
  RateLimitError,
  UnprocessableEntityError
} from "openai";
import { logger } from "../../../config/logger";
import {
  AiProviderAuthenticationError,
  AiProviderError,
  AiProviderRequestFailedError,
  AiProviderUnavailableError,
  AiQuotaExceededError,
  AiRateLimitedError,
  AiRequestTimeoutError
} from "../ai.errors";

function validRetryAfter(error: APIError): string | undefined {
  const value = error.headers?.get("retry-after")?.trim();
  if (!value) {
    return undefined;
  }

  if (/^\d+$/.test(value)) {
    return value;
  }

  return Number.isNaN(Date.parse(value)) ? undefined : value;
}

function redactProviderMessage(
  message: string,
  apiKey: string
): string {
  let sanitized = message;

  if (apiKey) {
    sanitized = sanitized.replaceAll(apiKey, "[REDACTED]");
  }

  return sanitized
    .replace(/\bsk-[A-Za-z0-9_-]+\b/g, "[REDACTED]")
    .replace(/Bearer\s+\S+/gi, "Bearer [REDACTED]");
}

function logOpenAiError(error: unknown, apiKey: string): void {
  const apiError = error instanceof APIError ? error : undefined;
  const message =
    error instanceof Error ? error.message : "Unknown OpenAI failure";

  logger.error("OpenAI request failed", undefined, {
    openAiRequestId: apiError?.requestID ?? null,
    status: apiError?.status ?? null,
    code: apiError?.code ?? null,
    type: apiError?.type ?? null,
    message: redactProviderMessage(message, apiKey)
  });
}

export function mapOpenAiError(
  error: unknown,
  apiKey: string
) {
  logOpenAiError(error, apiKey);

  const retryAfter =
    error instanceof APIError
      ? validRetryAfter(error)
      : undefined;

  if (error instanceof APIConnectionTimeoutError) {
    return new AiRequestTimeoutError(retryAfter);
  }

  if (
    error instanceof APIError &&
    error.code?.toLowerCase() === "insufficient_quota"
  ) {
    return new AiQuotaExceededError(retryAfter);
  }

  if (
    error instanceof RateLimitError ||
    (error instanceof APIError && error.status === 429)
  ) {
    return new AiRateLimitedError(retryAfter);
  }

  if (
    error instanceof OpenAiAuthenticationError ||
    error instanceof PermissionDeniedError ||
    (error instanceof APIError &&
      (error.status === 401 || error.status === 403))
  ) {
    return new AiProviderAuthenticationError(retryAfter);
  }

  if (error instanceof APIConnectionError) {
    return new AiProviderUnavailableError(retryAfter);
  }

  if (
    error instanceof BadRequestError ||
    error instanceof OpenAiNotFoundError ||
    error instanceof UnprocessableEntityError ||
    (error instanceof APIError &&
      (error.status === 400 ||
        error.status === 404 ||
        error.status === 422))
  ) {
    return new AiProviderRequestFailedError(retryAfter);
  }

  return new AiProviderError();
}
