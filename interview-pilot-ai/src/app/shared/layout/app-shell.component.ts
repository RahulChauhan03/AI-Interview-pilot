import { Component, OnInit, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { map } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

interface NavItem {
  label: string;
  icon: string;
  link: string;
}

const APP_NAV: NavItem[] = [
  { label: 'Dashboard', icon: 'dashboard', link: '/app/dashboard' },
  { label: 'Resumes', icon: 'resume', link: '/app/resumes' },
  { label: 'Job descriptions', icon: 'job', link: '/app/job-descriptions' },
  { label: 'Applications', icon: 'application', link: '/app/applications' },
  { label: 'Job matches', icon: 'match', link: '/app/matches' },
  { label: 'Interviews', icon: 'interview', link: '/app/interviews' },
  { label: 'Skill gaps', icon: 'skills', link: '/app/skill-gaps' },
];
const APP_ACCOUNT_NAV: NavItem[] = [
  { label: 'Profile', icon: 'profile', link: '/app/profile' },
  { label: 'Settings', icon: 'settings', link: '/app/settings' },
];
const ADMIN_NAV: NavItem[] = [
  { label: 'Dashboard', icon: 'analytics', link: '/admin/dashboard' },
  { label: 'Users', icon: 'users', link: '/admin/users' },
  { label: 'Activity', icon: 'history', link: '/admin/activity' },
];
const ADMIN_ACCOUNT_NAV: NavItem[] = [
  { label: 'Profile', icon: 'profile', link: '/admin/profile' },
  { label: 'Settings', icon: 'settings', link: '/admin/settings' },
];

/** Layout for both areas: route data `area` ('app' | 'admin') selects the navigation. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatButtonModule, MatIconModule, MatMenuModule, MatDividerModule],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  private readonly area = inject(ActivatedRoute).snapshot.data['area'] as 'app' | 'admin';

  readonly isAdminArea = this.area === 'admin';
  readonly nav = this.isAdminArea ? ADMIN_NAV : APP_NAV;
  readonly accountNav = this.isAdminArea ? ADMIN_ACCOUNT_NAV : APP_ACCOUNT_NAV;
  readonly profileLink = this.accountNav[0].link;
  readonly settingsLink = this.accountNav[1].link;

  readonly mobile = toSignal(
    inject(BreakpointObserver).observe('(max-width: 900px)').pipe(map((state) => state.matches)),
    { initialValue: window.innerWidth <= 900 },
  );
  readonly email = computed(() => this.auth.currentUser()?.email ?? '');

  ngOnInit(): void {
    // The login response has no name, so load it once for the header.
    if (!this.auth.currentUser()?.firstName) {
      this.auth.getProfile().subscribe({ error: () => undefined });
    }
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
