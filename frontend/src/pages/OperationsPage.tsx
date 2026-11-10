import { useState } from 'react';
import { AlertCircle, Check, LoaderCircle } from 'lucide-react';
import type { Genre, JobResponse, OscarJob } from '../types';
import { api } from '../api';
import { GENRES, genreLabels } from '../labels';
import { formatDate } from '../format';
import { TooltipButton } from '../components/ui';

const statusText = { PENDING: 'В очереди', RUNNING: 'Выполняется', COMPLETED: 'Готово', FAILED: 'Ошибка' } as const;

type Inspection = { kind: 'ok'; response: JobResponse } | { kind: 'invalid' } | { kind: 'error'; message: string };

export function OperationsPage({ jobs, currentJob, resetGenre, setResetGenre, runJob, onInspected }: { jobs: OscarJob[]; currentJob?: OscarJob; resetGenre: Genre | ''; setResetGenre: (genre: Genre | '') => void; runJob: (type: OscarJob['type'], genre?: Genre) => void; onInspected: (id: string, response: JobResponse) => void }) {
  const [lookup, setLookup] = useState('');
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const inspect = async () => {
    const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    if (!uuid.test(lookup)) {
      setInspection({ kind: 'invalid' });
      return;
    }
    try {
      const response = await api<JobResponse>(`/oscar/jobs/${lookup}/status`);
      setInspection({ kind: 'ok', response });
      onInspected(lookup, response);
    } catch (error) {
      setInspection({ kind: 'error', message: error instanceof Error ? error.message : 'Неизвестная ошибка' });
    }
  };
  return <>
    <section className="page-heading operations-heading"><div><h1>Статус операций</h1></div>
      <div className="heading-actions operations-actions">
        <TooltipButton className="button secondary" label="Наградить фильмы категории R" tooltip="Увеличить количество Оскаров на 1 у всех фильмов категории R" onClick={() => runJob('REWARD_R')} disabled={Boolean(currentJob)} />
        <div className="action-picker">
          <div className="action-picker-row">
            <span className="action-picker-label">Какой жанр вы ненавидите?</span>
            <select aria-label="Жанр для отбора Оскаров" value={resetGenre} onChange={(event) => setResetGenre(event.target.value as Genre)}>
              <option value="" disabled>Выберите жанр…</option>
              {GENRES.map((genre) => <option key={genre} value={genre}>{genreLabels[genre]}</option>)}
            </select>
          </div>
          <TooltipButton className="button primary" label="Уничтожить Оскары" tooltip="Отобрать все Оскары у всех фильмов режиссеров, снявших хоть один фильм в указанном жанре" onClick={() => runJob('RESET_BY_GENRE', resetGenre || undefined)} disabled={Boolean(currentJob) || !resetGenre} />
        </div>
      </div>
    </section>
    <section className="jobs-layout">
      <div className="jobs-card">
        <div className="card-heading"><div><h2>Журнал операций</h2><p>{jobs.length} записей</p></div></div>
        <div className="jobs-list">
          {jobs.map((job) => <article className="job-row" key={job.id}><span className={`job-status-icon ${job.status}`}>{job.status === 'COMPLETED' ? <Check /> : job.status === 'FAILED' ? <AlertCircle /> : <LoaderCircle className={job.status === 'RUNNING' ? 'spin' : ''} />}</span><div className="job-main"><b>{job.label}</b><span className="mono">{job.id}</span></div><div className="job-time"><b>{new Intl.DateTimeFormat('ru-RU', { hour: '2-digit', minute: '2-digit' }).format(new Date(job.createdAt))}</b><span>{formatDate(job.createdAt)}</span></div><span className={`status-badge ${job.status}`}>{statusText[job.status]}</span><div className="job-result">{job.status === 'COMPLETED' ? <><b>{job.updatedCount}</b><span>изменено</span></> : job.status === 'FAILED' ? <><b>—</b><span>{job.errorMessage}</span></> : <><b>···</b><span>ожидание</span></>}</div></article>)}
        </div>
      </div>
      <aside className="lookup-card">
        <h3>Проверить джобу</h3>
        <p>Введите идентификатор из ответа <code>202 Accepted</code>.</p>
        <label>ID операции<input maxLength={36} value={lookup} onChange={(event) => { setLookup(event.target.value); setInspection(null); }} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" /></label>
        <button className="button primary" onClick={() => void inspect()}>Проверить статус</button>
        {inspection && (
          <div className={`inspection-result ${inspection.kind === 'ok' ? inspection.response.status.toLowerCase() : inspection.kind}`}>
            {inspection.kind === 'invalid' ? <b>Невалидный идентификатор</b>
              : inspection.kind === 'error' ? <b>Не удалось получить статус</b>
              : <b>Статус: {statusText[inspection.response.status]}</b>}
            {inspection.kind === 'invalid' && <span>Введите UUID джобы в формате xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</span>}
            {inspection.kind === 'error' && <span>{inspection.message}</span>}
            {inspection.kind === 'ok' && inspection.response.status === 'COMPLETED' && <span>Обновлено фильмов: {inspection.response.result?.updatedCount ?? 0}</span>}
            {inspection.kind === 'ok' && inspection.response.status === 'FAILED' && <span>{inspection.response.result?.errorMessage ?? 'Нет деталей ошибки'}</span>}
            {inspection.kind === 'ok' && (inspection.response.status === 'PENDING' || inspection.response.status === 'RUNNING') && <span>Операция ещё выполняется — проверьте статус позже</span>}
          </div>
        )}
      </aside>
    </section>
  </>;
}
