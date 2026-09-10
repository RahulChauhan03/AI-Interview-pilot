import { HttpClient, HttpEvent, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HttpOptions } from './http-options';

@Injectable({ providedIn: 'root' })
export class ApiClientService {
  private readonly http = inject(HttpClient);

  get<T>(url: string, options?: HttpOptions): Observable<T> {
    return this.http.get<T>(this.normalizeUrl(url), this.buildOptions(options));
  }

  post<T>(url: string, body: unknown, options?: HttpOptions): Observable<T> {
    return this.http.post<T>(this.normalizeUrl(url), body, this.buildOptions(options));
  }

  put<T>(url: string, body: unknown, options?: HttpOptions): Observable<T> {
    return this.http.put<T>(this.normalizeUrl(url), body, this.buildOptions(options));
  }

  patch<T>(url: string, body: unknown, options?: HttpOptions): Observable<T> {
    return this.http.patch<T>(this.normalizeUrl(url), body, this.buildOptions(options));
  }

  delete<T>(url: string, options?: HttpOptions): Observable<T> {
    return this.http.delete<T>(this.normalizeUrl(url), this.buildOptions(options));
  }

  upload<T>(url: string, formData: FormData, options?: HttpOptions): Observable<T> {
    return this.http.post<T>(this.normalizeUrl(url), formData, this.buildOptions(options));
  }

  uploadWithProgress<T>(url: string, formData: FormData, options?: HttpOptions): Observable<HttpEvent<T>> {
    return this.http.post<T>(this.normalizeUrl(url), formData, {
      ...this.buildOptions(options),
      observe: 'events',
      reportProgress: true,
    });
  }

  download(url: string, options?: HttpOptions): Observable<HttpResponse<Blob>> {
    return this.http.get(this.normalizeUrl(url), {
      ...this.buildOptions(options),
      observe: 'response',
      responseType: 'blob',
    });
  }

  private normalizeUrl(url: string): string {
    if (!url) {
      return url;
    }

    if (url.startsWith('http')) {
      return url;
    }

    const normalizedPath = url.startsWith('/') ? url.slice(1) : url;
    return `${environment.apiUrl}/${normalizedPath}`;
  }

  private buildOptions(options?: HttpOptions): { headers?: HttpHeaders; params?: HttpParams; withCredentials?: boolean } {
    if (!options) {
      return {};
    }

    const normalizedParams = options.params instanceof HttpParams ? options.params : this.normalizeParams(options.params);

    return {
      headers: options.headers instanceof HttpHeaders ? options.headers : new HttpHeaders(options.headers),
      params: normalizedParams,
      withCredentials: options.withCredentials,
    };
  }

  private normalizeParams(
    params?: HttpOptions['params'],
  ): HttpParams {
    if (!params || params instanceof HttpParams) {
      return params ?? new HttpParams();
    }

    const entries = Object.entries(params).filter(
      ([, value]) => value !== undefined && value !== null,
    );

    return new HttpParams({
      fromObject: Object.fromEntries(
        entries.map(([key, value]) => [key, Array.isArray(value) ? value.join(',') : String(value)]),
      ),
    });
  }
}
