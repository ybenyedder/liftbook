/** Profile — port of Profile.kt (heatmap, monthly bars, muscle distribution). */
import React, { useMemo, useState } from "react";
import { Pressable, ScrollView, View } from "react-native";
import Svg, { Rect } from "react-native-svg";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { Cloud } from "../cloud";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C, accentColor } from "../theme";
import { s, getLang } from "../l10n";
import { muscleName } from "../l10nshim";
import { AppCard, AvatarImg, EmptyState, IllIcon, Row, Txt } from "./comps";
import { MIcon } from "./iconsEx";

export default function ProfileScreen() {
  useStore(Repo.rev);
  useStore(Cloud.uiStore);
  const st = Repo.settings;
  const unit = st.unit;
  const [heatYear, setHeatYear] = useState(new Date().getFullYear());

  const totalVol = useMemo(() => Calc.totalVol(Repo.workouts), [Repo.rev.value]);
  const totalSets = useMemo(() => Repo.workouts.reduce((a, w) => a + Calc.setsDone(w), 0), [Repo.rev.value]);
  const totalReps = useMemo(() => Repo.workouts.reduce((a, w) => a + Calc.reps(w), 0), [Repo.rev.value]);
  const dayVolumes = useMemo(() => {
    const m = new Map<string, number>();
    for (const w of Repo.workouts) {
      const k = Calc.dayKey(w.startedAt);
      m.set(k, (m.get(k) ?? 0) + Calc.vol(w));
    }
    return m;
  }, [Repo.rev.value]);
  const dist = useMemo(() => Calc.muscleDist(Repo.workouts, 15), [Repo.rev.value]);
  const maxVol = dist[0]?.[1] ?? 1;
  const accent = accentColor(st.accent);

  // month + rolling stats
  const monthStart = new Date(new Date().getFullYear(), new Date().getMonth(), 1).getTime();
  const month = Repo.workouts.filter((w) => w.startedAt >= monthStart);
  const now = Date.now();
  const last7 = Repo.workouts.filter((w) => w.startedAt >= now - 7 * 86400000);
  const prev7 = Repo.workouts.filter((w) => w.startedAt >= now - 14 * 86400000 && w.startedAt < now - 7 * 86400000);
  const v1 = last7.reduce((a, w) => a + Calc.vol(w), 0);
  const v2 = prev7.reduce((a, w) => a + Calc.vol(w), 0);
  const delta = v2 > 0 ? Math.round(((v1 - v2) / v2) * 100) : null;

  const monthly = useMemo(() => {
    const cal = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
    return Array.from({ length: 6 }, (_, i) => {
      const m = new Date(cal.getFullYear(), cal.getMonth() - (5 - i), 1);
      const start = m.getTime();
      const end = new Date(m.getFullYear(), m.getMonth() + 1, 1).getTime();
      const label = Calc.monthShort(start, getLang() === "fr" ? "fr-FR" : "en-US");
      return [label, Repo.workouts.filter((w) => w.startedAt >= start && w.startedAt < end).reduce((a, w) => a + Calc.vol(w), 0)] as [string, number];
    });
  }, [Repo.rev.value]);
  const maxV = Math.max(1, ...monthly.map((m) => m[1]));

  return (
    <ScrollView style={{ flex: 1, backgroundColor: C.Bg }} contentContainerStyle={{ paddingBottom: 24 }}>
      {/* header */}
      <Row style={{ paddingHorizontal: 16, paddingRight: 8, paddingTop: 10, alignItems: "flex-start" }}>
        <View style={{ flex: 1 }}>
          <Row>
            <AvatarImg letter={st.profileName.trim().slice(0, 1).toUpperCase() || "A"} size={52} />
            <View style={{ width: 14 }} />
            <View>
              <Txt weight={700} size={23}>
                {st.profileName}
              </Txt>
              <Txt size={13} color={C.Mut}>
                @{st.handle}
              </Txt>
            </View>
          </Row>
          <View style={{ height: 14 }} />
          <Row style={{ gap: 26 }}>
            <ProfileStat value={`${Repo.workouts.length}`} label={s("Workouts", "Entraînements")} />
            <ProfileStat value="0" label={s("Followers", "Abonnés")} />
            <ProfileStat value="0" label={s("Following", "Abonnements")} />
          </Row>
        </View>
        <Pressable onPress={() => Nav.push({ t: "Settings" })} hitSlop={6} style={{ padding: 8 }}>
          <MIcon name="settings" size={24} color={C.Text} />
        </Pressable>
      </Row>
      {/* volume heatmap */}
      <Row style={{ paddingHorizontal: 16, paddingTop: 16, paddingBottom: 8 }}>
        <Txt weight={700} size={16} style={{ flex: 1 }}>
          {s("Volume", "Volume")}
        </Txt>
        <Pressable onPress={() => setHeatYear(heatYear > new Date().getFullYear() - 5 ? heatYear - 1 : new Date().getFullYear())} style={{ flexDirection: "row", alignItems: "center" }}>
          <Txt weight={600} size={14} color={accent}>
            {heatYear}
          </Txt>
          <Txt weight={600} size={12} color={accent}>
            {" ˅"}
          </Txt>
        </Pressable>
      </Row>
      <HeatmapYear dayVolumes={dayVolumes} year={heatYear} accent={accent} />
      <Txt size={13} color={C.Mut} style={{ paddingHorizontal: 16, paddingTop: 6 }}>
        {`${Calc.fmtVol(totalVol, unit)} kg  ·  ${totalSets} ${totalSets > 1 ? s("%1$d series", "séries") : s("%1$d series", "série")}  ·  ${totalReps} ${s("%1$d reps", "réps")}`}
      </Txt>
      {month.length > 0 ? (
        <Txt size={13} color={C.Mut} style={{ paddingHorizontal: 16, paddingTop: 10 }}>
          {`${s("This month", "Ce mois-ci")} : ${month.length} ${s("workouts", "séances")} · ${Calc.fmtVol(month.reduce((a, w) => a + Calc.vol(w), 0), unit)} kg · ${month.reduce((a, w) => a + w.prs.length, 0)} ${s("PRs", "records")}`}
        </Txt>
      ) : null}
      {last7.length > 0 || prev7.length > 0 ? (
        <Txt size={13} color={C.Mut} style={{ paddingHorizontal: 16, paddingTop: 10, lineHeight: 19 }}>
          {`${s("Last 7 days", "7 derniers jours")} : ${Calc.fmtVol(v1, unit)} kg · ${last7.length} ${s("workouts", "séances")}` +
            (delta != null ? `  (${delta >= 0 ? "+" : ""}${delta}% ${s("vs previous week", "vs semaine précédente")})` : "") +
            `\n${s("Previous 7 days", "7 jours précédents")} : ${Calc.fmtVol(v2, unit)} kg · ${prev7.length} ${s("workouts", "séances")}`}
        </Txt>
      ) : null}
      {/* monthly bars */}
      <View style={{ paddingHorizontal: 16, paddingVertical: 8 }}>
        <Row style={{ height: 90, gap: 10 }}>
          {monthly.map(([label, vol], i) => (
            <View key={i} style={{ flex: 1, alignItems: "center" }}>
              <View style={{ flex: 1, width: "100%", justifyContent: "flex-end" }}>
                <View style={{ width: "100%", height: (Math.min(1, vol / maxV) * 76), borderRadius: 6, backgroundColor: accent }} />
              </View>
              <View style={{ height: 4 }} />
              <Txt size={10} color={C.Mut}>
                {label.slice(0, 3)}
              </Txt>
            </View>
          ))}
        </Row>
      </View>
      {/* history link */}
      <View style={{ height: 14 }} />
      <AppCard>
        <Pressable onPress={() => Nav.push({ t: "History" })} style={{ flexDirection: "row", alignItems: "center", paddingHorizontal: 14, paddingVertical: 13 }}>
          <MIcon name="calendar-month" size={20} color={accent} />
          <View style={{ width: 12 }} />
          <Txt weight={500} size={15} style={{ flex: 1 }}>
            {s("Workout history", "Historique des séances")}
          </Txt>
          <MIcon name="chevron-right" size={16} color={C.Mut} />
        </Pressable>
      </AppCard>
      {/* muscle distribution */}
      <Txt weight={700} size={16} style={{ paddingHorizontal: 16, paddingTop: 18, paddingBottom: 6 }}>
        {s("Volume by muscle group", "Volume par groupe musculaire")}
      </Txt>
      {dist.length === 0 ? (
        <EmptyState text={s("No data yet.\nYour stats will appear after your first workout.", "Aucune donnée.\nTes stats apparaîtront après ta première séance.")} />
      ) : (
        dist.map(([muscle, vol]) => (
          <Row key={muscle} style={{ paddingHorizontal: 16, paddingVertical: 7 }}>
            <IllIcon muscle={muscle} size={34} />
            <View style={{ width: 12 }} />
            <View style={{ flex: 1 }}>
              <Row>
                <Txt size={13} weight={500} style={{ flex: 1 }}>
                  {muscleName(muscle)}
                </Txt>
                <Txt size={12} color={C.Mut}>
                  {Calc.fmtVol(vol, unit)} kg
                </Txt>
              </Row>
              <View style={{ height: 5 }} />
              <View style={{ height: 7, borderRadius: 99, backgroundColor: C.Card2, overflow: "hidden" }}>
                <View style={{ height: 7, width: `${Math.min(1, Math.max(0.02, vol / maxVol)) * 100}%`, borderRadius: 99, backgroundColor: accent }} />
              </View>
            </View>
          </Row>
        ))
      )}
      <Txt size={11.5} color={C.Mut} style={{ textAlign: "center", paddingTop: 18 }}>
        {Cloud.session != null
          ? s("Synced to your account", "Synchronisé sur ton compte") + " · " + Cloud.session.email
          : s("Local app · your data stays on this device.", "Application locale · tes données restent sur cet appareil.")}
      </Txt>
    </ScrollView>
  );
}

function ProfileStat({ value, label }: { value: string; label: string }) {
  return (
    <View>
      <Txt size={17} weight={800}>
        {value}
      </Txt>
      <Txt size={11.5} color={C.Mut}>
        {label}
      </Txt>
    </View>
  );
}

/** GitHub-style year heatmap of daily volume, blue intensity steps. */
function HeatmapYear({ dayVolumes, year, accent }: { dayVolumes: Map<string, number>; year: number; accent: string }) {
  const [w, setW] = useState(0);
  const days = useMemo(() => {
    const out: { key: string; dow: number; week: number; future: boolean; v: number }[] = [];
    const start = new Date(year, 0, 1);
    const lead = (start.getDay() === 0 ? 7 : start.getDay()) - 1;
    const today = new Date();
    for (let d = 0; d < 365; d++) {
      const date = new Date(year, 0, 1 + d);
      if (date.getFullYear() !== year) break;
      out.push({
        key: `${year}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`,
        dow: (d + lead) % 7,
        week: Math.floor((d + lead) / 7),
        future: date > today,
        v: dayVolumes.get(`${year}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`) ?? 0,
      });
    }
    return out;
  }, [year, dayVolumes]);
  const maxDay = Math.max(1, ...dayVolumes.values());
  if (w === 0) return <View style={{ marginHorizontal: 16 }} onLayout={(e) => setW(e.nativeEvent.layout.width)} />;
  const weeks = Math.max(1, Math.ceil(days.length / 7));
  const gap = 2;
  const cell = Math.min((w - gap * (weeks - 1)) / weeks, 11);
  return (
    <View style={{ marginHorizontal: 16 }}>
      <Svg width={w} height={cell * 7 + gap * 6}>
        {days.map((d) => (
          <Rect
            key={d.key}
            x={d.week * (cell + gap)}
            y={d.dow * (cell + gap)}
            width={cell}
            height={cell}
            rx={cell * 0.22}
            fill={d.future ? C.Card2 : d.v <= 0 ? C.Card2 : accent}
            opacity={d.future ? 0.35 : d.v <= 0 ? 1 : 0.3 + 0.7 * Math.min(1, d.v / maxDay)}
          />
        ))}
      </Svg>
      <View style={{ height: 4 }} />
      <Row style={{ justifyContent: "space-between" }}>
        <Txt size={10} color={C.Mut}>
          {Calc.monthShort(new Date(year, 0, 1).getTime())}
        </Txt>
        <Txt size={10} color={C.Mut}>
          {Calc.monthShort(new Date(year, 11, 31).getTime())}
        </Txt>
      </Row>
    </View>
  );
}
