import { AppError } from "../../shared/errors/app-error";

export class AiConfigurationError extends AppError {
  constructor() {
    super(
      "The AI service is not configured",
      500,
      "AI_CONFIGURATION_ERROR",
      undefined,
      { retryable: false }
    );
  }
}

export class AiProviderError extends AppError {
  constructor() {
    super(
      "The AI service is temporarily unavailable. Please try again.",
      502,
      "AI_PROVIDER_ERROR",
      undefined,
      { retryable: false }
    );
  }
}

export class AiQuotaExceededError extends AppError {
  constructor(retryAfter?: string) {
    super(
      "AI service quota is unavailable.",
      503,
      "AI_QUOTA_EXCEEDED",
      undefined,
      { retryable: false, retryAfter }
    );
  }
}

export class AiRateLimitedError extends AppError {
  constructor(retryAfter?: string) {
    super(
      "AI service rate limit reached. Please try again.",
      429,
      "AI_RATE_LIMITED",
      undefined,
      { retryable: true, retryAfter }
    );
  }
}

export class AiProviderAuthenticationError extends AppError {
  constructor(retryAfter?: string) {
    super(
      "AI service authentication is unavailable.",
      503,
      "AI_PROVIDER_AUTH_ERROR",
      undefined,
      { retryable: false, retryAfter }
    );
  }
}

export class AiProviderUnavailableError extends AppError {
  constructor(retryAfter?: string) {
    super(
      "The AI service is temporarily unavailable. Please try again.",
      503,
      "AI_PROVIDER_UNAVAILABLE",
      undefined,
      { retryable: true, retryAfter }
    );
  }
}

export class AiProviderRequestFailedError extends AppError {
  constructor(retryAfter?: string) {
    super(
      "The AI service could not process the request.",
      502,
      "AI_PROVIDER_REQUEST_FAILED",
      undefined,
      { retryable: false, retryAfter }
    );
  }
}

export class AiEmptyResponseError extends AppError {
  constructor() {
    super(
      "The AI service returned an empty response. Please try again.",
      502,
      "AI_EMPTY_RESPONSE",
      undefined,
      { retryable: false }
    );
  }
}

export class AiRequestTimeoutError extends AppError {
  constructor(retryAfter?: string) {
    super(
      "The AI service took too long to respond. Please try again.",
      504,
      "AI_PROVIDER_TIMEOUT",
      undefined,
      { retryable: true, retryAfter }
    );
  }
}

export class AiImageInputUnsupportedError extends AppError {
  constructor() {
    super(
      "The configured AI model does not support image input.",
      422,
      "AI_IMAGE_INPUT_UNSUPPORTED",
      undefined,
      { retryable: false }
    );
  }
}
