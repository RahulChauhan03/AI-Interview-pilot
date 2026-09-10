import { HttpHeaders, HttpParams } from '@angular/common/http';

export interface HttpOptions {
  headers?: HttpHeaders | Record<string, string>;
  params?: HttpParams | Record<string, string | number | boolean | readonly (string | number | boolean)[] | null | undefined>;
  withCredentials?: boolean;
}
