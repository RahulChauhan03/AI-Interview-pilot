import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ParsedResume } from '../../models/parsed-resume.model';

export interface ResumeNavigationItem { id: string; icon: string; label: string; }
@Component({ selector: 'app-resume-sidebar', imports: [CommonModule, MatIconModule], templateUrl: './resume-sidebar.component.html', styleUrl: './resume-sidebar.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class ResumeSidebarComponent {
  readonly parsed = input.required<ParsedResume>(); readonly activeSection = input.required<string>(); readonly navigate = output<string>();
  readonly items: ResumeNavigationItem[] = [{id:'personal',icon:'person',label:'Personal information'},{id:'summary',icon:'notes',label:'Professional summary'},{id:'experience',icon:'business_center',label:'Experience'},{id:'education',icon:'school',label:'Education'},{id:'projects',icon:'rocket_launch',label:'Projects'},{id:'technical-skills',icon:'code',label:'Technical skills'},{id:'soft-skills',icon:'handshake',label:'Soft skills'},{id:'certifications',icon:'workspace_premium',label:'Certifications'},{id:'languages',icon:'language',label:'Languages'},{id:'achievements',icon:'emoji_events',label:'Achievements'},{id:'keywords',icon:'key',label:'ATS keywords'}];
  initials(): string { const info = this.parsed().personalInformation; const name = String(info['firstName'] ?? info['first_name'] ?? '') + ' ' + String(info['lastName'] ?? info['last_name'] ?? ''); return name.trim().split(/\s+/).filter(Boolean).map((part) => part[0]).join('').slice(0, 2).toUpperCase() || 'AI'; }
  name(): string { const info = this.parsed().personalInformation; return [info['firstName'] ?? info['first_name'], info['lastName'] ?? info['last_name']].filter(Boolean).join(' ') || 'Resume candidate'; }
}
