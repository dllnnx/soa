import type { Filters, MovieDraft } from './types';

export const TEXT_MAX_LENGTH = 255;
export const NUMBER_MAX_LENGTH = 64;
export const IDS_MAX_LENGTH = 2048;
export const INT_MIN = -2147483648;
export const INT_MAX = 2147483647;
export const SAFE_LONG_MAX = Number.MAX_SAFE_INTEGER;
export const FLOAT_MAX = 3.4028234663852886e38;

export type NumberRule = { integer?: boolean; min?: number; max?: number; exclusiveMin?: boolean };
export const numberRules = {
  double: {},
  coordinateX: { min: -130, exclusiveMin: true },
  coordinateY: { max: 388 },
  height: { min: 0, exclusiveMin: true },
  oscars: { integer: true, min: 0, max: SAFE_LONG_MAX },
  budget: { integer: true, min: 1, max: INT_MAX },
  int: { integer: true, min: INT_MIN, max: INT_MAX },
  long: { integer: true, min: -SAFE_LONG_MAX, max: SAFE_LONG_MAX },
  float: { min: -FLOAT_MAX, max: FLOAT_MAX },
} satisfies Record<string, NumberRule>;

export function numberError(raw: string, rule: NumberRule, required = true): string | undefined {
  if (raw === '') return required ? 'Обязательное поле' : undefined;
  if (raw.length > NUMBER_MAX_LENGTH) return `Не больше ${NUMBER_MAX_LENGTH} символов`;
  const syntax = rule.integer ? /^[+-]?\d+$/ : /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i;
  const value = Number(raw);
  if (!syntax.test(raw) || !Number.isFinite(value)) return rule.integer ? 'Введите целое число' : 'Введите конечное число';
  if (rule.integer && !Number.isSafeInteger(value)) return 'Целое число вне безопасного диапазона: от −9 007 199 254 740 991 до 9 007 199 254 740 991';
  if (rule.min !== undefined && (rule.exclusiveMin ? value <= rule.min : value < rule.min)) {
    return `Значение должно быть ${rule.exclusiveMin ? 'больше' : 'не меньше'} ${rule.min}`;
  }
  if (rule.max !== undefined && value > rule.max) return `Значение должно быть не больше ${rule.max}`;
  return undefined;
}

function textError(value: string, required = true): string | undefined {
  if (!value.trim()) return required ? 'Обязательное поле' : undefined;
  if (value.length > TEXT_MAX_LENGTH) return `Не больше ${TEXT_MAX_LENGTH} символов`;
  if (!/^[a-zA-Zа-яА-ЯЁё0-9 .,:!?()&-]+$/.test(value.trim())) return 'Есть недопустимые символы';
  return undefined;
}

export function validateMovieDraft(draft: MovieDraft): Record<string, string> {
  const errors: Record<string, string> = {};
  const check = (key: keyof MovieDraft, error: string | undefined) => { if (error) errors[key] = error; };
  check('name', textError(draft.name));
  check('coordinateX', numberError(draft.coordinateX, numberRules.coordinateX));
  check('coordinateY', numberError(draft.coordinateY, numberRules.coordinateY));
  check('oscarsCount', numberError(draft.oscarsCount, numberRules.oscars));
  check('budget', numberError(draft.budget, numberRules.budget, false));
  if (draft.screenwriterMode === 'existing' && !draft.existingScreenwriter) errors.existingScreenwriter = 'Выберите сценариста';
  if (draft.screenwriterMode === 'new') {
    check('screenwriterName', textError(draft.screenwriterName));
    check('screenwriterHeight', numberError(draft.screenwriterHeight, numberRules.height));
    check('locationX', numberError(draft.locationX, numberRules.float));
    check('locationY', numberError(draft.locationY, numberRules.int));
    check('locationZ', numberError(draft.locationZ, numberRules.long));
  }
  return errors;
}

const filterRanges = [
  ['oscarsFrom', 'oscarsTo', numberRules.oscars],
  ['budgetFrom', 'budgetTo', numberRules.budget],
  ['coordinateXFrom', 'coordinateXTo', numberRules.double],
  ['coordinateYFrom', 'coordinateYTo', numberRules.double],
  ['heightFrom', 'heightTo', numberRules.double],
  ['locationXFrom', 'locationXTo', numberRules.float],
  ['locationYFrom', 'locationYTo', numberRules.int],
  ['locationZFrom', 'locationZTo', numberRules.long],
] as const;

export function validateFilters(filters: Filters): Record<string, string> {
  const errors: Record<string, string> = {};
  const check = (key: keyof Filters, error: string | undefined) => { if (error) errors[key] = error; };
  check('name', textError(filters.name, false));
  check('screenwriterName', textError(filters.screenwriterName, false));
  if (filters.ids.trim()) {
    if (filters.ids.length > IDS_MAX_LENGTH) errors.ids = `Не больше ${IDS_MAX_LENGTH} символов`;
    else if (filters.ids.split(',').some((id) => numberError(id.trim(), { integer: true, min: 1, max: INT_MAX }))) {
      errors.ids = `Введите ID от 1 до ${INT_MAX} через запятую`;
    }
  }
  for (const [from, to, rule] of filterRanges) {
    check(from, numberError(filters[from], rule, false));
    check(to, numberError(filters[to], rule, false));
    if (!errors[from] && !errors[to] && filters[from] && filters[to] && Number(filters[from]) >= Number(filters[to])) {
      errors[to] = 'Верхняя граница должна быть больше нижней';
    }
  }
  check('coordinateXFrom', numberError(filters.coordinateXFrom, numberRules.coordinateX, false));
  check('coordinateYTo', numberError(filters.coordinateYTo, numberRules.coordinateY, false));
  check('heightFrom', numberError(filters.heightFrom, numberRules.height, false));
  for (const key of ['creationAfter', 'creationBefore'] as const) {
    if (filters[key] && !Number.isFinite(new Date(filters[key]).getTime())) errors[key] = 'Введите корректную дату';
  }
  if (!errors.creationAfter && !errors.creationBefore && filters.creationAfter && filters.creationBefore
    && new Date(filters.creationAfter) >= new Date(filters.creationBefore)) errors.creationBefore = 'Конечная дата должна быть позже начальной';
  return errors;
}
