import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApplicationPanelComponent } from '../components/application-panel.component';
import { JobApplication, statusInfo } from '../models/application.model';
import { ApplicationService } from '../services/application.service';

@Component({
  imports: [RouterLink, MatButtonModule, MatIconModule, ApplicationPanelComponent],
  template: `
    <a class="back-link" routerLink="/app/applications"><mat-icon svgIcon="arrow-left"></mat-icon>Applications</a>
    @if (loading()) {
      <div class="card"><span class="skeleton short"></span><span class="skeleton tall"></span></div>
    } @else if (failed()) {
      <div class="card error-state"><mat-icon svgIcon="error"></mat-icon><h3>This application could not be loaded</h3><button mat-stroked-button (click)="load()">Retry</button></div>
    } @else if (application(); as app) {
      <header class="page-header">
        <div><p class="eyebrow">{{ app.companyName }}</p><h1>{{ app.jobTitle }}</h1>
          <span [class]="'pill ' + statusInfo(app.status).tone">{{ statusInfo(app.status).label }}</span></div>
        <a mat-flat-button [routerLink]="['/app/job-descriptions', app.jobDescriptionId, 'workspace']"><mat-icon svgIcon="dashboard"></mat-icon>Open workspace</a>
      </header>
      <app-application-panel [application]="app" (changed)="application.set($event)" (deleted)="router.navigateByUrl('/app/applications')" />
    }
  `,
})
export class ApplicationDetailComponent implements OnInit {
  private readonly service = inject(ApplicationService);
  readonly router = inject(Router);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly application = signal<JobApplication | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly statusInfo = statusInfo;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.service.get(this.id).subscribe({
      next: (application) => { this.application.set(application); this.loading.set(false); },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }
}
