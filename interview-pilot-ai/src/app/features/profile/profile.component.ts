import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, TitleCasePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { Profile, ProfileService } from './services/profile.service';

/** The signed-in user's profile. Only the name is editable; email (the login) and role are read-only. */
@Component({
  imports: [ReactiveFormsModule, RouterLink, DatePipe, TitleCasePipe, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  readonly profile = signal<Profile | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly saving = signal(false);
  readonly settingsLink = this.auth.isAdmin() ? '/admin/settings' : '/app/settings';

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.profileService.get().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.form.reset({ firstName: profile.firstName, lastName: profile.lastName });
        this.loading.set(false);
      },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  save(): void {
    if (this.form.invalid || this.saving()) { this.form.markAllAsTouched(); return; }
    const { firstName, lastName } = this.form.getRawValue();
    this.saving.set(true);
    this.profileService.update(firstName.trim(), lastName.trim()).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.form.reset({ firstName: profile.firstName, lastName: profile.lastName });
        this.auth.updateCurrentUser({ firstName: profile.firstName, lastName: profile.lastName });
        this.saving.set(false);
        this.notifications.success('Profile updated.');
      },
      error: () => this.saving.set(false),
    });
  }
}
