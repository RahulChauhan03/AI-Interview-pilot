import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { AppShellComponent } from './shared/layout/app-shell.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/auth.component').then(
        (m) => m.AuthComponent
      ),
    data: { mode: 'login' },
  },

  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/auth.component').then(
        (m) => m.AuthComponent
      ),
    data: { mode: 'register' },
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/auth.component').then(
        (m) => m.AuthComponent
      ),
    data: { mode: 'forgot-password' },
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/auth.component').then(
        (m) => m.AuthComponent
      ),
    data: { mode: 'reset-password' },
  },

  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    canActivateChild: [authGuard],

    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent
          ),
      },

      {
        path: 'resumes',
        loadComponent: () =>
          import('./features/resume/resume.component').then(
            (m) => m.ResumeComponent
          ),
      },

      {
        path: 'job-descriptions',
        loadComponent: () =>
          import('./features/job-description/job-description.component').then(
            (m) => m.JobDescriptionComponent
          ),
      },

      {
        path: 'interviews',
        loadComponent: () =>
          import('./features/interview/interview.component').then(
            (m) => m.InterviewComponent
          ),
      },

      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then(
            (m) => m.ProfileComponent
          ),
      },

      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
    ],
  },

  {
    path: '**',
    redirectTo: 'login',
  },
];
