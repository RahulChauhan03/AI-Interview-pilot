import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTabsModule } from '@angular/material/tabs';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog.component';
import { JobDescriptionService } from '../../job-description/services/job-description.service';
import { matchLabel } from '../../matches/services/match.service';
import { statusInfo } from '../models/application.model';
import { WorkspaceStore } from './workspace.store';

/** One job description as an application workspace: header plus tabs (child routes) that share WorkspaceStore. */
@Component({
  imports: [RouterLink, RouterLinkActive, RouterOutlet, DecimalPipe, MatButtonModule, MatIconModule, MatMenuModule, MatTabsModule],
  providers: [WorkspaceStore],
  templateUrl: './workspace.component.html',
  styleUrl: './workspace.component.scss',
})
export class WorkspaceComponent {
  readonly store = inject(WorkspaceStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly jobService = inject(JobDescriptionService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly tabs = [
    { path: 'overview', label: 'Overview', icon: 'dashboard' },
    { path: 'match', label: 'Match', icon: 'join_inner' },
    { path: 'tailored-resume', label: 'Tailored resume', icon: 'description' },
    { path: 'cover-letter', label: 'Cover letter', icon: 'mail_outline' },
    { path: 'interview-prep', label: 'Interview prep', icon: 'forum' },
    { path: 'application', label: 'Application', icon: 'work_history' },
  ];
  readonly matchLabel = matchLabel;
  readonly statusInfo = statusInfo;

  constructor() {
    // The same component stays on screen when moving between workspaces, so follow the id.
    this.route.paramMap.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe((params) => {
      const resumeId = Number(this.route.snapshot.queryParamMap.get('resumeId'));
      this.store.load(Number(params.get('id')), resumeId || null);
    });
  }

  reload(): void {
    const job = this.store.job();
    this.store.load(job?.id ?? Number(this.route.snapshot.paramMap.get('id')), this.store.resumeId());
  }

  remove(): void {
    const job = this.store.job();
    if (!job) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete job description?', confirmLabel: 'Delete',
        message: `This removes "${job.jobTitle}" with its match results, application and generated documents.` },
    }).afterClosed().pipe(
      filter((confirmed): confirmed is true => confirmed === true),
      switchMap(() => this.jobService.delete(job.id)),
    ).subscribe({
      next: () => {
        this.notifications.success('Job description deleted.');
        this.router.navigateByUrl('/app/job-descriptions');
      },
    });
  }
}
