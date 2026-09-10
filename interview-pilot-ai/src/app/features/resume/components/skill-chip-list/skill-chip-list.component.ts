import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
@Component({ selector: 'app-skill-chip-list', imports: [CommonModule, MatChipsModule], templateUrl: './skill-chip-list.component.html', styleUrl: './skill-chip-list.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class SkillChipListComponent { readonly skills = input.required<readonly string[]>(); readonly tone = input<'blue' | 'green' | 'gold' | 'purple'>('blue'); }
