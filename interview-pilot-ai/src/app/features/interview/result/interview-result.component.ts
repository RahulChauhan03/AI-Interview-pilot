import { MatDialog } from '@angular/material/dialog';
import { switchMap } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { confirmAction } from '../../../shared/confirm-dialog.component';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, TitleCasePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AnswerFeedbackComponent } from '../components/answer-feedback/answer-feedback.component';
import { Interview } from '../models/interview.model';
import { InterviewService } from '../services/interview.service';

interface CategoryScore {
  category: string;
  average: number;
}

/** Results of a finished interview. The summary is computed only from the scores and feedback the backend returned. */
@Component({
  imports: [RouterLink, DatePipe, DecimalPipe, TitleCasePipe, MatButtonModule, MatIconModule, AnswerFeedbackComponent],
  templateUrl: './interview-result.component.html',
  styleUrl: './interview-result.component.scss',
})
export class InterviewResultComponent implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly interviewService = inject(InterviewService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly interview = signal<Interview | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);

  readonly tone = computed(() => {
    const score = this.interview()?.overallScore ?? 0;
    return score >= 75 ? 'good' : score >= 50 ? 'fair' : 'low';
  });
  readonly verdict = computed(() => ({ good: 'Strong performance', fair: 'Solid, with room to improve', low: 'Needs more practice' })[this.tone()]);

  /** Average score per question category; unanswered questions count as 0, as in the overall score. */
  readonly categories = computed<CategoryScore[]>(() => {
    const totals = new Map<string, { sum: number; count: number }>();
    for (const question of this.interview()?.questions ?? []) {
      const total = totals.get(question.category) ?? { sum: 0, count: 0 };
      total.sum += question.answer?.score ?? 0;
      total.count += 1;
      totals.set(question.category, total);
    }
    return [...totals].map(([category, { sum, count }]) => ({ category, average: sum / count }));
  });
  readonly strongAreas = computed(() => this.categories().filter((category) => category.average >= 70));
  readonly weakAreas = computed(() => this.categories().filter((category) => category.average < 60));
  /** The AI's improvement notes from answers that scored below 70, without duplicates. */
  readonly preparation = computed(() => {
    const notes = (this.interview()?.questions ?? [])
      .filter((question) => question.answer && (question.answer.score ?? 0) < 70)
      .flatMap((question) => question.answer?.improvements ?? []);
    return [...new Set(notes)].slice(0, 6);
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.interviewService.get(this.id).subscribe({
      next: (interview) => { this.interview.set(interview); this.loading.set(false); },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  label(category: string): string {
    return category.replace(/_/g, ' ');
  }

  remove(): void {
    confirmAction(this.dialog, { title: 'Delete this interview?', confirmLabel: 'Delete',
      message: 'Its questions, answers and scores will be removed.' })
      .pipe(switchMap(() => this.interviewService.delete(this.id)))
      .subscribe({ next: () => { this.notifications.success('Interview deleted.'); this.router.navigateByUrl('/app/interviews'); } });
  }
}
