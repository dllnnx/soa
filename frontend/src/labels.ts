import type { Country, Filters, Genre, Movie, MovieDraft } from './types';

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

export const countryLabels: Record<Country, string> = {
  UNITED_KINGDOM: 'Великобритания',
  SOUTH_KOREA: 'Южная Корея',
  JAPAN: 'Япония',
};

export const colorLabels: Record<string, string> = {
  GREEN: 'Зелёный', RED: 'Красный', BLUE: 'Синий', YELLOW: 'Жёлтый', BROWN: 'Карий',
  BLACK: 'Чёрный', ORANGE: 'Рыжий',
};

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
