# Contrat d'API du port web Liftbook (pour les modules d'écrans)

Architecture : SPA ES modules SANS build, SANS dépendance. Fidélité pixel/UX à l'app Android
(`android/app/src/main/java/com/hevyclone/app/`) = source de vérité. FR-first : tous les libellés
via `L10n.s(en, fr)` (l'app Android affiche FR sur téléphone FR, EN sinon).

Servir en local : `cd web && python3 -m http.server 8090` → http://localhost:8090

## Conventions impératives
- Un fichier = un écran (ou groupe cohéré). Chaque module se termine par `nav.registerScreen('<nom>', { render })`.
- `render(el, params)` reconstruit l'écran dans `el` (le routeur vide `el` avant). Pour les mises à jour
  live, s'abonner avec `ui.sub(el, 'topic', cb)` (auto-nettoyé au changement d'écran).
- CSS : classes globales de `web/styles.css` (voir section CSS). Pour du CSS spécifique, injecter UNE FOIS
  un `<style>` depuis le module : `if (!document.getElementById('css-<screen>')) document.head.append(styleEl)`
  avec des règles préfixées par une classe d'écran (ex `.scr-logger …`). NE JAMAIS modifier styles.css,
  ui.js, store.js, router.js, app.js — si un composant manque, le coder localement dans le module et le
  signaler dans le rapport final.
- Formats/sémantiques : TOUJOURS reprendre le code Kotlin correspondant (labels FR exacts, tailles,
  comportements, ordres, pluriels). Citer les fonctions portées dans les commentaires.
- Timestamps partout en ms epoch. Modèles JSON identiques au sync payload Android (kg, reps, mins, km,
  done, prW, prE / name, muscle, notes, superset, restSec, sets / id, name, startedAt, endedAt, exercises,
  prs, notes / id, name, exercises, pos).
- Jamais de `alert/confirm/prompt` natifs — utiliser `ui.dialog/confirmDialog/promptDialog/ctxMenu/sheet`.

## router.js
```js
import * as nav from '../router.js';
nav.registerScreen('logger', { render(el, params) {} });   // noms : homeTab, trainingTab, profileTab,
nav.nav                                                // { tab, stack, push(s, params), pop(), toTab(i), current, atTab }
```
Écrans poussables : settings, history, exercises, logger, summary, progress, workoutDetail{id},
exerciseDetail{name}, routineDetail{id}, photoViewer{id}, comparePhotos{aId,bId}.

## store.js (port de Repo.kt)
```js
import { store, on, off, emit } from '../store.js';
import * as S from '../store.js';
store.workouts / store.routines / store.photos / store.customs / store.settings / store.draft /
  store.session / store.skipped / store.deletedUndo / store.syncStatus / store.prCache
S.workoutById(id) S.routineById(id) S.workoutsDesc() S.photosDesc() S.photoById(id)
S.photoForWorkout(wId) S.photoUrl(id)→Promise<objectURL|null> S.photoBlob(id)
S.addPhoto(blob, ts?, wId?)→Promise<photo> S.updatePhoto(id, note, kg, kgSet) S.linkPhotoToWorkout(photoId, wId)
S.deletePhoto(id) S.avatarBlob() S.setAvatarBlob(blob)
S.startWorkout(routineId|null) S.startRoutine(routineId|null) S.startRepeat(workoutId|null)
S.addExToDraft(name) S.saveRoutine(name) S.finishWorkout(name) S.discardDraft()
S.draftDiffersFromRoutine() S.updateRoutineFromDraft() S.restoreWorkout(w) S.deleteWorkout(id)
S.routineFromWorkout(id, name) S.renameRoutine(id, name) S.duplicateRoutine(id) S.deleteRoutine(id)
S.moveRoutine(from, to) S.uniqueRoutineName(base)
S.addCustom(name, muscle, equip)→cx|null S.deleteCustom(name) S.customNameTaken(name)
S.setUnit(u) S.setRest(sec) S.setAccent(a) S.setProfile(name, handle) S.setAvatar(url)
S.backupJson() S.restoreBackup(text)→bool S.importCsv(text)→n S.importHevy(workouts, routines)→n
S.wipe(markDirty=true) S.snapshot() S.prFor(name) S.e1rmSeries(name)
S.prevFor(name)→string[]|null S.prevSetsBefore(beforeMs, name) S.routineLastPerformed(r)
S.markDirty() S.requestSync(ms) S.syncNow() S.persistDraftNow() S.persistSkip()
S.applySession(session) S.signOutNow()   // session = {email,userId,access,refresh,expiresAt}
S.on(topic, cb) — topics : workouts, routines, draft, photos, customs, settings, session, sync, change, avatar
```

## ui.js (port de Comps.kt)
```js
import * as ui from '../ui.js';
ui.h(tag, attrs, ...kids)     // hyperscript : attrs {class, style{}, onclick, dataset{}, value…}
ui.icon('fitness_center', { size: 24, fill: false, cls: '' })   // icône Material Symbols (nom snake_case)
ui.toast(msg)                 // toast bas d'écran 2,6 s
ui.dialog({ title, message, actions: [{label, class: 'acc'|'red'|'mut', onClick}], body, onDismiss })
ui.confirmDialog({ title, message, confirmLabel, cancelLabel, destructive, onConfirm, onCancel })
ui.promptDialog({ title, placeholder, value, maxLength, prefix, filter, confirmLabel, onConfirm, multiline, numeric })
ui.listDialog({ title, items, onPick, render })
ui.sheet({ title, okLabel, onOk, onClose, render(bodyEl, close) })
ui.ctxMenu(anchorEl, [{ label, icon, red, onClick }])           // menu ⋮ positionné
ui.primaryButton(label, { leading, onClick, compact, small, disabled })
ui.ghostButton(label, { leading, onClick })                     // bouton bordé (GhostButton)
ui.chipEl(label, selected, onClick) ui.sectionLabel(text) ui.emptyState(text, slim)
ui.avatarEl(letter, size, imgUrl) ui.exCircle(muscle, size)     // badge blanc + silhouette
ui.figureSvg(muscle, size, { variant: 'dark'|'light' })         // grande silhouette (instructions)
ui.bodyMap(front, musclesSet, w)                                // pictogramme 34×56
ui.lineChart([{ label, value }], fmtLabel)                      // courbe lissée animée + labels Y
ui.downscaleToJpeg(fileOrBlob, maxDim=1440, quality=.86)→Promise<blob>
ui.imgEl(blobUrl, { fit, radius }) ui.sub(el, topic, cb)
```

## calc.js (port de Calc.kt — en cours d'écriture, API garantie)
`e1rm(kg, reps), round125(x), vol(workout), repsOf(w), setsDone(w), totalVol(workouts),
fmtKg(kg, unit), unitLabel(unit), toKg(text, unit), toDisplay(kg, unit), fmtVol(k, unit), fmtDur(ms),
fmtClock(ms), fmtDateShort(ms), fmtDateFull(ms), fmtTime(ms), monthLabel(d), monthShort(d), dayName(ms),
dayKey(ms), weekStart(ms), streak(workouts, nowMs), weekStats(workouts, nowMs), weekly(n, workouts, nowMs),
muscleDist(workouts, n), e1rmSeries(name, workouts), prevFor(name, workouts, unit),
rebuildPrs(workouts)→prCache, parseCsv(text)→workouts, parseHevyCsv(text)→{workouts, routines},
routinesFromWorkouts(workouts, existingNames), seed(nowMs), platesForSide(target, barKg, unit),
isCardioName(name), fmtCardioSet(mins, km), exportCsvText(workouts, unit)`
⚠️ vérifier les noms réels dans web/src/calc.js avant d'appeler (l'agent peut avoir suffixé différemment :
les fonctions cardioplastes s'appellent peut-être exactement comme ci-dessus — en cas de doute `grep "export function"`).

## search.js (port de L10n/L10nData — en cours d'écriture, API garantie)
`L10n.s(en, fr, es?, de?) L10n.setLang(l) L10n.relativeTime(ms, nowMs) L10n.seriesLabel(n, name)
L10n.seeMoreExercises(n) exName(nameEn) muscleName(m) equipName(e) cuesFor(muscle) stepsFor(nameEn)
equipHintFor(equip) archetypeOf(nameEn) matches(query, nameEn)`
Catalogue : `import { EXERCISES, MUSCLES, NAME_FR, MUSCLE_FR, EQUIP_FR } from '../data.js'`
(les customs sont déjà fusionnés dans la recherche via setCustomDefs).

## cloud.js (en cours d'écriture)
`signIn, signUp, refreshSession, logout, mapAuthError, googleAuthorizeUrl, exchangePkce,
pullSnapshot, pushSnapshot, uploadAvatar, uploadProgressPhoto, downloadProgressPhoto,
deleteStorageObject, randomPkceVerifier, pkceChallenge, isSessionExpired, SUPABASE_URL, ANON_KEY`

## CSS global disponible (styles.css)
Conteneur `#app` 430px centré. `.screen` scrollable + `.screen-pad` (padding bas 96px pour tabbar).
`.topbar .topbar-title .iconbtn` · `.t-display 27/800 .t-title 22/800 .t-h1 19/700 .t-h2 17/700 .t-h3 15/600
.t-body .t-sm .t-xs .t-mut .t-mut2 .t-acc .t-red .t-gold` · `.card` (14dp, bord line) · `.btn-primary`
(pilule 50) `.btn-sec .btn-ghost .btn-blue-row .btn-white` · `.chip .sel .chips-row` · `.field .searchbar` ·
`.tabbar .tab .sel` · `.toast-host .toast` · `.undobar .undo-btn` · `.overlay .dialog .dialog-title
.dialog-msg .dialog-actions` · `.sheet-overlay .sheet .sheet-head .sheet-body` · `.menu-wrap .menu
.menu-item .red` · `.setrow .val .red .blue .section-label` · `.hairline .empty .pill .accbar-wrap
.accbar .link .divider-or` · logger : `.exsection .exsection-head .exname .setgrid-head .setrow-grid
.setnum .setfield .setcheck .on .setrow-pr` · `.restbar .rest-progress .rest-time .over .restbtn .skip` ·
`.avatar .excircle .badge-date` · accents via `#app[data-accent=teal|violet|orange]`.

## Pièges connus
- Le mode lb : saisie/affichage convertis, stockage TOUJOURS en kg (`toKg`/`fmtKg`).
- Le texte d'un SetField prime pendant le focus (ne réécris pas la valeur d'un input document.activeElement).
- Les tables de séries : entêtes SÉRIE | PRÉCÉDENT (séance) | KG/LBS ou MIN | RÉPS ou KM | ✓.
- Partages : `navigator.share({ text })` si dispo, sinon `navigator.clipboard.writeText(text)` + toast.
- Les boutons sociaux du feed (👍💬⎋) sont décoratifs → toasts (« Ajouté aux favoris », etc.).
- Toast « <nom> ajouté » du picker : nom canonique EN (comportement Android).
