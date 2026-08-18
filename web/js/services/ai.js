import { formatById } from "../domain/models.js";

function delay(ms, signal) {
  return new Promise((resolve, reject) => {
    const t = setTimeout(resolve, ms);
    if (signal) {
      const onAbort = () => {
        clearTimeout(t);
        reject(Object.assign(new Error("Stopped"), { name: "AbortError" }));
      };
      if (signal.aborted) return onAbort();
      signal.addEventListener("abort", onAbort, { once: true });
    }
  });
}

export function suggestTitle(idea, formatId) {
  const seed = String(idea || "").trim().replace(/[\r\n]+/g, " ").slice(0, 48) || "Untitled";
  const short = seed.includes(" ") && seed.length < String(idea).trim().length ? seed.slice(0, seed.lastIndexOf(" ")) : seed;
  if (formatId === "YOUTUBE_SCRIPT") return `${short} — Script`;
  if (formatId === "MOVIE_SCREENPLAY") return short.toUpperCase().slice(0, 40);
  return short.charAt(0).toUpperCase() + short.slice(1);
}

export function compose(idea, formatId, prefs) {
  const hook = String(idea || "").trim().replace(/\s+/g, " ");
  const tone = (prefs?.tone || "Cinematic").toLowerCase();
  const pov = prefs?.pov || "Third person";
  const lengthNote = String(prefs?.length || "Medium").toLowerCase() === "short"
    ? "tight"
    : String(prefs?.length || "").toLowerCase() === "long"
      ? "expansive"
      : "measured";

  switch (formatId) {
    case "NOVEL":
      return [
        "Chapter One",
        "The Weight of a Beginning",
        "",
        `${pov}. A ${tone} register. The book opens on a ${lengthNote} breath.`,
        "",
        `Nobody writes “${hook}” on a scrap of paper unless the sentence has already been living rent-free in the body. It arrived the way weather does — not asked for, impossible to argue with. By the time the kettle clicked off, the idea had a hallway, a weather system, and a person who would have to walk through both.`,
        "",
        "She did not yet know the antagonist's name. She knew the temperature of the room when the thought appeared, which is how most true stories start: not with a plot, but with a pulse.",
        "",
        "Outside, the city kept its ordinary appointments. Inside, a first chapter did the only job a first chapter has — it made going back feel like a smaller life.",
        "",
        "The door, when she finally opened it, did not creak for drama. It simply let the next page in.",
      ].join("\n");
    case "MOVIE_SCREENPLAY":
      return [
        "FADE IN:",
        "",
        `TITLE CARD: from a raw note — "${hook}"`,
        "",
        "EXT. CITY EDGE — DUSK",
        "",
        `A ${tone} sky. Wind worries a loose flyer against a chain-link fence. Our PROTAGONIST (30s) stands as if the thought just found them.`,
        "",
        "PROTAGONIST",
        "(under breath)",
        "Say it once, out loud, and it becomes a job.",
        "",
        "They fold a scrap of paper. The words are already smudged.",
        "",
        "INT. SMALL KITCHEN — CONTINUOUS",
        "",
        "Cheap light. A phone face-down. They write anyway — not a plan, a dare.",
        "",
        "PROTAGONIST",
        "We start here. We don't wait for permission.",
        "",
        "A kettle screams. They don't move. The story has begun.",
        "",
        "FADE OUT.",
      ].join("\n");
    case "YOUTUBE_SCRIPT":
      return [
        "[HOOK — 0:00]",
        `What if the whole video is just this: ${hook}`,
        "Stay. I'll turn that raw thought into something you can actually say out loud.",
        "",
        "[INTRO — 0:08]",
        `I'm drafting this in a ${tone} voice on purpose — not corporate, not chaotic. If you've ever stared at a notes app and felt the idea go cold, this is the fix.`,
        "",
        "[BEAT 1 — The raw thought]",
        "Read the idea once. Don't decorate it. The power is already in the first wording.",
        "",
        "[BEAT 2 — The shape]",
        "Hook. Stakes. One turn. A last line people remember. That's the whole architecture.",
        "",
        "[BEAT 3 — The craft]",
        "Speak it. Cut anything you wouldn't say to a friend. Keep the line that makes you slightly nervous.",
        "",
        "[CTA]",
        "If this helped, save the script, record a messy take today, and tell me what you made. Subscribe if you want the next forge session.",
        "",
        "[END SCREEN]",
        "Next: polish pass in under ten minutes.",
      ].join("\n");
    case "POLISHED_WRITING": {
      const cleaned = hook.charAt(0).toUpperCase() + hook.slice(1);
      return [
        cleaned,
        "",
        `That is the thought, stood upright. The hedges are gone. The rhythm is ${tone}, but it still sounds like you.`,
        "",
        "What you meant is allowed to be simple. Simple is not small. It is the version a reader can carry out of the room.",
        "",
        "Keep this draft. Change only what is untrue.",
      ].join("\n");
    }
    case "SHORT_STORY":
    default:
      return [
        "The Note",
        "",
        `It began as almost nothing: ${hook}.`,
        "",
        `${pov}, ${tone} without trying to be. The sentence sat on the table beside a chipped mug and refused to stay small. People think ideas arrive dressed as plots. This one arrived dressed as weather — a change in pressure, a reason to look up.`,
        "",
        "She almost deleted it. Deleting would have been tidy. Instead she left the words where they were and let the afternoon arrange itself around them. A neighbor laughed two floors down. A bus sighed at the curb. Ordinary life, briefly willing to be a stage.",
        "",
        "By evening the idea had a person in it, and the person had a choice, and the choice had a cost she could feel in her hands. That was enough. Not a novel. Not a thesis. A complete small true thing:",
        "",
        "She kept the note. She walked out the door as if the rest of the story already knew her name.",
      ].join("\n");
  }
}

export function continuation(idea, formatId) {
  const hook = String(idea || "").trim();
  switch (formatId) {
    case "NOVEL":
      return `Chapter Two\n\nThe promise inside “${hook}” does not stay on the first page. By morning the choice has a cost. She keeps moving because stopping would mean admitting the idea was only a wish.`;
    case "MOVIE_SCREENPLAY":
      return "INT. CONTINUOUS — LATER\n\nThe camera holds. A door that should stay closed does not.\n\nPROTAGONIST\n(quiet)\nWe don't get to pretend we didn't start this.\n\nThey step through. The next scene begins.";
    case "YOUTUBE_SCRIPT":
      return `[MID-ROLL BEAT]\nIf this is useful, stay for the last part — it's the piece I wish someone had told me about “${hook}.”\n\n[B-ROLL: notebook, coffee, a window]\nHere's the practical close: pick one next action and do it before you overthink it.`;
    case "POLISHED_WRITING":
      return "What follows is the second pass: fewer hedges, a cleaner verb, and a last line that earns the idea instead of decorating it. The thought is still yours. It simply stands up straighter.";
    default:
      return `Later, when the noise thinned, the meaning of “${hook}” sat in the room like a second person. Nothing supernatural — only the ordinary bravery of staying. She put the kettle on and let the ending arrive without being forced.`;
  }
}

/**
 * Provider-agnostic AI contract used by the UI.
 * Swap MockAiService for a networked implementation without changing screens.
 */
export class MockAiService {
  constructor({ failNext = false, stepMs = 260 } = {}) {
    this.providerId = "mock";
    this.displayName = "StoryForge Mock Writer";
    this.failNext = failNext;
    this.stepMs = stepMs;
  }

  async generate({ idea, format, preferences, existingText, continueFrom, onProgress, signal }) {
    const trimmed = String(idea || "").trim();
    if (!trimmed) {
      const err = new Error("Add an idea before generating.");
      err.retryable = false;
      throw err;
    }
    if (this.failNext) {
      onProgress?.({ percent: 8, stage: "Connecting" });
      await delay(200, signal);
      const err = new Error("The writer could not be reached. Try again.");
      err.retryable = true;
      throw err;
    }

    const formatMeta = formatById(format);
    const stages = [
      [12, "Reading your idea"],
      [28, "Choosing structure"],
      [46, `Drafting ${formatMeta.displayName.toLowerCase()}`],
      [68, `Shaping voice (${preferences?.tone || "Cinematic"})`],
      [86, "Polishing lines"],
      [100, "Ready"],
    ];

    for (const [percent, stage] of stages) {
      onProgress?.({ percent, stage });
      await delay(continueFrom ? Math.max(120, this.stepMs - 80) : this.stepMs, signal);
    }

    let text;
    if (continueFrom) {
      const base = String(existingText || "").trim() || compose(trimmed, format, preferences);
      text = `${base.trimEnd()}\n\n${continuation(trimmed, format)}`;
    } else {
      text = compose(trimmed, format, preferences);
    }
    return { fullText: text, suggestedTitle: suggestTitle(trimmed, format) };
  }
}

export function createAiService(settings) {
  const provider = settings?.ai?.provider || "mock";
  // Only mock is implemented in V1. Other providers stay selectable as placeholders.
  if (provider !== "mock") {
    return new MockAiService();
  }
  return new MockAiService();
}
