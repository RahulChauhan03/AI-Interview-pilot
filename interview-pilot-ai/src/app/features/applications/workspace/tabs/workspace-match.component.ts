import { Component, computed, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { matchLabel } from '../../../matches/services/match.service';
import { ResumePickerComponent } from '../../components/resume-picker.component';
import { WorkspaceStore } from '../workspace.store';

@Component({
  imports: [RouterLink, DatePipe, DecimalPipe, MatButtonModule, MatIconModule, MatProgressBarModule, ResumePickerComponent],
  template: `
    <section class="card">
      <div class="card-header"><div><h2>Compare your resume with this job</h2><p>The AI lists the requirements you meet and the gaps.</p></div></div>
      <div class="action-row">
        <app-resume-picker />
        <button mat-flat-button (click)="store.analyseMatch()" [disabled]="!!store.task() || store.resumeId() === null">
          {{ store.task() === 'match' ? 'Analysing…' : match() ? 'Re-analyse' : 'Analyse match' }}</button>
      </div>
      @if (store.task() === 'match') {
        <div class="inline-status"><mat-progress-bar mode="indeterminate"></mat-progress-bar>The AI is comparing your resume with this job. This can take a minute or two.</div>
      }
    </section>

    @if (match(); as m) {
      <section class="card verdict">
        <div [class]="'score-ring large ' + verdict().tone" [style.--value]="m.matchScore"><span>{{ m.matchScore | number: '1.0-0' }}%</span></div>
        <div>
          <h2>{{ verdict().label }}</h2>
          <p class="muted">{{ m.resumeFileName }} · analysed {{ m.createdAt | date: 'MMM d, y, h:mm a' }}</p>
          <div class="form-actions start">
            <a mat-flat-button routerLink="../tailored-resume"><mat-icon svgIcon="ai"></mat-icon>Customize resume</a>
            <a mat-stroked-button [routerLink]="['/app/matches', m.id]">Full details</a>
          </div>
        </div>
      </section>
      <div class="grid-2">
        <section class="card">
          <h2>Requirements you meet</h2>
          @if (m.strengths.length) { <div class="tag-list">@for (s of m.strengths; track $index) { <span class="tag">{{ s }}</span> }</div> }
          @else { <p class="muted">None identified.</p> }
        </section>
        <section class="card">
          <h2>Gaps</h2>
          @if (m.missingSkills.length) { <div class="tag-list">@for (s of m.missingSkills; track $index) { <span class="tag danger">{{ s }}</span> }</div> }
          @else { <p class="muted">No missing skills identified.</p> }
        </section>
      </div>
      @if (m.recommendations.length) {
        <section class="card"><h2>Recommendations</h2><ul>@for (r of m.recommendations; track $index) { <li>{{ r }}</li> }</ul></section>
      }
    } @else if (store.task() !== 'match') {
      <section class="card empty-state">
        <mat-icon svgIcon="match"></mat-icon><h3>Not matched yet</h3><p>Choose a resume and run the analysis to see your score.</p>
      </section>
    }
  `,
  styleUrl: './workspace-tabs.scss',
})
export class WorkspaceMatchComponent {
  readonly store = inject(WorkspaceStore);
  readonly match = this.store.match;
  readonly verdict = computed(() => matchLabel(this.match()?.matchScore ?? 0));
}
