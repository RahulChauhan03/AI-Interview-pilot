export interface ApiResponse<T> {
  status: number;
  message: string;
  timestamp: string;
  data: T;
}
