import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, TitleCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';
import { ResumeStatus } from '../resume/models/resume-status.enum';
import { ResumeSummary } from '../resume/models/resume.model';
import { ResumeService } from '../resume/services/resume.service';
import { JobDescription, ResumeMatch } from '../job-description/models/job-description.model';
import { JobDescriptionService } from '../job-description/services/job-description.service';
import { MatchService, matchLabel } from '../matches/services/match.service';
import { Interview } from '../interview/models/interview.model';
import { InterviewService } from '../interview/services/interview.service';
import { JobApplication, SkillGaps, statusInfo } from '../applications/models/application.model';
import { ApplicationService } from '../applications/services/application.service';

interface NextStep {
  title: string;
  text: string;
  action: string;
  link: string;
  icon: string;
}

/** Candidate home: real counts, the most useful next step and the latest activity. Nothing is estimated. */
@Component({
  imports: [RouterLink, DatePipe, DecimalPipe, TitleCasePipe, MatButtonModule, MatIconModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly resumeService = inject(ResumeService);
  private readonly jobService = inject(JobDescriptionService);
  private readonly matchService = inject(MatchService);
  private readonly interviewService = inject(InterviewService);
  private readonly applicationService = inject(ApplicationService);

  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly resumes = signal<ResumeSummary[]>([]);
  readonly jobs = signal<JobDescription[]>([]);
  readonly matches = signal<ResumeMatch[]>([]);
  readonly interviews = signal<Interview[]>([]);
  readonly applications = signal<JobApplication[]>([]);
  readonly skillGaps = signal<SkillGaps | null>(null);
  readonly statusInfo = statusInfo;
  readonly recentApplications = computed(() => this.applications().slice(0, 5));
  readonly appliedCount = computed(() => this.applications().filter((a) => a.appliedAt).length);
  readonly averageMatch = computed(() => {
    const scores = this.matches().map((match) => match.matchScore);
    return scores.length ? scores.reduce((sum, score) => sum + score, 0) / scores.length : null;
  });

  readonly greeting = computed(() => {
    const hour = new Date().getHours();
    const part = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
    const name = this.auth.currentUser()?.firstName;
    return name ? `${part}, ${name}` : part;
  });
  readonly parsedResumes = computed(() => this.resumes().filter((resume) => resume.status === ResumeStatus.Parsed).length);
  readonly completedInterviews = computed(() => this.interviews().filter((interview) => interview.status === 'COMPLETED'));
  readonly averageScore = computed(() => {
    const scores = this.completedInterviews().map((interview) => interview.overallScore ?? 0);
    return scores.length ? scores.reduce((sum, score) => sum + score, 0) / scores.length : null;
  });
  readonly bestMatch = computed(() => (this.matches().length ? Math.max(...this.matches().map((match) => match.matchScore)) : null));
  readonly latestResume = computed(() => this.resumes()[0] ?? null);
  readonly latestMatch = computed(() => this.matches()[0] ?? null);
  readonly latestInterview = computed(() => this.interviews()[0] ?? null);
  readonly nextStep = computed<NextStep>(() => this.computeNextStep());
  readonly matchLabel = matchLabel;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    forkJoin({
      resumes: this.resumeService.refresh(),
      jobs: this.jobService.list(),
      matches: this.matchService.list(),
      interviews: this.interviewService.list(),
      applications: this.applicationService.list(),
      skillGaps: this.applicationService.skillGaps(),
    }).subscribe({
      next: ({ resumes, jobs, matches, interviews, applications, skillGaps }) => {
        this.applications.set(applications);
        this.skillGaps.set(skillGaps);
        this.resumes.set(resumes);
        this.jobs.set(jobs);
        this.matches.set(matches);
        this.interviews.set(interviews);
        this.loading.set(false);
      },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  private computeNextStep(): NextStep {
    const inProgress = this.interviews().find((interview) => interview.status === 'IN_PROGRESS');
    if (inProgress) {
      return { title: 'Finish your interview', text: `You have answered ${inProgress.answeredQuestions} of ${inProgress.totalQuestions} questions for ${inProgress.jobTitle}.`,
        action: 'Continue interview', link: `/app/interviews/${inProgress.id}`, icon: 'play' };
    }
    if (!this.resumes().length) {
      return { title: 'Upload your resume', text: 'Everything starts with your resume: it is used for matching and for interview questions.',
        action: 'Upload resume', link: '/app/resumes', icon: 'upload' };
    }
    if (!this.parsedResumes()) {
      return { title: 'Your resume is being analysed', text: 'This usually takes a minute or two. You can add a job description meanwhile.',
        action: 'Add job description', link: '/app/job-descriptions/new', icon: 'pending' };
    }
    if (!this.jobs().length) {
      return { title: 'Add a job you are targeting', text: 'Paste a job description to compare it with your resume and practise for it.',
        action: 'Add job description', link: '/app/job-descriptions/new', icon: 'job' };
    }
    if (!this.matches().length) {
      return { title: 'See how well you match', text: 'Compare your resume with a job description to find your strengths and gaps.',
        action: 'Match resume', link: `/app/job-descriptions/${this.jobs()[0].id}/workspace/match`, icon: 'match' };
    }
    const unprepared = this.matches().find((match) => !this.applications().some((a) => a.jobDescriptionId === match.jobDescriptionId && a.tailoredResumeAt));
    if (unprepared) {
      return { title: `Tailor your resume for ${unprepared.jobTitle}`, text: 'Create a version of your resume and a cover letter for this job, ready to download.',
        action: 'Tailor resume', link: `/app/job-descriptions/${unprepared.jobDescriptionId}/workspace/tailored-resume`, icon: 'ai' };
    }
    return { title: 'Practise an interview', text: 'Generate questions for your target job and get feedback on every answer.',
      action: 'Start interview', link: '/app/interviews', icon: 'interview' };
  }
}
