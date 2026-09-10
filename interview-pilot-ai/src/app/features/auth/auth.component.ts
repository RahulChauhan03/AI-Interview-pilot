import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';

const strongPassword = (c: AbstractControl) =>
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(c.value || '')
    ? null
    : { strongPassword: true };

@Component({
  selector: 'app-auth',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
  ],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.scss',
})
export class AuthComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  readonly mode = this.route.snapshot.data['mode'] ?? 'login';
  readonly isLogin = this.mode === 'login';
  readonly isForgotPassword = this.mode === 'forgot-password';
  readonly isResetPassword = this.mode === 'reset-password';
  showPassword = false;
  showRegisterPassword = false;
  showConfirmPassword = false;
  authError: string | null = null;
  successMessage: string | null = null;

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    rememberMe: [true],
  });

  readonly registerForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, strongPassword]],
    confirmPassword: ['', [Validators.required]],
  });

  readonly forgotPasswordForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  readonly resetPasswordForm = this.fb.nonNullable.group({
    password: ['', [Validators.required, strongPassword]],
    confirmPassword: ['', [Validators.required]],
  });

  submitLogin(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.authError = null;

    const request = {
      email: this.loginForm.value.email ?? '',
      password: this.loginForm.value.password ?? '',
      // rememberMe: this.loginForm.value.rememberMe ?? false,
    };

    this.auth.login(request).subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
      error: (error: unknown) => this.handleAuthError(error),
    });
  }

  private handleAuthError(error: unknown): void {
    const message = error instanceof Error ? error.message : 'An unexpected error occurred.';

    setTimeout(() => {
      this.authError = message;
      this.cdr.detectChanges();
    }, 0);
  }

  submitRegister(): void {
    if (
      this.registerForm.invalid ||
      this.registerForm.value.password !== this.registerForm.value.confirmPassword
    ) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.authError = null;

    const request = {
      firstName: this.registerForm.value.firstName ?? '',
      lastName: this.registerForm.value.lastName ?? '',
      email: this.registerForm.value.email ?? '',
      password: this.registerForm.value.password ?? '',
    };

    this.auth.register(request).subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
      error: (error: unknown) => this.handleAuthError(error),
    });
  }

  submitForgotPassword(): void {
    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.authError = null;
    this.auth.requestPasswordReset(this.forgotPasswordForm.getRawValue().email).subscribe({
      next: () => {
        this.successMessage = 'If an account exists for this email, a password reset link has been sent.';
      },
      error: (error: unknown) => this.handleAuthError(error),
    });
  }

  submitResetPassword(): void {
    if (
      this.resetPasswordForm.invalid ||
      this.resetPasswordForm.value.password !== this.resetPasswordForm.value.confirmPassword
    ) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.authError = 'This password reset link is invalid or incomplete.';
      return;
    }

    this.authError = null;
    this.auth.resetPassword(token, this.resetPasswordForm.getRawValue().password).subscribe({
      next: () => {
        this.successMessage = 'Your password has been reset. You can now sign in.';
        this.resetPasswordForm.disable();
      },
      error: (error: unknown) => this.handleAuthError(error),
    });
  }
}
