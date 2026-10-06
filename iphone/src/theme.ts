/** Palette sampled from the user's Hevy screenshots (dark, blue accent) — port of Theme.kt. */

export const C = {
  Bg: "#000000",
  NavBar: "#121214",
  Card: "#1C1C1E",
  Card2: "#2A2A2D",
  Line: "#2C2C2F",
  Line2: "#3A3A3E",
  Text: "#F4F8F8",
  Mut: "#8D9399",
  Mut2: "#6B7076",
  Accent: "#028CFD",
  AccPress: "#0279DB",
  AccText: "#FFFFFF",
  Red: "#E5484D",
  Gold: "#F5C518",
  Orange: "#FFA03C",
  Green: "#34C759",
  GreenBg: "#1F3B2C",
};

export function accentColor(name: string): string {
  switch (name) {
    case "teal":
      return "#20B49A";
    case "violet":
      return "#7C5CFF";
    case "orange":
      return "#FF7A45";
    default:
      return "#028CFD";
  }
}

// Inter static weights (@expo-google-fonts/inter) — fontFamily per fontWeight.
export const FONT = {
  400: "Inter_400Regular",
  500: "Inter_500Medium",
  600: "Inter_600SemiBold",
  700: "Inter_700Bold",
  800: "Inter_800ExtraBold",
};

export type Weight = 400 | 500 | 600 | 700 | 800;
