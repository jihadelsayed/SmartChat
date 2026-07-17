type LogMetadata = Record<string, unknown>;

function formatMetadata(metadata?: LogMetadata): string {
  if (!metadata || Object.keys(metadata).length === 0) {
    return "";
  }

  return ` ${JSON.stringify(metadata)}`;
}

export const logger = {
  info(message: string, metadata?: LogMetadata): void {
    console.log(
      `[INFO] ${new Date().toISOString()} ${message}${formatMetadata(metadata)}`
    );
  },

  warn(message: string, metadata?: LogMetadata): void {
    console.warn(
      `[WARN] ${new Date().toISOString()} ${message}${formatMetadata(metadata)}`
    );
  },

  error(message: string, error?: unknown, metadata?: LogMetadata): void {
    const errorDetails =
      error instanceof Error
        ? {
            name: error.name,
            message: error.message,
            stack: error.stack
          }
        : error;

    console.error(
      `[ERROR] ${new Date().toISOString()} ${message}${formatMetadata({
        ...metadata,
        error: errorDetails
      })}`
    );
  },

  debug(message: string, metadata?: LogMetadata): void {
    if (process.env.NODE_ENV !== "development") {
      return;
    }

    console.debug(
      `[DEBUG] ${new Date().toISOString()} ${message}${formatMetadata(metadata)}`
    );
  }
};