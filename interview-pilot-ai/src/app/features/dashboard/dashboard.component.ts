import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../features/auth/models/user.model';

@Component({
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly auth = inject(AuthService);

  user: User | null = null;
  readonly actions = [
    {
      title: 'Upload resume',
      copy: 'Add your latest CV',
      icon: 'upload_file',
      color: 'blue',
      link: '/resumes',
    },
    {
      title: 'Job descriptions',
      copy: 'Save target roles',
      icon: 'assignment',
      color: 'purple',
      link: '/job-descriptions',
    },
    {
      title: 'Practice interview',
      copy: 'Start a new session',
      icon: 'forum',
      color: 'orange',
      link: '/interviews',
    },
    {
      title: 'My profile',
      copy: 'Manage your account',
      icon: 'person_outline',
      color: 'green',
      link: '/profile',
    },
  ];

  readonly activity = [
    {
      title: 'Resume activity',
      sub: 'Most recent upload or update',
      time: 'Today',
      icon: 'description',
    },
    {
      title: 'Interview prep',
      sub: 'Practice sessions and job context',
      time: 'Recently updated',
      icon: 'assignment',
    },
    {
      title: 'Profile status',
      sub: 'Account details synced with your workspace',
      time: 'Live',
      icon: 'forum',
    },
  ];

  ngOnInit(): void {
    this.user = JSON.parse(sessionStorage.getItem('current_user')!) || null;
    if (!this.user) {
      this.getUser();
    }

  }

  getUser() {
    this.auth.getProfile().subscribe({
      next: (profile) => {
        this.user = profile;
      },
      error: () => {
        this.user = this.auth.getCurrentUser();
      },
    });
  }

  get displayName(): string {
    return this.user?.firstName ? this.user.firstName : 'there';
  }

  get roleLabel(): string {
    return this.user?.role ?? 'Product Designer';
  }
}
