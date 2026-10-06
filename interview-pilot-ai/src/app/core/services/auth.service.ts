import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, of, tap } from 'rxjs';

import { AuthInterface } from '../../features/auth/interfaces/auth.interface';
import { AuthResponse } from '../../features/auth/models/auth-response.model';
import { LoginRequest } from '../../features/auth/models/login-request.model';
import { RegisterRequest } from '../../features/auth/models/register-request.model';
import { RegisterResponse } from '../../features/auth/models/register-response.model';
import { User } from '../../features/auth/models/user.model';

import { StorageService } from './storage.service';
import { ApiService } from '../http/api.service';
import { API_ENDPOINTS } from '../http/api-endpoints';

@Injectable({ providedIn: 'root' })
export class AuthService implements AuthInterface {

  private readonly api = inject(ApiService);
  private readonly storage = inject(StorageService);

  private readonly token = signal<string | null>(this.getToken());
  private readonly user = signal<User | null>(this.readStoredUser());

  readonly isAuthenticated = computed(() => this.isLoggedIn());
  readonly currentUser = this.user.asReadonly();
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly displayName = computed(() => {
    const user = this.user();
    const name = [user?.firstName, user?.lastName].filter(Boolean).join(' ');
    return name || user?.email || 'Account';
  });
  readonly initials = computed(() => {
    const user = this.user();
    const letters = (user?.firstName?.[0] ?? '') + (user?.lastName?.[0] ?? '');
    return (letters || user?.email?.[0] || '?').toUpperCase();
  });

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.api
      .post<RegisterResponse>(
        API_ENDPOINTS.AUTH.REGISTER,
        request
      )
      .pipe(
        tap(() => this.clearSession())
      );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api
      .post<AuthResponse>(
        API_ENDPOINTS.AUTH.LOGIN,
        request
      )
      .pipe(
        tap((response) => this.persistSession(response))
      );
  }

  requestPasswordReset(email: string): Observable<void> {
    return this.api.post<void>(API_ENDPOINTS.AUTH.FORGOT_PASSWORD, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.api.post<void>(API_ENDPOINTS.AUTH.RESET_PASSWORD, { token, newPassword });
  }

  logout(): void {
    this.clearSession();
  }

  getProfile(): Observable<User> {
    return this.api
      .get<User>(API_ENDPOINTS.AUTH.CURRENT_USER)
      .pipe(
        tap((response) => this.saveUser(response))
      );
  }

  saveToken(token: string): void {
    this.storage.saveToken(token);
    this.token.set(token);
  }

  getToken(): string | null {
    return this.storage.getToken();
  }

  removeToken(): void {
    this.storage.removeToken();
    this.token.set(null);
  }

  /** A stored token only counts while it has not expired. */
  isLoggedIn(): boolean {
    const token = this.token();
    return Boolean(token) && !this.isExpired(token as string);
  }

  /** Where a signed-in user lands: administrators get the admin console. */
  homeUrl(): string {
    return this.isAdmin() ? '/admin/dashboard' : '/app/dashboard';
  }

  /** Keeps the stored user in sync after a profile change. */
  updateCurrentUser(changes: Partial<User>): void {
    const current = this.user();
    if (current) this.saveUser({ ...current, ...changes });
  }

  getCurrentUser(): User | null {
    return this.user();
  }

  refreshToken(): Observable<string | null> {
    return of(this.getToken()).pipe(
      map((token) => token)
    );
  }

  private persistSession(response: AuthResponse): void {
    this.saveToken(response.accessToken);

    const user: User = {
      id: response.userId ?? '',
      firstName: response.firstName,
      lastName: response.lastName,
      email: response.email ?? '',
      role: response.role,
    };

    this.saveUser(user);
  }

  private saveUser(user: User): void {
    this.storage.saveUser(user);
    this.user.set(user);
  }

  private clearSession(): void {
    this.removeToken();
    this.storage.clear();
    this.user.set(null);
  }

  /** Reads the JWT's exp claim; an unreadable token is treated as expired. */
  private isExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }

  private readStoredUser(): User | null {
    return (this.storage.getUser() as User | null) ?? null;
  }
}
