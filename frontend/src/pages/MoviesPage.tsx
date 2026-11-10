import { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle, ArrowDown, ArrowUp, Award, Check, ChevronDown, ChevronLeft,
  ChevronRight, ChevronsUpDown, CircleDollarSign, Edit3, Film, Filter, LoaderCircle,
  MoreHorizontal, Plus, RotateCcw, Search, SlidersHorizontal, Trash2, X,
} from 'lucide-react';
import type { Filters, Movie, MovieDraft, MoviePage, Person, ShowError, ShowToast, SortDirection } from '../types';
import { api } from '../api';
import {
  COUNTRIES, EYE_COLORS, HAIR_COLORS, MPAA, colorLabels, countryLabels, draftFromMovie,
  emptyDraft, emptyFilters, GENRES, genreLabels,
} from '../labels';
import { compactMoney, formatDate, formatMoney, initials } from '../format';
import { ConfirmModal, Field, FilterSection, NumericInput, RangeFields } from '../components/ui';
import { IDS_MAX_LENGTH, TEXT_MAX_LENGTH, numberRules, validateFilters, validateMovieDraft } from '../validation';

const sortOptions = [
  ['id', 'ID'], ['name', 'Название'], ['coordinates-x', 'Координата X'], ['coordinates-y', 'Координата Y'],
  ['creation-date', 'Дата создания'], ['oscars-count', 'Оскары'], ['budget', 'Бюджет'], ['genre', 'Жанр'],
  ['mpaa-rating', 'Рейтинг MPAA'], ['screenwriter-name', 'Имя сценариста'], ['screenwriter-height', 'Рост сценариста'],
  ['screenwriter-eye-color', 'Цвет глаз'], ['screenwriter-hair-color', 'Цвет волос'], ['screenwriter-nationality', 'Гражданство'],
  ['screenwriter-location-x', 'Локация сценариста X'], ['screenwriter-location-y', 'Локация сценариста Y'], ['screenwriter-location-z', 'Локация сценариста Z'],
] as const;

const present = (value: string) => value === '' ? undefined : Number(value);
const compactObject = (value: Record<string, unknown>) => Object.fromEntries(
  Object.entries(value).filter(([, item]) => item !== undefined && item !== '' && (!Array.isArray(item) || item.length > 0)),
);

const toMovieFilter = (filters: Filters) => {
  const errors = validateFilters(filters);
  if (Object.keys(errors).length) throw new Error(Object.values(errors).join('; '));
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

export function MoviesPage({ revision, showToast, showError }: { revision: number; showToast: ShowToast; showError: ShowError }) {
  const [movies, setMovies] = useState<Movie[]>([]);
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
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [movieModal, setMovieModal] = useState<{ mode: 'create' | 'edit'; movie?: Movie } | null>(null);
  const [deleteRating, setDeleteRating] = useState('R');
  const [deleteWriter, setDeleteWriter] = useState('');
  const [confirmDelete, setConfirmDelete] = useState<Movie | null>(null);

  const totalPages = Math.max(1, serverTotalPages);
  const totalOscars = movies.reduce((sum, item) => sum + BigInt(item.oscarsCount), 0n);
  const writers = useMemo(() => Array.from(new Map(movies.flatMap((item) => item.screenwriter ? [[item.screenwriter.name, item.screenwriter] as const] : [])).values()), [movies]);
  const writerTarget = deleteWriter || writers[0]?.name || '';

  const handleSort = (field: string) => {
    setPage(0);
    if (field === sortField) {
      setSortDirection((direction) => direction === 'asc' ? 'desc' : 'asc');
      return;
    }
    setSortField(field);
    setSortDirection('asc');
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
      if (result.totalPages > 0 && page >= result.totalPages) setPage(result.totalPages - 1);
    } catch (error) {
      showError('Не удалось загрузить фильмы', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => { void loadMovies(); }, 250);
    return () => window.clearTimeout(timer);
  }, [filters, page, size, sortField, sortDirection, revision]);

  useEffect(() => { void loadAverage(); }, [revision]);

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
      showToast({ kind: 'success', title: editing ? 'Фильм обновлён' : 'Фильм создан', message: `${item.name} · ID ${item.id}` });
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
      showToast({ kind: 'success', title: 'Фильм удалён', message: `${item.name} больше не в коллекции` });
      await Promise.all([loadMovies(), loadAverage()]);
    } catch (error) {
      showError('Не удалось удалить фильм', error);
    }
  };

  const deleteByMpaa = async (rating: string) => {
    try {
      const result = await api<{ deletedCount: number }>(`/movies/by-mpaa-rating?mpaa-rating=${encodeURIComponent(rating)}`, { method: 'DELETE' });
      showToast({ kind: 'success', title: 'Массовое удаление завершено', message: `Удалено фильмов: ${result.deletedCount}` });
      await Promise.all([loadMovies(), loadAverage()]);
    } catch (error) {
      showError('Не удалось удалить фильмы', error);
    }
  };

  const deleteByWriter = async (name: string) => {
    try {
      await api<void>(`/movies/by-screenwriter?name=${encodeURIComponent(name)}`, { method: 'DELETE' });
      showToast({ kind: 'success', title: 'Фильм удалён', message: `Сценарист: ${name}` });
      await Promise.all([loadMovies(), loadAverage()]);
    } catch (error) {
      showError('Не удалось удалить фильм', error);
    }
  };

  const applyQuickSearch = (value: string) => {
    setFilters((current) => ({ ...current, name: value }));
    setDraftFilters((current) => ({ ...current, name: value }));
    setPage(0);
  };

  return (
    <>
      <section className="page-heading movies-heading">
        <div className="movies-title">
          <h1>Фильмы</h1>
          <button className="button primary" onClick={() => setMovieModal({ mode: 'create' })}><Plus size={18} /> Создать фильм</button>
        </div>
        <div className="movies-actions-row">
          <div className="danger-picker">
            <span className="danger-picker-label">Удалить фильмы с рейтингом</span>
            <div className="danger-group">
              <select aria-label="Рейтинг MPAA для удаления" value={deleteRating} onChange={(event) => setDeleteRating(event.target.value)}>
                {MPAA.map((item) => <option key={item} value={item}>{item.replace('_', '-')}</option>)}
              </select>
              <button className="button danger" onClick={() => { void deleteByMpaa(deleteRating); }}>Взорвать</button>
            </div>
          </div>
          <div className="danger-picker">
            <span className="danger-picker-label">Удалить один фильм режиссера</span>
            <div className="danger-group">
              <select aria-label="Сценарист для удаления" value={writerTarget} onChange={(event) => setDeleteWriter(event.target.value)}>
                {writers.map((item) => <option key={item.name} value={item.name}>{item.name}</option>)}
              </select>
              <button className="button danger" disabled={!writerTarget} onClick={() => { void deleteByWriter(writerTarget); }}>Взорвать</button>
            </div>
          </div>
        </div>
      </section>

      <section className="metrics-grid">
        <Metric icon={<Film />} label="Фильмов в выборке" value={String(totalElements)} note={`${movies.length} на текущей странице`} tone="ink" loading={loading} />
        <Metric icon={<CircleDollarSign />} label="Средний бюджет" value={compactMoney(averageBudget)} note="по всей коллекции" tone="green" loading={loading} />
        <Metric icon={<Award />} label="Оскаров на странице" value={String(totalOscars)} note={`${movies.filter((item) => item.oscarsCount > 0).length} награжденных фильмов`} tone="gold" loading={loading} />
      </section>

      <section className="table-card">
        <div className="table-toolbar">
          <div className="search-box"><Search size={18} /><input maxLength={TEXT_MAX_LENGTH} value={filters.name} onChange={(event) => applyQuickSearch(event.target.value)} placeholder="Название фильма…" /></div>
          <div className="toolbar-group">
            <button className={`button filter-button ${activeFilterCount(filters) ? 'has-filters' : ''}`} onClick={() => { setDraftFilters(filters); setFiltersOpen(true); }}>
              <Filter size={17} /> Фильтры {activeFilterCount(filters) > 0 && <span>{activeFilterCount(filters)}</span>}
            </button>
            <label className="sort-control"><span>Сортировка</span><select value={sortField} onChange={(event) => handleSort(event.target.value)}>{sortOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
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
            <colgroup>
              <col className="col-expander" /><col className="col-id" /><col className="col-name" /><col className="col-writer" /><col className="col-budget" />
              <col className="col-oscars" /><col className="col-mpaa" /><col className="col-date" /><col className="col-actions" />
            </colgroup>
            <thead><tr>
              <th className="expander-cell" />
              <SortableTh label="ID" field="id" current={sortField} direction={sortDirection} onSort={handleSort} />
              <SortableTh label="Фильм" field="name" current={sortField} direction={sortDirection} onSort={handleSort} />
              <th>Сценарист</th>
              <SortableTh label="Бюджет" field="budget" current={sortField} direction={sortDirection} onSort={handleSort} />
              <SortableTh label="Оскары" field="oscars-count" current={sortField} direction={sortDirection} onSort={handleSort} />
              <SortableTh label="MPAA" field="mpaa-rating" current={sortField} direction={sortDirection} onSort={handleSort} />
              <SortableTh label="Добавлен" field="creation-date" current={sortField} direction={sortDirection} onSort={handleSort} />
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

      {filtersOpen && <FiltersDrawer value={draftFilters} onChange={setDraftFilters} onClose={() => setFiltersOpen(false)} onApply={() => { setFilters(draftFilters); setPage(0); setFiltersOpen(false); }} onReset={() => setDraftFilters(emptyFilters)} />}
      {movieModal && <MovieModal mode={movieModal.mode} movie={movieModal.movie} writers={writers} onClose={() => setMovieModal(null)} onSave={saveMovie} />}
      {confirmDelete && <ConfirmModal title="Удалить фильм?" text={`«${confirmDelete.name}» будет удалён из коллекции. Отменить это действие не получится.`} confirm="Удалить" onCancel={() => setConfirmDelete(null)} onConfirm={() => removeMovie(confirmDelete)} />}
    </>
  );
}

function Metric({ icon, label, value, note, tone, loading }: { icon: React.ReactNode; label: string; value: string; note: string; tone: string; loading?: boolean }) {
  return <article className="metric"><span className={`metric-icon ${tone}`}>{icon}</span><div><small>{label}</small><b title={value}>{value}</b><p>{loading && <LoaderCircle className="spin" size={13} />}{note}</p></div></article>;
}

function SortableTh({ label, field, current, direction, onSort }: { label: string; field: string; current: string; direction: SortDirection; onSort: (field: string) => void }) {
  const active = current === field;
  const nextDirection = active && direction === 'asc' ? 'desc' : 'asc';
  return <th aria-sort={active ? direction === 'asc' ? 'ascending' : 'descending' : 'none'}><button className={active ? 'sort-active' : ''} onClick={() => onSort(field)} title={`Сортировать ${nextDirection === 'asc' ? 'по возрастанию' : 'по убыванию'}`}>{label}{active ? direction === 'asc' ? <ArrowUp size={13} /> : <ArrowDown size={13} /> : <ChevronsUpDown size={13} />}</button></th>;
}

function MovieRows({ item, expanded, onToggle, onEdit, onDelete }: { item: Movie; expanded: boolean; onToggle: () => void; onEdit: () => void; onDelete: () => void }) {
  return <>
    <tr className={`movie-row ${expanded ? 'expanded' : ''}`} onClick={onToggle}>
      <td className="expander-cell"><button className="row-expand" aria-label={expanded ? 'Свернуть' : 'Раскрыть'}><ChevronDown size={17} /></button></td>
      <td><span className="mono id-value">#{item.id}</span></td>
      <td><div className="movie-name"><span className={`genre-dot genre-${item.genre.toLowerCase()}`} /><div><b title={item.name}>{item.name}</b><small>{genreLabels[item.genre]}</small></div></div></td>
      <td>{item.screenwriter ? <div className="writer-cell"><span>{initials(item.screenwriter.name)}</span><div><b title={item.screenwriter.name}>{item.screenwriter.name}</b><small>{countryLabels[item.screenwriter.nationality]}</small></div></div> : <span className="muted">Не указан</span>}</td>
      <td className="tabular" title={formatMoney(item.budget)}>{formatMoney(item.budget)}</td>
      <td><span title={String(item.oscarsCount)} className={`oscar-count ${item.oscarsCount ? 'awarded' : ''}`}><Award size={15} /> {item.oscarsCount}</span></td>
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

function FiltersDrawer({ value, onChange, onClose, onApply, onReset }: { value: Filters; onChange: (value: Filters) => void; onClose: () => void; onApply: () => void; onReset: () => void }) {
  const [errors, setErrors] = useState<Record<string, string>>({});
  const apply = () => {
    const next = validateFilters(value);
    setErrors(next);
    if (!Object.keys(next).length) onApply();
  };
  const set = (key: keyof Filters, next: Filters[keyof Filters]) => onChange({ ...value, [key]: next });
  return <div className="overlay drawer-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><aside className="drawer">
    <div className="modal-heading"><div><span className="modal-icon"><SlidersHorizontal size={19} /></span><div><h2>Фильтры</h2><p>Все поля MovieFilter</p></div></div><button className="icon-button" onClick={onClose}><X size={20} /></button></div>
    <div className="drawer-body">
      <FilterSection title="Основное">
        <Field label="Название" error={errors.name}><input maxLength={TEXT_MAX_LENGTH} value={value.name} onChange={(e) => set('name', e.target.value)} placeholder="Подстрока без учёта регистра" /></Field>
        <Field label="ID через запятую" error={errors.ids}><input maxLength={IDS_MAX_LENGTH} value={value.ids} onChange={(e) => set('ids', e.target.value)} placeholder="1042, 1038, 1029" /></Field>
        <Field label="Жанры" wide><div className="choice-grid">{GENRES.map((genre) => <label className="check-choice" key={genre}><input type="checkbox" checked={value.genres.includes(genre)} onChange={() => set('genres', value.genres.includes(genre) ? value.genres.filter((item) => item !== genre) : [...value.genres, genre])} /><span><Check size={13} /></span>{genreLabels[genre]}</label>)}</div></Field>
        <Field label="Рейтинг MPAA"><select value={value.mpaaRating} onChange={(e) => set('mpaaRating', e.target.value)}><option value="">Любой</option>{MPAA.map((item) => <option key={item} value={item}>{item.replace('_', '-')}</option>)}</select></Field>
      </FilterSection>
      <FilterSection title="Числа и даты">
        <RangeFields label="Оскары" integer={true} error={errors.oscarsFrom || errors.oscarsTo} from={value.oscarsFrom} to={value.oscarsTo} onFrom={(v) => set('oscarsFrom', v)} onTo={(v) => set('oscarsTo', v)} />
        <RangeFields label="Бюджет, $" integer={true} error={errors.budgetFrom || errors.budgetTo} from={value.budgetFrom} to={value.budgetTo} onFrom={(v) => set('budgetFrom', v)} onTo={(v) => set('budgetTo', v)} />
        <RangeFields label="Координата X" integer={false} error={errors.coordinateXFrom || errors.coordinateXTo} from={value.coordinateXFrom} to={value.coordinateXTo} onFrom={(v) => set('coordinateXFrom', v)} onTo={(v) => set('coordinateXTo', v)} />
        <RangeFields label="Координата Y" integer={false} error={errors.coordinateYFrom || errors.coordinateYTo} from={value.coordinateYFrom} to={value.coordinateYTo} onFrom={(v) => set('coordinateYFrom', v)} onTo={(v) => set('coordinateYTo', v)} />
        <Field label="Создан после" error={errors.creationAfter}><input type="datetime-local" value={value.creationAfter} onChange={(e) => set('creationAfter', e.target.value)} /></Field>
        <Field label="Создан до" error={errors.creationBefore}><input type="datetime-local" value={value.creationBefore} onChange={(e) => set('creationBefore', e.target.value)} /></Field>
      </FilterSection>
      <FilterSection title="Сценарист">
        <Field label="Имя" error={errors.screenwriterName}><input maxLength={TEXT_MAX_LENGTH} value={value.screenwriterName} onChange={(e) => set('screenwriterName', e.target.value)} placeholder="Подстрока имени" /></Field>
        <RangeFields label="Рост, см" integer={false} error={errors.heightFrom || errors.heightTo} from={value.heightFrom} to={value.heightTo} onFrom={(v) => set('heightFrom', v)} onTo={(v) => set('heightTo', v)} />
        <Field label="Цвет глаз"><select value={value.eyeColor} onChange={(e) => set('eyeColor', e.target.value)}><option value="">Любой</option>{EYE_COLORS.map((item) => <option key={item} value={item}>{colorLabels[item]}</option>)}</select></Field>
        <Field label="Цвет волос"><select value={value.hairColor} onChange={(e) => set('hairColor', e.target.value)}><option value="">Любой</option>{HAIR_COLORS.map((item) => <option key={item} value={item}>{colorLabels[item]}</option>)}</select></Field>
        <Field label="Гражданство"><select value={value.nationality} onChange={(e) => set('nationality', e.target.value)}><option value="">Любое</option>{COUNTRIES.map((item) => <option key={item} value={item}>{countryLabels[item]}</option>)}</select></Field>
      </FilterSection>
      <FilterSection title="Локация сценариста">
        <RangeFields label="Локация X" integer={false} error={errors.locationXFrom || errors.locationXTo} from={value.locationXFrom} to={value.locationXTo} onFrom={(v) => set('locationXFrom', v)} onTo={(v) => set('locationXTo', v)} />
        <RangeFields label="Локация Y" integer={true} error={errors.locationYFrom || errors.locationYTo} from={value.locationYFrom} to={value.locationYTo} onFrom={(v) => set('locationYFrom', v)} onTo={(v) => set('locationYTo', v)} />
        <RangeFields label="Локация Z" integer={true} error={errors.locationZFrom || errors.locationZTo} from={value.locationZFrom} to={value.locationZTo} onFrom={(v) => set('locationZFrom', v)} onTo={(v) => set('locationZTo', v)} />
      </FilterSection>
    </div>
    <div className="modal-footer"><button className="button secondary" onClick={() => { setErrors({}); onReset(); }}><RotateCcw size={16} /> Сбросить</button><button className="button primary" onClick={apply}>Показать результаты</button></div>
  </aside></div>;
}

function MovieModal({ mode, movie, writers, onClose, onSave }: { mode: 'create' | 'edit'; movie?: Movie; writers: Person[]; onClose: () => void; onSave: (draft: MovieDraft, movie?: Movie) => void }) {
  const [draft, setDraft] = useState<MovieDraft>(movie ? draftFromMovie(movie) : { ...emptyDraft, existingScreenwriter: writers[0]?.name ?? '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = <K extends keyof MovieDraft>(key: K, value: MovieDraft[K]) => setDraft((current) => ({ ...current, [key]: value }));
  const submit = () => {
    const next = validateMovieDraft(draft);
    setErrors(next);
    if (!Object.keys(next).length) onSave(draft, movie);
  };
  return <div className="overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><div className="modal movie-modal">
    <div className="modal-heading"><div><span className="modal-icon"><Film size={19} /></span><div><h2>{mode === 'create' ? 'Новый фильм' : 'Редактировать фильм'}</h2><p>{mode === 'create' ? 'Заполните данные MovieInput' : `ID ${movie?.id} · полное обновление`}</p></div></div><button className="icon-button" onClick={onClose}><X size={20} /></button></div>
    <div className="modal-body">
      <div className="form-section"><h3>О фильме</h3><div className="form-grid">
        <Field label="Название *" hint={`${draft.name.length}/${TEXT_MAX_LENGTH}`} wide error={errors.name}><input autoFocus maxLength={TEXT_MAX_LENGTH} value={draft.name} onChange={(e) => set('name', e.target.value)} placeholder="Например, Интерстеллар" /></Field>
        <Field label="Жанр *"><select value={draft.genre} onChange={(e) => set('genre', e.target.value as MovieDraft['genre'])}>{GENRES.map((item) => <option key={item} value={item}>{genreLabels[item]}</option>)}</select></Field>
        <Field label="Рейтинг MPAA *"><select value={draft.mpaaRating} onChange={(e) => set('mpaaRating', e.target.value as MovieDraft['mpaaRating'])}>{MPAA.map((item) => <option key={item} value={item}>{item.replace('_', '-')}</option>)}</select></Field>
        <Field label="Оскары *" error={errors.oscarsCount}><NumericInput integer={true} value={draft.oscarsCount} onChange={(e) => set('oscarsCount', e.target.value)} /></Field>
        <Field label="Бюджет, $" hint={`До ${numberRules.budget.max}; необязательно`} error={errors.budget}><NumericInput integer={true} value={draft.budget} onChange={(e) => set('budget', e.target.value)} placeholder="165000000" /></Field>
        <Field label="Координата X *" error={errors.coordinateX}><NumericInput integer={false} value={draft.coordinateX} onChange={(e) => set('coordinateX', e.target.value)} placeholder="> −130" /></Field>
        <Field label="Координата Y *" error={errors.coordinateY}><NumericInput integer={false} value={draft.coordinateY} onChange={(e) => set('coordinateY', e.target.value)} placeholder="≤ 388" /></Field>
      </div></div>
      <div className="form-section"><h3>Сценарист</h3><div className="segmented"><button className={draft.screenwriterMode === 'existing' ? 'active' : ''} onClick={() => set('screenwriterMode', 'existing')}>Выбрать существующего</button><button className={draft.screenwriterMode === 'new' ? 'active' : ''} onClick={() => set('screenwriterMode', 'new')}>Создать нового</button><button className={draft.screenwriterMode === 'none' ? 'active' : ''} onClick={() => set('screenwriterMode', 'none')}>Не указывать</button></div>
        {draft.screenwriterMode === 'existing' && <div className="writer-picker">{writers.map((writer) => <button key={writer.name} className={draft.existingScreenwriter === writer.name ? 'selected' : ''} onClick={() => set('existingScreenwriter', writer.name)}><span>{initials(writer.name)}</span><div><b>{writer.name}</b><small>{countryLabels[writer.nationality]} · {writer.height} см</small></div><i><Check size={14} /></i></button>)}</div>}
        {errors.existingScreenwriter && <div className="validation-alert" role="alert">{errors.existingScreenwriter}</div>}
        {draft.screenwriterMode === 'new' && <div className="form-grid writer-form">
          <Field label="Имя *" hint={`${draft.screenwriterName.length}/${TEXT_MAX_LENGTH}`} wide error={errors.screenwriterName}><input maxLength={TEXT_MAX_LENGTH} value={draft.screenwriterName} onChange={(e) => set('screenwriterName', e.target.value)} /></Field>
          <Field label="Рост, см *" error={errors.screenwriterHeight}><NumericInput integer={false} value={draft.screenwriterHeight} onChange={(e) => set('screenwriterHeight', e.target.value)} /></Field>
          <Field label="Цвет глаз *"><select value={draft.eyeColor} onChange={(e) => set('eyeColor', e.target.value as MovieDraft['eyeColor'])}>{EYE_COLORS.map((item) => <option key={item} value={item}>{colorLabels[item]}</option>)}</select></Field>
          <Field label="Цвет волос"><select value={draft.hairColor} onChange={(e) => set('hairColor', e.target.value as MovieDraft['hairColor'])}><option value="">Не указан</option>{HAIR_COLORS.map((item) => <option key={item} value={item}>{colorLabels[item]}</option>)}</select></Field>
          <Field label="Гражданство *"><select value={draft.nationality} onChange={(e) => set('nationality', e.target.value as MovieDraft['nationality'])}>{COUNTRIES.map((item) => <option key={item} value={item}>{countryLabels[item]}</option>)}</select></Field>
          <Field label="Локация X *" error={errors.locationX}><NumericInput integer={false} value={draft.locationX} onChange={(e) => set('locationX', e.target.value)} /></Field>
          <Field label="Локация Y *" error={errors.locationY}><NumericInput integer={true} value={draft.locationY} onChange={(e) => set('locationY', e.target.value)} /></Field>
          <Field label="Локация Z *" error={errors.locationZ}><NumericInput integer={true} value={draft.locationZ} onChange={(e) => set('locationZ', e.target.value)} /></Field>
        </div>}
      </div>
      {Object.keys(errors).length > 0 && <div className="validation-alert"><AlertCircle size={18} /><div><b>Проверьте введённые данные</b><span>Исправьте отмеченные поля и повторите попытку.</span></div></div>}
    </div>
    <div className="modal-footer"><button className="button secondary" onClick={onClose}>Отмена</button><button className="button primary" onClick={submit}>{mode === 'create' ? <><Plus size={17} /> Создать фильм</> : <><Check size={17} /> Сохранить изменения</>}</button></div>
  </div></div>;
}
