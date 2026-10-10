// Écran Réglages — port de ui/Settings.kt (SettingsScreen + SectionHeader + SettingsRow
// + RowDivider + restLabel + applyProfilePhoto + les 4 pickers hevyPicker/csvPicker/
// restorePicker/photoPicker). Le unzip maison remplace java.util.zip.ZipInputStream
// (parse EOCD → central directory → local headers, méthodes STORED(0)/DEFLATE(8)).
import { h, icon, toast, confirmDialog, promptDialog, sectionLabel, avatarEl, downscaleToJpeg, sub } from '../ui.js';
import { store } from '../store.js';
import * as S from '../store.js';
import * as Cloud from '../cloud.js';
import * as nav from '../router.js';
import * as Calc from '../calc.js';
import { L10n } from '../search.js';

const VERSION = '1.53'; // version web (bump Android à part dans build.gradle.kts)

nav.registerScreen('settings', { render });

/* ============== mini-unzip (port du hevyPicker Kotlin : ZipInputStream → .csv) ============== */

const EOCD_SIG = 0x06054b50; // PK\x05\x06
const CEN_SIG = 0x02014b50;  // PK\x01\x02
const LOC_SIG = 0x04034b50;  // PK\x03\x04

/** Signature d'archive zip en tête de fichier (bytes[0..3] == 'P','K',3,4). */
export function isZipBytes(bytes) {
  return bytes.length > 4 && bytes[0] === 0x50 && bytes[1] === 0x4b && bytes[2] === 0x03 && bytes[3] === 0x04;
}

/**
 * Localise l'End of Central Directory : recherche ARRIÈRE de la signature 0x06054b50
 * (bornée au commentaire max 65 535 o + EOCD 22 o), cohérence comment-length vérifiée.
 * @returns {{eocdOffset:number, count:number, cdSize:number, cdOffset:number}|null}
 */
export function parseZipEocd(bytes) {
  const dv = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const min = Math.max(0, bytes.length - 22 - 65535);
  for (let i = bytes.length - 22; i >= min; i--) {
    if (dv.getUint32(i, true) !== EOCD_SIG) continue;
    const commentLen = dv.getUint16(i + 20, true);
    if (i + 22 + commentLen <= bytes.length) {
      return {
        eocdOffset: i,
        count: dv.getUint16(i + 10, true),     // entrées au total
        cdSize: dv.getUint32(i + 12, true),
        cdOffset: dv.getUint32(i + 16, true),
      };
    }
  }
  return null;
}

/** Entrées du central directory : {name, method, csize, usize, loff} (offset du local header). */
export function parseZipCentralDirectory(bytes, eocd) {
  const dv = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const td = new TextDecoder();
  const out = [];
  let p = eocd.cdOffset;
  for (let n = 0; n < eocd.count && p + 46 <= bytes.length; n++) {
    if (dv.getUint32(p, true) !== CEN_SIG) break;
    const method = dv.getUint16(p + 10, true);
    const csize = dv.getUint32(p + 20, true);
    const usize = dv.getUint32(p + 24, true);
    const nlen = dv.getUint16(p + 28, true);
    const elen = dv.getUint16(p + 30, true);
    const clen = dv.getUint16(p + 32, true);
    const loff = dv.getUint32(p + 42, true);
    out.push({ name: td.decode(bytes.subarray(p + 46, p + 46 + nlen)), method, csize, usize, loff });
    p += 46 + nlen + elen + clen;
  }
  return out;
}

/**
 * Octets d'une entrée lus directement depuis son local header (PK\x03\x04 + longueurs
 * nom/extra) — utilisable TEL QUEL pour une entrée STORED (method 0), sans DecompressionStream.
 */
export function zipEntryRaw(bytes, entry) {
  const dv = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  if (entry.loff + 30 > bytes.length || dv.getUint32(entry.loff, true) !== LOC_SIG) return null;
  const nlen = dv.getUint16(entry.loff + 26, true);
  const elen = dv.getUint16(entry.loff + 28, true);
  const start = entry.loff + 30 + nlen + elen;
  return bytes.subarray(start, start + entry.csize); // tailles réelles : toujours lues du central directory
}

/** Décompresse un flux DEFLATE brut (method 8) via DecompressionStream('deflate-raw'). */
export async function inflateRaw(cbytes) {
  const stream = new Blob([cbytes]).stream().pipeThrough(new DecompressionStream('deflate-raw'));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

/**
 * Texte UTF-8 de tous les .csv d'une archive (équivalent ZipInputStream : entrées non
 * répertoires dont le nom finit par .csv, insensible à la casse). Les entrées STORED sont
 * lues sans DecompressionStream ; seules les DEFLATE en dépendent.
 * Bornes anti-zip-bomb : DecompressionStream n'a AUCUN plafond natif — quelques Ko
 * compressés pouvaient décompresser en plusieurs Go et tuer l'onglet.
 */
const ZIP_MAX_ENTRY = 32 * 1024 * 1024;   // 32 Mo décompressés par entrée
const ZIP_MAX_TOTAL = 64 * 1024 * 1024;   // 64 Mo cumulés
const ZIP_MAX_ENTRIES = 512;
export async function unzipCsvTexts(bytes) {
  const eocd = parseZipEocd(bytes);
  if (!eocd) throw new Error('zip: End of Central Directory introuvable');
  if (eocd.count > ZIP_MAX_ENTRIES) throw new Error('zip: trop d\'entrées');
  const td = new TextDecoder();
  const out = [];
  let total = 0;
  for (const entry of parseZipCentralDirectory(bytes, eocd)) {
    if (entry.name.endsWith('/') || !entry.name.toLowerCase().endsWith('.csv')) continue;
    if (entry.usize > ZIP_MAX_ENTRY) throw new Error('zip: entrée trop volumineuse');
    const raw = zipEntryRaw(bytes, entry);
    if (raw == null) continue;
    if (entry.method === 0) out.push(td.decode(raw));
    else if (entry.method === 8) out.push(td.decode(await inflateRaw(raw)));
    total += entry.usize;
    if (total > ZIP_MAX_TOTAL) throw new Error('zip: archive trop volumineuse après décompression');
  }
  return out;
}

/* ============== libellés (ports directs de Settings.kt) ============== */

/** restLabel() de Settings.kt : 90 → « 1min 30s », 60 → « 1min 0s », 45 → « 45s ». */
export function restLabel(sec) {
  const m = Math.floor(sec / 60), s = sec % 60;
  return `${m > 0 ? `${m}min ` : ''}${s}s`;
}

/** Cycle du minuteur : premier de [60,90,120,180] strictement supérieur, sinon retour à 60. */
export function nextRest(sec) {
  return [60, 90, 120, 180].find(v => v > sec) ?? 60;
}

/* ============== écran ============== */

function css() {
  if (!document.getElementById('css-settings')) {
    const st = h('style', { id: 'css-settings' });
    st.textContent = `
.scr-settings .avatar-row { display:flex; align-items:center; gap:14px; width:100%; text-align:left;
  padding:12px 16px; border-bottom:.5px solid var(--line); }
.scr-settings .avatar-row .txt { flex:1; min-width:0; }
.scr-settings .avatar-row .t1 { font-size:15.5px; }
.scr-settings .avatar-row .t2 { font-size:12.5px; color:var(--mut); margin-top:1px; }
.scr-settings .acc-row { display:flex; align-items:center; padding:14px 16px; border-bottom:.5px solid var(--line); font-size:15.5px; }
.scr-settings .acc-row .lbl { flex:1; }
.scr-settings .acc-dots { display:flex; gap:10px; }
.scr-settings .acc-dot { width:28px; height:28px; border-radius:50%; border:2.5px solid transparent;
  box-sizing:border-box; padding:0; cursor:pointer; }
.scr-settings .acc-dot.sel { border-color:var(--text); }
.scr-settings .foot { text-align:center; color:var(--mut); font-size:11.5px; padding:26px 16px 30px; }
.scr-settings .hidden-input { display:none; }
`;
    document.head.append(st);
  }
}

function render(el) {
  css();
  el.classList.add('scr-settings');

  el.append(h('div', { class: 'topbar' },
    h('button', { class: 'iconbtn', onclick: () => nav.nav.pop() }, icon('arrow_back')),
    h('div', { class: 'topbar-title', style: { fontWeight: '800', fontSize: '18px' } },
      L10n.s('Settings', 'Réglages', 'Ajustes', 'Einstellungen')),
  ));

  const list = h('div', { class: 'screen-pad', style: { paddingTop: '6px' } });
  el.append(h('div', { class: 'screen' }, list));

  function body() {
    const sess = store.session;
    list.replaceChildren();

    // ---- COMPTE ----
    list.append(sectionLabel(L10n.s('ACCOUNT', 'COMPTE', 'CUENTA', 'KONTO')));
    if (sess) {
      list.append(avatarRow(), syncRow(), emailRow(sess), signOutRow());
    } else {
      list.append(h('button', {
        class: 'setrow blue',
        onclick: () => { store.skipped = false; S.persistSkip(); S.emit('session'); },
      }, L10n.s('Sign in or create an account', 'Se connecter ou créer un compte', 'Iniciar sesión o crear una cuenta', 'Anmelden oder Konto erstellen')));
    }

    // ---- PROFIL ----
    list.append(sectionLabel(L10n.s('PROFILE', 'PROFIL', 'PERFIL', 'PROFIL')));
    list.append(h('button', { class: 'setrow', onclick: editName },
      L10n.s('Name', 'Nom'), h('span', { class: 'val' }, store.settings.profileName)));
    list.append(h('button', { class: 'setrow', onclick: editHandle },
      L10n.s('Username', 'Pseudo'), h('span', { class: 'val' }, '@' + store.settings.handle)));

    // ---- GÉNÉRAL ----
    list.append(sectionLabel(L10n.s('GENERAL', 'GÉNÉRAL', 'GENERAL', 'ALLGEMEIN')));
    list.append(h('button', {
      class: 'setrow',
      onclick: () => S.setUnit(store.settings.unit === 'kg' ? 'lb' : 'kg'),
    }, L10n.s('Units', 'Unités', 'Unidades', 'Einheiten'), h('span', { class: 'val' }, store.settings.unit)));
    list.append(h('button', { class: 'setrow', onclick: () => S.setRest(nextRest(store.settings.restSec)) },
      L10n.s('Rest timer', 'Minuteur de repos', 'Temporizador de descanso', 'Ruhe-Timer'),
      h('span', { class: 'val' }, restLabel(store.settings.restSec))));
    list.append(accentRow());

    // ---- DONNÉES ----
    list.append(sectionLabel(L10n.s('DATA', 'DONNÉES', 'DATOS', 'DATEN')));
    list.append(h('button', { class: 'setrow', onclick: () => hevyInput.click() },
      L10n.s('Import from Hevy (account export)', 'Importer depuis Hevy (export du compte)')));
    list.append(h('button', { class: 'setrow', onclick: () => csvInput.click() },
      L10n.s('Import workouts (CSV)', 'Importer des séances (CSV)')));
    list.append(h('button', { class: 'setrow', onclick: exportCsv },
      L10n.s('Export workouts (CSV)', 'Exporter les séances (CSV)')));
    list.append(h('button', { class: 'setrow', onclick: backup },
      L10n.s('Backup data (JSON)', 'Sauvegarder les données (JSON)')));
    list.append(h('button', { class: 'setrow', onclick: () => restoreInput.click() },
      L10n.s('Restore backup', 'Restaurer une sauvegarde')));
    list.append(h('button', { class: 'setrow red', onclick: confirmWipe },
      L10n.s('Erase all data', 'Tout effacer')));

    list.append(h('div', { class: 'foot' },
      'Liftbook ' + VERSION + (sess
        ? ' · ' + L10n.s('synced as', 'synchronisé en tant que') + ' ' + sess.email : '')));

    // pickers cachés (rememberLauncherForActivityResult) — recréés à chaque body()
    list.append(hevyInput, csvInput, restoreInput, photoInput);
  }

  /* ---------- COMPTE ---------- */

  // photoPicker + applyProfilePhoto : downscale 640 max / JPEG 88 %, cache local puis upload
  const photoInput = h('input', { class: 'hidden-input', type: 'file', accept: 'image/*' });
  photoInput.addEventListener('change', async () => {
    const file = photoInput.files && photoInput.files[0];
    photoInput.value = '';
    if (!file) return;
    try {
      const blob = await downscaleToJpeg(file, 640, 0.88);
      await S.setAvatarBlob(blob); // AvatarCache local d'abord (échec upload ≠ perte de photo)
      toast(L10n.s('Profile photo updated', 'Photo de profil mise à jour'));
      const url = await Cloud.uploadAvatar(store.session, blob);
      if (url) S.setAvatar(url); // upload échoué → silencieux, comme le Log.w Android
    } catch {
      toast(L10n.s('Could not read this image', 'Impossible de lire cette image'));
    }
  });

  function avatarRow() {
    const letter = store.settings.profileName.trim().slice(0, 1).toUpperCase();
    const holder = h('div', {}, avatarEl(letter, 56, store.settings.avatarUrl || null));
    // la photo locale (IndexedDB) prime sur l'URL distante — AvatarImg/AvatarCache Android
    S.avatarObjectUrl().then(u => {
      if (u) holder.replaceChildren(avatarEl(letter, 56, u));
    }).catch(() => {});
    return h('button', { class: 'avatar-row', onclick: () => photoInput.click() },
      holder,
      h('div', { class: 'txt' },
        h('div', { class: 't1' }, L10n.s('Profile photo', 'Photo de profil')),
        h('div', { class: 't2' }, L10n.s('Tap to change', 'Touche pour changer'))),
      icon('chevron_right', { size: 18, cls: 't-mut' }));
  }

  function syncRow() {
    return h('button', {
      class: 'setrow',
      onclick: () => { toast(L10n.s('Syncing…', 'Synchronisation…')); S.syncNow(); },
    }, L10n.s('Sync now', 'Synchroniser maintenant'),
      store.syncStatus ? h('span', { class: 'val' }, store.syncStatus) : null); // SettingsRow : valeur si isNotBlank
  }

  function emailRow(sess) {
    return h('button', { class: 'setrow', onclick: () => {} }, sess.email); // rangée inactive (Settings.kt onClick = {})
  }

  function signOutRow() {
    return h('button', {
      class: 'setrow red',
      onclick: async () => {
        await S.signOutNow();
        if (store.skipped) nav.nav.toTab(2); // Nav.toTab(ProfileTab) — sinon renderAll affiche l'auth
      },
    }, L10n.s('Sign out', 'Se déconnecter', 'Cerrar sesión', 'Abmelden'));
  }

  /* ---------- PROFIL : dialogues (AlertDialog + OutlinedTextField) ---------- */

  function editName() {
    promptDialog({
      title: L10n.s('Name', 'Nom'),
      value: store.settings.profileName,
      maxLength: 30, // v.take(30)
      onConfirm: v => { if (v) S.setProfile(v, store.settings.handle); }, // isNotBlank → trim déjà fait par promptDialog
    });
  }

  function editHandle() {
    promptDialog({
      title: L10n.s('Username', 'Pseudo'),
      value: store.settings.handle,
      maxLength: 20, // take(20)
      prefix: '@',
      filter: s => s.replace(/[^a-zA-Z0-9._]/g, ''), // isLetterOrDigit || '.' || '_'
      onConfirm: v => { if (v) S.setProfile(store.settings.profileName, v); },
    });
  }

  /* ---------- GÉNÉRAL : pastilles d'accent ---------- */

  function accentRow() {
    const dots = h('div', { class: 'acc-dots' });
    for (const [key, color] of [['blue', '#028CFD'], ['teal', '#20B49A'], ['violet', '#7C5CFF'], ['orange', '#FF7A45']]) {
      dots.append(h('button', {
        class: 'acc-dot' + (store.settings.accent === key ? ' sel' : ''),
        style: { background: color },
        'aria-label': key,
        onclick: () => {
          S.setAccent(key);
          // équivalent (ctx as Activity).recreate() : l'accent live pilote #app[data-accent]
          const app = document.getElementById('app');
          if (app) app.dataset.accent = key;
        },
      }));
    }
    return h('div', { class: 'acc-row' },
      h('div', { class: 'lbl' }, L10n.s('Accent color', 'Couleur d\'accent', 'Color de acento', 'Akzentfarbe')),
      dots);
  }

  /* ---------- DONNÉES : import / export / sauvegarde ---------- */

  // hevyPicker : export du compte Hevy (.zip → csv concaténés, ou csv plat)
  const hevyInput = h('input', { class: 'hidden-input', type: 'file', accept: '.csv,.zip' });
  hevyInput.addEventListener('change', async () => {
    const file = hevyInput.files && hevyInput.files[0];
    hevyInput.value = '';
    if (!file) return;
    if (file.size > 100 * 1024 * 1024) { toast(L10n.s('File too large (100 MB max)', 'Fichier trop volumineux (100 Mo max)')); return; }
    try {
      const bytes = new Uint8Array(await file.arrayBuffer());
      const csvs = isZipBytes(bytes) ? await unzipCsvTexts(bytes) : [new TextDecoder().decode(bytes)];
      let ws = [], rs = [];
      for (const csv of csvs) {
        const [w, r] = Calc.parseHevyCsv(csv);
        ws = ws.concat(w); rs = rs.concat(r);
      }
      if (ws.length === 0 && rs.length === 0) {
        const head = ((csvs[0] || '').split(/\r?\n/)[0] || '').slice(0, 90);
        toast(L10n.s('No Hevy data found — header:', 'Aucune donnée Hevy trouvée — en-tête :') + ' ' + head);
      } else {
        const n = S.importHevy(ws, rs);
        toast(L10n.s('%1$d workouts imported from Hevy (routines rebuilt)', '%1$d séances importées (routines reconstruites)').replace('%1$d', n));
      }
    } catch {
      toast(L10n.s('Import failed (check format)', 'Import échoué (vérifie le format)'));
    }
  });

  // csvPicker : CSV Liftbook, repli parseHevyCsv si rien reconnu
  const csvInput = h('input', { class: 'hidden-input', type: 'file', accept: 'text/*,.csv' });
  csvInput.addEventListener('change', async () => {
    const file = csvInput.files && csvInput.files[0];
    csvInput.value = '';
    if (!file) return;
    let text = null;
    try { text = await file.text(); } catch { text = null; }
    if (text == null) { toast(L10n.s('Import failed (check format)', 'Import échoué (vérifie le format)')); return; }
    let n = S.importCsv(text);
    if (n === 0) {
      const [ws, rs] = Calc.parseHevyCsv(text);
      if (ws.length > 0 || rs.length > 0) n = S.importHevy(ws, rs);
    }
    toast(n > 0
      ? L10n.s('%1$d workouts imported', '%1$d séances importées').replace('%1$d', n)
      : L10n.s('Nothing imported (check format)', 'Rien d\'importé (vérifie le format)'));
  });

  // exportCsv(ctx) : export CSV de TOUTES les séances (store.workouts, ordre chronologique)
  function exportCsv() {
    download('hevy-seances.csv', 'text/csv', Calc.exportCsvText(store.workouts, store.settings.unit));
  }

  // shareBackup(ctx) : sauvegarde JSON { workouts, routines, photos }
  function backup() {
    download('hevy-sauvegarde.json', 'application/json', S.backupJson());
  }

  // restorePicker
  const restoreInput = h('input', { class: 'hidden-input', type: 'file', accept: '.json,application/json' });
  restoreInput.addEventListener('change', async () => {
    const file = restoreInput.files && restoreInput.files[0];
    restoreInput.value = '';
    if (!file) return;
    let ok = false;
    try { ok = S.restoreBackup(await file.text()); } catch { ok = false; }
    toast(ok
      ? L10n.s('Backup restored', 'Sauvegarde restaurée')
      : L10n.s('Invalid backup file', 'Fichier de sauvegarde invalide'));
  });

  function download(filename, mime, text) {
    const url = URL.createObjectURL(new Blob([text], { type: mime }));
    const a = h('a', { href: url, download: filename });
    document.body.append(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
  }

  /* ---------- confirmation d'effacement (confirmWipe Android) ---------- */

  function confirmWipe() {
    confirmDialog({
      title: L10n.s('Erase all data?', 'Tout effacer ?'),
      message: L10n.s('All workouts, routines and records will be permanently deleted from this device and your account.',
        'Toutes les séances, routines et records seront définitivement supprimés de cet appareil et de ton compte.'),
      confirmLabel: L10n.s('Erase', 'Effacer'),
      destructive: true,
      onConfirm: () => { S.wipe(true); nav.nav.toTab(0); }, // Nav.toTab(Screen.HomeTab)
    });
  }

  // premier rendu APRÈS la déclaration des inputs cachés (body les append immédiatement)
  body();
  // mises à jour live : statut de sync, réglages (unités/repos/accent), session, avatar local
  for (const topic of ['sync', 'settings', 'session', 'avatar']) sub(el, topic, body);
}
