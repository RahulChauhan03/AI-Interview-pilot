import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize, tap } from 'rxjs';
import { LoadingService } from '../services/loading.service';

export const loadingInterceptor: HttpInterceptorFn = (request, next) => {
  const loadingService = inject(LoadingService);
  loadingService.start();

  return next(request).pipe(
    tap({
      error: () => loadingService.stop(),
    }),
    finalize(() => loadingService.stop()),
  );
};
