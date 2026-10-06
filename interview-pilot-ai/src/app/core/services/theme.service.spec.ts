import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.style.colorScheme = '';
    TestBed.resetTestingModule();
  });

  it('defaults to following the system', () => {
    const theme = TestBed.inject(ThemeService);
    expect(theme.mode()).toBe('system');
    expect(document.documentElement.style.colorScheme).toBe('light dark');
  });

  it('applies and remembers an explicit choice', () => {
    TestBed.inject(ThemeService).setMode('dark');

    expect(document.documentElement.style.colorScheme).toBe('dark');
    expect(localStorage.getItem('ip-theme')).toBe('dark');
  });

  it('restores the saved choice after a reload', () => {
    localStorage.setItem('ip-theme', 'light');

    const theme = TestBed.inject(ThemeService);

    expect(theme.mode()).toBe('light');
    expect(theme.isDark()).toBe(false);
  });

  it('ignores an unknown saved value', () => {
    localStorage.setItem('ip-theme', 'purple');
    expect(TestBed.inject(ThemeService).mode()).toBe('system');
  });

  it('toggles between light and dark', () => {
    const theme = TestBed.inject(ThemeService);
    theme.setMode('light');
    theme.toggle();
    expect(theme.mode()).toBe('dark');
    theme.toggle();
    expect(theme.mode()).toBe('light');
  });
});
