export interface AppErrorOptions {
  retryable?: boolean;
  retryAfter?: string;
}

export class AppError extends Error {
  constructor(
    message: string,
    public readonly statusCode = 500,
    public readonly code = "INTERNAL_SERVER_ERROR",
    public readonly details?: unknown,
    options: AppErrorOptions = {}
  ) {
    super(message);
    this.name = new.target.name;
    this.retryable = options.retryable;
    this.retryAfter = options.retryAfter;
  }

  public readonly retryable?: boolean;
  public readonly retryAfter?: string;
}
