import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoggerService {
  info(message: string, context?: string): void {
    console.info(`[${context ?? 'app'}] ${message}`);
  }

  error(message: string, context?: string): void {
    console.error(`[${context ?? 'app'}] ${message}`);
  }
}
