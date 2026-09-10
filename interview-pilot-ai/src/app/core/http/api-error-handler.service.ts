import { Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

@Injectable({ providedIn: 'root' })
export class ApiErrorHandlerService {
  private readonly notificationService = inject(NotificationService);

  handle(error: HttpErrorResponse): Observable<never> {
    const message = this.resolveMessage(error.status);
    this.notificationService.error(message);
    return throwError(() => new Error(message));
  }

  private resolveMessage(status: number): string {
    switch (status) {
      case 400:
        return 'The submitted information is invalid.';
      case 401:
        return 'Your session has expired. Please sign in again.';
      case 403:
        return 'You do not have permission to perform this action.';
      case 404:
        return 'The requested resource was not found.';
      case 409:
        return 'The requested action conflicts with the current state.';
      case 422:
        return 'The provided data could not be processed.';
      case 500:
        return 'The service is temporarily unavailable. Please try again.';
      default:
        return 'An unexpected error occurred.';
    }
  }
}
