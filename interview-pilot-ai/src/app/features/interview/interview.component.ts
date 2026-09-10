import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
@Component({
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './interview.component.html',
  styleUrl: './interview.component.scss',
})
export class InterviewComponent {
  sessions = [
    { role: 'Senior Product Designer', company: 'Figma', date: 'Jul 28, 2026', score: 82 },
    { role: 'Product Designer', company: 'Notion', date: 'Jul 19, 2026', score: 74 },
  ];
}
