# CHANGELOG — Liftbook

Historique des livraisons (les notes complètes vivent dans les messages de commit).

## Web

- **v1.55** — Bouclier du démarrage : toute donnée du localStorage (séances, routines, photos, customs, brouillon, session, méta sync) passe désormais par les normalisateurs au boot — un localStorage corrompu ne provoque plus de page blanche, les données inexploitables sont écartées avec un toast invitant à restaurer une sauvegarde. Multi-onglets : pendant une séance en cours dans un onglet, les écritures de l'autre onglet sont maintenant aussi *relues* (et pas seulement signalées) — le prochain `persistAll()` ne peut plus écraser l'autre onglet avec une copie périmée.
- **v1.54** — Parité pixel Android↔web : boutons, espacements, tailles calés capture par capture sur l'APK.
- **v1.53 / v1.53.1** — Correctifs du méga-audit sécurité : API pertes de données (LWW par `uTs` par séance, accusé serveur du push), callback OAuth Google réparé (`/auth-callback` sans `.html`), gestion `lww_stale`, dialogue de fusion au premier login.
- **v1.52** — Port web complet (SPA sans build ni dépendance, PWA, offline, 10 écrans, parité totale Android + iOS).

## Android (source de vérité)

- **v1.52** — Parité totale iOS + web : nav de restauration au process death, i18n des partages/exports.
- **v1.51** — Suppression d'exercice personnalisé (tombstone `delC`, SyncPayload v4), la lacune n°1 de l'audit.
- **v1.50** — Méga-audit : 4 bugs P0 (double import CSV, annuler/restaure écrasé par la sync, poids corporel corrompu ×2,2 en lb, customs résurrectibles après wipe) + drag & drop réanimé + saisie SetField définitive + chime de repos + durcissement serveur (RPC borné 8 Mo, buckets 10 Mo, policies RLS).
- **v1.49** — Exercices personnalisés (création, catalogue global, sync v3) + streak affiché sur le profil.
- **v1.48** — Cardio façon Hevy (MIN/KM au lieu de KG×RÉPS sur 21 exercices) + calculateur de disques + fix majeur de perte de données (valeur saisies/coches jamais persistées au kill du process).
- **v1.47** — Photos de progression (séance, galerie, avant/après, poids corporel) + sync cloud bucket privé + tombstones.
- **v1.46** — Chime de fin de repos toujours audible (bascule auto sur le flux ALARME en silencieux) + vibrations.
- **v1.45** — Notifs refondues au template système Android 12 (repos −15/+15/Passer dans la notification).
- **v1.44 / v1.35** — Port iOS natif (SwiftUI + LiftbookCore SwiftPM, 17 tests) / durcissement sécurité (tokens chiffrés AndroidKeyStore AES-GCM, TLS-only) + kit de soumission Play Store.
- **v1.42 → v1.28** — Connexion Google native réparée de bout en bout (Credential Manager, Custom Tabs, PKCE sans `state`, GoTrue).
- **Historique antérieur** : voir les messages de commit (refonte de l'écran de séance v1.31/1.33, blocage « séance en cours » v1.32…).

## CI

- **data-parity** (nouveauté) — job GitHub Actions qui re-génère les 4 ports depuis les sources Kotlin (`extract_data.py` → `extract_web.py` / `extract_swift.py` / `extract_figures.py`) et échoue avec le diff si les modules générés divergent du commit.
