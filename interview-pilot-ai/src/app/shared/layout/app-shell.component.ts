import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';
@Component({
  selector: 'app-shell',
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  mobile = signal(typeof window !== 'undefined' && window.innerWidth < 760);
  nav = [
    { label: 'Dashboard', icon: 'grid_view', link: '/dashboard' },
    { label: 'Resumes', icon: 'description', link: '/resumes' },
    { label: 'Job descriptions', icon: 'assignment', link: '/job-descriptions' },
    { label: 'Interviews', icon: 'forum', link: '/interviews' },
    { label: 'Profile', icon: 'person_outline', link: '/profile' },
  ];
  logout(): void {
   this.auth.logout();
    this.router.navigate(['/auth/login']);
  }
}
