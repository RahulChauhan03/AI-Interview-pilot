import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { WorkspaceStore } from '../workspace.store';

@Component({
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="ws-layout">
      <div class="stack">
        <section class="card">
          <div class="card-header">
            <div><h2>Application progress</h2><p>{{ store.completedSteps() }} of {{ store.steps().length }} steps done</p></div>
          </div>
          <mat-progress-bar mode="determinate" [value]="(store.completedSteps() / store.steps().length) * 100"></mat-progress-bar>
          <ol class="steps">
            @for (step of store.steps(); track step.label) {
              <li [class.done]="step.done">
                <mat-icon aria-hidden="true" [svgIcon]="step.done ? 'success' : 'circle'"></mat-icon>
                <div><b>{{ step.label }}</b><span class="subtle">{{ step.detail }}</span></div>
                @if (step.tab !== 'overview') { <a mat-button [routerLink]="['..', step.tab]">{{ step.done ? 'View' : 'Start' }}</a> }
              </li>
            }
          </ol>
        </section>
        <section class="card">
          <div class="card-header"><h2>Job description</h2></div>
          <p class="prewrap jd">{{ store.job()?.jobDescription }}</p>
        </section>
      </div>
      <div class="stack">
        @if (next(); as step) {
          <section class="card">
            <p class="eyebrow">Next step</p>
            <h2>{{ step.label }}</h2>
            <p class="muted">{{ step.detail }}</p>
            <a mat-flat-button [routerLink]="['..', step.tab]">Continue</a>
          </section>
        }
        <section class="card">
          <div class="card-header"><h2>Skill snapshot</h2></div>
          @if (gaps().missing.length || gaps().strong.length) {
            @if (gaps().strong.length) {
              <p class="subtle">Strong</p>
              <div class="tag-list">@for (s of gaps().strong; track s.name) { <span class="tag success">{{ s.name }}</span> }</div>
            }
            @if (gaps().missing.length) {
              <p class="subtle">Missing from your resume</p>
              <div class="tag-list">@for (s of gaps().missing; track s.name) { <span class="tag danger">{{ s.name }}</span> }</div>
            }
          } @else {
            <p class="muted">Match your resume with this job to see your strengths and gaps.</p>
          }
        </section>
      </div>
    </div>
  `,
  styleUrl: './workspace-tabs.scss',
})
export class WorkspaceOverviewComponent {
  readonly store = inject(WorkspaceStore);
  readonly next = computed(() => this.store.steps().find((step) => !step.done) ?? null);
  readonly gaps = computed(() => this.store.data()?.skillGaps ?? { strong: [], developing: [], missing: [], recommendations: [] });
}
