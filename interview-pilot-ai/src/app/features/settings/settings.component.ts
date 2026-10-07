import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { ThemeMode, ThemeService } from '../../core/services/theme.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';

@Component({
  imports: [RouterLink, MatButtonModule, MatButtonToggleModule, MatIconModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent {
  readonly theme = inject(ThemeService);
  readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly sendingReset = signal(false);
  readonly profileLink = this.auth.isAdmin() ? '/admin/profile' : '/app/profile';
  readonly modes: { value: ThemeMode; label: string; icon: string }[] = [
    { value: 'light', label: 'Light', icon: 'sun' },
    { value: 'dark', label: 'Dark', icon: 'moon' },
    { value: 'system', label: 'System', icon: 'monitor' },
  ];

  /** Uses the existing password-reset flow: a one-time link is emailed to the account's address. */
  sendPasswordReset(): void {
    const email = this.auth.currentUser()?.email;
    if (!email || this.sendingReset()) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Change your password?', message: `We will email a password reset link to ${email}.`, confirmLabel: 'Send link' },
    }).afterClosed().pipe(
      filter((confirmed): confirmed is true => confirmed === true),
      switchMap(() => { this.sendingReset.set(true); return this.auth.requestPasswordReset(email); }),
    ).subscribe({
      next: () => { this.sendingReset.set(false); this.notifications.success('Check your email for the reset link.'); },
      error: () => this.sendingReset.set(false),
    });
  }
}
