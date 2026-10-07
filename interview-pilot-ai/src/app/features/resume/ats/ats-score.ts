import { ParsedResume } from '../models/parsed-resume.model';
import { PLACEHOLDER, fieldList, fieldValue } from '../resume-fields';

export interface AtsCheck {
  label: string;
  points: number;
  max: number;
  /** What to improve; empty when the check earned all its points. */
  tip: string;
}

export interface AtsScore {
  score: number;
  band: 'Excellent' | 'Good' | 'Needs improvement' | 'Needs attention';
  tone: 'good' | 'fair' | 'low';
  checks: AtsCheck[];
}

/** Short summary of the rubric for the "How is this calculated?" tooltip. */
export const ATS_SCORE_EXPLANATION =
  'Scored from what was extracted from your resume, out of 100: contact details 15, summary 10, skills 15, ' +
  'experience 25, dates 10, education 10, standard sections 10, keywords 5. It shows how easily an applicant ' +
  'tracking system can read your resume, not how well you fit a job.';

const KEYS = {
  title: ['designation', 'title', 'role', 'position'],
  company: ['company', 'companyName', 'organization', 'employer'],
  duration: ['duration', 'dates', 'employmentDates', 'period', 'year', 'years'],
  bullets: ['responsibilities', 'description', 'achievements'],
  degree: ['degree', 'qualification'],
  institution: ['institution', 'university', 'school', 'college'],
};

/**
 * ATS friendliness of a parsed resume, 0-100, using only the data the parser extracted (nothing is assumed).
 * Each check explains what is missing so the score is actionable.
 */
export function calculateAtsScore(resume: ParsedResume): AtsScore {
  const info = resume.personalInformation ?? {};
  const clean = (items: string[] | null | undefined) => (items ?? []).map((item) => item?.trim()).filter((item) => item && !PLACEHOLDER.test(item));
  const checks: AtsCheck[] = [];
  const add = (label: string, points: number, max: number, tip: string) =>
    checks.push({ label, points: Math.round(Math.min(points, max)), max, tip: points >= max ? '' : tip });

  // Contact details (15)
  const email = fieldValue(info, 'email', 'Email');
  const phone = fieldValue(info, 'phone', 'Phone');
  const location = fieldValue(info, 'location', 'Location');
  const link = fieldValue(info, 'linkedIn', 'LinkedIn', 'linkedin', 'github', 'Github', 'GitHub', 'portfolio', 'Portfolio');
  add('Contact details', (email ? 6 : 0) + (phone ? 5 : 0) + (location ? 2 : 0) + (link ? 2 : 0), 15,
    'Add ' + [!email && 'an email', !phone && 'a phone number', !location && 'your city', !link && 'a LinkedIn or portfolio link']
      .filter(Boolean).join(', ') + ' in plain text at the top.');

  // Summary (10): present, and a readable length
  const summary = (resume.summary ?? '').trim();
  const summaryWords = summary ? summary.split(/\s+/).length : 0;
  add('Professional summary', summary && !PLACEHOLDER.test(summary) ? (summaryWords >= 20 && summaryWords <= 120 ? 10 : 6) : 0, 10,
    summary ? 'Keep the summary to 2-4 sentences (about 20-120 words).' : 'Add a short professional summary.');

  // Skills (15)
  const skills = new Set([...clean(resume.technicalSkills), ...clean(resume.skills)].map((skill) => skill.toLowerCase()));
  add('Skills section', skills.size >= 12 ? 15 : skills.size >= 8 ? 12 : skills.size >= 5 ? 9 : skills.size >= 1 ? 5 : 0, 15,
    'List more of your relevant tools and technologies in a dedicated Skills section.');

  // Experience (25): entries, titles and companies, bullet points, measurable results, concise bullets
  const jobs = (resume.experience ?? []).filter((job) => fieldValue(job, ...KEYS.title, ...KEYS.company));
  const share = (count: number, total: number) => (total ? count / total : 0);
  const bullets = jobs.flatMap((job) => fieldList(job, ...KEYS.bullets));
  const bulletWords = bullets.map((bullet) => bullet.split(/\s+/).length);
  const concise = bulletWords.length ? share(bulletWords.filter((words) => words >= 5 && words <= 35).length, bulletWords.length) : 0;
  add('Work experience',
    (jobs.length ? 6 : 0)
      + 6 * share(jobs.filter((job) => fieldValue(job, ...KEYS.title) && fieldValue(job, ...KEYS.company)).length, jobs.length)
      + 6 * share(jobs.filter((job) => fieldList(job, ...KEYS.bullets).length).length, jobs.length)
      + (bullets.some((bullet) => /\d/.test(bullet)) ? 4 : 0)
      + 3 * concise,
    25,
    jobs.length ? 'Give every role a title, company and 2-5 short bullet points; add measurable results where you have them.'
      : 'Add your work experience (or internships) with job titles and companies.');

  // Dates (10): on work and education entries
  const education = (resume.education ?? []).filter((entry) => fieldValue(entry, ...KEYS.degree, ...KEYS.institution));
  const dated = [...jobs, ...education];
  add('Dates', 10 * share(dated.filter((entry) => fieldValue(entry, ...KEYS.duration)).length, dated.length), 10,
    'Add start and end dates (e.g. "Jul 2022 - Present") to every role and degree.');

  // Education (10)
  const complete = education.some((entry) => fieldValue(entry, ...KEYS.degree) && fieldValue(entry, ...KEYS.institution));
  add('Education', complete ? 10 : education.length ? 6 : 0, 10,
    education.length ? 'Show both the degree and the institution.' : 'Add your education.');

  // Standard sections (10): sections an ATS recognises
  const sections = [summary, skills.size, jobs.length, education.length, (resume.projects ?? []).length,
    clean(resume.certifications).length + clean(resume.achievements).length].filter(Boolean).length;
  add('Standard sections', sections * 2, 10, 'Use standard headings: Summary, Skills, Experience, Education, Projects, Certifications.');

  // Keyword signals (5)
  const keywords = clean(resume.keywords).length;
  add('Keyword signals', keywords >= 10 ? 5 : keywords >= 5 ? 3 : keywords >= 1 ? 1 : 0, 5,
    'Use the exact terms from your target job descriptions (tools, methods, role names).');

  const score = Math.round(checks.reduce((sum, check) => sum + check.points, 0));
  return { score, checks, ...band(score) };
}

export function band(score: number): Pick<AtsScore, 'band' | 'tone'> {
  if (score >= 90) return { band: 'Excellent', tone: 'good' };
  if (score >= 75) return { band: 'Good', tone: 'good' };
  if (score >= 60) return { band: 'Needs improvement', tone: 'fair' };
  return { band: 'Needs attention', tone: 'low' };
}
