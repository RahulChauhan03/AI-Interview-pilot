import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/http/api-endpoints';
import { ApiService } from '../../../core/http/api.service';
import { CreateInterviewRequest, Interview, InterviewAnswer } from '../models/interview.model';

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private readonly api = inject(ApiService);
  private readonly baseUrl = API_ENDPOINTS.INTERVIEWS;

  list(): Observable<Interview[]> {
    return this.api.get<Interview[]>(this.baseUrl);
  }

  get(id: number): Observable<Interview> {
    return this.api.get<Interview>(`${this.baseUrl}/${id}`);
  }

  /** Generates the questions with AI; with a local model this can take a minute or more. */
  create(request: CreateInterviewRequest): Observable<Interview> {
    return this.api.post<Interview>(this.baseUrl, request);
  }

  /** Scores the answer with AI before returning. */
  answer(interviewId: number, questionId: number, answer: string): Observable<InterviewAnswer> {
    return this.api.post<InterviewAnswer>(`${this.baseUrl}/${interviewId}/questions/${questionId}/answer`, { answer });
  }

  /** Deletes the interview with its questions and answers. */
  delete(interviewId: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${interviewId}`);
  }

  complete(interviewId: number): Observable<Interview> {
    return this.api.post<Interview>(`${this.baseUrl}/${interviewId}/complete`, {});
  }
}
