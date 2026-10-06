import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { APP_CONSTANTS } from '../constants/app.constants';

/** Signed-in users only; an expired session is cleared and sent to the login page. */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  if (authService.isLoggedIn()) return true;
  authService.logout();
  return inject(Router).createUrlTree([APP_CONSTANTS.loginRoute]);
};

/**
 * Hides the admin area from other users. This is only navigation: the backend rejects /api/admin/** for
 * anyone without the ADMIN role, whatever the browser does.
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  return authService.isAdmin() ? true : inject(Router).createUrlTree(['/app/dashboard']);
};

/** Signed-in users who open the login or register page go to their home page instead. */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  return authService.isLoggedIn() ? inject(Router).createUrlTree([authService.homeUrl()]) : true;
};
