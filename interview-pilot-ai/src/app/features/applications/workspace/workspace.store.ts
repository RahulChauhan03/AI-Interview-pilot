import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { saveDownloadedFile } from '../../../core/http/download';
import { NotificationService } from '../../../core/services/notification.service';
import { InterviewService } from '../../interview/services/interview.service';
import { JobDescriptionService } from '../../job-description/services/job-description.service';
import { ResumeStatus } from '../../resume/models/resume-status.enum';
import { ResumeSummary } from '../../resume/models/resume.model';
import { ResumeService } from '../../resume/services/resume.service';
import { ApplicationStatus, CoverLetter, JobApplication, TailoredResume, Workspace, statusInfo } from '../models/application.model';
import { ApplicationDownload, ApplicationService } from '../services/application.service';

/** What the AI (or a download) is doing right now; only one action runs at a time. */
export type WorkspaceTask = 'match' | 'resume' | 'letter' | 'interview' | 'status' | 'download';

export interface ProgressStep {
  label: string;
  done: boolean;
  detail: string;
  /** Workspace tab that completes this step. */
  tab: string;
}

const APPLIED_OR_LATER: ApplicationStatus[] = ['APPLIED', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED'];

/**
 * State of one job's application workspace, shared by its tabs (provided by WorkspaceComponent).
 * Everything shown comes from the backend; nothing is estimated in the browser.
 */
@Injectable()
export class WorkspaceStore {
  private readonly applications = inject(ApplicationService);
  private readonly jobService = inject(JobDescriptionService);
  private readonly resumeService = inject(ResumeService);
  private readonly interviewService = inject(InterviewService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  private jobId = 0;

  readonly data = signal<Workspace | null>(null);
  /** Parsed resumes only: the others cannot be matched or tailored. */
  readonly resumes = signal<ResumeSummary[]>([]);
  readonly resumeId = signal<number | null>(null);
  readonly tailoredResume = signal<TailoredResume | null>(null);
  readonly coverLetter = signal<CoverLetter | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly task = signal<WorkspaceTask | null>(null);

  readonly job = computed(() => this.data()?.job ?? null);
  readonly application = computed(() => this.data()?.application ?? null);
  readonly match = computed(() => this.data()?.latestMatch ?? null);
  readonly interviews = computed(() => this.data()?.interviews ?? []);
  readonly selectedResume = computed(() => this.resumes().find((resume) => resume.id === this.resumeId()) ?? null);
  readonly steps = computed<ProgressStep[]>(() => this.buildSteps());
  readonly completedSteps = computed(() => this.steps().filter((step) => step.done).length);

  load(jobId: number, preferredResumeId: number | null): void {
    this.jobId = jobId;
    this.loading.set(true);
    this.failed.set(false);
    this.tailoredResume.set(null);
    this.coverLetter.set(null);
    forkJoin({ workspace: this.applications.workspace(jobId), resumes: this.resumeService.refresh() }).pipe(
      switchMap(({ workspace, resumes }) => this.withDocuments(workspace).pipe(map((documents) => ({ workspace, resumes, documents })))),
    ).subscribe({
      next: ({ workspace, resumes, documents }) => {
        const parsed = resumes.filter((resume) => resume.status === ResumeStatus.Parsed);
        this.resumes.set(parsed);
        this.data.set(workspace);
        this.tailoredResume.set(documents.resume);
        this.coverLetter.set(documents.letter);
        const ids = parsed.map((resume) => resume.id);
        const preferred = [preferredResumeId, workspace.application?.resumeId, workspace.latestMatch?.resumeId]
          .find((id): id is number => id != null && ids.includes(id));
        this.resumeId.set(preferred ?? ids[0] ?? null);
        this.loading.set(false);
      },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  /** Refreshes the workspace data (match, application, interviews, skill gaps) after a change. */
  reload(): void {
    this.applications.workspace(this.jobId).subscribe({ next: (workspace) => this.data.set(workspace), error: () => undefined });
  }

  analyseMatch(): void {
    const resumeId = this.resumeId();
    if (resumeId === null || this.task()) return;
    this.task.set('match');
    this.jobService.match(this.jobId, resumeId).subscribe({
      next: (match) => {
        this.notifications.info(match.reused ? 'Nothing changed since the last analysis, so that result is shown.' : 'Match analysis finished.');
        this.finish();
      },
      error: () => this.task.set(null),
    });
  }

  generateTailoredResume(): void {
    const resumeId = this.resumeId();
    if (resumeId === null || this.task()) return;
    this.task.set('resume');
    this.ensureApplication(resumeId).pipe(
      switchMap((application) => this.applications.generateTailoredResume(application.id, resumeId)),
    ).subscribe({
      next: (resume) => {
        this.tailoredResume.set(resume);
        this.notifications.success('Your tailored resume is ready.');
        this.finish();
      },
      error: () => this.task.set(null),
    });
  }

  generateCoverLetter(): void {
    const resumeId = this.resumeId();
    if (resumeId === null || this.task()) return;
    this.task.set('letter');
    this.ensureApplication(resumeId).pipe(
      switchMap((application) => this.applications.generateCoverLetter(application.id, resumeId)),
    ).subscribe({
      next: (letter) => {
        this.coverLetter.set(letter);
        this.notifications.success('Your cover letter is ready.');
        this.finish();
      },
      error: () => this.task.set(null),
    });
  }

  saveCoverLetter(paragraphs: string[]): Observable<CoverLetter> {
    const application = this.application();
    if (!application) throw new Error('No application');
    return this.applications.updateCoverLetter(application.id, paragraphs).pipe(tap((letter) => this.coverLetter.set(letter)));
  }

  /** Creates the application (if needed) so the job shows up in the tracker. */
  track(): void {
    if (this.task()) return;
    this.task.set('status');
    this.ensureApplication(this.resumeId()).subscribe({
      next: () => { this.notifications.success('This job is now in your applications.'); this.finish(); },
      error: () => this.task.set(null),
    });
  }

  applicationChanged(application: JobApplication): void {
    const current = this.data();
    if (current) this.data.set({ ...current, application });
    this.reload();
  }

  applicationDeleted(): void {
    this.tailoredResume.set(null);
    this.coverLetter.set(null);
    const current = this.data();
    if (current) this.data.set({ ...current, application: null });
    this.reload();
  }

  download(file: ApplicationDownload): void {
    const application = this.application();
    if (!application || this.task()) return;
    this.task.set('download');
    this.applications.download(application.id, file).subscribe({
      next: (downloaded) => { saveDownloadedFile(downloaded); this.task.set(null); },
      error: () => this.task.set(null),
    });
  }

  /** Generates questions with the AI and opens the interview. */
  startInterview(questionCount: number): void {
    const resumeId = this.resumeId();
    if (resumeId === null || this.task()) return;
    this.task.set('interview');
    this.interviewService.create({ resumeId, jobDescriptionId: this.jobId, questionCount }).subscribe({
      next: (interview) => {
        this.task.set(null);
        if (interview.reused) this.notifications.info('You already have an interview in progress for this job, so it was opened.');
        this.router.navigate(['/app/interviews', interview.id]);
      },
      error: () => this.task.set(null),
    });
  }

  private finish(): void {
    this.task.set(null);
    this.reload();
  }

  private ensureApplication(resumeId: number | null): Observable<JobApplication> {
    const existing = this.application();
    return existing ? of(existing) : this.applications.createForJob(this.jobId, resumeId);
  }

  private withDocuments(workspace: Workspace): Observable<{ resume: TailoredResume | null; letter: CoverLetter | null }> {
    const application = workspace.application;
    return forkJoin({
      resume: application?.tailoredResumeAt ? this.applications.getTailoredResume(application.id) : of(null),
      letter: application?.coverLetterAt ? this.applications.getCoverLetter(application.id) : of(null),
    });
  }

  private buildSteps(): ProgressStep[] {
    const data = this.data();
    if (!data) return [];
    const { job, application, latestMatch, interviews } = data;
    const completed = interviews.filter((interview) => interview.status === 'COMPLETED');
    const best = completed.length ? Math.max(...completed.map((interview) => interview.overallScore ?? 0)) : null;
    const applied = !!application && (APPLIED_OR_LATER.includes(application.status) || !!application.appliedAt);
    return [
      { label: 'Job description saved', done: true, detail: `${job.jobTitle} at ${job.companyName}`, tab: 'overview' },
      { label: 'Resume matched', done: !!latestMatch, tab: 'match',
        detail: latestMatch ? `${Math.round(latestMatch.matchScore)}% match with ${latestMatch.resumeFileName}` : 'Compare your resume with this job' },
      { label: 'Tailored resume', done: !!application?.tailoredResumeAt, tab: 'tailored-resume',
        detail: application?.tailoredResumeAt ? 'Ready to download' : 'Emphasise what this job asks for' },
      { label: 'Cover letter', done: !!application?.coverLetterAt, tab: 'cover-letter',
        detail: application?.coverLetterAt ? 'Ready to download' : 'Written from your real experience' },
      { label: 'Interview practice', done: completed.length > 0, tab: 'interview-prep',
        detail: best !== null ? `${completed.length} completed, best score ${Math.round(best)}/100`
          : interviews.length ? 'Interview in progress' : 'Practise with questions for this job' },
      { label: 'Applied', done: applied, tab: 'application',
        detail: applied ? `Status: ${statusInfo(application!.status).label}` : 'Track your application status' },
    ];
  }
}
