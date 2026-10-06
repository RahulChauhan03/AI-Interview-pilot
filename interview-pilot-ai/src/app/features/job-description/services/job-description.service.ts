import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/http/api-endpoints';
import { ApiService } from '../../../core/http/api.service';
import { JobDescription, JobDescriptionRequest, ResumeMatch } from '../models/job-description.model';

@Injectable({ providedIn: 'root' })
export class JobDescriptionService {
  private readonly api = inject(ApiService);
  private readonly baseUrl = API_ENDPOINTS.JOB_DESCRIPTIONS;

  list(): Observable<JobDescription[]> {
    return this.api.get<JobDescription[]>(this.baseUrl);
  }

  get(id: number): Observable<JobDescription> {
    return this.api.get<JobDescription>(`${this.baseUrl}/${id}`);
  }

  create(request: JobDescriptionRequest): Observable<JobDescription> {
    return this.api.post<JobDescription>(this.baseUrl, request);
  }

  update(id: number, request: JobDescriptionRequest): Observable<JobDescription> {
    return this.api.put<JobDescription>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}`);
  }

  /** Runs the AI comparison; with a local model this can take a minute or more. */
  match(id: number, resumeId: number): Observable<ResumeMatch> {
    return this.api.post<ResumeMatch>(`${this.baseUrl}/${id}/matches`, { resumeId });
  }

  matches(id: number): Observable<ResumeMatch[]> {
    return this.api.get<ResumeMatch[]>(`${this.baseUrl}/${id}/matches`);
  }
}
