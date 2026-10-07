import { Component, inject, input, output, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { confirmAction } from '../../../shared/confirm-dialog.component';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { saveDownloadedFile } from '../../../core/http/download';
import { NotificationService } from '../../../core/services/notification.service';
import { APPLICATION_STATUSES, ApplicationStatus, JobApplication, statusInfo } from '../models/application.model';
import { ApplicationDownload, ApplicationService } from '../services/application.service';

/** Status, documents and interview of one application; used in the workspace and on the application page. */
@Component({
  selector: 'app-application-panel',
  imports: [MatTooltipModule, RouterLink, DatePipe, DecimalPipe, MatButtonModule, MatFormFieldModule, MatIconModule, MatSelectModule],
  template: `
    @let app = application();
    @let ws = ['/app/job-descriptions', app.jobDescriptionId, 'workspace'];
    <div class="grid-2">
      <section class="card">
        <h2>Status</h2>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Application status</mat-label>
          <mat-select [value]="app.status" (selectionChange)="setStatus($event.value)" [disabled]="busy()">
            @for (s of statuses; track s.value) { <mat-option [value]="s.value">{{ s.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        @if (app.status === 'SAVED' || app.status === 'PREPARING') {
          <button mat-flat-button class="full-width" (click)="setStatus('APPLIED')" [disabled]="busy()"><mat-icon svgIcon="send"></mat-icon>Mark as applied</button>
        }
        <ul class="timeline">
          <li>Tracked since {{ app.createdAt | date: 'MMM d, y' }}</li>
          @if (app.appliedAt) { <li>Applied {{ app.appliedAt | date: 'MMM d, y' }}</li> }
          @if (app.updatedAt) { <li>Last updated {{ app.updatedAt | date: 'MMM d, y, h:mm a' }}</li> }
        </ul>
      </section>
      <section class="card">
        <h2>Documents</h2>
        <div class="doc-row">
          <mat-icon aria-hidden="true" svgIcon="tailored-resume"></mat-icon>
          <div><b>Tailored resume</b><span class="subtle">{{ app.tailoredResumeAt ? 'Generated ' + (app.tailoredResumeAt | date: 'MMM d, h:mm a') : 'Not generated yet' }}</span></div>
          @if (app.tailoredResumeAt) { <button mat-icon-button matTooltip="Download PDF" (click)="download('tailored-resume/pdf')" [disabled]="busy()" aria-label="Download tailored resume PDF"><mat-icon svgIcon="download"></mat-icon></button> }
          @else { <a mat-button [routerLink]="[ws[0], ws[1], ws[2], 'tailored-resume']">Create</a> }
        </div>
        <div class="doc-row">
          <mat-icon aria-hidden="true" svgIcon="cover-letter"></mat-icon>
          <div><b>Cover letter</b><span class="subtle">{{ app.coverLetterAt ? 'Updated ' + (app.coverLetterAt | date: 'MMM d, h:mm a') : 'Not generated yet' }}</span></div>
          @if (app.coverLetterAt) { <button mat-icon-button matTooltip="Download PDF" (click)="download('cover-letter/pdf')" [disabled]="busy()" aria-label="Download cover letter PDF"><mat-icon svgIcon="download"></mat-icon></button> }
          @else { <a mat-button [routerLink]="[ws[0], ws[1], ws[2], 'cover-letter']">Create</a> }
        </div>
        <button mat-stroked-button class="full-width" (click)="download('package')" [disabled]="busy() || (!app.tailoredResumeAt && !app.coverLetterAt)">
          <mat-icon svgIcon="package"></mat-icon>Download application package</button>
      </section>
      <section class="card">
        <h2>Resume and match</h2>
        @if (app.resumeId) { <p><a [routerLink]="['/app/resumes', app.resumeId]">{{ app.resumeFileName }}</a></p> } @else { <p class="muted">No resume chosen yet.</p> }
        @if (app.matchScore !== null) {
          <p>Match score: <a [routerLink]="['/app/matches', app.matchId]"><b>{{ app.matchScore | number: '1.0-0' }}%</b></a></p>
        } @else { <p class="muted">Not matched yet.</p> }
      </section>
      <section class="card">
        <h2>Interview practice</h2>
        @if (app.latestInterviewId) {
          <p>{{ app.interviewCount }} {{ app.interviewCount === 1 ? 'interview' : 'interviews' }}.
            <a [routerLink]="app.latestInterviewStatus === 'COMPLETED' ? ['/app/interviews', app.latestInterviewId, 'result'] : ['/app/interviews', app.latestInterviewId]">
              Latest: {{ app.latestInterviewStatus === 'COMPLETED' ? (app.latestInterviewScore | number: '1.0-0') + '/100' : 'in progress' }}</a></p>
        } @else { <p class="muted">No mock interview yet.</p> }
        <a mat-stroked-button [routerLink]="[ws[0], ws[1], ws[2], 'interview-prep']"><mat-icon svgIcon="interview"></mat-icon>Interview prep</a>
      </section>
    </div>
    <div class="form-actions">
      <button mat-stroked-button (click)="remove()" [disabled]="busy()"><mat-icon svgIcon="delete"></mat-icon>Delete application</button>
    </div>
  `,
  styles: `
    h2 { margin: 0 0 12px; font-size: 16px; }
    .timeline { margin: 12px 0 0; padding-left: 18px; font-size: 13px; color: var(--app-text-muted); }
    .doc-row { display: flex; align-items: center; gap: 12px; padding: 8px 0 12px; }
    .doc-row > div { display: grid; flex: 1; }
  `,
})
export class ApplicationPanelComponent {
  private readonly service = inject(ApplicationService);
  private readonly notifications = inject(NotificationService);
  readonly application = input.required<JobApplication>();
  readonly changed = output<JobApplication>();
  readonly deleted = output<void>();
  private readonly dialog = inject(MatDialog);
  readonly busy = signal(false);
  readonly statuses = APPLICATION_STATUSES;

  setStatus(status: ApplicationStatus): void {
    if (this.busy() || status === this.application().status) return;
    this.busy.set(true);
    this.service.updateStatus(this.application().id, status).subscribe({
      next: (updated) => { this.busy.set(false); this.notifications.success(`Status set to ${statusInfo(status).label}.`); this.changed.emit(updated); },
      error: () => this.busy.set(false),
    });
  }

  remove(): void {
    confirmAction(this.dialog, { title: 'Delete this application?', confirmLabel: 'Delete',
      message: 'Its status, tailored resume and cover letter will be removed. The job description, matches and interviews stay.' })
      .subscribe(() => {
        this.busy.set(true);
        this.service.delete(this.application().id).subscribe({
          next: () => { this.busy.set(false); this.notifications.success('Application deleted.'); this.deleted.emit(); },
          error: () => this.busy.set(false),
        });
      });
  }

  download(file: ApplicationDownload): void {
    this.busy.set(true);
    this.service.download(this.application().id, file).subscribe({
      next: (downloaded) => { saveDownloadedFile(downloaded); this.busy.set(false); },
      error: () => this.busy.set(false),
    });
  }
}
