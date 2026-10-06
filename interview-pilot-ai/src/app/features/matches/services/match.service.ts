import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/http/api-endpoints';
import { ApiService } from '../../../core/http/api.service';
import { ResumeMatch } from '../../job-description/models/job-description.model';

/** Read access to all of the user's matches; new matches are created through JobDescriptionService.match(). */
@Injectable({ providedIn: 'root' })
export class MatchService {
  private readonly api = inject(ApiService);

  list(): Observable<ResumeMatch[]> {
    return this.api.get<ResumeMatch[]>(API_ENDPOINTS.MATCHES);
  }

  get(id: number): Observable<ResumeMatch> {
    return this.api.get<ResumeMatch>(`${API_ENDPOINTS.MATCHES}/${id}`);
  }
}

/** Shared wording for a 0-100 match score. */
export function matchLabel(score: number): { label: string; tone: 'good' | 'fair' | 'low' } {
  if (score >= 75) return { label: 'Strong match', tone: 'good' };
  if (score >= 50) return { label: 'Partial match', tone: 'fair' };
  return { label: 'Weak match', tone: 'low' };
}
