/** Shared UI components — port of Comps.kt + dialogs/menus/sheets in Hevy dark style. */
import React, { useEffect, useRef, useState } from "react";
import {
  ActivityIndicator,
  Animated,
  Dimensions,
  Easing,
  Image,
  Modal,
  PanResponder,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text as RNText,
  TextInput,
  TouchableWithoutFeedback,
  View,
} from "react-native";
import Svg, { Circle, ClipPath, Defs, G, LinearGradient, Path, Polygon, Rect, Stop } from "react-native-svg";
import { C, FONT, Weight, accentColor } from "../theme";
import { Repo } from "../repo";
import { avatar } from "../avatar";
import { useStore } from "../store";
import { s } from "../l10n";
import { MIcon, IconName } from "./icons";
import { ILL } from "./figures_gen";

// ---------------- text ----------------

export function Txt({
  weight = 400,
  size = 14,
  color = C.Text,
  style,
  children,
  ...rest
}: {
  weight?: Weight;
  size?: number;
  color?: string;
  style?: any;
  children?: React.ReactNode;
} & Omit<React.ComponentProps<typeof RNText>, "style" | "children">) {
  return (
    <RNText {...rest} style={[{ fontFamily: FONT[weight], fontSize: size, color }, style]}>
      {children}
    </RNText>
  );
}

// ---------------- primitives ----------------

export function Row({ style, children, ...rest }: { style?: any; children?: React.ReactNode } & Omit<React.ComponentProps<typeof View>, "style" | "children">) {
  return <View {...rest} style={[{ flexDirection: "row", alignItems: "center" }, style]}>{children}</View>;
}

export function Col({ style, children, ...rest }: { style?: any; children?: React.ReactNode } & Omit<React.ComponentProps<typeof View>, "style" | "children">) {
  return <View {...rest} style={[{ flexDirection: "column" }, style]}>{children}</View>;
}

export function SectionLabel({ text }: { text: string }) {
  return (
    <Txt weight={800} size={11.5} color={C.Mut} style={styles.sectionLabel}>
      {text.toUpperCase()}
    </Txt>
  );
}

export function AppCard({ children, style }: { children?: React.ReactNode; style?: any }) {
  return <View style={[styles.appCard, style]}>{children}</View>;
}

export function Chip({ label, selected, onClick }: { label: string; selected: boolean; onClick: () => void }) {
  const accent = accentColor(Repo.settings.accent);
  return (
    <Pressable onPress={onClick} hitSlop={4} style={[styles.chip, { backgroundColor: selected ? accent : C.Card, borderColor: selected ? accent : C.Line }]}>
      <Txt weight={700} size={12.5} color={selected ? C.AccText : C.Mut}>
        {label}
      </Txt>
    </Pressable>
  );
}

export function PrimaryButton({
  text,
  onClick,
  leading,
  style,
  textStyle,
}: {
  text: string;
  onClick: () => void;
  leading?: React.ReactNode;
  style?: any;
  textStyle?: any;
}) {
  const accent = accentColor(Repo.settings.accent);
  return (
    <Pressable onPress={onClick} style={[{ height: 50, borderRadius: 999, backgroundColor: accent, justifyContent: "center", alignItems: "center", flexDirection: "row" }, style]}>
      {leading}
      <Txt weight={700} size={15} color={C.AccText} style={textStyle}>
        {text}
      </Txt>
    </Pressable>
  );
}

export function GhostButton({ text, onClick, leading, style }: { text: string; onClick: () => void; leading?: React.ReactNode; style?: any }) {
  return (
    <Pressable onPress={onClick} style={[{ height: 44, borderRadius: 12, backgroundColor: C.Card, borderWidth: 1, borderColor: C.Line, justifyContent: "center", alignItems: "center", flexDirection: "row" }, style]}>
      {leading}
      <Txt weight={700} size={13.5}>
        {text}
      </Txt>
    </Pressable>
  );
}

export function Avatar({ letter, size }: { letter: string; size: number }) {
  const accent = accentColor(Repo.settings.accent);
  return (
    <View style={{ width: size, height: size, borderRadius: size / 2, backgroundColor: accent, justifyContent: "center", alignItems: "center" }}>
      <Txt weight={800} size={Math.round(size * 0.42)} color={C.AccText}>
        {letter}
      </Txt>
    </View>
  );
}

/** Profile photo when set, letter avatar otherwise. */
export function AvatarImg({ letter, size }: { letter: string; size: number }) {
  useStore(Repo.rev);
  useStore(avatar.rev);
  const uri = avatar.localUri();
  if (uri) {
    return <Image source={{ uri }} style={{ width: size, height: size, borderRadius: size / 2, backgroundColor: C.Card }} />;
  }
  return <Avatar letter={letter} size={size} />;
}

export function MuscleTag({ text }: { text: string }) {
  return (
    <View style={styles.muscleTag}>
      <Txt weight={700} size={10.5} color={C.Mut} numberOfLines={1}>
        {text}
      </Txt>
    </View>
  );
}

export function EmptyState({ text, slim = false }: { text: string; slim?: boolean }) {
  return (
    <View style={{ alignItems: "center" }}>
      <Txt size={13.5} color={C.Mut} style={{ textAlign: "center", lineHeight: 20, paddingHorizontal: 20, paddingVertical: slim ? 8 : 30 }}>
        {text}
      </Txt>
    </View>
  );
}

// ---------------- muscle figures (extracted 1:1 from the Android vector drawables) ----------------

export function FigureSvg({ muscle, dark, size }: { muscle: string; dark?: boolean; size: number }) {
  const entry = ILL[muscle] ?? ILL.Chest;
  const fig = dark ? entry.dark : entry.light;
  const h = (size * fig.vh) / fig.vw;
  return (
    <Svg width={size} height={h} viewBox={`0 0 ${fig.vw} ${fig.vh}`}>
      {fig.paths.map(([fill, d], i) => (
        <Path key={i} fill={fill} d={d} />
      ))}
    </Svg>
  );
}

export function ExCircle({ muscle, size }: { muscle: string; size: number }) {
  return (
    <View style={{ width: size, height: size, borderRadius: size / 2, backgroundColor: "#FFFFFF", justifyContent: "center", alignItems: "center", overflow: "hidden" }}>
      <FigureSvg muscle={muscle} dark size={size * 0.62} />
    </View>
  );
}

export function IllIcon({ muscle, size }: { muscle: string; size: number }) {
  return <FigureSvg muscle={muscle} size={size} />;
}

/** Hevy-style mini body pictogram (front/back) with the worked muscles highlighted in accent. */
const BODY_SPOTS: Record<string, [boolean, number, number][]> = {
  Chest: [[true, 0.37, 0.28], [true, 0.63, 0.28]],
  Shoulders: [[true, 0.3, 0.21], [true, 0.7, 0.21]],
  Biceps: [[true, 0.23, 0.32], [true, 0.77, 0.32]],
  Forearms: [[true, 0.17, 0.4], [true, 0.83, 0.4]],
  Abs: [[true, 0.5, 0.38]],
  Quads: [[true, 0.42, 0.6], [true, 0.58, 0.6]],
  Adductors: [[true, 0.46, 0.52], [true, 0.54, 0.52]],
  Calves: [[true, 0.41, 0.82], [true, 0.59, 0.82], [false, 0.41, 0.82], [false, 0.59, 0.82]],
  Traps: [[true, 0.5, 0.17], [false, 0.5, 0.17]],
  Lats: [[false, 0.37, 0.3], [false, 0.63, 0.3]],
  Triceps: [[false, 0.23, 0.32], [false, 0.77, 0.32]],
  "Lower back": [[false, 0.5, 0.38]],
  Glutes: [[false, 0.43, 0.47], [false, 0.57, 0.47]],
  Hamstrings: [[false, 0.42, 0.6], [false, 0.58, 0.6]],
  Abductors: [[false, 0.36, 0.52], [false, 0.64, 0.52]],
};

export function BodyMap({ front, muscles }: { front: boolean; muscles: Set<string> }) {
  const accent = accentColor(Repo.settings.accent);
  const w = 34;
  const h = 56;
  const body = "#4A4F56";
  const Line = ({ x1, y1, x2, y2, t }: { x1: number; y1: number; x2: number; y2: number; t: number }) => (
    <Path d={`M ${x1} ${y1} L ${x2} ${y2}`} stroke={body} strokeWidth={w * t} strokeLinecap="round" fill="none" />
  );
  return (
    <View style={{ width: w, height: h, borderRadius: 9, overflow: "hidden" }}>
      <Svg width={w} height={h} viewBox={`0 0 ${w} ${h}`}>
        <Circle cx={w * 0.5} cy={h * 0.085} r={w * 0.115} fill={body} />
        <Line x1={w * 0.5} y1={h * 0.14} x2={w * 0.5} y2={h * 0.185} t={0.11} />
        <Polygon
          points={`${w * 0.36},${h * 0.19} ${w * 0.64},${h * 0.19} ${w * 0.585},${h * 0.475} ${w * 0.415},${h * 0.475}`}
          fill={body}
        />
        <Rect x={w * 0.415} y={h * 0.475} width={w * 0.17} height={h * 0.075} rx={w * 0.09} fill={body} />
        <Line x1={w * 0.345} y1={h * 0.205} x2={w * 0.245} y2={h * 0.31} t={0.105} />
        <Line x1={w * 0.245} y1={h * 0.31} x2={w * 0.275} y2={h * 0.435} t={0.09} />
        <Line x1={w * 0.655} y1={h * 0.205} x2={w * 0.755} y2={h * 0.31} t={0.105} />
        <Line x1={w * 0.755} y1={h * 0.31} x2={w * 0.725} y2={h * 0.435} t={0.09} />
        <Line x1={w * 0.455} y1={h * 0.545} x2={w * 0.43} y2={h * 0.75} t={0.125} />
        <Line x1={w * 0.43} y1={h * 0.75} x2={w * 0.425} y2={h * 0.925} t={0.1} />
        <Line x1={w * 0.545} y1={h * 0.545} x2={w * 0.57} y2={h * 0.75} t={0.125} />
        <Line x1={w * 0.57} y1={h * 0.75} x2={w * 0.575} y2={h * 0.925} t={0.1} />
        {[...muscles].map((m) =>
          (BODY_SPOTS[m] ?? []).map(([f, x, y], i) => (f === front ? <Circle key={`${m}-${i}`} cx={w * x} cy={h * y} r={w * 0.085} fill={accent} /> : null)),
        )}
      </Svg>
    </View>
  );
}

// ---------------- smooth line chart with gradient fill ----------------

const AnimatedRect = Animated.createAnimatedComponent(Rect);

export function LineChart({ points, fmtLabel }: { points: [string, number][]; fmtLabel?: (v: number) => string }) {
  const accent = accentColor(Repo.settings.accent);
  const [w, setW] = useState(0);
  const progress = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    progress.setValue(0);
    Animated.timing(progress, { toValue: 1, duration: 650, easing: Easing.inOut(Easing.ease), useNativeDriver: false }).start();
  }, [points]);
  const H = 150;
  if (points.length < 2) return <View style={{ height: H }} />;
  const padL = 6;
  const padR = 54;
  const padT = 16;
  const padB = 20;
  const iw = Math.max(10, w - padL - padR);
  const ih = H - padT - padB;
  let min = Math.min(...points.map((p) => p[1]));
  let max = Math.max(...points.map((p) => p[1]));
  if (max - min < max * 0.06 + 1) {
    const mid = (max + min) / 2;
    min = mid * 0.97;
    max = mid * 1.03;
  }
  const pad = (max - min) * 0.1;
  min -= pad;
  max += pad;
  const x = (i: number) => padL + (i / (points.length - 1)) * iw;
  const y = (v: number) => padT + ih - ((v - min) / (max - min)) * ih;
  let d = `M ${x(0)} ${y(points[0][1])}`;
  for (let i = 1; i < points.length; i++) {
    const x0 = x(i - 1);
    const y0 = y(points[i - 1][1]);
    const x1 = x(i);
    const y1 = y(points[i][1]);
    d += ` Q ${x0} ${y0} ${(x0 + x1) / 2} ${(y0 + y1) / 2}`;
  }
  d += ` L ${x(points.length - 1)} ${y(points[points.length - 1][1])}`;
  const fillD = d + ` L ${x(points.length - 1)} ${padT + ih} L ${x(0)} ${padT + ih} Z`;
  const lastLabel = fmtLabel ? fmtLabel(points[points.length - 1][1]) : null;
  return (
    <View style={{ height: H, marginTop: 4 }} onLayout={(e) => setW(e.nativeEvent.layout.width)}>
      {w > 0 && (
        <Svg width={w} height={H}>
          <Defs>
            <LinearGradient id="lcg" x1="0" y1={padT} x2="0" y2={padT + ih}>
              <Stop offset="0" stopColor={accent} stopOpacity={0.35} />
              <Stop offset="1" stopColor={accent} stopOpacity={0} />
            </LinearGradient>
            <ClipPath id="lcc">
              <AnimatedRect width={progress.interpolate({ inputRange: [0, 1], outputRange: [0, w] })} height={H} />
            </ClipPath>
          </Defs>
          {[0, 1, 2].map((g) => (
            <Path key={g} d={`M ${padL} ${padT + (ih * g) / 2} L ${padL + iw} ${padT + (ih * g) / 2}`} stroke={C.Mut} strokeOpacity={0.15} strokeWidth={1} />
          ))}
          <G clipPath="url(#lcc)">
            <Path d={fillD} fill="url(#lcg)" />
            <Path d={d} stroke={accent} strokeWidth={2.5} strokeLinecap="round" fill="none" />
            <Circle cx={x(points.length - 1)} cy={y(points[points.length - 1][1])} r={6} fill={accent} />
            <Circle cx={x(points.length - 1)} cy={y(points[points.length - 1][1])} r={3.5} fill={C.Bg} />
          </G>
        </Svg>
      )}
      {lastLabel != null && (
        <Txt size={10} color={C.Mut} style={{ position: "absolute", right: 4, top: 2 }}>
          {lastLabel}
        </Txt>
      )}
    </View>
  );
}

// ---------------- menu (anchored dropdown) ----------------

export interface MenuItem {
  label: string;
  icon?: IconName;
  red?: boolean;
  onPress: () => void;
}

export function Menu({ visible, onClose, items, anchorRef }: { visible: boolean; onClose: () => void; items: MenuItem[]; anchorRef: React.RefObject<any> }) {
  const [pos, setPos] = useState<{ x: number; y: number } | null>(null);
  useEffect(() => {
    if (!visible) {
      setPos(null);
      return;
    }
    const node = anchorRef?.current;
    if (!node) return;
    const win = Dimensions.get("window");
    const place = (x: number, y: number) => setPos({ x: Math.max(8, Math.min(x, win.width - 216 - 8)), y: Math.min(y, win.height - items.length * 50 - 60) });
    if (Platform.OS === "web") {
      const r = node.getBoundingClientRect?.();
      if (r) place(r.left, r.bottom + 4);
    } else {
      try {
        node.measureInWindow((x: number, y: number, w: number, h: number) => place(x, y + h + 4));
      } catch {}
    }
  }, [visible]);
  if (!visible || !pos) return null;
  return (
    <Modal transparent visible onRequestClose={onClose} animationType="fade">
      <TouchableWithoutFeedback onPress={onClose}>
        <View style={{ flex: 1, backgroundColor: "transparent" }}>
          <View style={{ position: "absolute", left: pos.x, top: pos.y, minWidth: 200, backgroundColor: C.Card, borderRadius: 10, paddingVertical: 4, shadowColor: "#000", shadowOpacity: 0.5, shadowRadius: 12, shadowOffset: { width: 0, height: 4 }, elevation: 6 }}>
            {items.map((it, i) => (
              <Pressable
                key={i}
                onPress={() => {
                  onClose();
                  it.onPress();
                }}
                style={{ flexDirection: "row", alignItems: "center", minHeight: 48, paddingHorizontal: 14 }}
              >
                {it.icon ? <MIcon name={it.icon} size={16} color={it.red ? C.Red : C.Text} /> : null}
                {it.icon ? <View style={{ width: 10 }} /> : null}
                <Txt size={15} weight={500} color={it.red ? C.Red : C.Text}>
                  {it.label}
                </Txt>
              </Pressable>
            ))}
          </View>
        </View>
      </TouchableWithoutFeedback>
    </Modal>
  );
}

// ---------------- alert dialog (m3 dark) ----------------

export function Alert({
  visible,
  title,
  message,
  confirmLabel,
  dismissLabel,
  confirmColor,
  dismissColor,
  onConfirm,
  onDismiss,
  onDismissPress,
  customBody,
}: {
  visible: boolean;
  title: string;
  message?: string;
  confirmLabel: string;
  dismissLabel?: string;
  confirmColor?: string;
  dismissColor?: string;
  onConfirm: () => void;
  onDismiss?: () => void;
  onDismissPress?: () => void;
  customBody?: React.ReactNode;
}) {
  if (!visible) return null;
  return (
    <Modal transparent visible onRequestClose={onDismiss ?? onConfirm} animationType="fade">
      <TouchableWithoutFeedback onPress={onDismiss}>
        <View style={styles.dialogScrim}>
          <TouchableWithoutFeedback>
            <View style={styles.dialogCard}>
              <Txt weight={800} size={20} style={{ paddingBottom: message || customBody ? 14 : 16 }}>
                {title}
              </Txt>
              {message ? (
                <Txt size={14.5} color={C.Text} style={{ lineHeight: 20, paddingBottom: 18 }}>
                  {message}
                </Txt>
              ) : null}
              {customBody ? <View style={{ paddingBottom: 14 }}>{customBody}</View> : null}
              <View style={{ flexDirection: "row", justifyContent: "flex-end", alignItems: "center" }}>
                {dismissLabel ? (
                  <Pressable onPress={onDismissPress ?? onDismiss} style={{ paddingHorizontal: 14, paddingVertical: 8 }}>
                    <Txt weight={600} size={14} color={dismissColor ?? C.Mut}>
                      {dismissLabel}
                    </Txt>
                  </Pressable>
                ) : null}
                <Pressable onPress={onConfirm} style={{ paddingHorizontal: 14, paddingVertical: 8 }}>
                  <Txt weight={600} size={14} color={confirmColor ?? accentColor(Repo.settings.accent)}>
                    {confirmLabel}
                  </Txt>
                </Pressable>
              </View>
            </View>
          </TouchableWithoutFeedback>
        </View>
      </TouchableWithoutFeedback>
    </Modal>
  );
}

/** Text prompt dialog (rename flows). */
export function PromptDialog({
  visible,
  title,
  initial,
  confirmLabel,
  onConfirm,
  onDismiss,
  maxLength = 40,
  prefix,
  filter,
}: {
  visible: boolean;
  title: string;
  initial: string;
  confirmLabel: string;
  onConfirm: (v: string) => void;
  onDismiss: () => void;
  maxLength?: number;
  prefix?: string;
  filter?: (c: string) => boolean;
}) {
  const [v, setV] = useState(initial);
  useEffect(() => {
    if (visible) setV(initial);
  }, [visible]);
  if (!visible) return null;
  return (
    <Modal transparent visible onRequestClose={onDismiss} animationType="fade">
      <View style={styles.dialogScrim}>
        <View style={styles.dialogCard}>
          <Txt weight={700} size={18} style={{ paddingBottom: 14 }}>
            {title}
          </Txt>
          <View style={{ backgroundColor: C.Bg, borderRadius: 10, borderWidth: 1, borderColor: C.Line2, marginBottom: 12 }}>
            <Row style={{ paddingHorizontal: 12 }}>
              {prefix ? <Txt size={16}>{prefix}</Txt> : null}
              <TextInput
                value={v}
                autoFocus
                onChangeText={(t) => setV(filter ? [...t].filter(filter).join("").slice(0, maxLength) : t.slice(0, maxLength))}
                style={{ flex: 1, color: C.Text, fontFamily: FONT[400], fontSize: 16, paddingVertical: 12 }}
                onSubmitEditing={() => onConfirm(v)}
              />
            </Row>
          </View>
          <View style={{ flexDirection: "row", justifyContent: "flex-end" }}>
            <Pressable onPress={onDismiss} style={{ paddingHorizontal: 14, paddingVertical: 8 }}>
              <Txt weight={600} size={14} color={C.Mut}>{s("Cancel", "Annuler")}</Txt>
            </Pressable>
            <Pressable onPress={() => onConfirm(v)} style={{ paddingHorizontal: 14, paddingVertical: 8 }}>
              <Txt weight={600} size={14} color={accentColor(Repo.settings.accent)}>{confirmLabel}</Txt>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}

// ---------------- bottom sheet ----------------

export function Sheet({ visible, onClose, children }: { visible: boolean; onClose: () => void; children: React.ReactNode }) {
  const [shown, setShown] = useState(false);
  const anim = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    if (visible) {
      setShown(true);
      Animated.timing(anim, { toValue: 1, duration: 240, useNativeDriver: true, easing: Easing.out(Easing.cubic) }).start();
    } else if (shown) {
      Animated.timing(anim, { toValue: 0, duration: 180, useNativeDriver: true }).start(() => setShown(false));
    }
  }, [visible]);
  if (!shown) return null;
  const ty = anim.interpolate({ inputRange: [0, 1], outputRange: [600, 0] });
  return (
    <Modal transparent visible onRequestClose={onClose}>
      <View style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.55)", justifyContent: "flex-end" }}>
        <TouchableWithoutFeedback onPress={onClose}>
          <View style={{ flex: 1 }} />
        </TouchableWithoutFeedback>
        <Animated.View style={{ backgroundColor: C.Card, borderTopLeftRadius: 20, borderTopRightRadius: 20, transform: [{ translateY: ty }], maxHeight: "88%" }}>
          <View style={{ alignSelf: "center", width: 36, height: 4, borderRadius: 2, backgroundColor: C.Line2, marginTop: 8 }} />
          {children}
        </Animated.View>
      </View>
    </Modal>
  );
}

// ---------------- text field (filled, Hevy style) ----------------

export function TF({
  value,
  onChange,
  placeholder,
  leadingIcon,
  height = 52,
  multiline = false,
  password = false,
  showPw,
  onTogglePw,
  onSubmit,
  autoFocus,
  align = "left",
  keyboardType = "default",
  returnKey = "done",
  weight = 600,
  size = 16,
  style,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  leadingIcon?: IconName;
  height?: number;
  multiline?: boolean;
  password?: boolean;
  showPw?: boolean;
  onTogglePw?: () => void;
  onSubmit?: () => void;
  autoFocus?: boolean;
  align?: "left" | "center";
  keyboardType?: any;
  returnKey?: any;
  weight?: Weight;
  size?: number;
  style?: any;
}) {
  return (
    <View style={[{ backgroundColor: C.Card2, borderRadius: 12, flexDirection: "row", alignItems: "center", height, paddingHorizontal: 12 }, style]}>
      {leadingIcon ? <MIcon name={leadingIcon} size={17} color={C.Mut} /> : null}
      {leadingIcon ? <View style={{ width: 8 }} /> : null}
      <TextInput
        value={value}
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor={C.Mut}
        secureTextEntry={password && !showPw}
        multiline={multiline}
        autoFocus={autoFocus}
        keyboardType={keyboardType}
        returnKeyType={returnKey}
        onSubmitEditing={onSubmit}
        textAlignVertical={multiline ? "top" : "center"}
        style={{ flex: 1, color: C.Text, fontFamily: FONT[weight], fontSize: size, textAlign: align as any }}
      />
      {onTogglePw ? (
        <Pressable onPress={onTogglePw} hitSlop={8}>
          <MIcon name={showPw ? "visibility-off" : "visibility"} size={20} color={C.Mut} />
        </Pressable>
      ) : null}
    </View>
  );
}

/** Editor's inline title field (big text over a hairline, Hevy routine editor). */
export function BigField({ value, onChange, placeholder }: { value: string; onChange: (v: string) => void; placeholder: string }) {
  return (
    <View>
      <View style={{ height: 38, justifyContent: "center" }}>
        {value === "" ? <Txt size={22} weight={700} color={C.Mut}>{placeholder}</Txt> : null}
        <TextInput value={value} onChangeText={onChange} style={{ color: C.Text, fontFamily: FONT[700], fontSize: 24, padding: 0 }} />
      </View>
      <View style={{ height: 1, backgroundColor: C.Line }} />
    </View>
  );
}

// ---------------- workout-in-progress guard ----------------

export function WorkoutInProgressDialog({ visible, onDismiss, onResume, onRestart }: { visible: boolean; onDismiss: () => void; onResume: () => void; onRestart: () => void }) {
  return (
    <Alert
      visible={visible}
      title={s("Workout in progress", "Une séance est déjà en cours")}
      message={s("Resume it, or discard it and start a new one.", "Reprends-la, ou supprime-la pour en commencer une nouvelle.")}
      confirmLabel={s("Resume", "Reprendre")}
      dismissLabel={s("Discard & restart", "Supprimer et recommencer")}
      confirmColor={accentColor(Repo.settings.accent)}
      dismissColor={C.Red}
      onDismiss={onDismiss}
      onConfirm={onResume}
      onDismissPress={onRestart}
    />
  );
}

// ---------------- toast ----------------

let toastListeners = new Set<() => void>();
let toastMsg: string | null = null;
let toastTimer: ReturnType<typeof setTimeout> | null = null;

export function toast(msg: string) {
  toastMsg = msg;
  toastListeners.forEach((l) => l());
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastMsg = null;
    toastListeners.forEach((l) => l());
  }, 2600);
}

export function ToastHost() {
  const [, force] = useState(0);
  useEffect(() => {
    const fn = () => force((x) => x + 1);
    toastListeners.add(fn);
    return () => {
      toastListeners.delete(fn);
    };
  }, []);
  if (!toastMsg) return null;
  return (
    <View pointerEvents="none" style={{ position: "absolute", bottom: 90, left: 0, right: 0, alignItems: "center" }}>
      <View style={{ backgroundColor: C.Card2, borderRadius: 999, paddingHorizontal: 18, paddingVertical: 10, maxWidth: "86%" }}>
        <Txt size={13.5} color={C.Text}>
          {toastMsg}
        </Txt>
      </View>
    </View>
  );
}

// ---------------- rest label ----------------

export function fmtRestLabel(sec: number): string {
  return sec >= 60 ? `${Math.floor(sec / 60)}min ${sec % 60}s` : `${sec}s`;
}

// ---------------- drag-reorder list (long-press to lift, drag to swap) ----------------
// The item wrapper stays passive until the caller's Pressable reports a long-press
// (onGrab); from then on this ancestor captures the moves (capture phase wins over
// the child Pressable / ScrollView) and swaps items when midpoints are crossed —
// same algorithm as the Android DragDropState.

export function DragList({
  count,
  onMove,
  renderItem,
  scrollRef,
  scrollOffsetRef,
  enabled = true,
}: {
  count: number;
  onMove: (from: number, to: number) => void;
  renderItem: (info: { index: number; dragging: boolean; translateY: Animated.Value; onGrab: (pageY: number) => void }) => React.ReactNode;
  scrollRef?: React.RefObject<any>;
  scrollOffsetRef?: React.RefObject<number>;
  enabled?: boolean;
}) {
  const tops = useRef<number[]>([]);
  const heights = useRef<number[]>([]);
  const dragRef = useRef(-1);
  const [dragging, setDragging] = useState(-1);
  const dragY = useRef(new Animated.Value(0)).current;
  const grabOffset = useRef(0);
  const dyBase = useRef(0);
  const scrollLoop = useRef<ReturnType<typeof setInterval> | null>(null);
  const countRef = useRef(count);
  countRef.current = count;

  const stopScrollLoop = () => {
    if (scrollLoop.current) {
      clearInterval(scrollLoop.current);
      scrollLoop.current = null;
    }
  };

  const endDrag = () => {
    stopScrollLoop();
    dragRef.current = -1;
    setDragging(-1);
    dragY.setValue(0);
  };

  const pan = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_e, g) => dragRef.current >= 0 && Math.abs(g.dy) > 2,
      onPanResponderGrant: (_e, g) => {
        dyBase.current = g.dy;
      },
      onPanResponderMove: (_e, g) => {
        const cur = dragRef.current;
        if (cur < 0) return;
        const dy = g.dy - dyBase.current;
        dragY.setValue(dy);
        const h = heights.current[cur] || 70;
        const itemTop = tops.current[cur] + grabOffset.current + dy;
        const middle = itemTop + h / 2;
        let target = -1;
        for (let i = 0; i < countRef.current; i++) {
          if (i === cur) continue;
          const ih_ = heights.current[i] || 70;
          const mid = tops.current[i] + ih_ / 2;
          if (i < cur && middle < mid - 2) target = i;
          if (i > cur && middle > mid + 2) target = i;
        }
        if (target >= 0) {
          const th = heights.current[target] || 70;
          onMove(cur, target);
          grabOffset.current += target > cur ? -th : h;
          dragRef.current = target;
          dyBase.current = g.dy;
          dragY.setValue(0);
          setDragging(target);
        } else if (scrollRef?.current && scrollOffsetRef) {
          stopScrollLoop();
          const win = Dimensions.get("window").height;
          const dir = itemTop + h > win - 150 ? 1 : itemTop < 170 ? -1 : 0;
          if (dir !== 0) {
            scrollLoop.current = setInterval(() => {
              const y = (scrollOffsetRef.current ?? 0) + 12 * dir;
              scrollRef.current?.scrollTo({ y: Math.max(0, y), animated: false });
            }, 16);
          }
        }
      },
      onPanResponderRelease: endDrag,
      onPanResponderTerminate: endDrag,
    }),
  ).current;

  useEffect(() => stopScrollLoop, []);

  const grab = (i: number) => (pageY: number) => {
    if (!enabled || dragRef.current >= 0) return;
    dragRef.current = i;
    grabOffset.current = pageY - (tops.current[i] ?? 0) - (scrollOffsetRef?.current ?? 0) - (heights.current[i] || 70) / 2;
    setDragging(i);
    dragY.setValue(0);
  };

  return (
    <View {...pan.panHandlers}>
      {Array.from({ length: count }, (_, i) =>
        renderItem({
          index: i,
          dragging: dragging === i,
          translateY: dragY,
          onGrab: grab(i),
        }),
      )}
    </View>
  );
}

/** Convenience: attaches long-press grab to a card (Hevy lift behaviour). */
export function DragHandle({ onGrab, dragging, children, style }: { onGrab: (pageY: number) => void; dragging: boolean; children: React.ReactNode; style?: any }) {
  const boxRef = useRef<View>(null);
  const scale = useRef(new Animated.Value(1)).current;
  useEffect(() => {
    Animated.spring(scale, { toValue: dragging ? 1.02 : 1, useNativeDriver: true, friction: 8 }).start();
  }, [dragging]);
  return (
    <Animated.View
      ref={boxRef as any}
      collapsable={false}
      style={[style, { transform: [{ scale }], opacity: dragging ? 0.94 : 1 }, dragging ? { shadowColor: "#000", shadowOpacity: 0.5, shadowRadius: 18, shadowOffset: { width: 0, height: 8 }, elevation: 20, zIndex: 10 } : null]}
    >
      <Pressable
        delayLongPress={400}
        onLongPress={() => {
          const node: any = boxRef.current;
          if (Platform.OS === "web") {
            const r = (node as any)?.getBoundingClientRect?.();
            if (r) onGrab(r.top + r.height / 2 + window.scrollY * 0);
          } else {
            (node as any)?.measureInWindow?.((_x: number, y: number, _w: number, h: number) => onGrab(y + h / 2));
          }
        }}
      >
        {children}
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  sectionLabel: { letterSpacing: 1.2, paddingHorizontal: 16, paddingTop: 18, paddingBottom: 8 },
  appCard: { borderRadius: 14, backgroundColor: C.Card, borderWidth: 1, borderColor: C.Line, marginHorizontal: 16, marginVertical: 5 },
  chip: { borderRadius: 999, borderWidth: 1, paddingHorizontal: 13, paddingVertical: 7 },
  muscleTag: { borderRadius: 6, backgroundColor: C.Card2, paddingHorizontal: 7, paddingVertical: 3, alignSelf: "flex-start" },
  dialogScrim: { flex: 1, backgroundColor: "rgba(0,0,0,0.62)", justifyContent: "center", alignItems: "center", padding: 40 },
  dialogCard: { backgroundColor: C.Card2, borderRadius: 22, padding: 22, minWidth: 280, maxWidth: 360, width: "100%" },
});

/** Activity indicator in the app accent. */
export function Spinner({ size = 22, color = C.Mut }: { size?: number; color?: string }) {
  return <ActivityIndicator size="small" color={color} style={{ width: size, height: size }} />;
}

/** Google "G" (official 4 colors) — no asset file needed. */
export function GoogleG({ size = 22 }: { size?: number }) {
  return (
    <Svg width={size} height={size} viewBox="0 0 48 48">
      <Path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
      <Path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
      <Path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
      <Path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
    </Svg>
  );
}
