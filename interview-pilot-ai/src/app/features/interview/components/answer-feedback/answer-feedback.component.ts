import { Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { InterviewAnswer } from '../../models/interview.model';

/** The AI's evaluation of one answer: score, correctness, relevance, feedback, strengths and improvements. */
@Component({
  selector: 'app-answer-feedback',
  imports: [DecimalPipe],
  template: `
    @let a = answer();
    <div class="feedback">
      <div [class]="'score-ring ' + tone()" [style.--value]="a.score ?? 0"><span>{{ a.score ?? 0 | number: '1.0-0' }}</span></div>
      <div class="body">
        @if (a.feedback) { <p>{{ a.feedback }}</p> }
        @if (a.correctness) { <p><b>Correctness</b> · {{ a.correctness }}</p> }
        @if (a.relevance) { <p><b>Relevance</b> · {{ a.relevance }}</p> }
        <div class="lists">
          @if (a.strengths.length) {
            <div><b>Strengths</b><ul>@for (item of a.strengths; track $index) { <li>{{ item }}</li> }</ul></div>
          }
          @if (a.improvements.length) {
            <div><b>To improve</b><ul>@for (item of a.improvements; track $index) { <li>{{ item }}</li> }</ul></div>
          }
        </div>
      </div>
    </div>
  `,
  styles: `
    .feedback { display: flex; gap: 16px; }
    .body { flex: 1; min-width: 0; font-size: 14px; line-height: 1.6; }
    .body p { margin: 0 0 6px; }
    .lists { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; margin-top: 8px; }
    ul { margin: 4px 0 0; padding-left: 18px; color: var(--app-text-muted); }
    @media (max-width: 600px) { .feedback { flex-direction: column; } }
  `,
})
export class AnswerFeedbackComponent {
  readonly answer = input.required<InterviewAnswer>();
  readonly tone = computed(() => {
    const score = this.answer().score ?? 0;
    return score >= 75 ? 'good' : score >= 50 ? 'fair' : 'low';
  });
}
