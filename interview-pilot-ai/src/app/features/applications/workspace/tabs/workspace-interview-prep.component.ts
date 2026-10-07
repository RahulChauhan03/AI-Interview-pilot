import { Component, computed, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { ResumePickerComponent } from '../../components/resume-picker.component';
import { WorkspaceStore } from '../workspace.store';

@Component({
  imports: [
    RouterLink,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressBarModule,
    MatSelectModule,
    ResumePickerComponent,
  ],
  template: `
    <div class="ws-layout">
      <div class="stack">
        <section class="card">
          <div class="card-header">
            <div>
              <h2>Likely topics</h2>
              <p>From your skill gaps for this job and your interview scores.</p>
            </div>
          </div>
          @if (topics().length) {
            <div class="tag-list">
              @for (t of topics(); track t.name) {
                <span [class]="'tag ' + t.tone">{{ t.name }}</span>
              }
            </div>
            <p class="subtle">
              Red: missing from your resume · orange: low interview scores · green: your strengths.
            </p>
          } @else {
            <p class="muted">Match your resume with this job to see the topics to prepare.</p>
          }
        </section>
        <section class="card">
          <div class="card-header">
            <div><h2>Questions from your last mock interview</h2></div>
          </div>
          @if (data()?.recentQuestions?.length) {
            <ol>
              @for (q of data()!.recentQuestions; track $index) {
                <li>{{ q }}</li>
              }
            </ol>
          } @else {
            <p class="muted">Start a mock interview to get questions generated for this job.</p>
          }
        </section>
        @if (data()?.skillGaps?.recommendations?.length) {
          <section class="card">
            <h2>What to work on</h2>
            <ul>
              @for (r of data()!.skillGaps.recommendations; track $index) {
                <li>{{ r }}</li>
              }
            </ul>
          </section>
        }
      </div>
      <div class="stack">
        <section class="card">
          <h2>Mock interview</h2>
          <div class="interview-form-fields">
            <app-resume-picker />
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Questions</mat-label>
              <mat-select [formControl]="count">
                @for (n of counts; track n) {
                  <mat-option [value]="n">{{ n }} questions</mat-option>
                }
              </mat-select>
            </mat-form-field>
          </div>
          <button
            mat-flat-button
            class="full-width"
            (click)="store.startInterview(count.value)"
            [disabled]="!!store.task() || store.resumeId() === null"
          >
            {{ store.task() === 'interview' ? 'Generating questions…' : 'Start mock interview' }}
          </button>
          @if (store.task() === 'interview') {
            <div class="inline-status">
              <mat-progress-bar mode="indeterminate"></mat-progress-bar>The AI is writing questions
              for this job. This can take a minute or two.
            </div>
          }
        </section>
        <section class="card">
          <h2>Past interviews</h2>
          @for (i of store.interviews(); track i.id) {
            <p>
              <a
                [routerLink]="
                  i.status === 'COMPLETED'
                    ? ['/app/interviews', i.id, 'result']
                    : ['/app/interviews', i.id]
                "
                >{{ i.startedAt | date: 'MMM d, h:mm a' }}</a
              >
              ·
              @if (i.status === 'COMPLETED') {
                <span class="pill success">{{ i.overallScore | number: '1.0-0' }}/100</span>
              } @else {
                <span class="pill info"
                  >{{ i.answeredQuestions }}/{{ i.totalQuestions }} answered</span
                >
              }
            </p>
          } @empty {
            <p class="muted">No interviews for this job yet.</p>
          }
        </section>
      </div>
    </div>
  `,
  styleUrl: './workspace-tabs.scss',
})
export class WorkspaceInterviewPrepComponent {
  readonly store = inject(WorkspaceStore);
  readonly data = this.store.data;
  readonly counts = [3, 5, 7, 10];
  readonly count = new FormControl(5, { nonNullable: true });
  readonly topics = computed(() => {
    const data = this.data();
    if (!data) return [];
    const tone = (name: string) =>
      data.skillGaps.missing.some((s) => s.name === name)
        ? 'danger'
        : data.skillGaps.developing.some((s) => s.name === name)
          ? 'warning'
          : data.skillGaps.strong.some((s) => s.name === name)
            ? 'success'
            : '';
    return data.topics.map((name) => ({ name, tone: tone(name) }));
  });
}
