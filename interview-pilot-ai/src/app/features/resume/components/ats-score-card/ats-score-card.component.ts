import { Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ParsedResume } from '../../models/parsed-resume.model';
import { ATS_SCORE_EXPLANATION, calculateAtsScore } from '../../ats/ats-score';

const RADIUS = 52;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;
let nextId = 0;

/** ATS friendliness of a parsed resume: animated score ring, rating and what to improve. */
@Component({
  selector: 'app-ats-score-card',
  imports: [MatIconModule, MatTooltipModule],
  templateUrl: './ats-score-card.component.html',
  styleUrl: './ats-score-card.component.scss',
})
export class AtsScoreCardComponent {
  readonly resume = input.required<ParsedResume>();

  readonly result = computed(() => calculateAtsScore(this.resume()));
  readonly improvements = computed(() => this.result().checks.filter((check) => check.tip));
  /** The number shown in the ring; counts up to the score. */
  readonly shown = signal(0);
  readonly explanation = ATS_SCORE_EXPLANATION;
  readonly radius = RADIUS;
  readonly circumference = CIRCUMFERENCE;
  readonly offset = computed(() => CIRCUMFERENCE * (1 - this.result().score / 100));
  readonly gradientId = `ats-ring-${nextId++}`;

  private frame = 0;

  constructor() {
    inject(DestroyRef).onDestroy(() => cancelAnimationFrame(this.frame));
    effect(() => this.countUp(this.result().score));
  }

  private countUp(target: number): void {
    cancelAnimationFrame(this.frame);
    const reduceMotion = typeof window.matchMedia === 'function' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion || typeof requestAnimationFrame !== 'function') { this.shown.set(target); return; }
    const start = performance.now();
    const step = (now: number) => {
      const progress = Math.min((now - start) / 900, 1);
      this.shown.set(Math.round(target * (1 - Math.pow(1 - progress, 3))));
      if (progress < 1) this.frame = requestAnimationFrame(step);
    };
    this.frame = requestAnimationFrame(step);
  }
}
