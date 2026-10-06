/** Locally cached profile photo — port of AvatarCache (new expo-file-system API on device, data URL on web). */
import { Platform } from "react-native";
import { File, Paths } from "expo-file-system";
import { makeRev } from "./store";

const isWeb = Platform.OS === "web";
const KEY = "liftbook.avatar";

class AvatarImpl {
  rev = makeRev();

  private file(): File | null {
    if (isWeb) return null;
    try {
      return new File(Paths.document, "avatar.jpg");
    } catch {
      return null;
    }
  }

  localUri(): string | null {
    if (isWeb) return localStorage.getItem(KEY);
    const f = this.file();
    return f && f.exists ? f.uri : null;
  }

  async writeBase64(base64: string): Promise<string> {
    if (isWeb) {
      localStorage.setItem(KEY, `data:image/jpeg;base64,${base64}`);
      this.rev.bump();
      return "";
    }
    const f = this.file();
    if (!f) return "";
    f.write(base64, { encoding: "base64" });
    this.rev.bump();
    return f.uri;
  }

  async download(url: string) {
    if (isWeb) return;
    const f = this.file();
    if (!f) return;
    try {
      const task = File.createDownloadTask(url, f, { idempotent: true } as any);
      await task.downloadAsync();
      this.rev.bump();
    } catch {}
  }

  async remove() {
    if (isWeb) localStorage.removeItem(KEY);
    else {
      const f = this.file();
      try {
        f?.delete();
      } catch {}
    }
    this.rev.bump();
  }

  bump() {
    this.rev.bump();
  }
}

export const avatar = new AvatarImpl();
