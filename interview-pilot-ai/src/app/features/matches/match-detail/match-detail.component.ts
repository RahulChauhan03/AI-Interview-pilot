import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ResumeMatch } from '../../job-description/models/job-description.model';
import { MatchService, matchLabel } from '../services/match.service';

@Component({
  imports: [RouterLink, DatePipe, DecimalPipe, MatButtonModule, MatIconModule],
  templateUrl: './match-detail.component.html',
  styleUrl: './match-detail.component.scss',
})
export class MatchDetailComponent implements OnInit {
  private readonly matchService = inject(MatchService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly match = signal<ResumeMatch | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly verdict = computed(() => matchLabel(this.match()?.matchScore ?? 0));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.matchService.get(this.id).subscribe({
      next: (match) => { this.match.set(match); this.loading.set(false); },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }
}
