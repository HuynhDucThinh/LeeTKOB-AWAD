export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}

export interface ApiErrorResponse {
  success: boolean;
  message: string;
  data?: Record<string, string>;
}
