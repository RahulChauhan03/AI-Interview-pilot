export interface User {
  id?: string;
  firstName: string;
  lastName: string;
  email: string;
  role?: string;
}
export interface Resume {
  id: string;
  name: string;
  uploadedAt: string;
  status: 'Ready' | 'Processing';
}
export interface JobDescription {
  id: string;
  company: string;
  role: string;
  createdAt: string;
}
export interface Interview {
  id: string;
  role: string;
  company: string;
  date: string;
  score?: number;
  status: 'Completed' | 'Scheduled';
}
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}
