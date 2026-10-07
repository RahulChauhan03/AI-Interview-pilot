import { ParsedResume } from '../models/parsed-resume.model';
import { band, calculateAtsScore } from './ats-score';

function resume(overrides: Partial<ParsedResume> = {}): ParsedResume {
  return {
    resumeId: 1, createdAt: '', personalInformation: {}, experience: [], education: [], skills: [], projects: [], certifications: [],
    achievements: [], languages: [], softSkills: [], technicalSkills: [], companies: [], designations: [], yearsOfExperience: null,
    summary: null, keywords: [], ...overrides,
  };
}

const complete = resume({
  personalInformation: { email: 'priya@example.com', phone: '+91 90000 11111', location: 'Bengaluru', linkedIn: 'linkedin.com/in/priya' },
  summary: 'Backend developer with 3 years of experience building REST APIs with Java and Spring Boot for insurance and retail clients, '
    + 'focused on reliable services, clean code and automated tests.',
  technicalSkills: ['Java', 'Spring Boot', 'MySQL', 'REST APIs', 'JUnit', 'Mockito', 'Git', 'Docker', 'Maven', 'Linux', 'SQL', 'Kafka'],
  experience: [
    { designation: 'Software Engineer', company: 'Infosys', duration: 'Jul 2022 - Present',
      responsibilities: ['Built REST APIs with Spring Boot and MySQL for a claims platform', 'Reduced report time by 30% with caching'] },
    { designation: 'Associate Developer', company: 'TCS', duration: 'Jun 2021 - Jun 2022',
      responsibilities: ['Maintained Java services for a retail billing system'] },
  ],
  education: [{ degree: 'B.Tech Computer Science', institution: 'VIT University', duration: '2017 - 2021' }],
  projects: [{ name: 'Library Manager' }],
  certifications: ['Oracle Certified Professional: Java SE 11'],
  keywords: ['Java', 'Spring Boot', 'REST', 'MySQL', 'Microservices', 'JUnit', 'Docker', 'Git', 'SQL', 'APIs'],
});

describe('calculateAtsScore', () => {
  it('scores a complete, well-structured resume as excellent with no tips', () => {
    const result = calculateAtsScore(complete);
    expect(result.score).toBe(100);
    expect(result.band).toBe('Excellent');
    expect(result.checks.every((check) => !check.tip)).toBe(true);
    expect(result.checks.reduce((sum, check) => sum + check.max, 0)).toBe(100);
  });

  it('gives an empty resume a low score with a tip for every check', () => {
    const result = calculateAtsScore(resume());
    expect(result.score).toBe(0);
    expect(result.band).toBe('Needs attention');
    expect(result.checks.every((check) => check.tip)).toBe(true);
  });

  it('treats AI placeholders as missing data', () => {
    const result = calculateAtsScore(resume({ personalInformation: { email: 'N/A', phone: 'None' }, summary: 'Not specified', skills: ['none'] }));
    expect(result.checks.find((check) => check.label === 'Contact details')?.points).toBe(0);
    expect(result.checks.find((check) => check.label === 'Professional summary')?.points).toBe(0);
    expect(result.checks.find((check) => check.label === 'Skills section')?.points).toBe(0);
  });

  it('reads the older free-form keys and explains what is missing', () => {
    const result = calculateAtsScore(resume({
      personalInformation: { Email: 'dev@example.com' },
      experience: [{ title: 'Developer', companyName: 'Wipro', description: 'Python scripts' }],
    }));
    const experience = result.checks.find((check) => check.label === 'Work experience')!;
    expect(experience.points).toBeGreaterThan(0);
    expect(result.checks.find((check) => check.label === 'Dates')?.tip).toContain('dates');
    expect(result.checks.find((check) => check.label === 'Contact details')?.tip).toContain('phone');
  });

  it('maps scores to the agreed bands', () => {
    expect(band(95).band).toBe('Excellent');
    expect(band(90).band).toBe('Excellent');
    expect(band(89).band).toBe('Good');
    expect(band(75).band).toBe('Good');
    expect(band(74).band).toBe('Needs improvement');
    expect(band(60).band).toBe('Needs improvement');
    expect(band(59).band).toBe('Needs attention');
  });
});
