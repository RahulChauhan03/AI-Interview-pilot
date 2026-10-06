import { Injectable, inject } from '@angular/core';
import { HttpEvent, HttpEventType, HttpResponse } from '@angular/common/http';
import { Observable, map, catchError, filter } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { ApiClientService } from './api-client.service';
import { ApiErrorHandlerService } from './api-error-handler.service';
import { HttpOptions } from './http-options';
import { DownloadedFile, fileNameFromContentDisposition } from './download';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly apiClient = inject(ApiClientService);
  private readonly errorHandler = inject(ApiErrorHandlerService);

  get<T>(url: string, options?: HttpOptions): Observable<T> {
    return this.apiClient.get<ApiResponse<T>>(url, options).pipe(
      map((response) => this.extractData(response)),
      catchError((error) => this.errorHandler.handle(error)),
    );
  }

  post<T>(url: string, body: unknown, options?: HttpOptions): Observable<T> {
    return this.apiClient.post<ApiResponse<T>>(url, body, options).pipe(
      map((response) => this.extractData(response)),
      catchError((error) => this.errorHandler.handle(error)),
    );
  }

  put<T>(url: string, body: unknown, options?: HttpOptions): Observable<T> {
    return this.apiClient.put<ApiResponse<T>>(url, body, options).pipe(
      map((response) => this.extractData(response)),
      catchError((error) => this.errorHandler.handle(error)),
    );
  }

  patch<T>(url: string, body: unknown, options?: HttpOptions): Observable<T> {
    return this.apiClient.patch<ApiResponse<T>>(url, body, options).pipe(
      map((response) => this.extractData(response)),
      catchError((error) => this.errorHandler.handle(error)),
    );
  }

  delete<T>(url: string, options?: HttpOptions): Observable<T> {
    return this.apiClient.delete<ApiResponse<T>>(url, options).pipe(
      map((response) => this.extractData(response)),
      catchError((error) => this.errorHandler.handle(error)),
    );
  }

  upload<T>(url: string, formData: FormData, options?: HttpOptions): Observable<T> {
    return this.apiClient.upload<ApiResponse<T>>(url, formData, options).pipe(
      map((response) => this.extractData(response)),
      catchError((error) => this.errorHandler.handle(error)),
    );
  }

  uploadWithProgress<T>(url: string, formData: FormData, options?: HttpOptions): Observable<ApiUploadEvent<T>> {
    return this.apiClient.uploadWithProgress<ApiResponse<T>>(url, formData, options).pipe(
      filter((event): event is HttpEvent<ApiResponse<T>> =>
        event.type === HttpEventType.UploadProgress || event.type === HttpEventType.Response,
      ),
      map((event) => {
        if (event.type === HttpEventType.UploadProgress) {
          return { kind: 'progress' as const, percent: event.total ? Math.round((event.loaded / event.total) * 100) : 0 };
        }
        const response = event as HttpResponse<ApiResponse<T>>;
        return { kind: 'response' as const, data: this.extractData(response.body as ApiResponse<T>) };
      }),
      catchError((error) => this.errorHandler.handle(error)),
    );
  }

  /** A generated file (PDF, ZIP) with the file name the server chose. */
  download(url: string, options?: HttpOptions): Observable<DownloadedFile> {
    return this.apiClient.download(url, options).pipe(
      map((response) => ({
        blob: response.body ?? new Blob(),
        fileName: fileNameFromContentDisposition(response.headers.get('Content-Disposition')) ?? 'download',
      })),
      catchError((error) => this.errorHandler.handle(error)),
    );
  }

  private extractData<T>(response: ApiResponse<T>): T {
    return response.data;
  }
}

export type ApiUploadEvent<T> =
  | { kind: 'progress'; percent: number }
  | { kind: 'response'; data: T };
