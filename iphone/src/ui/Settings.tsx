/** Settings — port of Settings.kt (account, profile, units, accent, data import/export, avatar). */
import React, { useState } from "react";
import { Platform, Pressable, ScrollView, Share, View } from "react-native";
import * as DocumentPicker from "expo-document-picker";
import * as ImagePicker from "expo-image-picker";
import { File, Paths } from "expo-file-system";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { Cloud } from "../cloud";
import { avatar } from "../avatar";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C } from "../theme";
import { s } from "../l10n";
import { exName } from "../l10nshim";
import { Alert, AvatarImg, PromptDialog, Row, Txt, toast } from "./comps";
import { MIcon } from "./iconsEx";

export default function SettingsScreen() {
  useStore(Repo.rev);
  useStore(Cloud.uiStore);
  const [editName, setEditName] = useState(false);
  const [editHandle, setEditHandle] = useState(false);
  const [confirmWipe, setConfirmWipe] = useState(false);
  const sess = Cloud.session;
  const st = Repo.settings;

  // ---- imports ----
  const pickHevyExport = async () => {
    try {
      const res = await DocumentPicker.getDocumentAsync({ type: "*/*", copyToCacheDirectory: true });
      if (res.canceled || !res.assets?.length) return;
      const file = res.assets[0];
      let text = "";
      if (Platform.OS === "web") {
        const blob = await (await fetch(file.uri)).blob();
        text = await blob.text();
      } else {
        text = await new File(file.uri).text();
      }
      // Hevy export = a zip of CSVs; RN can't unzip — accept plain CSV ( unzip via JSZip later if needed )
      let ws: any[] = [];
      let rs: any[] = [];
      const [w, r] = Calc.parseHevyCsv(text);
      ws = ws.concat(w);
      rs = rs.concat(r);
      if (ws.length === 0 && rs.length === 0) {
        toast(s("No Hevy data found — header:", "Aucune donnée Hevy trouvée — en-tête :") + " " + (text.split("\n")[0] ?? "").slice(0, 90));
      } else {
        const n = Repo.importHevy(ws, rs);
        toast(s("%1$d workouts imported from Hevy (routines rebuilt)", "%1$d séances importées (routines reconstruites)").replace("%1$d", String(n)));
      }
    } catch {
      toast(s("Import failed (check format)", "Import échoué (vérifie le format)"));
    }
  };

  const pickCsv = async () => {
    try {
      const res = await DocumentPicker.getDocumentAsync({ type: Platform.OS === "web" ? "*/*" : "text/*", copyToCacheDirectory: true });
      if (res.canceled || !res.assets?.length) return;
      const file = res.assets[0];
      const text =
        Platform.OS === "web" ? await (await (await fetch(file.uri)).blob()).text() : await new File(file.uri).text();
      let n = Repo.importCsv(text);
      if (n === 0) {
        const [ws, rs] = Calc.parseHevyCsv(text);
        if (ws.length || rs.length) n = Repo.importHevy(ws, rs);
      }
      toast(n > 0 ? s("%1$d workouts imported", "%1$d séances importées").replace("%1$d", String(n)) : s("Nothing imported (check format)", "Rien d'importé (vérifie le format)"));
    } catch {
      toast(s("Import failed (check format)", "Import échoué (vérifie le format)"));
    }
  };

  const restoreBackup = async () => {
    try {
      const res = await DocumentPicker.getDocumentAsync({ type: "*/*", copyToCacheDirectory: true });
      if (res.canceled || !res.assets?.length) return;
      const file = res.assets[0];
      const text =
        Platform.OS === "web" ? await (await (await fetch(file.uri)).blob()).text() : await new File(file.uri).text();
      const ok = Repo.restoreBackup(text);
      toast(ok ? s("Backup restored", "Sauvegarde restaurée") : s("Invalid backup file", "Fichier de sauvegarde invalide"));
    } catch {
      toast(s("Invalid backup file", "Fichier de sauvegarde invalide"));
    }
  };

  const exportCsv = () => {
    const unit = st.unit;
    const lines = ["Date;Heure;Exercice;Serie;KG;Reps"];
    for (const w of [...Repo.workouts].sort((a, b) => a.startedAt - b.startedAt)) {
      const date = Calc.fmtDateShort(w.startedAt);
      const time = Calc.fmtTime(w.startedAt);
      for (const ex of w.exercises)
        ex.sets.forEach((stt, i) => {
          lines.push(`${date};${time};${exName(ex.name).replace(/;/g, ",")};${i + 1};${stt.kg != null ? Calc.fmtKg(stt.kg, unit) : ""};${stt.reps ?? ""}`);
        });
    }
    const content = lines.join("\n");
    if (Platform.OS === "web") downloadFile("hevy-seances.csv", content, "text/csv");
    else void shareFile("hevy-seances.csv", content);
  };

  const backupJson = () => {
    const content = Repo.backupJson();
    if (Platform.OS === "web") downloadFile("hevy-sauvegarde.json", content, "application/json");
    else void shareFile("hevy-sauvegarde.json", content);
  };

  const shareFile = async (name: string, content: string) => {
    try {
      const f = new File(Paths.cache, name);
      f.write(content);
      if (await Sharing.isAvailableAsync()) await Sharing.shareAsync(f.uri);
      else toast(name);
    } catch {
      toast(name);
    }
  };

  const pickPhoto = async () => {
    const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) {
      toast("Autorise l'accès aux photos dans les Réglages.");
      return;
    }
    const res = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ["images"],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.85,
      base64: true,
    });
    if (res.canceled || !res.assets?.[0]?.base64) return;
    const base64 = res.assets[0].base64!;
    await avatar.writeBase64(base64);
    Repo.touchPublic();
    if (Cloud.session) {
      const url = await Cloud.uploadAvatar(base64);
      if (url) Repo.setAvatar(url);
    }
  };

  return (
    <View style={{ flex: 1, backgroundColor: C.Bg }}>
      <Row style={{ paddingHorizontal: 8, paddingVertical: 8 }}>
        <Pressable onPress={() => Nav.pop()} hitSlop={6} style={{ padding: 6 }}>
          <MIcon name="arrow-back" size={24} color={C.Text} />
        </Pressable>
        <Txt weight={700} size={18}>
          {s("Settings", "Réglages")}
        </Txt>
      </Row>
      <ScrollView contentContainerStyle={{ paddingBottom: 30 }}>
        {/* COMPTE */}
        <SectionHeader text={s("ACCOUNT", "COMPTE")} />
        {sess ? (
          <>
            <SettingsRow
              left={
                <Row>
                  <AvatarImg letter={st.profileName.trim().slice(0, 1).toUpperCase() || "A"} size={56} />
                  <View style={{ width: 14 }} />
                  <View>
                    <Txt size={15.5}>{s("Profile photo", "Photo de profil")}</Txt>
                    <Txt size={12.5} color={C.Mut}>
                      {s("Tap to change", "Touche pour changer")}
                    </Txt>
                  </View>
                </Row>
              }
              onClick={pickPhoto}
              chevron
            />
            <RowDivider />
            <SettingsRow title={s("Sync now", "Synchroniser maintenant")} value={Cloud.syncStatus ?? ""} onClick={() => { Cloud.requestSync(0); toast(s("Syncing…", "Synchronisation…")); }} />
            <RowDivider />
            <SettingsRow title={sess.email} value="" onClick={() => {}} />
            <RowDivider />
            <SettingsRow
              title={s("Sign out", "Se déconnecter")}
              red
              onClick={() => {
                Cloud.signOut();
                Nav.toTab({ t: "ProfileTab" });
              }}
            />
          </>
        ) : (
          <SettingsRow
            title={s("Sign in or create an account", "Se connecter ou créer un compte")}
            blue
            onClick={() => {
              Cloud.skipped = false;
              void Cloud.persistSkip();
            }}
          />
        )}
        {/* PROFIL */}
        <SectionHeader text="PROFIL" />
        <SettingsRow title={s("Name", "Nom")} value={st.profileName} onClick={() => setEditName(true)} />
        <RowDivider />
        <SettingsRow title={s("Username", "Pseudo")} value={`@${st.handle}`} onClick={() => setEditHandle(true)} />
        {/* GÉNÉRAL */}
        <SectionHeader text={s("GENERAL", "GÉNÉRAL")} />
        <SettingsRow title={s("Units", "Unités")} value={st.unit} onClick={() => Repo.setUnit(st.unit === "kg" ? "lb" : "kg")} />
        <RowDivider />
        <SettingsRow
          title={s("Rest timer", "Minuteur de repos")}
          value={restLabel(st.restSec)}
          onClick={() => {
            const next = [60, 90, 120, 180].find((v) => v > st.restSec) ?? 60;
            Repo.setRest(next);
          }}
        />
        <RowDivider />
        <Row style={{ paddingHorizontal: 16, paddingVertical: 14 }}>
          <Txt size={15.5} style={{ flex: 1 }}>
            {s("Accent color", "Couleur d'accent")}
          </Txt>
          <Row style={{ gap: 10 }}>
            {[
              ["blue", "#028CFD"],
              ["teal", "#20B49A"],
              ["violet", "#7C5CFF"],
              ["orange", "#FF7A45"],
            ].map(([key, color]) => (
              <Pressable
                key={key}
                onPress={() => Repo.setAccent(key)}
                style={{
                  width: 26,
                  height: 26,
                  borderRadius: 13,
                  backgroundColor: color,
                  borderWidth: st.accent === key ? 2.5 : 0,
                  borderColor: C.Text,
                }}
              />
            ))}
          </Row>
        </Row>
        {/* DONNÉES */}
        <SectionHeader text={s("DATA", "DONNÉES")} />
        <SettingsRow title={s("Import from Hevy (account export)", "Importer depuis Hevy (export du compte)")} onClick={pickHevyExport} />
        <RowDivider />
        <SettingsRow title={s("Import workouts (CSV)", "Importer des séances (CSV)")} onClick={pickCsv} />
        <RowDivider />
        <SettingsRow title={s("Export workouts (CSV)", "Exporter les séances (CSV)")} onClick={exportCsv} />
        <RowDivider />
        <SettingsRow title={s("Backup data (JSON)", "Sauvegarder les données (JSON)")} onClick={backupJson} />
        <RowDivider />
        <SettingsRow title={s("Restore backup", "Restaurer une sauvegarde")} onClick={restoreBackup} />
        <RowDivider />
        <SettingsRow title={s("Erase all data", "Tout effacer")} red onClick={() => setConfirmWipe(true)} />
        <Txt size={11.5} color={C.Mut} style={{ textAlign: "center", paddingTop: 26 }}>
          {`Liftbook iPhone 1.0${sess ? " · " + s("synced as", "synchronisé en tant que") + " " + sess.email : ""}`}
        </Txt>
      </ScrollView>

      <PromptDialog
        visible={editName}
        title={s("Name", "Nom")}
        initial={st.profileName}
        maxLength={30}
        confirmLabel={s("Save", "Enregistrer")}
        onConfirm={(v) => {
          if (v.trim()) Repo.setProfile(v.trim(), st.handle);
          setEditName(false);
        }}
        onDismiss={() => setEditName(false)}
      />
      <PromptDialog
        visible={editHandle}
        title={s("Username", "Pseudo")}
        initial={st.handle}
        maxLength={20}
        prefix="@"
        filter={(c) => /[\w.]/.test(c)}
        confirmLabel={s("Save", "Enregistrer")}
        onConfirm={(v) => {
          if (v.trim()) Repo.setProfile(st.profileName, v.trim());
          setEditHandle(false);
        }}
        onDismiss={() => setEditHandle(false)}
      />
      <Alert
        visible={confirmWipe}
        title={s("Erase all data?", "Tout effacer ?")}
        message={s(
          "All workouts, routines and records will be permanently deleted from this device and your account.",
          "Toutes les séances, routines et records seront définitivement supprimés de cet appareil et de ton compte.",
        )}
        confirmLabel={s("Erase", "Effacer")}
        confirmColor={C.Red}
        dismissLabel={s("Cancel", "Annuler")}
        onConfirm={() => {
          setConfirmWipe(false);
          Repo.wipe();
          Nav.toTab({ t: "HomeTab" });
        }}
        onDismiss={() => setConfirmWipe(false)}
      />
    </View>
  );
}

function restLabel(sec: number): string {
  const m = Math.floor(sec / 60);
  const r = sec % 60;
  return `${m > 0 ? `${m}min ` : ""}${r}s`;
}

function SectionHeader({ text }: { text: string }) {
  return (
    <Txt weight={700} size={11} color={C.Mut} style={{ paddingHorizontal: 16, paddingTop: 24, paddingBottom: 4, letterSpacing: 1.1 }}>
      {text}
    </Txt>
  );
}

function RowDivider() {
  return <View style={{ height: 0.5, backgroundColor: C.Card, marginLeft: 16 }} />;
}

function SettingsRow({
  title,
  value = "",
  red = false,
  blue = false,
  onClick,
  left,
  chevron = false,
}: {
  title?: string;
  value?: string;
  red?: boolean;
  blue?: boolean;
  onClick: () => void;
  left?: React.ReactNode;
  chevron?: boolean;
}) {
  return (
    <Pressable onPress={onClick} style={{ flexDirection: "row", alignItems: "center", paddingHorizontal: 16, paddingVertical: 15 }}>
      {left ?? (
        <Txt size={15.5} numberOfLines={1} color={red ? C.Red : blue ? "#028CFD" : C.Text} style={{ flex: 1 }}>
          {title}
        </Txt>
      )}
      {!left && value.trim() !== "" ? (
        <Txt size={13.5} color={C.Mut} numberOfLines={1}>
          {value}
        </Txt>
      ) : null}
      {left || chevron ? <MIcon name="chevron-right" size={18} color={C.Mut} /> : null}
    </Pressable>
  );
}

function downloadFile(name: string, content: string, mime: string) {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
}

import * as Sharing from "expo-sharing";
