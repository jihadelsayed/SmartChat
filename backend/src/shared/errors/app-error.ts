export class AppError extends Error {
  constructor(
    message: string,
    public readonly statusCode = 500,
    public readonly code = "INTERNAL_SERVER_ERROR",
    public readonly details?: unknown
  ) {
    super(message);
    this.name = new.target.name;
  }
}
