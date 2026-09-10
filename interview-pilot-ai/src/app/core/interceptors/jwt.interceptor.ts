import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

const publicAuthEndpoints = ['/auth/login', '/auth/register'];

export const jwtInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  const isPublicAuthRequest = publicAuthEndpoints.some((endpoint) => request.url.includes(endpoint));

  if (!token || isPublicAuthRequest) {
    return next(request);
  }

  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
