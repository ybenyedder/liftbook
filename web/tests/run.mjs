// Harnais de tests du port web — lance tous les web/tests/*.test.mjs
// Usage : node web/tests/run.mjs
import { readdir } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

const dir = new URL('.', import.meta.url).pathname;
const files = (await readdir(dir)).filter(f => f.endsWith('.test.mjs')).sort();
let ok = 0, fail = 0;
for (const f of files) {
  const mod = await import(pathToFileURL(dir + f));
  const tests = Object.entries(mod).filter(([k]) => k.startsWith('test'));
  for (const [name, fn] of tests) {
    try {
      await fn();
      ok++;
      console.log(`  ✓ ${f.replace('.test.mjs', '')} :: ${name.slice(4)}`);
    } catch (e) {
      fail++;
      console.error(`  ✗ ${f.replace('.test.mjs', '')} :: ${name.slice(4)}\n    ${e.message}`);
    }
  }
}
console.log(`\n${ok} OK, ${fail} ÉCHEC(S)`);
process.exit(fail ? 1 : 0);
