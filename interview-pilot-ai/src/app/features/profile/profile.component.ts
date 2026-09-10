import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { delay } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { NotificationService } from '../../core/services/notification.service';
import { LoadingService } from '../../core/services/loading.service';
import { ProfileService } from './services/profile.service';
import { ProfileDetails, ProfileUpdateRequest, ChangePasswordRequest } from './interfaces/profile.interface';

@Component({
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);
  private readonly notificationService = inject(NotificationService);
  private readonly loadingService = inject(LoadingService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly profileForm: FormGroup;
  readonly passwordForm: FormGroup;
  isLoading = true;
  isSubmitting = false;
  profile: ProfileDetails | null = null;
  passwordStrength: 'weak' | 'medium' | 'strong' = 'weak';

  constructor() {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: [''],
      location: [''],
      linkedInUrl: [''],
      githubUrl: [''],
      yearsOfExperience: [''],
      currentJobTitle: [''],
      aboutMe: [''],
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading = true;
    this.loadingService.start();

    this.profileService.getProfile().pipe(delay(2000)).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.patchProfileForm(profile);
        this.isLoading = false;
        this.loadingService.stop();
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.loadingService.stop();
        this.notificationService.error('Unable to load your profile right now.');
      },
    });
  }

  patchProfileForm(profile: ProfileDetails): void {
    this.profileForm.patchValue({
      firstName: profile.firstName ?? '',
      lastName: profile.lastName ?? '',
      email: profile.email ?? '',
      phoneNumber: profile.phoneNumber ?? '',
      location: profile.location ?? '',
      linkedInUrl: profile.linkedInUrl ?? '',
      githubUrl: profile.githubUrl ?? '',
      yearsOfExperience: profile.yearsOfExperience ?? '',
      currentJobTitle: profile.currentJobTitle ?? '',
      aboutMe: profile.aboutMe ?? '',
    }, { emitEvent: false });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const payload: ProfileUpdateRequest = this.profileForm.getRawValue();

    this.isSubmitting = true;
    this.loadingService.start();

    this.profileService.updateProfile(payload).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.patchProfileForm(profile);
        this.isSubmitting = false;
        this.loadingService.stop();
        this.cdr.detectChanges();
        this.notificationService.success('Profile updated successfully.');
      },
      error: () => {
        this.isSubmitting = false;
        this.loadingService.stop();
        this.notificationService.error('Profile update failed. Please try again.');
      },
    });
  }

  updatePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const payload: ChangePasswordRequest = this.passwordForm.getRawValue();

    if (payload.newPassword !== payload.confirmPassword) {
      this.notificationService.error('New password and confirmation must match.');
      return;
    }

    this.isSubmitting = true;
    this.loadingService.start();

    this.profileService.changePassword(payload).subscribe({
      next: () => {
        this.passwordForm.reset();
        this.isSubmitting = false;
        this.loadingService.stop();
        this.notificationService.success('Password updated successfully.');
      },
      error: () => {
        this.isSubmitting = false;
        this.loadingService.stop();
        this.notificationService.error('Password update failed.');
      },
    });
  }

  get hasProfileChanges(): boolean {
    return this.profileForm.dirty;
  }

  get displayName(): string {
    const first = this.profileForm.get('firstName')?.value?.toString().trim() || '';
    const last = this.profileForm.get('lastName')?.value?.toString().trim() || '';

    if (first || last) {
      return `${first} ${last}`.trim();
    }

    return this.profile?.firstName || this.profile?.lastName
      ? `${this.profile?.firstName ?? ''} ${this.profile?.lastName ?? ''}`.trim()
      : 'Your profile';
  }

  get displayEmail(): string {
    return this.profileForm.get('email')?.value?.toString().trim() || this.profile?.email || 'you@example.com';
  }

  get displayRole(): string {
    return this.profile?.role || 'Member';
  }

  get displayMemberSince(): string {
    return this.profile?.createdAt ? new Date(this.profile.createdAt).toLocaleDateString() : 'Recently joined';
  }

  get initials(): string {
    const first = this.profileForm.get('firstName')?.value?.toString()[0]?.toUpperCase() ?? '';
    const last = this.profileForm.get('lastName')?.value?.toString()[0]?.toUpperCase() ?? '';

    return (first + last) || this.profile?.firstName?.[0]?.toUpperCase() || this.profile?.lastName?.[0]?.toUpperCase() || 'IP';
  }

  checkPasswordStrength(): void {
    const value = this.passwordForm.get('newPassword')?.value || '';

    if (!value) {
      this.passwordStrength = 'weak';
      return;
    }

    const hasUpper = /[A-Z]/.test(value);
    const hasLower = /[a-z]/.test(value);
    const hasNumber = /\d/.test(value);
    const hasSpecial = /[^A-Za-z0-9]/.test(value);
    const score = [hasUpper, hasLower, hasNumber, hasSpecial].filter(Boolean).length;

    this.passwordStrength = score >= 3 ? 'strong' : score >= 2 ? 'medium' : 'weak';
  }

  get passwordStrengthLabel(): string {
    return this.passwordStrength === 'strong' ? 'Strong' : this.passwordStrength === 'medium' ? 'Medium' : 'Weak';
  }

  resetProfileForm(): void {
    if (this.profile) {
      this.patchProfileForm(this.profile);
    }
    this.profileForm.markAsPristine();
    this.profileForm.markAsUntouched();
  }
}
