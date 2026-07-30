import { AppError } from "../../shared/errors/app-error";

export class IdempotencyConflictError extends AppError {
  constructor() {
    super(
      "The idempotency key was already used with a different request.",
      409,
      "IDEMPOTENCY_KEY_CONFLICT",
      undefined,
      { retryable: false }
    );
  }
}

export class AiRequestInProgressError extends AppError {
  constructor() {
    super(
      "An AI request with this idempotency key is already processing.",
      409,
      "AI_REQUEST_IN_PROGRESS",
      undefined,
      { retryable: true }
    );
  }
}

export class StoredMessageRequestError extends AppError {
  constructor(
    statusCode: number,
    code: string,
    message: string,
    retryable: boolean,
    retryAfter?: string
  ) {
    super(
      message,
      statusCode,
      code,
      undefined,
      { retryable, retryAfter }
    );
  }
}
