import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/http/api-endpoints';
import { ApiService } from '../../../core/http/api.service';
import { DownloadedFile } from '../../../core/http/download';
import { ApplicationStatus, CoverLetter, JobApplication, SkillGaps, TailoredResume, Workspace } from '../models/application.model';

export type ApplicationDownload = 'tailored-resume/pdf' | 'cover-letter/pdf' | 'package';

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  private readonly api = inject(ApiService);
  private readonly baseUrl = API_ENDPOINTS.APPLICATIONS;

  workspace(jobDescriptionId: number): Observable<Workspace> {
    return this.api.get<Workspace>(`${API_ENDPOINTS.JOB_DESCRIPTIONS}/${jobDescriptionId}/workspace`);
  }

  /** Returns the job's application, creating it the first time. */
  createForJob(jobDescriptionId: number, resumeId: number | null): Observable<JobApplication> {
    return this.api.post<JobApplication>(`${API_ENDPOINTS.JOB_DESCRIPTIONS}/${jobDescriptionId}/application`, { resumeId });
  }

  list(): Observable<JobApplication[]> {
    return this.api.get<JobApplication[]>(this.baseUrl);
  }

  get(id: number): Observable<JobApplication> {
    return this.api.get<JobApplication>(`${this.baseUrl}/${id}`);
  }

  /** Deletes the application and its generated documents; the job, matches and interviews stay. */
  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, status: ApplicationStatus): Observable<JobApplication> {
    return this.api.patch<JobApplication>(`${this.baseUrl}/${id}/status`, { status });
  }

  /** AI generation; with a local model this can take a few minutes. */
  generateTailoredResume(id: number, resumeId: number | null): Observable<TailoredResume> {
    return this.api.post<TailoredResume>(`${this.baseUrl}/${id}/tailored-resume`, { resumeId });
  }

  getTailoredResume(id: number): Observable<TailoredResume> {
    return this.api.get<TailoredResume>(`${this.baseUrl}/${id}/tailored-resume`);
  }

  /** AI generation; with a local model this can take a few minutes. */
  generateCoverLetter(id: number, resumeId: number | null): Observable<CoverLetter> {
    return this.api.post<CoverLetter>(`${this.baseUrl}/${id}/cover-letter`, { resumeId });
  }

  getCoverLetter(id: number): Observable<CoverLetter> {
    return this.api.get<CoverLetter>(`${this.baseUrl}/${id}/cover-letter`);
  }

  updateCoverLetter(id: number, paragraphs: string[]): Observable<CoverLetter> {
    return this.api.put<CoverLetter>(`${this.baseUrl}/${id}/cover-letter`, { paragraphs });
  }

  download(id: number, file: ApplicationDownload): Observable<DownloadedFile> {
    return this.api.download(`${this.baseUrl}/${id}/${file}`);
  }

  skillGaps(): Observable<SkillGaps> {
    return this.api.get<SkillGaps>(API_ENDPOINTS.SKILL_GAPS);
  }
}
