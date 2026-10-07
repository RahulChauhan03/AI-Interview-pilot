import { ResumeRecord } from './models/parsed-resume.model';

/** Values the AI writes for missing data; they count as empty. */
export const PLACEHOLDER = /^(none|n\/?a|null|undefined|-+|not (available|specified|provided|mentioned))$/i;

/** First real value among the given keys (also reads resumes parsed in the older free-form format). */
export function fieldValue(record: ResumeRecord | null | undefined, ...keys: string[]): string {
  for (const key of keys) {
    const value = record?.[key];
    if (typeof value === 'number') return String(value);
    if (typeof value === 'string' && value.trim() && !PLACEHOLDER.test(value.trim())) return value.trim();
  }
  return '';
}

/** Non-empty text items of a list field, or of a single text field. */
export function fieldList(record: ResumeRecord | null | undefined, ...keys: string[]): string[] {
  for (const key of keys) {
    const value = record?.[key];
    if (Array.isArray(value)) return value.map((item) => String(item).trim()).filter((item) => item && !PLACEHOLDER.test(item));
    if (typeof value === 'string' && value.trim() && !PLACEHOLDER.test(value.trim())) return [value.trim()];
  }
  return [];
}
