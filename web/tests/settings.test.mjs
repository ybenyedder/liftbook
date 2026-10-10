// Tests de l'écran Réglages — mini-unzip maison (port de ZipInputStream dans Settings.kt)
// + libellés restLabel/nextRest. Fixture générée par python3 (zipfile) :
// web/tests/fixtures/hevy.zip = trainings.csv (STORED) + more.csv (DEFLATE) + readme.txt (ignoré).
// Harnais : node web/tests/run.mjs (fonctions exportées préfixées « test »).

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  isZipBytes, parseZipEocd, parseZipCentralDirectory, zipEntryRaw,
  inflateRaw, unzipCsvTexts, restLabel, nextRest,
} from '../src/screens/settings.js';

const fixture = () => new Uint8Array(readFileSync(new URL('./fixtures/hevy.zip', import.meta.url)));

/* ---- construction d'un zip STORED à la main (local header + central dir + EOCD) ---- */

const u16 = v => [v & 255, (v >> 8) & 255];
const u32 = v => [v & 255, (v >>> 8) & 255, (v >>> 16) & 255, (v >>> 24) & 255];
function concat(parts) {
  const len = parts.reduce((a, p) => a + p.length, 0);
  const out = new Uint8Array(len);
  let at = 0;
  for (const p of parts) { out.set(p, at); at += p.length; }
  return out;
}
const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();
function crc32(bytes) {
  let c = 0xffffffff;
  for (const b of bytes) c = CRC_TABLE[(c ^ b) & 255] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
/** Zip minimal sans compression (method 0), CRC-32 corrects — validé par python3 zipfile. */
function makeStoredZip(files, contents, comment = '') {
  const enc = new TextEncoder();
  const locals = [], cens = [];
  let off = 0;
  files.forEach((name, i) => {
    const nb = enc.encode(name), db = enc.encode(contents[i]);
    const crc = crc32(db);
    // local header 30 o : sig(4) ver(2) flags(2) method(2) time(2) date(2) crc(4) csize(4) usize(4) nlen(2) elen(2)
    const lh = concat([new Uint8Array([0x50, 0x4b, 0x03, 0x04, ...u16(20), ...u16(0), ...u16(0), ...u16(0), ...u16(0),
      ...u32(crc), ...u32(db.length), ...u32(db.length), ...u16(nb.length), ...u16(0)]), nb, db]);
    // central 46 o : sig(4) verMadeBy(2) verNeeded(2) flags(2) method(2) time(2) date(2) crc(4) csize(4) usize(4)
    //              nlen(2) elen(2) clen(2) disk(2) internal(2) external(4) loff(4)
    const ch = concat([new Uint8Array([0x50, 0x4b, 0x01, 0x02, ...u16(20), ...u16(20), ...u16(0), ...u16(0), ...u16(0), ...u16(0),
      ...u32(crc), ...u32(db.length), ...u32(db.length), ...u16(nb.length), ...u16(0), ...u16(0), ...u16(0), ...u16(0), ...u32(0), ...u32(off)]), nb]);
    locals.push(lh);
    cens.push(ch);
    off += lh.length;
  });
  const cd = concat(cens);
  const cm = enc.encode(comment);
  const eocd = concat([new Uint8Array([0x50, 0x4b, 0x05, 0x06, ...u16(0), ...u16(0),
    ...u16(files.length), ...u16(files.length), ...u32(cd.length), ...u32(off), ...u16(cm.length)]), cm]);
  return concat([...locals, cd, eocd]);
}

/* ---- tests ---- */

export function testIsZipMagic() {
  assert.equal(isZipBytes(fixture()), true);
  assert.equal(isZipBytes(new TextEncoder().encode('date,workout_name\n1,Push\n')), false);
  assert.equal(isZipBytes(new Uint8Array([0x50, 0x4b])), false); // trop court
}

export function testZipEocdAndCentralDirectory() {
  const bytes = fixture();
  const eocd = parseZipEocd(bytes);
  assert.ok(eocd, 'EOCD introuvable');
  assert.equal(eocd.count, 3);
  assert.equal(eocd.cdOffset + eocd.cdSize, eocd.eocdOffset, 'le central directory doit jouxter l\'EOCD');
  const entries = parseZipCentralDirectory(bytes, eocd);
  assert.deepEqual(entries.map(e => e.name), ['trainings.csv', 'more.csv', 'readme.txt']);
  assert.deepEqual(entries.map(e => e.method), [0, 8, 0]); // STORED / DEFLATE / STORED
  assert.equal(entries[0].usize, 50);
  assert.equal(entries[1].usize, 1800);
}

export function testStoredEntryReadableWithoutDecompressionStream() {
  const orig = globalThis.DecompressionStream;
  globalThis.DecompressionStream = undefined; // Node < 18 / environnements sans DS
  try {
    const zip = makeStoredZip(['a.csv', 'note.txt', 'sub/b.CSV'], ['hello,csv\n', 'ignore', 'UPPER,CSV\n'], 'commentaire');
    assert.ok(isZipBytes(zip));
    const eocd = parseZipEocd(zip);
    assert.ok(eocd && eocd.count === 3, 'EOCD du zip fait main (+ commentaire)');
    // lecture brute d'une entrée STORED : octets directement décodable, zéro décompression
    const raw = zipEntryRaw(zip, parseZipCentralDirectory(zip, eocd)[0]);
    assert.equal(new TextDecoder().decode(raw), 'hello,csv\n');
    // unzipCsvTexts ne doit PAS toucher à DecompressionStream pour un zip 100 % STORED
    return unzipCsvTexts(zip).then(csvs => {
      assert.deepEqual(csvs, ['hello,csv\n', 'UPPER,CSV\n'], '.csv gardés (casse ignorée), .txt et répertoires exclus');
    });
  } finally {
    globalThis.DecompressionStream = orig;
  }
}

export async function testInflateRawDeflate() {
  if (typeof DecompressionStream === 'undefined') return; // garde demandée par l'énoncé
  const bytes = fixture();
  const eocd = parseZipEocd(bytes);
  const more = parseZipCentralDirectory(bytes, eocd).find(e => e.name === 'more.csv');
  assert.equal(more.method, 8);
  const raw = zipEntryRaw(bytes, more);
  assert.ok(raw.length < 1800, 'données compressées plus courtes que l\'original');
  const text = new TextDecoder().decode(await inflateRaw(raw));
  assert.equal(text.length, 1800);
  assert.ok(text.startsWith('date,workout_name,exercise_name,set_index,kg,reps\n'));
  assert.ok(text.trimEnd().endsWith('2024-01-01,Push,Bench Press,1,60,5'));
}

export async function testUnzipCsvTextsConcat() {
  const csvs = await unzipCsvTexts(fixture());
  assert.equal(csvs.length, 2, 'readme.txt doit être exclu');
  assert.equal(csvs[0], 'date,workout_name,exercise_name,set_index,kg,reps\n');
  assert.ok(csvs[1].includes('Bench Press'));
  // texte plat (csv sans zip) → géré par l'appelant via isZipBytes, pas par unzipCsvTexts
  await assert.rejects(() => unzipCsvTexts(new TextEncoder().encode('pas un zip')), /Central Directory/);
}

export function testRestLabelAndCycle() {
  // restLabel() de Settings.kt
  assert.equal(restLabel(60), '1min 0s');
  assert.equal(restLabel(90), '1min 30s');
  assert.equal(restLabel(120), '2min 0s');
  assert.equal(restLabel(180), '3min 0s');
  assert.equal(restLabel(45), '45s');
  // cycle 60 → 90 → 120 → 180 → 60
  assert.equal(nextRest(60), 90);
  assert.equal(nextRest(90), 120);
  assert.equal(nextRest(120), 180);
  assert.equal(nextRest(180), 60);
  assert.equal(nextRest(45), 60); // valeur hors cycle → premier cran
}
