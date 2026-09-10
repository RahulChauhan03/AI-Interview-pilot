import { User } from './user.model';

export interface LoginResponse {
  accessToken: string;
  userId?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: string;
}
