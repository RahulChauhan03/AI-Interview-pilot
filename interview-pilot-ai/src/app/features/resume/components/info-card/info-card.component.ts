import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Clipboard } from '@angular/cdk/clipboard';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({ selector: 'app-info-card', imports: [CommonModule, MatIconModule, MatButtonModule], templateUrl: './info-card.component.html', styleUrl: './info-card.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class InfoCardComponent {
  private readonly clipboard = new Clipboard();
  readonly label = input.required<string>(); readonly value = input<string | null>(); readonly icon = input<string>('info'); readonly href = input<string | null>(null);
  copy(): void { const value = this.value(); if (value) this.clipboard.copy(value); }
}
