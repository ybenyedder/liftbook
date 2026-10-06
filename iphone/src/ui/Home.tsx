/** Home feed — port of Home.kt (posts of past workouts, Hevy social layout). */
import React, { useMemo, useState } from "react";
import { FlatList, Pressable, View } from "react-native";
import * as Calc from "../calc";
import { Repo } from "../repo";
import { Nav } from "../nav";
import { useStore } from "../store";
import { C } from "../theme";
import { s, relativeTime, seriesLabel, seeMoreExercises, getLang } from "../l10n";
import { exName } from "../l10nshim";
import { AvatarImg, EmptyState, Row, Txt, toast } from "./comps";
import { MIcon } from "./icons";

interface PostUi {
  id: number;
  author: string;
  timeLabel: string;
  dayTitle: string;
  duration: string;
  volume: string;
  prCount: number;
  exerciseLines: string[];
  names: string[];
  setCounts: number[];
}

export default function HomeScreen() {
  useStore(Repo.rev);
  const unit = Repo.settings.unit;
  const posts = useMemo<PostUi[]>(
    () =>
      Repo.workoutsDesc().slice(0, 30).map((w) => ({
        id: w.id,
        author: Repo.settings.profileName,
        timeLabel: relativeTime(w.startedAt, Date.now()),
        dayTitle: Calc.dayName(w.startedAt, getLang() === "fr" ? "fr-FR" : "en-US"),
        duration: Calc.fmtDur(w.endedAt - w.startedAt),
        volume: `${Calc.fmtVol(Calc.vol(w), unit)} kg`,
        prCount: w.prs.length,
        exerciseLines: w.exercises.map((ex) => seriesLabel(ex.sets.length, exName(ex.name))),
        names: w.exercises.map((ex) => exName(ex.name)),
        setCounts: w.exercises.map((ex) => ex.sets.length),
      })),
    [Repo.rev.value, unit],
  );

  return (
    <FlatList
      style={{ flex: 1, backgroundColor: C.Bg }}
      data={posts}
      keyExtractor={(p) => String(p.id)}
      ListHeaderComponent={
        <Row style={{ paddingHorizontal: 16, paddingRight: 4, paddingTop: 14 }}>
          <Txt weight={800} size={27} style={{ flex: 1 }}>
            {s("Home", "Accueil")}
          </Txt>
          <Pressable onPress={() => Nav.push({ t: "Exercises" })} hitSlop={6} style={{ paddingHorizontal: 8 }}>
            <MIcon name="search" size={24} color={C.Text} />
          </Pressable>
          <Pressable onPress={() => toast(s("No new notifications", "Pas de nouvelles notifications"))} hitSlop={6} style={{ paddingHorizontal: 8 }}>
            <MIcon name="notifications" size={24} color={C.Text} />
          </Pressable>
        </Row>
      }
      ListEmptyComponent={<EmptyState text={s("No workouts yet.\nHead to Training to get started!", "Aucune séance pour le moment.\nVa dans Entraînement pour démarrer !")} />}
      ItemSeparatorComponent={() => <View style={{ height: 8, backgroundColor: C.Bg }} />}
      renderItem={({ item }) => <FeedPost p={item} />}
      ListFooterComponent={<View style={{ height: 20 }} />}
    />
  );
}

function FeedPost({ p }: { p: PostUi }) {
  const [expanded, setExpanded] = useState(false);
  const shown = expanded ? p.exerciseLines.length : Math.min(3, p.exerciseLines.length);
  const hidden = p.exerciseLines.length - shown;
  return (
    <Pressable
      onPress={() => Nav.push({ t: "WorkoutDetail", id: p.id })}
      style={{ paddingVertical: 10 }}
    >
      <Row style={{ paddingHorizontal: 16 }}>
        <AvatarImg letter={Repo.settings.profileName.trim().slice(0, 1).toUpperCase() || "A"} size={44} />
        <View style={{ width: 12 }} />
        <View style={{ flex: 1 }}>
          <Txt weight={700} size={16}>
            {p.author}
          </Txt>
          <Txt size={13} color={C.Mut}>
            {p.timeLabel}
          </Txt>
        </View>
        <Pressable onPress={() => Nav.push({ t: "WorkoutDetail", id: p.id })} hitSlop={6}>
          <MIcon name="more-horiz" size={18} color={C.Mut} />
        </Pressable>
      </Row>
      <Txt weight={800} size={21} style={{ paddingHorizontal: 16, paddingTop: 10, paddingBottom: 8 }}>
        {p.dayTitle}
      </Txt>
      <Row style={{ paddingHorizontal: 16 }}>
        <FeedStat label={s("Time", "Temps")} value={p.duration} flex={1.2} />
        <FeedStat label={s("Volume", "Volume")} value={p.volume} flex={1.2} />
        <FeedStat label={s("Records", "Records")} value="" flex={1} medal={p.prCount} />
      </Row>
      <View style={{ height: 1, backgroundColor: C.Line, marginHorizontal: 16, marginVertical: 12 }} />
      {p.names.slice(0, shown).map((n, i) => (
        <Row key={i} style={{ paddingHorizontal: 16, paddingVertical: 7 }}>
          <View style={{ borderRadius: 8, backgroundColor: C.Card2, paddingHorizontal: 9, paddingVertical: 5 }}>
            <Txt weight={700} size={13}>
              {p.setCounts[i]}
            </Txt>
          </View>
          <View style={{ width: 12 }} />
          <Txt weight={500} size={16} numberOfLines={2} style={{ flex: 1 }}>
            {n}
          </Txt>
        </Row>
      ))}
      {hidden > 0 ? (
        <Pressable onPress={() => setExpanded(true)}>
          <Txt size={14.5} color={C.Mut} style={{ textAlign: "center", paddingVertical: 8 }}>
            {seeMoreExercises(hidden)}
          </Txt>
        </Pressable>
      ) : null}
      <View style={{ height: 1, backgroundColor: C.Line, marginHorizontal: 16, marginVertical: 6 }} />
      <Row style={{ paddingHorizontal: 4 }}>
        <Pressable onPress={() => toast(s("Added to favorites", "Ajouté aux favoris"))} hitSlop={4} style={{ padding: 8 }}>
          <MIcon name="thumb-up" size={22} color={C.Mut} />
        </Pressable>
        <Pressable onPress={() => toast(s("Comments coming soon", "Les commentaires arrivent bientôt"))} hitSlop={4} style={{ padding: 8 }}>
          <MIcon name="chat-bubble-outline" size={22} color={C.Mut} />
        </Pressable>
        <Pressable onPress={() => toast(s("Shared!", "Partagé !"))} hitSlop={4} style={{ padding: 8 }}>
          <MIcon name="ios-share" size={22} color={C.Mut} />
        </Pressable>
      </Row>
    </Pressable>
  );
}

function FeedStat({ label, value, flex, medal = -1 }: { label: string; value: string; flex: number; medal?: number }) {
  return (
    <View style={{ flex }}>
      <Txt size={13} color={C.Mut}>
        {label}
      </Txt>
      <View style={{ height: 2 }} />
      {medal >= 0 ? (
        <Row>
          <Txt size={15}>🏅</Txt>
          <View style={{ width: 5 }} />
          <Txt weight={600} size={15}>
            {medal}
          </Txt>
        </Row>
      ) : (
        <Txt weight={600} size={15} numberOfLines={1}>
          {value}
        </Txt>
      )}
    </View>
  );
}
