import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { APP_ICONS, iconSvg } from './app-icons';

/** Registers the app icon set with Angular Material, so templates use <mat-icon svgIcon="name" />. */
export function provideAppIcons(): EnvironmentProviders {
  return provideAppInitializer(() => {
    const registry = inject(MatIconRegistry);
    const sanitizer = inject(DomSanitizer);
    for (const [name, body] of Object.entries(APP_ICONS)) {
      // Static markup from this repository, never user input.
      registry.addSvgIconLiteral(name, sanitizer.bypassSecurityTrustHtml(iconSvg(body)));
    }
  });
}
