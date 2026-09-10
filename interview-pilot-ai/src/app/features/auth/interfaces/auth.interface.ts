import { Observable } from 'rxjs';
import { AuthResponse } from '../models/auth-response.model';
import { LoginRequest } from '../models/login-request.model';
import { RegisterRequest } from '../models/register-request.model';
import { RegisterResponse } from '../models/register-response.model';
import { User } from '../models/user.model';

export interface AuthInterface {
  login(request: LoginRequest): Observable<AuthResponse>;
  register(request: RegisterRequest): Observable<RegisterResponse>;
  requestPasswordReset(email: string): Observable<void>;
  resetPassword(token: string, password: string): Observable<void>;
  logout(): void;
  getProfile(): Observable<User>;
  saveToken(token: string): void;
  getToken(): string | null;
  removeToken(): void;
  getCurrentUser(): User | null;
  isLoggedIn(): boolean;
}
