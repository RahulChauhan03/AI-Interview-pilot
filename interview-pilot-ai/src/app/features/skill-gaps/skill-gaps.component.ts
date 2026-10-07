import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SkillGaps } from '../applications/models/application.model';
import { ApplicationService } from '../applications/services/application.service';

/** Skill gaps across all of the user's jobs and interviews; only real gaps are shown. */
@Component({
  imports: [RouterLink, DecimalPipe, MatButtonModule, MatIconModule],
  template: `
    <header class="page-header">
      <div><h1>Skill gaps</h1><p>What your target jobs ask for compared with your resume and your interview scores.</p></div>
    </header>
    @if (loading()) {
      <div class="card"><span class="skeleton short"></span><span class="skeleton tall"></span></div>
    } @else if (failed()) {
      <div class="card error-state"><mat-icon svgIcon="offline"></mat-icon><h3>Skill gaps could not be loaded</h3><button mat-stroked-button (click)="load()">Retry</button></div>
    } @else if (gaps(); as g) {
      @if (empty()) {
        <section class="card empty-state"><mat-icon svgIcon="skills"></mat-icon><h3>No skill data yet</h3>
          <p>Match your resume with a job description and practise an interview to see your strengths and gaps.</p>
          <a mat-flat-button routerLink="/app/job-descriptions">Open job descriptions</a></section>
      } @else {
        <div class="columns">
          <section class="card">
            <h2><mat-icon aria-hidden="true" svgIcon="error"></mat-icon>Missing</h2>
            <p class="subtle">Required by your jobs but not on your resume.</p>
            @for (s of g.missing; track s.name) { <div class="row"><span class="tag danger">{{ s.name }}</span><span class="subtle">{{ s.jobs }} {{ s.jobs === 1 ? 'job' : 'jobs' }}</span></div> }
            @empty { <p class="muted">No missing skills found.</p> }
          </section>
          <section class="card">
            <h2><mat-icon aria-hidden="true" svgIcon="skills"></mat-icon>Developing</h2>
            <p class="subtle">Interview topics averaging below 70/100.</p>
            @for (s of g.developing; track s.name) { <div class="row"><span class="tag">{{ s.name }}</span><span class="subtle">{{ s.score | number: '1.0-0' }}/100</span></div> }
            @empty { <p class="muted">No weak interview topics yet.</p> }
          </section>
          <section class="card">
            <h2><mat-icon aria-hidden="true" svgIcon="success"></mat-icon>Strong</h2>
            <p class="subtle">On your resume and asked for, or scored 70+ in interviews.</p>
            @for (s of g.strong; track s.name) {
              <div class="row"><span class="tag success">{{ s.name }}</span><span class="subtle">{{ s.score !== null ? (s.score | number: '1.0-0') + '/100' : s.jobs + (s.jobs === 1 ? ' job' : ' jobs') }}</span></div>
            } @empty { <p class="muted">Nothing yet.</p> }
          </section>
        </div>
        @if (g.recommendations.length) {
          <section class="card"><h2><mat-icon aria-hidden="true" svgIcon="tip"></mat-icon>Recommendations</h2>
            <ul>@for (r of g.recommendations; track $index) { <li>{{ r }}</li> }</ul></section>
        }
      }
    }
  `,
  styles: `
    .columns { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-bottom: 16px; }
    .columns > .card { margin-top: 0; }
    h2 { display: flex; align-items: center; gap: 8px; margin: 0 0 4px; font-size: 16px; }
    .row { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 6px 0; }
    .row > .subtle { flex: none; white-space: nowrap; }
    @media (max-width: 900px) { .columns { grid-template-columns: 1fr; } }
  `,
})
export class SkillGapsComponent implements OnInit {
  private readonly service = inject(ApplicationService);
  readonly gaps = signal<SkillGaps | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly empty = computed(() => {
    const g = this.gaps();
    return !g || (!g.missing.length && !g.developing.length && !g.strong.length);
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.service.skillGaps().subscribe({
      next: (gaps) => { this.gaps.set(gaps); this.loading.set(false); },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }
}
