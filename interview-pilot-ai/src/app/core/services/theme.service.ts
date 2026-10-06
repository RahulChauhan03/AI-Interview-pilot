import { Injectable, computed, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark' | 'system';

const STORAGE_KEY = 'ip-theme';

/**
 * Application-wide light/dark mode. The choice is applied as the CSS `color-scheme` of <html>; every colour in
 * styles.scss (Material and app tokens) is a `light-dark()` value, so nothing else needs to change per theme.
 * "system" uses `light dark`, which follows the operating system automatically.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  // matchMedia is missing in some test environments and very old browsers; "system" then means light.
  private readonly media = typeof window.matchMedia === 'function' ? window.matchMedia('(prefers-color-scheme: dark)') : null;
  private readonly systemPrefersDark = signal(this.media?.matches ?? false);

  readonly mode = signal<ThemeMode>(this.readSavedMode());
  readonly isDark = computed(() => this.mode() === 'dark' || (this.mode() === 'system' && this.systemPrefersDark()));

  constructor() {
    this.media?.addEventListener('change', (event) => this.systemPrefersDark.set(event.matches));
    this.apply(this.mode());
  }

  setMode(mode: ThemeMode): void {
    this.mode.set(mode);
    try {
      localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      // Storage can be unavailable (private mode); the choice then lasts until reload.
    }
    this.apply(mode);
  }

  toggle(): void {
    this.setMode(this.isDark() ? 'light' : 'dark');
  }

  private apply(mode: ThemeMode): void {
    document.documentElement.style.colorScheme = mode === 'system' ? 'light dark' : mode;
  }

  private readSavedMode(): ThemeMode {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved === 'light' || saved === 'dark' || saved === 'system' ? saved : 'system';
    } catch {
      return 'system';
    }
  }
}
