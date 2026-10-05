export type Genre = 'WESTERN' | 'ADVENTURE' | 'TRAGEDY' | 'SCIENCE_FICTION';
export type MpaaRating = 'G' | 'PG' | 'PG_13' | 'R' | 'NC_17';
export type EyeColor = 'GREEN' | 'RED' | 'BLUE' | 'YELLOW' | 'BROWN';
export type HairColor = 'GREEN' | 'RED' | 'BLACK' | 'YELLOW' | 'ORANGE';
export type Country = 'UNITED_KINGDOM' | 'SOUTH_KOREA' | 'JAPAN';

export interface Person {
  name: string;
  height: number;
  eyeColor: EyeColor;
  hairColor: HairColor | null;
  nationality: Country;
  location: { x: number; y: number; z: number };
}

export interface Movie {
  id: number;
  name: string;
  coordinates: { x: number; y: number };
  creationDate: string;
  oscarsCount: number;
  budget: number | null;
  genre: Genre;
  mpaaRating: MpaaRating;
  screenwriter: Person | null;
}

export interface MovieDraft {
  name: string;
  coordinateX: string;
  coordinateY: string;
  oscarsCount: string;
  budget: string;
  genre: Genre;
  mpaaRating: MpaaRating;
  screenwriterMode: 'existing' | 'new' | 'none';
  existingScreenwriter: string;
  screenwriterName: string;
  screenwriterHeight: string;
  eyeColor: EyeColor;
  hairColor: HairColor | '';
  nationality: Country;
  locationX: string;
  locationY: string;
  locationZ: string;
}

export interface Filters {
  name: string;
  ids: string;
  genres: Genre[];
  mpaaRating: MpaaRating | '';
  coordinateXFrom: string;
  coordinateXTo: string;
  coordinateYFrom: string;
  coordinateYTo: string;
  creationAfter: string;
  creationBefore: string;
  oscarsFrom: string;
  oscarsTo: string;
  budgetFrom: string;
  budgetTo: string;
  screenwriterName: string;
  heightFrom: string;
  heightTo: string;
  eyeColor: EyeColor | '';
  hairColor: HairColor | '';
  nationality: Country | '';
  locationXFrom: string;
  locationXTo: string;
  locationYFrom: string;
  locationYTo: string;
  locationZFrom: string;
  locationZTo: string;
}

export type JobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface OscarJob {
  id: string;
  type: 'REWARD_R' | 'RESET_BY_GENRE';
  label: string;
  status: JobStatus;
  createdAt: string;
  updatedCount?: number;
  errorMessage?: string;
}
