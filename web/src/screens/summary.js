// Résumé de séance — port de ui/WorkoutSummary.kt.
import { h, icon, toast, dialog, confirmDialog, downscaleToJpeg, bodyMap } from '../ui.js';
import { store, photoById, photoUrl, addPhoto, deletePhoto } from '../store.js';
import * as S from '../store.js';
import * as nav from '../router.js';
import * as Calc from '../calc.js';
import { L10n, exName } from '../search.js';
import { clear as restClear } from '../restimer.js';

nav.registerScreen('summary', { render });

function render(el) {
  el.classList.add('scr-summary');
  if (!document.getElementById('css-summary')) {
    const st = h('style', { id: 'css-summary' });
    st.textContent = `
.scr-summary .namefield { width:100%; background:var(--card); border:1px solid transparent; border-radius:11px; padding:12px 14px; font-size:17px; font-weight:700; color:var(--text); outline:none; }
.scr-summary .namefield:focus { border-color:var(--accent); }
.scr-summary .stat { flex:1; padding:4px 0; }
.scr-summary .stat .l { color:var(--mut); font-size:13px; font-weight:500; }
.scr-summary .stat .v { font-size:21px; font-weight:800; margin-top:2px; white-space:nowrap; }
.scr-summary .prrow { display:flex; align-items:center; gap:8px; background:var(--green-bg); border-radius:10px; padding:9px 10px; margin:6px 16px; }
.scr-summary .setrow-s { display:flex; align-items:center; background:var(--card); border-radius:8px; padding:8px 10px; margin:3px 0; }
.scr-summary .setrow-s.pr { background:var(--green-bg); }
.scr-summary .photobox { position:relative; width:120px; aspect-ratio:1; border-radius:12px; overflow:hidden; background:var(--card2); flex:0 0 auto; }
.scr-summary .photodel { position:absolute; top:5px; right:5px; width:22px; height:22px; border-radius:50%; background:rgba(0,0,0,.8); color:#fff; display:flex; align-items:center; justify-content:center; }
.scr-summary .addphoto { display:flex; align-items:center; justify-content:center; gap:8px; border:1px solid var(--line2); border-radius:12px; padding:18px 0; color:var(--accent); font-size:13.5px; font-weight:600; cursor:pointer; }
`;
    document.head.append(st);
  }

  const d = store.draft;
  if (!d || d.mode !== 'workout' || d.startedAt == null) { nav.nav.pop(); return; }
  const unit = store.settings.unit;
  let name = d.name;
  let addedPhotoId = null;

  // stats figées à l'ouverture
  let vol = 0, reps = 0, sets = 0;
  for (const ex of d.exercises) for (const s of ex.sets) {
    if (!s.done || (s.kg == null && s.reps == null && s.mins == null && s.km == null)) continue;
    if (s.kg != null && s.reps != null) { vol += s.kg * s.reps; reps += s.reps; }
    sets++;
  }
  const duration = Calc.fmtDur(Date.now() - d.startedAt);

  // records vs historique strictement antérieur
  const prs = [];
  for (const ex of d.exercises) {
    const done = ex.sets.filter(s => (s.kg || 0) > 0 && (s.reps || 0) > 0 && s.done && Number.isFinite(s.kg) && Number.isFinite(s.reps));
    if (!done.length) continue;
    const prev = S.prFor(ex.name);
    const pw = prev ? prev.weight : 0, pe = prev ? prev.e1rm : 0;
    const bw = Calc.maxOf(done.map(s => s.kg));
    const be = Calc.maxOf(done.map(s => Calc.e1rm(s.kg, s.reps)));
    if (bw > pw) prs.push({ ex: ex.name, kind: 'Weight', value: bw });
    if (be > pe) prs.push({ ex: ex.name, kind: 'Est. 1RM', value: be });
  }
  const muscles = new Set(d.exercises.map(e => e.muscle).filter(Boolean));

  el.append(h('div', { class: 'topbar' },
    h('button', { class: 'iconbtn', onclick: () => nav.nav.pop() }, icon('arrow_back')),
    h('div', { class: 'topbar-title t-center', style: { fontSize: '17px', fontWeight: '800' } }, L10n.s('Workout Summary', 'Résumé de la séance')),
    h('div', { style: { width: '48px' } }),
  ));

  const list = h('div', { class: 'screen screen-pad', style: { paddingTop: '6px' } });
  const nameField = h('input', { class: 'namefield', value: name });
  nameField.addEventListener('input', () => { name = nameField.value; d.name = name; S.persistDraftNow(); });
  list.append(h('div', { style: { padding: '6px 0' } }, nameField));

  list.append(h('div', { style: { display: 'flex', alignItems: 'center', gap: '8px', padding: '4px 0' } },
    statCol(duration, L10n.s('Duration', 'Durée'), true),
    statCol(`${Calc.fmtVol(vol, unit)} ${Calc.unitLabel(unit)}`, L10n.s('Volume', 'Volume')),
    statCol(String(sets), L10n.s('Sets', 'Séries')),
    bodyMaps(),
  ));

  if (prs.length) {
    list.append(h('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', padding: '10px 16px 0' } },
      icon('emoji_events', { size: 17, cls: 't-gold' }),
      h('div', { style: { color: 'var(--gold)', fontSize: 13, fontWeight: 800 } }, L10n.s('RECORDS', 'RECORDS'))));
    for (const p of prs) {
      list.append(h('div', { class: 'prrow' },
        icon('emoji_events', { size: 15, cls: 't-gold' }),
        h('div', { style: { flex: 1, fontSize: '13.5px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' } }, exName(p.ex)),
        h('div', { style: { color: 'var(--orange)', fontWeight: 700, fontSize: 12, whiteSpace: 'nowrap' } },
          `${p.kind === 'Weight' ? L10n.s('Heaviest Weight', 'Plus Gros Poids') : L10n.s('Best Est. 1RM', 'Meilleure Est. 1RM')} · ${Calc.fmtKg(p.value, unit)}${Calc.unitLabel(unit)}`)));
    }
  }

  list.append(h('div', { style: { color: 'var(--mut)', fontSize: 13, fontWeight: 800, padding: '8px 16px' } },
    L10n.s('Exercises', 'Exercices')));

  for (const ex of d.exercises.filter(ex => ex.sets.some(s => s.done))) {
    const box = h('div', { style: { padding: '8px 0' } },
      h('div', { style: { color: 'var(--accent)', fontSize: '15.5px', fontWeight: 700 } }, exName(ex.name)));
    ex.sets.filter(s => s.done && (s.kg != null || s.reps != null || s.mins != null || s.km != null)).forEach((s, i) => {
      box.append(h('div', { class: 'setrow-s' + ((s.prW || s.prE) ? ' pr' : '') },
        h('div', { style: { width: '20px', color: 'var(--mut)', fontSize: 13 } }, String(i + 1)),
        h('div', { style: { flex: 1, fontSize: '13.5px', fontWeight: 600 } },
          ex.muscle === 'Cardio' ? Calc.fmtCardioSet(s.mins, s.km)
            : `${s.kg != null ? Calc.fmtKg(s.kg, unit) + Calc.unitLabel(unit) : '—'} × ${s.reps ?? '—'}`),
        (s.prW || s.prE) ? icon('emoji_events', { size: 14, cls: 't-gold' }) : null));
    });
    list.append(box);
  }

  if (d.notes && d.notes.trim()) {
    list.append(h('div', { style: { padding: '8px 16px' } },
      h('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--mut)', fontSize: 13, fontWeight: 800 } },
        icon('notes', { size: 15 }), L10n.s('Notes', 'Notes')),
      h('div', { style: { fontSize: '13.5px', lineHeight: '19px', marginTop: '6px' } }, d.notes)));
  }

  /* ---- PHOTO DE PROGRESSION ---- */
  const photoSection = h('div', { style: { padding: '10px 0' } });
  const fileInput = h('input', { type: 'file', accept: 'image/*', style: { display: 'none' } });
  fileInput.addEventListener('change', async () => {
    const f = fileInput.files[0];
    if (!f) return;
    try {
      const blob = await downscaleToJpeg(f, 1440, 0.86);
      const p = await addPhoto(blob, Date.now(), null);
      addedPhotoId = p.id;
      paintPhoto();
    } catch { toast(L10n.s('Could not read this image', 'Impossible de lire cette image')); }
    fileInput.value = '';
  });
  el.append(fileInput);

  function paintPhoto() {
    const added = addedPhotoId != null ? photoById(addedPhotoId) : null;
    const wrap2 = h('div', { style: { padding: '0 16px' } },
      h('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--mut)', fontSize: 13, fontWeight: 800, marginBottom: '8px' } },
        icon('add_a_photo', { size: 15 }), L10n.s('PROGRESS PHOTO', 'PHOTO DE PROGRESSION')));
    if (added) {
      const box = h('div', { class: 'photobox', onclick: () => nav.nav.push('photoViewer', { id: added.id }) });
      photoUrl(added.id).then(u => {
        if (u) box.append(h('img', { src: u, alt: '', style: { width: '100%', height: '100%', objectFit: 'cover', display: 'block' } }));
      });
      box.append(h('button', {
        class: 'photodel', onclick: e => { e.stopPropagation(); deletePhoto(added.id); addedPhotoId = null; paintPhoto(); },
      }, icon('close', { size: 14 })));
      wrap2.append(h('div', { style: { display: 'flex', gap: '12px' } }, box,
        h('div', { style: { flex: 1, color: 'var(--mut)', fontSize: 12, lineHeight: '17px' } },
          L10n.s('Saved to your progress gallery — compare it later with your previous photos.',
            'Ajoutée à ta galerie progression — compare-la plus tard avec tes anciennes photos.'))));
    } else {
      wrap2.append(h('button', { class: 'addphoto', onclick: () => fileInput.click() },
        icon('add_a_photo', { size: 18 }),
        L10n.s('Add a photo of your physique', 'Ajouter une photo de ton physique')));
    }
    photoSection.replaceChildren(wrap2);
  }
  paintPhoto();
  list.append(photoSection);

  list.append(h('div', { style: { height: '16px' } }));
  el.append(list);

  function doFinish() {
    const w = S.finishWorkout(name);
    if (addedPhotoId != null && w) S.linkPhotoToWorkout(addedPhotoId, w.id);
    restClear();
    nav.nav.toTab(0);
    if (w && (w.prs || []).length) {
      toast(L10n.s('Workout saved', 'Séance enregistrée') + ` · ${(w.prs || []).length} ${L10n.s('PRs', 'records')}!`);
    } else toast(L10n.s('Workout saved', 'Séance enregistrée'));
  }

  const finishBtn = h('div', { style: { padding: '8px 16px calc(8px + env(safe-area-inset-bottom))' } },
    h('button', {
      class: 'btn-primary',
      onclick: () => {
        if (S.draftDiffersFromRoutine()) {
          dialog({
            title: L10n.s('Routine modified', 'Routine modifiée'),
            message: L10n.s('Save these changes for your next sessions?', 'Enregistrer les modifications pour les prochaines séances ?', '¿Guardar los cambios para las próximas sesiones?', 'Änderungen für kommende Einheiten speichern?'),
            actions: [
              { label: L10n.s('Skip', 'Ignorer', 'Descartar', 'Verwerfen'), class: 'mut', onClick: doFinish },
              { label: L10n.s('Save', 'Enregistrer', 'Guardar', 'Speichern'), class: 'acc', onClick: () => { S.updateRoutineFromDraft(); doFinish(); } },
            ],
          });
        } else doFinish();
      },
    }, L10n.s('TERMINER', 'TERMINER')));
  el.append(finishBtn);
}

function statCol(value, label, accent = false) {
  return h('div', { class: 'stat' },
    h('div', { class: 'l' }, label),
    h('div', { class: 'v', style: accent ? { color: 'var(--accent)' } : null }, value));
}

function bodyMaps() {
  const muscles = new Set(store.draft.exercises.map(e => e.muscle).filter(Boolean));
  return h('div', { style: { display: 'flex', gap: '8px', flex: '0 0 auto' } },
    bodyMap(true, muscles), bodyMap(false, muscles));
}
