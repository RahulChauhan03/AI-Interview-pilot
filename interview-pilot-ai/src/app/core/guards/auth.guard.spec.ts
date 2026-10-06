import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { adminGuard, authGuard, guestGuard } from './auth.guard';

/** A JWT whose payload has the given exp (seconds); signature is irrelevant on the client. */
function token(expSeconds: number): string {
  const payload = btoa(JSON.stringify({ sub: 'user@example.com', exp: expSeconds })).replace(/=+$/, '');
  return `header.${payload}.signature`;
}

function signIn(role: 'USER' | 'ADMIN', exp = Math.floor(Date.now() / 1000) + 3600): void {
  sessionStorage.setItem('access_token', token(exp));
  sessionStorage.setItem('current_user', JSON.stringify({ id: '1', email: 'user@example.com', role }));
}

function run(guard: typeof authGuard): boolean | UrlTree {
  return TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)) as boolean | UrlTree;
}

describe('route guards', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient()] });
  });

  it('sends signed-out users to the login page', () => {
    expect(String(run(authGuard))).toBe('/login');
  });

  it('treats an expired token as signed out', () => {
    signIn('USER', Math.floor(Date.now() / 1000) - 60);
    expect(String(run(authGuard))).toBe('/login');
  });

  it('lets signed-in users through', () => {
    signIn('USER');
    expect(run(authGuard)).toBe(true);
  });

  it('keeps regular users out of the admin area', () => {
    signIn('USER');
    expect(String(run(adminGuard))).toBe('/app/dashboard');
  });

  it('lets administrators into the admin area', () => {
    signIn('ADMIN');
    expect(run(adminGuard)).toBe(true);
  });

  it('sends signed-in users away from the login page to their home', () => {
    signIn('ADMIN');
    expect(String(run(guestGuard))).toBe('/admin/dashboard');
  });
});
