import { useEffect, useId, useMemo, useState } from 'react';
import {
  AlertCircle, ArrowDown, ArrowUp, Award, BarChart3, Check, ChevronDown, ChevronLeft,
  ChevronRight, ChevronsUpDown, CircleDollarSign, Clapperboard, Clock3,
  Edit3, ExternalLink, Film, Filter, LoaderCircle, MoreHorizontal, Plus,
  RefreshCw, RotateCcw, Search, SlidersHorizontal, Trash2, UserRound, X,
} from 'lucide-react';
import type { Filters, Genre, Movie, MovieDraft, OscarJob, Person } from './types';
import {
  COUNTRIES, EYE_COLORS, emptyDraft, emptyFilters, formatDate, formatMoney, GENRES,
  genreLabels, HAIR_COLORS, MPAA, countryLabels, colorLabels, draftFromMovie,
} from './mock';

type Toast = { kind: 'success' | 'error' | 'info'; title: string; message: string } | null;
type SortDirection = 'asc' | 'desc';

const sortOptions = [
  ['id', 'ID'], ['name', 'Название'], ['coordinates-x', 'Координата X'], ['coordinates-y', 'Координата Y'],
  ['creation-date', 'Дата создания'], ['oscars-count', 'Оскары'], ['budget', 'Бюджет'], ['genre', 'Жанр'],
  ['mpaa-rating', 'Рейтинг MPAA'], ['screenwriter-name', 'Имя сценариста'], ['screenwriter-height', 'Рост сценариста'],
  ['screenwriter-eye-color', 'Цвет глаз'], ['screenwriter-hair-color', 'Цвет волос'], ['screenwriter-nationality', 'Гражданство'],
  ['screenwriter-location-x', 'Локация сценариста X'], ['screenwriter-location-y', 'Локация сценариста Y'], ['screenwriter-location-z', 'Локация сценариста Z'],
] as const;

type MoviePage = { page: number; size: number; totalElements: number; totalPages: number; content: Movie[] };
type JobResponse = { jobId: string; status: OscarJob['status']; result?: { updatedCount?: number; errorMessage?: string } };
type ApiErrorBody = { message?: string; details?: Array<{ field: string; message: string }> };

const api = async <T,>(path: string, init: RequestInit = {}): Promise<T> => {
  const response = await fetch(path, {
    ...init,
    headers: { ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...init.headers },
  });
  const text = await response.text();
  const body = text ? JSON.parse(text) as T | ApiErrorBody : undefined;
  if (!response.ok) {
    const error = (body ?? {}) as ApiErrorBody;
    const details = error.details?.map((item) => `${item.field}: ${item.message}`).join('; ');
    throw new Error([error.message || `HTTP ${response.status}`, details].filter(Boolean).join(' — '));
  }
  return body as T;
};

const present = (value: string) => value === '' ? undefined : Number(value);
const compactObject = (value: Record<string, unknown>) => Object.fromEntries(
  Object.entries(value).filter(([, item]) => item !== undefined && item !== '' && (!Array.isArray(item) || item.length > 0)),
);

const toMovieFilter = (filters: Filters) => {
  const coordinates = compactObject({
    xFrom: present(filters.coordinateXFrom), xTo: present(filters.coordinateXTo),
    yFrom: present(filters.coordinateYFrom), yTo: present(filters.coordinateYTo),
  });
  const location = compactObject({
    xFrom: present(filters.locationXFrom), xTo: present(filters.locationXTo),
    yFrom: present(filters.locationYFrom), yTo: present(filters.locationYTo),
    zFrom: present(filters.locationZFrom), zTo: present(filters.locationZTo),
  });
  const screenwriter = compactObject({
    name: filters.screenwriterName || undefined,
    heightFrom: present(filters.heightFrom), heightTo: present(filters.heightTo),
    eyeColor: filters.eyeColor || undefined, hairColor: filters.hairColor || undefined,
    nationality: filters.nationality || undefined,
    location: Object.keys(location).length ? location : undefined,
  });
  return compactObject({
    ids: filters.ids.split(',').map((id) => Number(id.trim())).filter((id) => Number.isInteger(id) && id > 0),
    name: filters.name || undefined,
    coordinates: Object.keys(coordinates).length ? coordinates : undefined,
    creationDateAfter: filters.creationAfter ? new Date(filters.creationAfter).toISOString() : undefined,
    creationDateBefore: filters.creationBefore ? new Date(filters.creationBefore).toISOString() : undefined,
    oscarsCountFrom: present(filters.oscarsFrom), oscarsCountTo: present(filters.oscarsTo),
    budgetFrom: present(filters.budgetFrom), budgetTo: present(filters.budgetTo),
    genres: filters.genres, mpaaRating: filters.mpaaRating || undefined,
    screenwriter: Object.keys(screenwriter).length ? screenwriter : undefined,
  });
};

const activeFilterCount = (filters: Filters) => Object.values(filters).filter((value) => Array.isArray(value) ? value.length > 0 : Boolean(value)).length;
const statusText = { PENDING: 'В очереди', RUNNING: 'Выполняется', COMPLETED: 'Готово', FAILED: 'Ошибка' } as const;

function App() {
  const [activeTab, setActiveTab] = useState<'movies' | 'operations'>('movies');
  const [movies, setMovies] = useState<Movie[]>([]);
  const [jobs, setJobs] = useState<OscarJob[]>([]);
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [draftFilters, setDraftFilters] = useState<Filters>(emptyFilters);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);
  const [sortField, setSortField] = useState('id');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [totalElements, setTotalElements] = useState(0);
  const [serverTotalPages, setServerTotalPages] = useState(0);
  const [averageBudget, setAverageBudget] = useState(0);
  const [loading, setLoading] = useState(true);
  const [apiOnline, setApiOnline] = useState(false);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [movieModal, setMovieModal] = useState<{ mode: 'create' | 'edit'; movie?: Movie } | null>(null);
  const [toolsOpen, setToolsOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Movie | null>(null);
  const [toast, setToast] = useState<Toast>(null);
  const [resetGenre, setResetGenre] = useState<Genre>('SCIENCE_FICTION');

  const totalPages = Math.max(1, serverTotalPages);
  const totalOscars = movies.reduce((sum, item) => sum + item.oscarsCount, 0);
  const currentJob = jobs.find((job) => job.status === 'PENDING' || job.status === 'RUNNING');
  const writers = useMemo(() => Array.from(new Map(movies.flatMap((item) => item.screenwriter ? [[item.screenwriter.name, item.screenwriter] as const] : [])).values()), [movies]);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), 4500);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const showError = (title: string, error: unknown) => {
    setToast({ kind: 'error', title, message: error instanceof Error ? error.message : 'Неизвестная ошибка' });
  };

  const loadAverage = async () => {
    try {
      const result = await api<{ averageBudget: number }>('/movies/average-budget');
      setAverageBudget(result.averageBudget);
    } catch (error) {
      showError('Не удалось получить средний бюджет', error);
    }
  };

  const loadMovies = async () => {
    setLoading(true);
    try {
      const query = new URLSearchParams({ page: String(page), size: String(size), sort: `${sortField},${sortDirection}` });
      const result = await api<MoviePage>(`/movies/filter?${query}`, { method: 'POST', body: JSON.stringify(toMovieFilter(filters)) });
      setMovies(result.content);
      setTotalElements(result.totalElements);
      setServerTotalPages(result.totalPages);
      setApiOnline(true);
      if (result.totalPages > 0 && page >= result.totalPages) setPage(result.totalPages - 1);
    } catch (error) {
      setApiOnline(false);
      showError('Не удалось загрузить фильмы', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => { void loadMovies(); }, 250);
    return () => window.clearTimeout(timer);
  }, [filters, page, size, sortField, sortDirection]);

  useEffect(() => { void loadAverage(); }, []);

  const updateJob = (id: string, response: JobResponse) => {
    setJobs((items) => items.map((item) => item.id === id ? {
      ...item,
      status: response.status,
      updatedCount: response.result?.updatedCount,
      errorMessage: response.result?.errorMessage,
    } : item));
  };

  const pollJob = async (job: OscarJob) => {
    try {
      for (let attempt = 0; attempt < 120; attempt += 1) {
        if (attempt) await new Promise((resolve) => window.setTimeout(resolve, 1000));
        const response = await api<JobResponse>(`/oscar/jobs/${job.id}/status`);
        updateJob(job.id, response);
        if (response.status === 'COMPLETED' || response.status === 'FAILED') {
          setToast(response.status === 'COMPLETED'
            ? { kind: 'success', title: 'Операция завершена', message: `Обновлено фильмов: ${response.result?.updatedCount ?? 0}` }
            : { kind: 'error', title: 'Операция завершилась ошибкой', message: response.result?.errorMessage ?? 'Нет деталей ошибки' });
          await Promise.all([loadMovies(), loadAverage()]);
          return;
        }
      }
      throw new Error('Истекло время ожидания результата операции');
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

  const saveMovie = async (draft: MovieDraft, editing?: Movie) => {
    let screenwriter: Person | null = null;
    if (draft.screenwriterMode === 'existing') screenwriter = writers.find((item) => item.name === draft.existingScreenwriter) ?? null;
    if (draft.screenwriterMode === 'new') {
      screenwriter = {
        name: draft.screenwriterName.trim(), height: Number(draft.screenwriterHeight), eyeColor: draft.eyeColor,
        hairColor: draft.hairColor || null, nationality: draft.nationality,
        location: { x: Number(draft.locationX), y: Number(draft.locationY), z: Number(draft.locationZ) },
      };
    }
    const input = {
      name: draft.name.trim(),
      coordinates: { x: Number(draft.coordinateX), y: Number(draft.coordinateY) },
      oscarsCount: Number(draft.oscarsCount),
      budget: draft.budget ? Number(draft.budget) : null, genre: draft.genre, mpaaRating: draft.mpaaRating, screenwriter,
    };
    try {
      const item = await api<Movie>(editing ? `/movies/${editing.id}` : '/movies', {
        method: editing ? 'PUT' : 'POST', body: JSON.stringify(input),
      });
      setMovieModal(null);
      setToast({ kind: 'success', title: editing ? 'Фильм обновлён' : 'Фильм создан', message: `${item.name} · ID ${item.id}` });
      await Promise.all([loadMovies(), loadAverage()]);
    } catch (error) {
      showError(editing ? 'Не удалось обновить фильм' : 'Не удалось создать фильм', error);
    }
  };

  const removeMovie = async (item: Movie) => {
    try {
      await api<void>(`/movies/${item.id}`, { method: 'DELETE' });
      setConfirmDelete(null);
      setExpandedId(null);
      setToast({ kind: 'success', title: 'Фильм удалён', message: `${item.name} больше не в коллекции` });
      await Promise.all([loadMovies(), loadAverage()]);
    } catch (error) {
      showError('Не удалось удалить фильм', error);
    }
  };

  const deleteByMpaa = async (rating: string) => {
    try {
      const result = await api<{ deletedCount: number }>(`/movies/by-mpaa-rating?mpaa-rating=${encodeURIComponent(rating)}`, { method: 'DELETE' });
      setToast({ kind: 'success', title: 'Массовое удаление завершено', message: `Удалено фильмов: ${result.deletedCount}` });
      await Promise.all([loadMovies(), loadAverage()]);
    } catch (error) {
      showError('Не удалось удалить фильмы', error);
    }
  };

  const deleteByWriter = async (name: string) => {
    try {
      await api<void>(`/movies/by-screenwriter?name=${encodeURIComponent(name)}`, { method: 'DELETE' });
      setToast({ kind: 'success', title: 'Фильм удалён', message: `Сценарист: ${name}` });
      await Promise.all([loadMovies(), loadAverage()]);
    } catch (error) {
      showError('Не удалось удалить фильм', error);
    }
  };

  const inspectJob = async (id: string) => {
    try {
      const response = await api<JobResponse>(`/oscar/jobs/${id}/status`);
      const known = jobs.find((item) => item.id === id);
      if (known) updateJob(id, response);
      else setJobs((items) => [{ id, type: 'REWARD_R', label: 'Проверенная джоба', status: response.status, createdAt: new Date().toISOString(), updatedCount: response.result?.updatedCount, errorMessage: response.result?.errorMessage }, ...items]);
      setToast({
        kind: response.status === 'FAILED' ? 'error' : 'success',
        title: statusText[response.status],
        message: response.result?.errorMessage ?? `Обновлено фильмов: ${response.result?.updatedCount ?? 0}`,
      });
    } catch (error) {
      showError('Не удалось получить статус джобы', error);
    }
  };

  const applyQuickSearch = (value: string) => {
    setFilters((current) => ({ ...current, name: value }));
    setDraftFilters((current) => ({ ...current, name: value }));
    setPage(0);
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
        <div className={`service-state ${apiOnline ? '' : 'offline'}`}><span /> API · {apiOnline ? 'подключено' : 'недоступно'}</div>
      </header>

      <main>
        {activeTab === 'movies' ? (
          <>
            <section className="page-heading">
              <div>
                <h1>Фильмы</h1>
              </div>
              <div className="heading-actions">
                <button className="button secondary" onClick={() => setToolsOpen(true)}><MoreHorizontal size={18} /> Инструменты</button>
                <button className="button primary" onClick={() => setMovieModal({ mode: 'create' })}><Plus size={18} /> Создать фильм</button>
              </div>
            </section>

            <section className="metrics-grid">
              <Metric icon={<Film />} label="Фильмов в выборке" value={String(totalElements)} note={`${movies.length} на текущей странице`} tone="ink" loading={loading} />
              <Metric icon={<CircleDollarSign />} label="Средний бюджет" value={compactMoney(averageBudget)} note="по всей коллекции" tone="green" loading={loading} />
              <Metric icon={<Award />} label="Оскаров на странице" value={String(totalOscars)} note={`${movies.filter((item) => item.oscarsCount > 0).length} награждённых`} tone="gold" loading={loading} />
            </section>

            <section className="table-card">
              <div className="table-toolbar">
                <div className="search-box"><Search size={18} /><input value={filters.name} onChange={(event) => applyQuickSearch(event.target.value)} placeholder="Название фильма…" /><kbd>⌘ K</kbd></div>
                <div className="toolbar-group">
                  <button className={`button filter-button ${activeFilterCount(filters) ? 'has-filters' : ''}`} onClick={() => { setDraftFilters(filters); setFiltersOpen(true); }}>
                    <Filter size={17} /> Фильтры {activeFilterCount(filters) > 0 && <span>{activeFilterCount(filters)}</span>}
                  </button>
                  <label className="sort-control"><span>Сортировка</span><select value={sortField} onChange={(event) => { setSortField(event.target.value); setPage(0); }}>{sortOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                  <button className="icon-button direction" onClick={() => setSortDirection((value) => value === 'asc' ? 'desc' : 'asc')} title="Изменить направление сортировки">
                    {sortDirection === 'asc' ? <ArrowUp size={18} /> : <ArrowDown size={18} />}
                  </button>
                </div>
              </div>

              {activeFilterCount(filters) > 0 && (
                <div className="filter-summary">
                  <span>Активные фильтры</span>
                  {filters.genres.map((genre) => <button key={genre} onClick={() => setFilters((current) => ({ ...current, genres: current.genres.filter((item) => item !== genre) }))}>{genreLabels[genre]} <X size={12} /></button>)}
                  {filters.mpaaRating && <button onClick={() => setFilters((current) => ({ ...current, mpaaRating: '' }))}>MPAA: {filters.mpaaRating.replace('_', '-')} <X size={12} /></button>}
                  {filters.screenwriterName && <button onClick={() => setFilters((current) => ({ ...current, screenwriterName: '' }))}>Сценарист: {filters.screenwriterName} <X size={12} /></button>}
                  <button className="clear-all" onClick={() => { setFilters(emptyFilters); setDraftFilters(emptyFilters); }}>Сбросить всё</button>
                </div>
              )}

              <div className="table-scroll">
                <table>
                  <thead><tr>
                    <th className="expander-cell" />
                    <SortableTh label="ID" field="id" current={sortField} direction={sortDirection} onSort={setSortField} />
                    <SortableTh label="Фильм" field="name" current={sortField} direction={sortDirection} onSort={setSortField} />
                    <th>Сценарист</th>
                    <SortableTh label="Бюджет" field="budget" current={sortField} direction={sortDirection} onSort={setSortField} />
                    <SortableTh label="Оскары" field="oscars-count" current={sortField} direction={sortDirection} onSort={setSortField} />
                    <SortableTh label="MPAA" field="mpaa-rating" current={sortField} direction={sortDirection} onSort={setSortField} />
                    <SortableTh label="Добавлен" field="creation-date" current={sortField} direction={sortDirection} onSort={setSortField} />
                    <th aria-label="Действия" />
                  </tr></thead>
                  <tbody>
                    {movies.map((item) => (
                      <MovieRows key={item.id} item={item} expanded={expandedId === item.id} onToggle={() => setExpandedId(expandedId === item.id ? null : item.id)} onEdit={() => setMovieModal({ mode: 'edit', movie: item })} onDelete={() => setConfirmDelete(item)} />
                    ))}
                  </tbody>
                </table>
                {!movies.length && !loading && <EmptyState onReset={() => { setFilters(emptyFilters); setDraftFilters(emptyFilters); }} />}
              </div>
              <Pagination page={page} size={size} total={totalElements} totalPages={totalPages} onPage={setPage} onSize={(value) => { setSize(value); setPage(0); }} />
            </section>
          </>
        ) : (
          <OperationsPage jobs={jobs} currentJob={currentJob} resetGenre={resetGenre} setResetGenre={setResetGenre} runJob={runOscarJob} inspectJob={inspectJob} onToast={setToast} />
        )}
      </main>

      {filtersOpen && <FiltersDrawer value={draftFilters} onChange={setDraftFilters} onClose={() => setFiltersOpen(false)} onApply={() => { setFilters(draftFilters); setPage(0); setFiltersOpen(false); }} onReset={() => setDraftFilters(emptyFilters)} />}
      {movieModal && <MovieModal mode={movieModal.mode} movie={movieModal.movie} writers={writers} onClose={() => setMovieModal(null)} onSave={saveMovie} />}
      {toolsOpen && <ToolsModal movies={movies} writers={writers} averageBudget={averageBudget} onClose={() => setToolsOpen(false)} onDeleteMpaa={(rating) => { void deleteByMpaa(rating); }} onDeleteWriter={(name) => { void deleteByWriter(name); }} />}
      {confirmDelete && <ConfirmModal title="Удалить фильм?" text={`«${confirmDelete.name}» будет удалён из коллекции. Отменить это действие не получится.`} confirm="Удалить" onCancel={() => setConfirmDelete(null)} onConfirm={() => removeMovie(confirmDelete)} />}
      {toast && <div className={`toast ${toast.kind}`} role="status"><span>{toast.kind === 'success' ? <Check /> : toast.kind === 'error' ? <AlertCircle /> : <Clock3 />}</span><div><b>{toast.title}</b><p>{toast.message}</p></div><button onClick={() => setToast(null)}><X size={16} /></button></div>}
    </div>
  );
}

function Metric({ icon, label, value, note, tone, loading }: { icon: React.ReactNode; label: string; value: string; note: string; tone: string; loading?: boolean }) {
  return <article className="metric"><span className={`metric-icon ${tone}`}>{icon}</span><div><small>{label}</small><b>{value}</b><p>{loading && <LoaderCircle className="spin" size={13} />}{note}</p></div></article>;
}

function SortableTh({ label, field, current, direction, onSort }: { label: string; field: string; current: string; direction: SortDirection; onSort: (field: string) => void }) {
  return <th><button className={current === field ? 'sort-active' : ''} onClick={() => onSort(field)}>{label}{current === field ? direction === 'asc' ? <ArrowUp size={13} /> : <ArrowDown size={13} /> : <ChevronsUpDown size={13} />}</button></th>;
}

function MovieRows({ item, expanded, onToggle, onEdit, onDelete }: { item: Movie; expanded: boolean; onToggle: () => void; onEdit: () => void; onDelete: () => void }) {
  return <>
    <tr className={`movie-row ${expanded ? 'expanded' : ''}`} onClick={onToggle}>
      <td className="expander-cell"><button className="row-expand" aria-label={expanded ? 'Свернуть' : 'Раскрыть'}><ChevronDown size={17} /></button></td>
      <td><span className="mono id-value">#{item.id}</span></td>
      <td><div className="movie-name"><span className={`genre-dot genre-${item.genre.toLowerCase()}`} /><div><b>{item.name}</b><small>{genreLabels[item.genre]}</small></div></div></td>
      <td>{item.screenwriter ? <div className="writer-cell"><span>{initials(item.screenwriter.name)}</span><div><b>{item.screenwriter.name}</b><small>{countryLabels[item.screenwriter.nationality]}</small></div></div> : <span className="muted">Не указан</span>}</td>
      <td className="tabular">{formatMoney(item.budget)}</td>
      <td><span className={`oscar-count ${item.oscarsCount ? 'awarded' : ''}`}><Award size={15} /> {item.oscarsCount}</span></td>
      <td><span className={`rating rating-${item.mpaaRating.toLowerCase()}`}>{item.mpaaRating.replace('_', '-')}</span></td>
      <td><span className="date-cell">{formatDate(item.creationDate)}</span></td>
      <td><button className="icon-button" aria-label="Действия" onClick={(event) => { event.stopPropagation(); onEdit(); }}><MoreHorizontal size={18} /></button></td>
    </tr>
    {expanded && <tr className="details-row"><td colSpan={9}><div className="movie-details">
      <div className="detail-block"><span>Координаты фильма</span><b>X {item.coordinates.x} <i /> Y {item.coordinates.y}</b></div>
      <div className="detail-block"><span>Сценарист</span>{item.screenwriter ? <b>{item.screenwriter.height} см <i /> {colorLabels[item.screenwriter.eyeColor]} цвет глаз <i /> {item.screenwriter.hairColor ? `${colorLabels[item.screenwriter.hairColor]} цвет волос` : 'цвет волос не указан'}</b> : <b>Не указан</b>}</div>
      <div className="detail-block"><span>Локация сценариста</span>{item.screenwriter ? <b>X {item.screenwriter.location.x} <i /> Y {item.screenwriter.location.y} <i /> Z {item.screenwriter.location.z}</b> : <b>—</b>}</div>
      <div className="detail-actions"><button className="button secondary" onClick={onEdit}><Edit3 size={16} /> Изменить</button><button className="button danger-ghost" onClick={onDelete}><Trash2 size={16} /> Удалить</button></div>
    </div></td></tr>}
  </>;
}

function Pagination({ page, size, total, totalPages, onPage, onSize }: { page: number; size: number; total: number; totalPages: number; onPage: (page: number) => void; onSize: (size: number) => void }) {
  const from = total ? page * size + 1 : 0;
  const to = Math.min((page + 1) * size, total);
  return <div className="pagination"><div>Показано <b>{from}–{to}</b> из <b>{total}</b></div><div className="page-size">Строк на странице <select value={size} onChange={(event) => onSize(Number(event.target.value))}><option>5</option><option>10</option><option>25</option><option>50</option></select></div><div className="page-buttons"><button disabled={page === 0} onClick={() => onPage(page - 1)}><ChevronLeft size={17} /></button><span>{page + 1} / {totalPages}</span><button disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}><ChevronRight size={17} /></button></div></div>;
}

function EmptyState({ onReset }: { onReset: () => void }) {
  return <div className="empty-state"><span><Film size={25} /></span><b>Ничего не найдено</b><p>Попробуйте изменить условия фильтрации.</p><button className="button secondary" onClick={onReset}><RotateCcw size={16} /> Сбросить фильтры</button></div>;
}

function OperationsPage({ jobs, currentJob, resetGenre, setResetGenre, runJob, inspectJob, onToast }: { jobs: OscarJob[]; currentJob?: OscarJob; resetGenre: Genre; setResetGenre: (genre: Genre) => void; runJob: (type: OscarJob['type'], genre?: Genre) => void; inspectJob: (id: string) => void; onToast: (toast: Toast) => void }) {
  const [lookup, setLookup] = useState('');
  const inspect = () => {
    const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    if (!uuid.test(lookup)) return onToast({ kind: 'error', title: 'Невалидный идентификатор', message: 'Введите UUID джобы в формате xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx' });
    inspectJob(lookup);
  };
  return <>
    <section className="page-heading operations-heading"><div><h1>Статус операций</h1></div><div className="heading-actions operations-actions"><TooltipButton className="button secondary" label="Наградить фильмы категории R" tooltip="Увеличить количество Оскаров на 1 у всех фильмов категории R" onClick={() => runJob('REWARD_R')} disabled={Boolean(currentJob)} /><div className="compound-action light"><select aria-label="Жанр для отбора Оскаров" value={resetGenre} onChange={(event) => setResetGenre(event.target.value as Genre)}>{GENRES.map((genre) => <option key={genre} value={genre}>{genreLabels[genre]}</option>)}</select><TooltipButton label="Отобрать Оскары у фильмов" tooltip="Обнулить количество Оскаров у всех фильмов, режиссёр которых снял хотя бы один фильм в выбранном жанре" onClick={() => runJob('RESET_BY_GENRE', resetGenre)} disabled={Boolean(currentJob)} /></div></div></section>
    <section className="job-hero">
      <div className={`job-hero-state ${currentJob ? 'running' : 'idle'}`}>{currentJob ? <LoaderCircle className="spin" /> : <Check />}</div>
      <div><span>Текущая операция</span><h2>{currentJob?.label ?? 'Операции не выполняются'}</h2>{currentJob && <p>Джоба {currentJob.id} · {statusText[currentJob.status].toLowerCase()}</p>}</div>
      {currentJob && <div className="job-progress"><span><i /></span><small>Сервис обрабатывает фильмы</small></div>}
    </section>
    <section className="jobs-layout">
      <div className="jobs-card">
        <div className="card-heading"><div><h2>Журнал операций</h2><p>{jobs.length} записей</p></div><button className="icon-button" title="Обновить"><RefreshCw size={17} /></button></div>
        <div className="jobs-list">
          {jobs.map((job) => <article className="job-row" key={job.id}><span className={`job-status-icon ${job.status}`}>{job.status === 'COMPLETED' ? <Check /> : job.status === 'FAILED' ? <AlertCircle /> : <LoaderCircle className={job.status === 'RUNNING' ? 'spin' : ''} />}</span><div className="job-main"><b>{job.label}</b><span className="mono">{job.id}</span></div><div className="job-time"><b>{new Intl.DateTimeFormat('ru-RU', { hour: '2-digit', minute: '2-digit' }).format(new Date(job.createdAt))}</b><span>{formatDate(job.createdAt)}</span></div><span className={`status-badge ${job.status}`}>{statusText[job.status]}</span><div className="job-result">{job.status === 'COMPLETED' ? <><b>{job.updatedCount}</b><span>изменено</span></> : job.status === 'FAILED' ? <><b>—</b><span>{job.errorMessage}</span></> : <><b>···</b><span>ожидание</span></>}</div></article>)}
        </div>
      </div>
      <aside className="lookup-card"><span className="lookup-icon"><Search size={20} /></span><h3>Проверить джобу</h3><p>Введите идентификатор из ответа <code>202 Accepted</code>.</p><label>ID операции<input value={lookup} onChange={(event) => setLookup(event.target.value)} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" /></label><button className="button primary" onClick={inspect}>Проверить статус <ExternalLink size={16} /></button><div className="endpoint-note"><span>GET</span><code>/oscar/jobs/{'{job-id}'}/status</code></div></aside>
    </section>
  </>;
}

function FiltersDrawer({ value, onChange, onClose, onApply, onReset }: { value: Filters; onChange: (value: Filters) => void; onClose: () => void; onApply: () => void; onReset: () => void }) {
  const set = (key: keyof Filters, next: Filters[keyof Filters]) => onChange({ ...value, [key]: next });
  return <div className="overlay drawer-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><aside className="drawer">
    <div className="modal-heading"><div><span className="modal-icon"><SlidersHorizontal size={19} /></span><div><h2>Фильтры</h2><p>Все поля MovieFilter</p></div></div><button className="icon-button" onClick={onClose}><X size={20} /></button></div>
    <div className="drawer-body">
      <FilterSection title="Основное">
        <Field label="Название"><input value={value.name} onChange={(e) => set('name', e.target.value)} placeholder="Подстрока без учёта регистра" /></Field>
        <Field label="ID через запятую"><input value={value.ids} onChange={(e) => set('ids', e.target.value)} placeholder="1042, 1038, 1029" /></Field>
        <Field label="Жанры" wide><div className="choice-grid">{GENRES.map((genre) => <label className="check-choice" key={genre}><input type="checkbox" checked={value.genres.includes(genre)} onChange={() => set('genres', value.genres.includes(genre) ? value.genres.filter((item) => item !== genre) : [...value.genres, genre])} /><span><Check size={13} /></span>{genreLabels[genre]}</label>)}</div></Field>
        <Field label="Рейтинг MPAA"><select value={value.mpaaRating} onChange={(e) => set('mpaaRating', e.target.value)}><option value="">Любой</option>{MPAA.map((item) => <option key={item} value={item}>{item.replace('_', '-')}</option>)}</select></Field>
      </FilterSection>
      <FilterSection title="Числа и даты">
        <RangeFields label="Оскары" from={value.oscarsFrom} to={value.oscarsTo} onFrom={(v) => set('oscarsFrom', v)} onTo={(v) => set('oscarsTo', v)} />
        <RangeFields label="Бюджет, $" from={value.budgetFrom} to={value.budgetTo} onFrom={(v) => set('budgetFrom', v)} onTo={(v) => set('budgetTo', v)} />
        <RangeFields label="Координата X" from={value.coordinateXFrom} to={value.coordinateXTo} onFrom={(v) => set('coordinateXFrom', v)} onTo={(v) => set('coordinateXTo', v)} />
        <RangeFields label="Координата Y" from={value.coordinateYFrom} to={value.coordinateYTo} onFrom={(v) => set('coordinateYFrom', v)} onTo={(v) => set('coordinateYTo', v)} />
        <Field label="Создан после"><input type="datetime-local" value={value.creationAfter} onChange={(e) => set('creationAfter', e.target.value)} /></Field>
        <Field label="Создан до"><input type="datetime-local" value={value.creationBefore} onChange={(e) => set('creationBefore', e.target.value)} /></Field>
      </FilterSection>
      <FilterSection title="Сценарист">
        <Field label="Имя"><input value={value.screenwriterName} onChange={(e) => set('screenwriterName', e.target.value)} placeholder="Подстрока имени" /></Field>
        <RangeFields label="Рост, см" from={value.heightFrom} to={value.heightTo} onFrom={(v) => set('heightFrom', v)} onTo={(v) => set('heightTo', v)} />
        <Field label="Цвет глаз"><select value={value.eyeColor} onChange={(e) => set('eyeColor', e.target.value)}><option value="">Любой</option>{EYE_COLORS.map((item) => <option key={item} value={item}>{colorLabels[item]}</option>)}</select></Field>
        <Field label="Цвет волос"><select value={value.hairColor} onChange={(e) => set('hairColor', e.target.value)}><option value="">Любой</option>{HAIR_COLORS.map((item) => <option key={item} value={item}>{colorLabels[item]}</option>)}</select></Field>
        <Field label="Гражданство"><select value={value.nationality} onChange={(e) => set('nationality', e.target.value)}><option value="">Любое</option>{COUNTRIES.map((item) => <option key={item} value={item}>{countryLabels[item]}</option>)}</select></Field>
      </FilterSection>
      <FilterSection title="Локация сценариста">
        <RangeFields label="Локация X" from={value.locationXFrom} to={value.locationXTo} onFrom={(v) => set('locationXFrom', v)} onTo={(v) => set('locationXTo', v)} />
        <RangeFields label="Локация Y" from={value.locationYFrom} to={value.locationYTo} onFrom={(v) => set('locationYFrom', v)} onTo={(v) => set('locationYTo', v)} />
        <RangeFields label="Локация Z" from={value.locationZFrom} to={value.locationZTo} onFrom={(v) => set('locationZFrom', v)} onTo={(v) => set('locationZTo', v)} />
      </FilterSection>
    </div>
    <div className="modal-footer"><button className="button secondary" onClick={onReset}><RotateCcw size={16} /> Сбросить</button><button className="button primary" onClick={onApply}>Показать результаты</button></div>
  </aside></div>;
}

function MovieModal({ mode, movie, writers, onClose, onSave }: { mode: 'create' | 'edit'; movie?: Movie; writers: Person[]; onClose: () => void; onSave: (draft: MovieDraft, movie?: Movie) => void }) {
  const [draft, setDraft] = useState<MovieDraft>(movie ? draftFromMovie(movie) : { ...emptyDraft, existingScreenwriter: writers[0]?.name ?? '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = <K extends keyof MovieDraft>(key: K, value: MovieDraft[K]) => setDraft((current) => ({ ...current, [key]: value }));
  const submit = () => {
    const next: Record<string, string> = {};
    if (!draft.name.trim()) next.name = 'Укажите название фильма';
    else if (!/^[a-zA-Zа-яА-ЯЁё0-9 .,:!?()&-]+$/.test(draft.name.trim())) next.name = 'Есть недопустимые символы';
    if (draft.coordinateX === '' || Number(draft.coordinateX) <= -130) next.coordinateX = 'Значение должно быть больше −130';
    if (draft.coordinateY === '' || Number(draft.coordinateY) > 388) next.coordinateY = 'Значение должно быть не больше 388';
    if (draft.oscarsCount === '' || !Number.isInteger(Number(draft.oscarsCount)) || Number(draft.oscarsCount) < 0) next.oscarsCount = 'Укажите целое число не меньше 0';
    if (draft.budget && Number(draft.budget) < 1) next.budget = 'Бюджет должен быть больше 0';
    if (draft.screenwriterMode === 'existing' && !draft.existingScreenwriter) next.existingScreenwriter = 'Выберите сценариста';
    if (draft.screenwriterMode === 'new') {
      if (!draft.screenwriterName.trim()) next.screenwriterName = 'Укажите имя';
      if (Number(draft.screenwriterHeight) <= 0) next.screenwriterHeight = 'Рост должен быть больше 0';
      if (draft.locationX === '') next.locationX = 'Обязательное поле';
      if (draft.locationY === '' || !Number.isInteger(Number(draft.locationY))) next.locationY = 'Введите целое число';
      if (draft.locationZ === '' || !Number.isInteger(Number(draft.locationZ))) next.locationZ = 'Введите целое число';
    }
    setErrors(next);
    if (!Object.keys(next).length) onSave(draft, movie);
  };
  return <div className="overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><div className="modal movie-modal">
    <div className="modal-heading"><div><span className="modal-icon"><Film size={19} /></span><div><h2>{mode === 'create' ? 'Новый фильм' : 'Редактировать фильм'}</h2><p>{mode === 'create' ? 'Заполните данные MovieInput' : `ID ${movie?.id} · полное обновление`}</p></div></div><button className="icon-button" onClick={onClose}><X size={20} /></button></div>
    <div className="modal-body">
      <div className="form-section"><h3>О фильме</h3><div className="form-grid">
        <Field label="Название *" wide error={errors.name}><input autoFocus value={draft.name} onChange={(e) => set('name', e.target.value)} placeholder="Например, Интерстеллар" /></Field>
        <Field label="Жанр *"><select value={draft.genre} onChange={(e) => set('genre', e.target.value as MovieDraft['genre'])}>{GENRES.map((item) => <option key={item} value={item}>{genreLabels[item]}</option>)}</select></Field>
        <Field label="Рейтинг MPAA *"><select value={draft.mpaaRating} onChange={(e) => set('mpaaRating', e.target.value as MovieDraft['mpaaRating'])}>{MPAA.map((item) => <option key={item} value={item}>{item.replace('_', '-')}</option>)}</select></Field>
        <Field label="Оскары *" error={errors.oscarsCount}><input type="number" min="0" value={draft.oscarsCount} onChange={(e) => set('oscarsCount', e.target.value)} /></Field>
        <Field label="Бюджет, $" hint="Можно оставить пустым" error={errors.budget}><input type="number" min="1" value={draft.budget} onChange={(e) => set('budget', e.target.value)} placeholder="165000000" /></Field>
        <Field label="Координата X *" error={errors.coordinateX}><input type="number" step="any" value={draft.coordinateX} onChange={(e) => set('coordinateX', e.target.value)} placeholder="> −130" /></Field>
        <Field label="Координата Y *" error={errors.coordinateY}><input type="number" step="any" value={draft.coordinateY} onChange={(e) => set('coordinateY', e.target.value)} placeholder="≤ 388" /></Field>
      </div></div>
      <div className="form-section"><h3>Сценарист</h3><div className="segmented"><button className={draft.screenwriterMode === 'existing' ? 'active' : ''} onClick={() => set('screenwriterMode', 'existing')}>Выбрать существующего</button><button className={draft.screenwriterMode === 'new' ? 'active' : ''} onClick={() => set('screenwriterMode', 'new')}>Создать нового</button><button className={draft.screenwriterMode === 'none' ? 'active' : ''} onClick={() => set('screenwriterMode', 'none')}>Не указывать</button></div>
        {draft.screenwriterMode === 'existing' && <div className="writer-picker">{writers.map((writer) => <button key={writer.name} className={draft.existingScreenwriter === writer.name ? 'selected' : ''} onClick={() => set('existingScreenwriter', writer.name)}><span>{initials(writer.name)}</span><div><b>{writer.name}</b><small>{countryLabels[writer.nationality]} · {writer.height} см</small></div><i><Check size={14} /></i></button>)}</div>}
        {draft.screenwriterMode === 'new' && <div className="form-grid writer-form">
          <Field label="Имя *" wide error={errors.screenwriterName}><input value={draft.screenwriterName} onChange={(e) => set('screenwriterName', e.target.value)} /></Field>
          <Field label="Рост, см *" error={errors.screenwriterHeight}><input type="number" value={draft.screenwriterHeight} onChange={(e) => set('screenwriterHeight', e.target.value)} /></Field>
          <Field label="Цвет глаз *"><select value={draft.eyeColor} onChange={(e) => set('eyeColor', e.target.value as MovieDraft['eyeColor'])}>{EYE_COLORS.map((item) => <option key={item} value={item}>{colorLabels[item]}</option>)}</select></Field>
          <Field label="Цвет волос"><select value={draft.hairColor} onChange={(e) => set('hairColor', e.target.value as MovieDraft['hairColor'])}><option value="">Не указан</option>{HAIR_COLORS.map((item) => <option key={item} value={item}>{colorLabels[item]}</option>)}</select></Field>
          <Field label="Гражданство *"><select value={draft.nationality} onChange={(e) => set('nationality', e.target.value as MovieDraft['nationality'])}>{COUNTRIES.map((item) => <option key={item} value={item}>{countryLabels[item]}</option>)}</select></Field>
          <Field label="Локация X *" error={errors.locationX}><input type="number" step="any" value={draft.locationX} onChange={(e) => set('locationX', e.target.value)} /></Field>
          <Field label="Локация Y *" error={errors.locationY}><input type="number" value={draft.locationY} onChange={(e) => set('locationY', e.target.value)} /></Field>
          <Field label="Локация Z *" error={errors.locationZ}><input type="number" value={draft.locationZ} onChange={(e) => set('locationZ', e.target.value)} /></Field>
        </div>}
      </div>
      {Object.keys(errors).length > 0 && <div className="validation-alert"><AlertCircle size={18} /><div><b>Сервис отклонил бы эти данные</b><span>Исправьте отмеченные поля и повторите попытку.</span></div></div>}
    </div>
    <div className="modal-footer"><button className="button secondary" onClick={onClose}>Отмена</button><button className="button primary" onClick={submit}>{mode === 'create' ? <><Plus size={17} /> Создать фильм</> : <><Check size={17} /> Сохранить изменения</>}</button></div>
  </div></div>;
}

function ToolsModal({ movies, writers, averageBudget, onClose, onDeleteMpaa, onDeleteWriter }: { movies: Movie[]; writers: Person[]; averageBudget: number; onClose: () => void; onDeleteMpaa: (rating: string) => void; onDeleteWriter: (name: string) => void }) {
  const [rating, setRating] = useState('R');
  const [writer, setWriter] = useState(writers[0]?.name ?? '');
  return <div className="overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><div className="modal tools-modal"><div className="modal-heading"><div><span className="modal-icon"><SlidersHorizontal size={19} /></span><div><h2>Инструменты коллекции</h2><p>Дополнительные операции с фильмами</p></div></div><button className="icon-button" onClick={onClose}><X size={20} /></button></div><div className="modal-body tools-grid">
    <article className="tool-item"><span className="tool-icon green"><BarChart3 /></span><div><small>GET /movies/average-budget</small><h3>Средний бюджет</h3><strong>{formatMoney(averageBudget)}</strong><p>Рассчитано по {movies.filter((item) => item.budget != null).length} фильмам с указанным бюджетом.</p></div></article>
    <article className="tool-item danger-tool"><span className="tool-icon red"><Trash2 /></span><div><small>DELETE /movies/by-mpaa-rating</small><h3>Удалить по рейтингу</h3><p>Будут удалены все фильмы с выбранным MPAA.</p><div className="tool-action"><select value={rating} onChange={(e) => setRating(e.target.value)}>{MPAA.map((item) => <option key={item} value={item}>{item.replace('_', '-')}</option>)}</select><button onClick={() => onDeleteMpaa(rating)}>Удалить {movies.filter((item) => item.mpaaRating === rating).length}</button></div></div></article>
    <article className="tool-item danger-tool"><span className="tool-icon orange"><UserRound /></span><div><small>DELETE /movies/by-screenwriter</small><h3>Удалить один фильм</h3><p>Удалится первый найденный фильм указанного сценариста.</p><div className="tool-action"><select value={writer} onChange={(e) => setWriter(e.target.value)}>{writers.map((item) => <option key={item.name}>{item.name}</option>)}</select><button onClick={() => onDeleteWriter(writer)}>Удалить</button></div></div></article>
  </div><div className="modal-footer"><button className="button secondary" onClick={onClose}>Закрыть</button></div></div></div>;
}

function ConfirmModal({ title, text, confirm, onCancel, onConfirm }: { title: string; text: string; confirm: string; onCancel: () => void; onConfirm: () => void }) {
  return <div className="overlay confirm-overlay"><div className="modal confirm-modal"><span className="confirm-icon"><Trash2 /></span><h2>{title}</h2><p>{text}</p><div><button className="button secondary" onClick={onCancel}>Отмена</button><button className="button danger" onClick={onConfirm}>{confirm}</button></div></div></div>;
}

function TooltipButton({ label, tooltip, className = '', disabled, onClick }: { label: string; tooltip: string; className?: string; disabled?: boolean; onClick: () => void }) {
  const tooltipId = useId();
  return <span className="action-tooltip-wrap">
    <button className={className} disabled={disabled} onClick={onClick} aria-describedby={tooltipId}>{label}</button>
    <span className="action-tooltip" id={tooltipId} role="tooltip">{tooltip}</span>
  </span>;
}

function FilterSection({ title, children }: { title: string; children: React.ReactNode }) { return <section className="filter-section"><h3>{title}</h3><div className="filter-grid">{children}</div></section>; }
function Field({ label, hint, error, wide, children }: { label: string; hint?: string; error?: string; wide?: boolean; children: React.ReactNode }) { return <label className={`field ${wide ? 'wide' : ''} ${error ? 'invalid' : ''}`}><span>{label}{hint && <small>{hint}</small>}</span>{children}{error && <em>{error}</em>}</label>; }
function RangeFields({ label, from, to, onFrom, onTo }: { label: string; from: string; to: string; onFrom: (value: string) => void; onTo: (value: string) => void }) { return <Field label={label} wide><div className="range-fields"><input type="number" value={from} onChange={(e) => onFrom(e.target.value)} placeholder="Строго больше" /><span>—</span><input type="number" value={to} onChange={(e) => onTo(e.target.value)} placeholder="Строго меньше" /></div></Field>; }
const initials = (name: string) => name.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase();
const compactMoney = (value: number) => `$${new Intl.NumberFormat('ru-RU', { notation: 'compact', maximumFractionDigits: 1 }).format(value)}`;

export default App;
