import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import ts from 'typescript';

async function loadTs(path) {
  const source = await readFile(new URL(path, import.meta.url), 'utf8');
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } });
  return import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);
}
const { numberError, numberRules, validateMovieDraft, validateFilters } = await loadTs('../src/validation.ts');
const { emptyDraft, emptyFilters } = await loadTs('../src/labels.ts');
const validDraft = { ...emptyDraft, name: 'Фильм', coordinateX: '1', coordinateY: '2', screenwriterMode: 'none' };
const writerDraft = {
  ...validDraft, screenwriterMode: 'new', screenwriterName: 'Сценарист', screenwriterHeight: '180',
  locationX: '1', locationY: '2', locationZ: '3',
};

test('million and billion budgets remain valid through the int32 maximum', () => {
  for (const budget of ['1000000', '1000000000', '2147483647']) {
    assert.deepEqual(validateMovieDraft({ ...validDraft, budget }), {});
  }
});

test('budget overflow and fractions are rejected before Number conversion for the request', () => {
  for (const budget of ['2147483648', '3000000000', '99999999999999999999', '1.5', '1e9', 'Infinity', 'NaN', '-1', '0', ' ']) {
    assert.ok(validateMovieDraft({ ...validDraft, budget }).budget, budget);
  }
  assert.deepEqual(validateMovieDraft(validDraft), {});
});

test('signed int32 location boundaries are preserved', () => {
  for (const locationY of ['-2147483648', '2147483647']) assert.deepEqual(validateMovieDraft({ ...writerDraft, locationY }), {});
  for (const locationY of ['-2147483649', '2147483648', '1.5']) assert.ok(validateMovieDraft({ ...writerDraft, locationY }).locationY);
});

test('long fields reject integers which JavaScript would round', () => {
  assert.equal(numberError('9007199254740991', numberRules.oscars), undefined);
  assert.equal(numberError('-9007199254740991', numberRules.long), undefined);
  for (const value of ['9007199254740992', '9007199254740993', '9223372036854775807']) {
    assert.ok(validateMovieDraft({ ...writerDraft, oscarsCount: value, locationZ: value }).oscarsCount);
    assert.ok(validateMovieDraft({ ...writerDraft, locationZ: value }).locationZ);
  }
  assert.ok(numberError('-9007199254740992', numberRules.long));
});

test('non-finite doubles, malformed numbers and float overflow are rejected', () => {
  for (const value of ['1e309', '-1e309', 'Infinity', 'NaN', '12abc', '0x10', ' ']) assert.ok(numberError(value, numberRules.double));
  assert.ok(validateMovieDraft({ ...writerDraft, locationX: '3.5e38' }).locationX);
  assert.deepEqual(validateMovieDraft({ ...writerDraft, locationX: '3.4e38', screenwriterHeight: '1000000000' }), {});
  assert.ok(numberError('1'.repeat(65), numberRules.double));
});

test('coordinate restrictions and required positive height still apply', () => {
  for (const coordinateY of ['12,1', '12.1']) {
    assert.equal(validateMovieDraft({ ...validDraft, coordinateY }).coordinateY, 'Введите целое число');
  }
  for (const coordinateY of ['-12', '0', '12', '388']) {
    assert.deepEqual(validateMovieDraft({ ...validDraft, coordinateY }), {});
  }
  for (const coordinateX of ['12,1', '12.1']) {
    assert.equal(validateMovieDraft({ ...validDraft, coordinateX }).coordinateX, 'Введите целое число');
  }
  for (const coordinateX of ['-129', '0', '12']) {
    assert.deepEqual(validateMovieDraft({ ...validDraft, coordinateX }), {});
  }
  assert.ok(validateMovieDraft({ ...validDraft, coordinateX: '-130' }).coordinateX);
  assert.ok(validateMovieDraft({ ...validDraft, coordinateY: '389' }).coordinateY);
  for (const screenwriterHeight of ['', '0', '-1', 'abc']) assert.ok(validateMovieDraft({ ...writerDraft, screenwriterHeight }).screenwriterHeight);
  assert.deepEqual(validateMovieDraft({ ...validDraft, coordinateY: '388', oscarsCount: '0' }), {});
});

test('names accept 255 characters and reject longer or malformed names', () => {
  assert.deepEqual(validateMovieDraft({ ...writerDraft, name: 'Я'.repeat(255), screenwriterName: 'А'.repeat(255) }), {});
  assert.ok(validateMovieDraft({ ...validDraft, name: 'Я'.repeat(256) }).name);
  assert.ok(validateMovieDraft({ ...writerDraft, screenwriterName: 'А'.repeat(256) }).screenwriterName);
  assert.ok(validateMovieDraft({ ...writerDraft, screenwriterName: '<script>' }).screenwriterName);
});

test('numeric filters share the same type limits', () => {
  assert.equal(validateFilters({ ...emptyFilters, coordinateXFrom: '12,1' }).coordinateXFrom, 'Введите целое число');
  assert.equal(validateFilters({ ...emptyFilters, coordinateXTo: '12.1' }).coordinateXTo, 'Введите целое число');
  assert.deepEqual(validateFilters({ ...emptyFilters, coordinateXFrom: '-129', coordinateXTo: '12' }), {});
  assert.equal(validateFilters({ ...emptyFilters, coordinateYFrom: '12,1' }).coordinateYFrom, 'Введите целое число');
  assert.equal(validateFilters({ ...emptyFilters, coordinateYTo: '12.1' }).coordinateYTo, 'Введите целое число');
  assert.deepEqual(validateFilters({ ...emptyFilters, coordinateYFrom: '12', coordinateYTo: '388' }), {});
  assert.deepEqual(validateFilters({ ...emptyFilters, budgetFrom: '1000000000', budgetTo: '2147483647' }), {});
  for (const field of ['budgetFrom', 'budgetTo', 'locationYFrom', 'locationYTo']) assert.ok(validateFilters({ ...emptyFilters, [field]: '2147483648' })[field]);
  assert.ok(validateFilters({ ...emptyFilters, oscarsFrom: '9007199254740993' }).oscarsFrom);
  assert.ok(validateFilters({ ...emptyFilters, locationXTo: '1e39' }).locationXTo);
  assert.ok(validateFilters({ ...emptyFilters, heightTo: '1e309' }).heightTo);
});

test('invalid IDs cannot silently disappear from the filter', () => {
  assert.deepEqual(validateFilters({ ...emptyFilters, ids: '1, 1000000000, 2147483647' }), {});
  for (const ids of ['2147483648', '1,abc', '1,', '0', '1.5', '1e9']) assert.ok(validateFilters({ ...emptyFilters, ids }).ids, ids);
});

test('reversed ranges, invalid dates and oversized filter text are rejected', () => {
  assert.ok(validateFilters({ ...emptyFilters, budgetFrom: '100', budgetTo: '1' }).budgetTo);
  assert.ok(validateFilters({ ...emptyFilters, creationAfter: 'invalid' }).creationAfter);
  assert.ok(validateFilters({ ...emptyFilters, name: 'a'.repeat(256) }).name);
  assert.ok(validateFilters({ ...emptyFilters, creationAfter: '2026-11-11T12:00', creationBefore: '2026-11-10T12:00' }).creationBefore);
  assert.deepEqual(validateFilters(emptyFilters), {});
});
