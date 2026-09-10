import { Injectable } from '@angular/core';
import { ResumeStatus } from './models/resume-status.enum';
import { ResumeSummary } from './models/resume.model';

@Injectable({ providedIn: 'root' })
export class ResumeMapper {
  statusLabel(status: ResumeStatus): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  hasPendingProcessing(resumes: readonly ResumeSummary[]): boolean {
    return resumes.some((resume) => resume.status === ResumeStatus.Uploaded || resume.status === ResumeStatus.Processing);
  }

  formatFileSize(fileSize: number | null | undefined): string {
    if (fileSize === null || fileSize === undefined) return 'Size unavailable';
    if (fileSize < 1024 * 1024) return `${Math.max(1, Math.round(fileSize / 1024))} KB`;
    return `${(fileSize / (1024 * 1024)).toFixed(1)} MB`;
  }

  formatProcessingTime(milliseconds: number | null): string {
    return milliseconds === null ? 'Processing pending' : `${(milliseconds / 1000).toFixed(1)} sec`;
  }
}
