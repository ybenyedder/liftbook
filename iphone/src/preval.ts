/** PR evaluation at check-time + the one-shot "record battu" badge — port of evaluatePr / PrBadge (Logger.kt). */
import * as Calc from "./calc";
import { Repo } from "./repo";
import { ExEntry, SetEntry } from "./models";
import { makeRev } from "./store";
import { s } from "./l10n";

export interface Badge {
  ex: string;
  muscle: string;
  kind: string;
  value: string;
}

class PrBadgeImpl {
  current: Badge | null = null;
  rev = makeRev();
  show(ex: string, muscle: string, kind: string, value: string) {
    this.current = { ex, muscle, kind, value };
    this.rev.bump();
  }
  clear() {
    this.current = null;
    this.rev.bump();
  }
}

export const PrBadge = new PrBadgeImpl();

/** PR evaluation at check-time, against history strictly before this workout.
 *  Announces the badge once per new record; flags the set for the green row + recap. */
export function evaluatePr(ex: ExEntry, st: SetEntry) {
  const kg = st.kg;
  const reps = st.reps;
  if (kg == null || reps == null) return;
  if (kg <= 0 || reps <= 0) return;
  const pr = Repo.prFor(ex.name);
  const bestW = pr?.weight ?? 0;
  const bestE = pr?.e1rm ?? 0;
  const e = Calc.e1rm(kg, reps);
  if (kg > bestW && !st.prW) {
    st.prW = true;
    PrBadge.show(
      ex.name,
      ex.muscle,
      s("Heaviest Weight", "Plus Gros Poids"),
      `${Calc.fmtKg(kg, Repo.settings.unit)} ${Calc.unitLabel(Repo.settings.unit)}`,
    );
  } else if (e > bestE && !st.prE) {
    st.prE = true;
    PrBadge.show(
      ex.name,
      ex.muscle,
      s("Best Est. 1RM", "Meilleure Est. 1RM"),
      `${Calc.fmtKg(e, Repo.settings.unit)} ${Calc.unitLabel(Repo.settings.unit)}`,
    );
  }
}
