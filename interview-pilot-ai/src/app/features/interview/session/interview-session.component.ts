import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog.component';
import { AnswerFeedbackComponent } from '../components/answer-feedback/answer-feedback.component';
import { Interview } from '../models/interview.model';
import { InterviewService } from '../services/interview.service';

/** Taking an interview: one question at a time. The backend holds the state; unsent drafts are kept per question. */
@Component({
  imports: [ReactiveFormsModule, RouterLink, TitleCasePipe, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule, AnswerFeedbackComponent],
  templateUrl: './interview-session.component.html',
  styleUrl: './interview-session.component.scss',
})
export class InterviewSessionComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly interviewService = inject(InterviewService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly interviewId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  private readonly drafts = new Map<number, string>();

  readonly interview = signal<Interview | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly submitting = signal(false);
  readonly finishing = signal(false);
  readonly index = signal(0);

  readonly answerControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(5000)],
  });

  readonly questions = computed(() => this.interview()?.questions ?? []);
  readonly current = computed(() => this.questions()[this.index()] ?? null);
  readonly progress = computed(() => {
    const interview = this.interview();
    return interview?.totalQuestions ? (interview.answeredQuestions / interview.totalQuestions) * 100 : 0;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.interviewService.get(this.interviewId).subscribe({
      next: (interview) => {
        if (interview.status === 'COMPLETED') {
          this.router.navigate(['/app/interviews', interview.id, 'result'], { replaceUrl: true });
          return;
        }
        this.interview.set(interview);
        const next = interview.questions?.findIndex((question) => question.id === interview.nextQuestionId) ?? -1;
        this.index.set(next >= 0 ? next : 0);
        this.loading.set(false);
      },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  go(index: number): void {
    const current = this.current();
    if (current && !current.answer) this.drafts.set(current.id, this.answerControl.value);
    this.index.set(index);
    const target = this.current();
    this.answerControl.reset(target ? (this.drafts.get(target.id) ?? '') : '');
  }

  submit(): void {
    const question = this.current();
    if (!question || this.submitting()) return;
    if (this.answerControl.invalid) { this.answerControl.markAsTouched(); return; }
    this.submitting.set(true);
    this.interviewService.answer(this.interviewId, question.id, this.answerControl.value.trim()).pipe(
      switchMap(() => this.interviewService.get(this.interviewId)),
    ).subscribe({
      next: (interview) => {
        this.drafts.delete(question.id);
        this.interview.set(interview);
        this.answerControl.reset('');
        this.submitting.set(false);
      },
      // The answer stays in the box so it can be sent again.
      error: () => this.submitting.set(false),
    });
  }

  goToNextUnanswered(): void {
    const next = this.questions().findIndex((question) => question.id === this.interview()?.nextQuestionId);
    if (next >= 0) this.go(next);
  }

  /** Asks first when questions are unanswered (they count as 0). */
  finish(): void {
    const interview = this.interview();
    if (!interview || this.finishing()) return;
    const unanswered = interview.totalQuestions - interview.answeredQuestions;
    if (unanswered === 0) { this.complete(); return; }
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Finish the interview?', message: `${unanswered} ${unanswered === 1 ? 'question is' : 'questions are'} unanswered and will count as 0.`, confirmLabel: 'Finish' },
    }).afterClosed().pipe(filter((confirmed): confirmed is true => confirmed === true)).subscribe(() => this.complete());
  }

  private complete(): void {
    this.finishing.set(true);
    this.interviewService.complete(this.interviewId).subscribe({
      next: () => {
        this.notifications.success('Interview finished.');
        this.router.navigate(['/app/interviews', this.interviewId, 'result']);
      },
      error: () => this.finishing.set(false),
    });
  }
}
