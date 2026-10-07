import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription, filter, switchMap, timer, withLatestFrom } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { ResumeMapper } from './resume.mapper';
import { ResumeStatus } from './models/resume-status.enum';
import { ResumeSummary } from './models/resume.model';
import { ResumeService } from './services/resume.service';
import { JobDescription } from '../job-description/models/job-description.model';
import { JobDescriptionService } from '../job-description/services/job-description.service';

@Component({
  imports: [MatTooltipModule, CommonModule, RouterLink, MatMenuModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatProgressSpinnerModule],
  templateUrl: './resume.component.html',
  styleUrl: './resume.component.scss',
})
export class ResumeComponent implements OnInit, OnDestroy {
  readonly resumeService = inject(ResumeService);
  readonly mapper = inject(ResumeMapper);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly jobService = inject(JobDescriptionService);

  /** Target jobs for "Use for application"; loaded when the menu is first opened. */
  readonly jobs = signal<JobDescription[] | null>(null);
  private pollingSubscription?: Subscription;
  readonly loaded = signal(false);
  readonly loadError = signal(false);

  readonly resumes$ = this.resumeService.resumes$;
  readonly parsedStatus = ResumeStatus.Parsed;
  selectedFile: File | null = null;
  uploading = false;
  uploadProgress = 0;
  uploadMessage = '';
  isDragging = false;

  ngOnInit(): void {
    this.reload();
    this.pollingSubscription = timer(5000, 5000).pipe(
      withLatestFrom(this.resumes$),
      filter(([, resumes]) => this.mapper.hasPendingProcessing(resumes)),
      switchMap(() => this.resumeService.refresh()),
    ).subscribe();
  }

  ngOnDestroy(): void { this.pollingSubscription?.unsubscribe(); }

  loadJobs(): void {
    if (this.jobs() === null) this.jobService.list().subscribe({ next: (jobs) => this.jobs.set(jobs), error: () => this.jobs.set([]) });
  }

  reload(): void {
    this.loadError.set(false);
    this.resumeService.refresh().subscribe({
      next: () => this.loaded.set(true),
      error: () => { this.loadError.set(true); this.loaded.set(true); },
    });
  }

  statusLabel(status: ResumeStatus): string {
    return { UPLOADED: 'Queued', PROCESSING: 'Processing', PARSED: 'Parsed', FAILED: 'Failed' }[status] ?? status;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectFile(input.files?.item(0) ?? null);
    input.value = '';
  }

  onDragOver(event: DragEvent): void { event.preventDefault(); this.isDragging = true; }
  onDragLeave(event: DragEvent): void { event.preventDefault(); this.isDragging = false; }
  onDrop(event: DragEvent): void { event.preventDefault(); this.isDragging = false; this.selectFile(event.dataTransfer?.files.item(0) ?? null); }

  upload(): void {
    if (!this.selectedFile || this.uploading) return;
    this.uploading = true;
    this.uploadProgress = 0;
    this.uploadMessage = 'Uploading…';
    this.resumeService.upload(this.selectedFile).subscribe({
      next: (event) => {
        if (event.kind === 'progress') {
          this.uploadProgress = event.percent;
          return;
        }
        this.uploadMessage = 'Processing…';
        this.selectedFile = null;
        this.resumeService.refresh().subscribe({
          next: () => { this.uploading = false; this.uploadMessage = ''; this.notifications.success('Resume uploaded. AI parsing has started.'); },
          error: () => { this.uploading = false; this.uploadMessage = ''; },
        });
      },
      error: () => { this.uploading = false; this.uploadMessage = ''; },
    });
  }

  view(resume: ResumeSummary): void {
    if (resume.status !== ResumeStatus.Parsed) { this.notifications.info('This resume is still being processed.'); return; }
    this.router.navigate(['/app/resumes', resume.id]);
  }

  delete(resume: ResumeSummary): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete resume?', message: `This removes "${resume.originalFileName}" from your library.`, confirmLabel: 'Delete' },
    }).afterClosed().pipe(
      filter((confirmed): confirmed is true => confirmed === true),
      switchMap(() => this.resumeService.delete(resume.id)),
      switchMap(() => this.resumeService.refresh()),
    ).subscribe({ next: () => this.notifications.success('Resume deleted.') });
  }

  private selectFile(file: File | null): void {
    if (!file) return;
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!extension || !['pdf', 'doc', 'docx'].includes(extension)) { this.notifications.error('Choose a PDF, DOC, or DOCX file.'); return; }
    if (file.size > 10 * 1024 * 1024) { this.notifications.error('Resume files must not exceed 10 MB.'); return; }
    this.selectedFile = file;
  }
}
