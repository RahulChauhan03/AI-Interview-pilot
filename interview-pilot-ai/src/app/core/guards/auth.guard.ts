import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { APP_CONSTANTS } from '../constants/app.constants';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  return authService.isLoggedIn()
    ? true
    : inject(Router).createUrlTree([APP_CONSTANTS.loginRoute]);
};
