import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import { HttpStatusEnum } from '../enums/http-status.enum';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const notificationService = inject(NotificationService);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const message = resolveErrorMessage(error.status);
      notificationService.error(message);
      return throwError(() => new Error(message));
    }),
  );
};

function resolveErrorMessage(status: number): string {
  switch (status) {
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
