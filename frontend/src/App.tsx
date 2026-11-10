import { useEffect, useRef, useState } from 'react';
import { Clapperboard, Clock3, Film } from 'lucide-react';
import type { Genre, JobResponse, OscarJob, Toast } from './types';
import { api, ApiRequestError } from './api';
import { genreLabels } from './labels';
import { MoviesPage } from './pages/MoviesPage';
import { OperationsPage } from './pages/OperationsPage';
import { ToastCard } from './components/ui';

function App() {
  const pollingActive = useRef(true);
  useEffect(() => {
    pollingActive.current = true;
    return () => { pollingActive.current = false; };
  }, []);
  const [activeTab, setActiveTab] = useState<'movies' | 'operations'>('movies');
  const [jobs, setJobs] = useState<OscarJob[]>([]);
  const [resetGenre, setResetGenre] = useState<Genre | ''>('');
  const [moviesRevision, setMoviesRevision] = useState(0);
  const [toast, setToast] = useState<Toast>(null);

  const currentJob = jobs.find((job) => job.status === 'PENDING' || job.status === 'RUNNING');

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), 4500);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const showError = (title: string, error: unknown) => {
    setToast({ kind: 'error', title, message: error instanceof Error ? error.message : 'Неизвестная ошибка' });
  };

  const updateJob = (id: string, response: JobResponse) => {
    setJobs((items) => items.map((item) => item.id === id ? {
      ...item,
      status: response.status,
      updatedCount: response.result?.updatedCount,
      errorMessage: response.result?.errorMessage,
    } : item));
  };

  const pollJob = async (job: OscarJob) => {
    let failures = 0;
    try {
      while (pollingActive.current) {
        let response: JobResponse;
        try {
          response = await api<JobResponse>(`/oscar/jobs/${job.id}/status`);
          failures = 0;
        } catch (error) {
          if (error instanceof ApiRequestError && error.status < 500 && error.status !== 408 && error.status !== 429) throw error;
          if (!failures) setToast({ kind: 'info', title: 'Статус временно недоступен', message: 'Проверка продолжится автоматически. Джоба сохраняется на сервере.' });
          failures += 1;
          await new Promise((resolve) => window.setTimeout(resolve, Math.min(30_000, 1000 * 2 ** Math.min(failures, 5))));
          continue;
        }
        if (!pollingActive.current) return;
        updateJob(job.id, response);
        if (response.status === 'COMPLETED' || response.status === 'FAILED') {
          setToast(response.status === 'COMPLETED'
            ? { kind: 'success', title: 'Операция завершена', message: `Обновлено фильмов: ${response.result?.updatedCount ?? 0}` }
            : { kind: 'error', title: 'Операция завершилась ошибкой', message: response.result?.errorMessage ?? 'Нет деталей ошибки' });
          setMoviesRevision((revision) => revision + 1);
          return;
        }
        await new Promise((resolve) => window.setTimeout(resolve, 2000));
      }
    } catch (error) {
      showError('Не удалось проверить джобу', error);
    }
  };

  const runOscarJob = async (type: OscarJob['type'], genre?: Genre) => {
    const label = type === 'REWARD_R' ? 'Наградить фильмы категории R' : `Отобрать Оскары у фильмов · ${genreLabels[genre!]}`;
    try {
      const path = type === 'REWARD_R' ? '/oscar/movies/reward-r' : `/oscar/movies/reset-oscars-by-genre/${genre}`;
      const accepted = await api<{ jobId: string; statusUrl: string }>(path, { method: 'POST' });
      const job: OscarJob = { id: accepted.jobId, type, label, status: 'PENDING', createdAt: new Date().toISOString() };
      setJobs((items) => [job, ...items]);
      setToast({ kind: 'info', title: 'Операция принята', message: `Джоба ${job.id.slice(0, 8)} добавлена в очередь` });
      void pollJob(job);
    } catch (error) {
      showError('Не удалось запустить операцию', error);
    }
  };

  const recordInspectedJob = (id: string, response: JobResponse) => {
    const known = jobs.find((item) => item.id === id);
    if (known) updateJob(id, response);
    else setJobs((items) => [{ id, type: 'REWARD_R', label: 'Проверенная джоба', status: response.status, createdAt: new Date().toISOString(), updatedCount: response.result?.updatedCount, errorMessage: response.result?.errorMessage }, ...items]);
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="brand" onClick={() => setActiveTab('movies')} aria-label="На главную">
          <span className="brand-mark"><Clapperboard size={20} strokeWidth={2.2} /></span>
          <span><b>Movies Service Console</b></span>
        </button>
        <nav className="tabs" aria-label="Разделы">
          <button className={activeTab === 'movies' ? 'active' : ''} onClick={() => setActiveTab('movies')}><Film size={17} /> Фильмы</button>
          <button className={activeTab === 'operations' ? 'active' : ''} onClick={() => setActiveTab('operations')}>
            <Clock3 size={17} /> Операции
            {currentJob && <span className="nav-pulse" />}
          </button>
        </nav>
      </header>

      <main>
        {activeTab === 'movies'
          ? <MoviesPage revision={moviesRevision} showToast={setToast} showError={showError} />
          : <OperationsPage jobs={jobs} currentJob={currentJob} resetGenre={resetGenre} setResetGenre={setResetGenre} runJob={runOscarJob} onInspected={recordInspectedJob} />}
      </main>

      {toast && <ToastCard toast={toast} onClose={() => setToast(null)} />}
    </div>
  );
}

export default App;
