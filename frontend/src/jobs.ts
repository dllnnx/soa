import type { OscarJob } from './types';

const STORAGE_KEY = 'soa.oscar-jobs.v1';
const states = new Set(['PENDING', 'RUNNING', 'COMPLETED', 'FAILED']);

export function loadJobs(): OscarJob[] {
  try {
    const value: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    if (!Array.isArray(value)) return [];
    return value.filter((job): job is OscarJob => job && typeof job.id === 'string'
      && typeof job.label === 'string' && typeof job.createdAt === 'string'
      && (job.type === 'REWARD_R' || job.type === 'RESET_BY_GENRE') && states.has(job.status));
  } catch {
    return [];
  }
}

export function saveJobs(jobs: OscarJob[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(jobs));
  } catch {
    // Storage can be disabled or full; the current session must remain usable.
  }
}
