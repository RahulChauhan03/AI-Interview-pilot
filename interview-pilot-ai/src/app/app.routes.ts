import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard } from './core/guards/auth.guard';
import { AuthService } from './core/services/auth.service';
import { AppShellComponent } from './shared/layout/app-shell.component';

const auth = () => import('./features/auth/auth.component').then((m) => m.AuthComponent);
const settings = () => import('./features/settings/settings.component').then((m) => m.SettingsComponent);
const profile = () => import('./features/profile/profile.component').then((m) => m.ProfileComponent);

/** "/" and unknown URLs: the user's home (admin console for administrators); the guards handle signed-out users. */
const home = () => inject(AuthService).homeUrl();

export const routes: Routes = [
  { path: 'login', loadComponent: auth, canActivate: [guestGuard], data: { mode: 'login' } },
  { path: 'register', loadComponent: auth, canActivate: [guestGuard], data: { mode: 'register' } },
  { path: 'forgot-password', loadComponent: auth, data: { mode: 'forgot-password' } },
  { path: 'reset-password', loadComponent: auth, data: { mode: 'reset-password' } },

  {
    path: 'app',
    component: AppShellComponent,
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    data: { area: 'app' },
    children: [
      { path: 'dashboard', title: 'Dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'resumes', title: 'Resumes', loadComponent: () => import('./features/resume/resume.component').then((m) => m.ResumeComponent) },
      { path: 'resumes/:id', title: 'Resume', loadComponent: () => import('./features/resume/resume-detail/resume-detail.component').then((m) => m.ResumeDetailComponent) },
      { path: 'job-descriptions', title: 'Job descriptions', loadComponent: () => import('./features/job-description/job-description.component').then((m) => m.JobDescriptionComponent) },
      { path: 'job-descriptions/new', title: 'New job description', loadComponent: () => import('./features/job-description/job-description-form/job-description-form.component').then((m) => m.JobDescriptionFormComponent) },
      { path: 'job-descriptions/:id/edit', title: 'Edit job description', loadComponent: () => import('./features/job-description/job-description-form/job-description-form.component').then((m) => m.JobDescriptionFormComponent) },
      {
        path: 'job-descriptions/:id/workspace',
        loadComponent: () => import('./features/applications/workspace/workspace.component').then((m) => m.WorkspaceComponent),
        children: [
          { path: 'overview', title: 'Application workspace', loadComponent: () => import('./features/applications/workspace/tabs/workspace-overview.component').then((m) => m.WorkspaceOverviewComponent) },
          { path: 'match', title: 'Match', loadComponent: () => import('./features/applications/workspace/tabs/workspace-match.component').then((m) => m.WorkspaceMatchComponent) },
          { path: 'tailored-resume', title: 'Tailored resume', loadComponent: () => import('./features/applications/workspace/tabs/workspace-tailored-resume.component').then((m) => m.WorkspaceTailoredResumeComponent) },
          { path: 'cover-letter', title: 'Cover letter', loadComponent: () => import('./features/applications/workspace/tabs/workspace-cover-letter.component').then((m) => m.WorkspaceCoverLetterComponent) },
          { path: 'interview-prep', title: 'Interview prep', loadComponent: () => import('./features/applications/workspace/tabs/workspace-interview-prep.component').then((m) => m.WorkspaceInterviewPrepComponent) },
          { path: 'application', title: 'Application', loadComponent: () => import('./features/applications/workspace/tabs/workspace-application.component').then((m) => m.WorkspaceApplicationComponent) },
          { path: '', pathMatch: 'full', redirectTo: 'overview' },
        ],
      },
      { path: 'job-descriptions/:id/tailored-resume', redirectTo: 'job-descriptions/:id/workspace/tailored-resume' },
      { path: 'job-descriptions/:id/cover-letter', redirectTo: 'job-descriptions/:id/workspace/cover-letter' },
      { path: 'job-descriptions/:id', pathMatch: 'full', redirectTo: 'job-descriptions/:id/workspace' },
      { path: 'applications', title: 'Applications', loadComponent: () => import('./features/applications/list/applications.component').then((m) => m.ApplicationsComponent) },
      { path: 'applications/:id', title: 'Application', loadComponent: () => import('./features/applications/detail/application-detail.component').then((m) => m.ApplicationDetailComponent) },
      { path: 'skill-gaps', title: 'Skill gaps', loadComponent: () => import('./features/skill-gaps/skill-gaps.component').then((m) => m.SkillGapsComponent) },
      { path: 'matches', title: 'Job matches', loadComponent: () => import('./features/matches/matches.component').then((m) => m.MatchesComponent) },
      { path: 'matches/:id', title: 'Job match', loadComponent: () => import('./features/matches/match-detail/match-detail.component').then((m) => m.MatchDetailComponent) },
      { path: 'interviews', title: 'Interviews', loadComponent: () => import('./features/interview/interview.component').then((m) => m.InterviewComponent) },
      { path: 'interviews/:id', title: 'Interview', loadComponent: () => import('./features/interview/session/interview-session.component').then((m) => m.InterviewSessionComponent) },
      { path: 'interviews/:id/result', title: 'Interview results', loadComponent: () => import('./features/interview/result/interview-result.component').then((m) => m.InterviewResultComponent) },
      { path: 'profile', title: 'Profile', loadComponent: profile },
      { path: 'settings', title: 'Settings', loadComponent: settings },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },

  {
    path: 'admin',
    component: AppShellComponent,
    canActivate: [authGuard, adminGuard],
    canActivateChild: [authGuard, adminGuard],
    data: { area: 'admin' },
    children: [
      { path: 'dashboard', title: 'Admin dashboard', loadComponent: () => import('./features/admin/admin-dashboard/admin-dashboard.component').then((m) => m.AdminDashboardComponent) },
      { path: 'users', title: 'Users', loadComponent: () => import('./features/admin/admin-users/admin-users.component').then((m) => m.AdminUsersComponent) },
      { path: 'activity', title: 'Activity', loadComponent: () => import('./features/admin/admin-activity/admin-activity.component').then((m) => m.AdminActivityComponent) },
      { path: 'profile', title: 'Profile', loadComponent: profile },
      { path: 'settings', title: 'Settings', loadComponent: settings },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },

  // Old URLs from before the /app and /admin areas existed.
  { path: 'dashboard', redirectTo: 'app/dashboard' },
  { path: 'resumes', redirectTo: 'app/resumes' },
  { path: 'job-descriptions', redirectTo: 'app/job-descriptions' },
  { path: 'interviews/:id', redirectTo: 'app/interviews/:id' },
  { path: 'interviews', redirectTo: 'app/interviews' },
  { path: 'profile', redirectTo: 'app/profile' },

  { path: '', pathMatch: 'full', redirectTo: home },
  { path: '**', redirectTo: home },
];
