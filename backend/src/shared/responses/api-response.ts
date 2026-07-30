export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: unknown;
    retryable?: boolean;
    requestId?: string;
  };
}

export function successResponse<T>(data: T): ApiResponse<T> {
  return { success: true, data };
}
