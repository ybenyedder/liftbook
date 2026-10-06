/** Icon wrapper — Material Icons via @expo/vector-icons (same glyphs as the Android app). */
import { MaterialIcons } from "@expo/vector-icons";
import { C } from "../theme";

export type IconName = React.ComponentProps<typeof MaterialIcons>["name"];

export function MIcon({ name, size = 22, color = C.Text }: { name: IconName; size?: number; color?: string }) {
  return <MaterialIcons name={name} size={size} color={color} />;
}
