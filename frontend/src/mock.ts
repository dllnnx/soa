import type { Filters, Genre, Movie, MovieDraft, Person } from './types';

export const GENRES: Genre[] = ['WESTERN', 'ADVENTURE', 'TRAGEDY', 'SCIENCE_FICTION'];
export const MPAA = ['G', 'PG', 'PG_13', 'R', 'NC_17'] as const;
export const EYE_COLORS = ['GREEN', 'RED', 'BLUE', 'YELLOW', 'BROWN'] as const;
export const HAIR_COLORS = ['GREEN', 'RED', 'BLACK', 'YELLOW', 'ORANGE'] as const;
export const COUNTRIES = ['UNITED_KINGDOM', 'SOUTH_KOREA', 'JAPAN'] as const;

export const genreLabels: Record<Genre, string> = {
  WESTERN: 'Вестерн',
  ADVENTURE: 'Приключения',
  TRAGEDY: 'Трагедия',
  SCIENCE_FICTION: 'Научная фантастика',
};

export const countryLabels = {
  UNITED_KINGDOM: 'Великобритания',
  SOUTH_KOREA: 'Южная Корея',
  JAPAN: 'Япония',
};

export const colorLabels: Record<string, string> = {
  GREEN: 'Зелёный', RED: 'Красный', BLUE: 'Синий', YELLOW: 'Жёлтый', BROWN: 'Карий',
  BLACK: 'Чёрный', ORANGE: 'Рыжий',
};

const people: Record<string, Person> = {
  nolan: { name: 'Кристофер Нолан', height: 181, eyeColor: 'BLUE', hairColor: 'BLACK', nationality: 'UNITED_KINGDOM', location: { x: 51.5, y: 0, z: 15 } },
  bong: { name: 'Пон Джун-хо', height: 182, eyeColor: 'BROWN', hairColor: 'BLACK', nationality: 'SOUTH_KOREA', location: { x: 37.5, y: 126, z: 38 } },
  miyazaki: { name: 'Хаяо Миядзаки', height: 164, eyeColor: 'BROWN', hairColor: null, nationality: 'JAPAN', location: { x: 35.7, y: 139, z: 44 } },
  ishiguro: { name: 'Кадзуо Исигуро', height: 175, eyeColor: 'BROWN', hairColor: 'BLACK', nationality: 'UNITED_KINGDOM', location: { x: 51.4, y: -1, z: 22 } },
};

const movie = (id: number, name: string, genre: Genre, mpaaRating: Movie['mpaaRating'], budget: number | null, oscarsCount: number, writer: Person | null, x: number, y: number, date: string): Movie => ({
  id, name, genre, mpaaRating, budget, oscarsCount, screenwriter: writer, coordinates: { x, y }, creationDate: date,
});

export const initialMovies: Movie[] = [
  movie(1042, 'Интерстеллар', 'SCIENCE_FICTION', 'PG_13', 165_000_000, 1, people.nolan, -12.5, 300, '2026-09-24T11:48:00Z'),
  movie(1041, 'Паразиты', 'TRAGEDY', 'R', 11_400_000, 4, people.bong, 37.5, 126, '2026-09-23T15:21:00Z'),
  movie(1040, 'Унесённые призраками', 'ADVENTURE', 'PG', 19_000_000, 1, people.miyazaki, 35.7, 139, '2026-09-22T08:14:00Z'),
  movie(1039, 'Престиж', 'TRAGEDY', 'PG_13', 40_000_000, 0, people.nolan, -19.2, 211, '2026-09-20T17:06:00Z'),
  movie(1038, 'Воспоминания об убийстве', 'TRAGEDY', 'R', 2_800_000, 0, people.bong, 38.1, 127, '2026-09-19T12:44:00Z'),
  movie(1037, 'Ходячий замок', 'ADVENTURE', 'PG', 24_000_000, 0, people.miyazaki, 32.3, 128, '2026-09-18T09:30:00Z'),
  movie(1036, 'Начало', 'SCIENCE_FICTION', 'PG_13', 160_000_000, 4, people.nolan, -8.4, 276, '2026-09-16T14:05:00Z'),
  movie(1035, 'Сквозь снег', 'SCIENCE_FICTION', 'R', 40_000_000, 0, people.bong, 41.2, 88, '2026-09-15T10:18:00Z'),
  movie(1034, 'Жить', 'TRAGEDY', 'PG_13', null, 0, people.ishiguro, 51.5, -1, '2026-09-12T13:17:00Z'),
  movie(1033, 'Дюнкерк', 'TRAGEDY', 'PG_13', 100_000_000, 3, people.nolan, -4.2, 187, '2026-09-10T18:26:00Z'),
  movie(1032, 'Принцесса Мононоке', 'ADVENTURE', 'PG_13', 23_500_000, 0, people.miyazaki, 27.8, 164, '2026-09-08T07:56:00Z'),
  movie(1031, 'Окча', 'ADVENTURE', 'PG_13', 50_000_000, 0, people.bong, 36.6, 129, '2026-09-05T16:37:00Z'),
  movie(1030, 'Оппенгеймер', 'TRAGEDY', 'R', 100_000_000, 7, people.nolan, -22.1, 344, '2026-09-03T10:09:00Z'),
  movie(1029, 'Ветер крепчает', 'TRAGEDY', 'PG_13', 30_000_000, 0, people.miyazaki, 34.1, 141, '2026-09-01T19:42:00Z'),
  movie(1028, 'Хороший, плохой, злой', 'WESTERN', 'R', 1_200_000, 0, null, -42.3, 73, '2026-08-29T11:11:00Z'),
];

export const emptyFilters: Filters = {
  name: '', ids: '', genres: [], mpaaRating: '', coordinateXFrom: '', coordinateXTo: '', coordinateYFrom: '', coordinateYTo: '',
  creationAfter: '', creationBefore: '', oscarsFrom: '', oscarsTo: '', budgetFrom: '', budgetTo: '', screenwriterName: '', heightFrom: '', heightTo: '',
  eyeColor: '', hairColor: '', nationality: '', locationXFrom: '', locationXTo: '', locationYFrom: '', locationYTo: '', locationZFrom: '', locationZTo: '',
};

export const emptyDraft: MovieDraft = {
  name: '', coordinateX: '', coordinateY: '', oscarsCount: '1', budget: '', genre: 'SCIENCE_FICTION', mpaaRating: 'PG_13',
  screenwriterMode: 'existing', existingScreenwriter: '', screenwriterName: '', screenwriterHeight: '', eyeColor: 'BROWN', hairColor: '',
  nationality: 'JAPAN', locationX: '', locationY: '', locationZ: '',
};

export const draftFromMovie = (item: Movie): MovieDraft => ({
  name: item.name, coordinateX: String(item.coordinates.x), coordinateY: String(item.coordinates.y), oscarsCount: String(item.oscarsCount),
  budget: item.budget == null ? '' : String(item.budget), genre: item.genre, mpaaRating: item.mpaaRating,
  screenwriterMode: item.screenwriter ? 'existing' : 'none', existingScreenwriter: item.screenwriter?.name ?? '', screenwriterName: item.screenwriter?.name ?? '',
  screenwriterHeight: item.screenwriter ? String(item.screenwriter.height) : '', eyeColor: item.screenwriter?.eyeColor ?? 'BROWN', hairColor: item.screenwriter?.hairColor ?? '',
  nationality: item.screenwriter?.nationality ?? 'JAPAN', locationX: item.screenwriter ? String(item.screenwriter.location.x) : '',
  locationY: item.screenwriter ? String(item.screenwriter.location.y) : '', locationZ: item.screenwriter ? String(item.screenwriter.location.z) : '',
});

export const formatMoney = (value: number | null) => value == null ? '—' : new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value);
export const formatDate = (value: string) => new Intl.DateTimeFormat('ru-RU', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value));
