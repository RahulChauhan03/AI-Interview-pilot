import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, from, map, of, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { HttpStatusEnum } from '../enums/http-status.enum';

/** Shows one notification per failed request and passes the message on as an Error. */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const notificationService = inject(NotificationService);
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => withJsonBody(error)),
    catchError((error: HttpErrorResponse) => {
      // An expired or revoked session: sign out and go to the login page (a failed login keeps its own message).
      if (error.status === HttpStatusEnum.Unauthorized && !request.url.includes('/auth/login')) {
        authService.logout();
        router.navigateByUrl('/login');
      }
      const message = resolveErrorMessage(error);
      notificationService.error(message);
      return throwError(() => new Error(message));
    }),
  );
};

/** File downloads receive their error body as a Blob; reads it as JSON so the backend's message can be shown. */
function withJsonBody(error: HttpErrorResponse): Observable<never> {
  const body: unknown = error.error;
  if (!(body instanceof Blob) || !body.type.includes('json')) {
    return throwError(() => error);
  }
  return from(body.text()).pipe(
    map((text) => new HttpErrorResponse({ error: JSON.parse(text), headers: error.headers, status: error.status,
      statusText: error.statusText, url: error.url ?? undefined })),
    catchError(() => of(error)),
    switchMap((parsed) => throwError(() => parsed)),
  );
}

/** Prefers the backend's message (safe to show since the API's error handling was hardened), else a generic one. */
function resolveErrorMessage(error: HttpErrorResponse): string {
  const body = error.error as { message?: unknown; data?: { validationErrors?: Record<string, string> | null } } | null;
  const validationErrors = body?.data?.validationErrors;
  if (validationErrors && Object.keys(validationErrors).length > 0) {
    return Object.values(validationErrors).join(' ');
  }
  if (typeof body?.message === 'string' && body.message.trim() && error.status !== HttpStatusEnum.InternalServerError) {
    return body.message;
  }
  return resolveStatusMessage(error.status);
}

function resolveStatusMessage(status: number): string {
  switch (status) {
    case 0:
      return 'The server could not be reached. Check your connection and try again.';
    case HttpStatusEnum.BadRequest:
      return 'The submitted information is invalid.';
    case HttpStatusEnum.Unauthorized:
      return 'Your session has expired. Please sign in again.';
    case HttpStatusEnum.Forbidden:
      return 'You do not have permission to perform this action.';
    case HttpStatusEnum.NotFound:
      return 'The requested resource was not found.';
    case HttpStatusEnum.InternalServerError:
      return 'The service is temporarily unavailable. Please try again later.';
    default:
      return 'An unexpected error occurred.';
  }
}
