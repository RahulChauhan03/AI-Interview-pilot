import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { Workspace } from '../models/application.model';
import { WorkspaceStore } from './workspace.store';

const api = (path: string) => `${environment.apiUrl}/${path}`;

function workspace(overrides: Partial<Workspace> = {}): Workspace {
  return {
    job: { id: 10, userId: 1, companyName: 'Acme', jobTitle: 'Backend Engineer', jobDescription: 'Java', createdAt: '2026-10-01', updatedAt: null },
    application: null, latestMatch: null, interviews: [],
    skillGaps: { strong: [], developing: [], missing: [], recommendations: [] }, topics: [], recentQuestions: [],
    ...overrides,
  };
}

describe('WorkspaceStore', () => {
  let store: WorkspaceStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [WorkspaceStore, provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    store = TestBed.inject(WorkspaceStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function load(data: Workspace, preferredResumeId: number | null = null): void {
    store.load(10, preferredResumeId);
    http.expectOne(api('job-descriptions/10/workspace')).flush({ data });
    http.expectOne((req) => req.url.endsWith('/resumes')).flush({ data: [
      { id: 1, originalFileName: 'a.pdf', status: 'PARSED', summary: null, uploadTime: '' },
      { id: 2, originalFileName: 'b.pdf', status: 'PARSED', summary: null, uploadTime: '' },
      { id: 3, originalFileName: 'c.pdf', status: 'FAILED', summary: null, uploadTime: '' },
    ] });
  }

  it('shows only real progress for a new job', () => {
    load(workspace());

    expect(store.loading()).toBe(false);
    expect(store.steps().map((step) => step.done)).toEqual([true, false, false, false, false, false]);
    expect(store.resumes().map((resume) => resume.id)).toEqual([1, 2]);
    expect(store.resumeId()).toBe(1);
  });

  it('uses the requested resume when it is parsed', () => {
    load(workspace(), 2);
    expect(store.resumeId()).toBe(2);
  });

  it('creates the application before generating the first document', () => {
    load(workspace());

    store.generateTailoredResume();
    expect(store.task()).toBe('resume');
    const create = http.expectOne(api('job-descriptions/10/application'));
    expect(create.request.body).toEqual({ resumeId: 1 });
    create.flush({ data: { id: 7 } });
    const generate = http.expectOne(api('applications/7/tailored-resume'));
    generate.flush({ data: { applicationId: 7, content: {}, omittedSkills: [], removedSuggestions: 0 } });
    http.expectOne(api('job-descriptions/10/workspace')).flush({ data: workspace() });

    expect(store.task()).toBeNull();
    expect(store.tailoredResume()?.applicationId).toBe(7);
  });

  it('loads existing documents with the workspace', () => {
    const application = { id: 7, status: 'APPLIED', appliedAt: '2026-10-05', tailoredResumeAt: '2026-10-04', coverLetterAt: null } as never;
    store.load(10, null);
    http.expectOne(api('job-descriptions/10/workspace')).flush({ data: workspace({ application }) });
    http.expectOne((req) => req.url.endsWith('/resumes')).flush({ data: [] });
    http.expectOne(api('applications/7/tailored-resume')).flush({ data: { applicationId: 7 } });

    expect(store.tailoredResume()).not.toBeNull();
    expect(store.coverLetter()).toBeNull();
    expect(store.steps().find((step) => step.label === 'Applied')?.done).toBe(true);
  });
});
