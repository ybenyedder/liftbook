/** Hand-rolled navigation stack — port of Nav (App.kt). */
import { Workout } from "./models";
import { makeRev } from "./store";

export type Screen =
  | { t: "HomeTab" }
  | { t: "TrainingTab" }
  | { t: "ProfileTab" }
  | { t: "Settings" }
  | { t: "WorkoutDetail"; id: number }
  | { t: "ExerciseDetail"; name: string }
  | { t: "RoutineDetail"; id: number }
  | { t: "History" }
  | { t: "Exercises" }
  | { t: "Logger" }
  | { t: "WorkoutSummary" };

class NavState {
  pendingStartEmpty = false;
  stack: Screen[] = [{ t: "HomeTab" }];
  rev = makeRev();

  get current(): Screen {
    return this.stack[this.stack.length - 1];
  }
  push(s: Screen) {
    this.stack.push(s);
    this.rev.bump();
  }
  pop() {
    if (this.stack.length > 1) {
      this.stack.pop();
      this.rev.bump();
    }
  }
  toTab(s: Screen) {
    this.stack = [s];
    this.rev.bump();
  }
  get atTab(): boolean {
    return this.stack.length === 1;
  }
  /** Pop back to the enclosing tab after a detail screen is dropped. */
  resetToTabOf(s: Screen) {
    this.stack = [s];
    this.rev.bump();
  }
}

export const Nav = new NavState();

/** Global 5-second undo for a deleted workout — survives navigation. */
class DeletedUndoState {
  workout: Workout | null = null;
  rev = makeRev();
  set(w: Workout | null) {
    this.workout = w;
    this.rev.bump();
  }
}
export const DeletedUndo = new DeletedUndoState();
