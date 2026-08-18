export const OutputFormat = Object.freeze({
  NOVEL: {
    id: "NOVEL",
    displayName: "Novel",
    tagline: "Chaptered long-form fiction",
    description: "Turn a spark into chapters, characters, and a narrative arc ready to grow.",
    icon: "book",
  },
  MOVIE_SCREENPLAY: {
    id: "MOVIE_SCREENPLAY",
    displayName: "Movie Screenplay",
    tagline: "Industry-style script pages",
    description: "Scene headings, action lines, and dialogue shaped for the screen.",
    icon: "clapper",
  },
  SHORT_STORY: {
    id: "SHORT_STORY",
    displayName: "Short Story",
    tagline: "A complete tale in one sitting",
    description: "A focused beginning, turn, and ending with a clear emotional beat.",
    icon: "quill",
  },
  YOUTUBE_SCRIPT: {
    id: "YOUTUBE_SCRIPT",
    displayName: "YouTube Script",
    tagline: "Hook, story, and CTA",
    description: "Spoken-word pacing with a hook, beats, b-roll notes, and a closer.",
    icon: "play",
  },
  POLISHED_WRITING: {
    id: "POLISHED_WRITING",
    displayName: "Polished Writing",
    tagline: "Cleaner, stronger prose",
    description: "Keep your voice. Tighten rhythm, clarity, and word choice.",
    icon: "spark",
  },
});

export const FORMAT_LIST = Object.values(OutputFormat);

export function formatById(id) {
  return OutputFormat[id] || OutputFormat.SHORT_STORY;
}

export function createProject({ mode = "TEXT" } = {}) {
  const now = Date.now();
  return {
    id: cryptoRandomId(),
    title: "Untitled draft",
    rawIdea: "",
    generatedText: "",
    format: "SHORT_STORY",
    inputMode: mode === "VOICE" ? "VOICE" : "TEXT",
    createdAt: now,
    updatedAt: now,
    status: "DRAFT",
  };
}

export function defaultWritingPrefs() {
  return { tone: "Cinematic", length: "Medium", language: "English", pov: "Third person" };
}

export function defaultSettings() {
  return {
    theme: "system",
    writing: defaultWritingPrefs(),
    ai: { provider: "mock", model: "storyforge-mock-v1", apiKeyConfigured: false },
  };
}

export function wordCount(text) {
  const trimmed = String(text || "").trim();
  if (!trimmed) return 0;
  return trimmed.split(/\s+/).filter(Boolean).length;
}

export function previewLine(project) {
  const source = project.generatedText || project.rawIdea || "Empty draft";
  return source.replace(/\s+/g, " ").trim().slice(0, 110);
}

export function formatModified(millis) {
  try {
    return new Intl.DateTimeFormat(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
    }).format(new Date(millis));
  } catch {
    return new Date(millis).toLocaleString();
  }
}

export function cryptoRandomId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID();
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
