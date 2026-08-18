import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { createProject, formatById, wordCount, previewLine } from "./domain/models.js";
import { createProjectStore, createSettingsStore } from "./services/storage.js";
import { MockAiService, compose, suggestTitle } from "./services/ai.js";

class Memory {
  constructor() {
    this.map = new Map();
  }
  getItem(k) {
    return this.map.has(k) ? this.map.get(k) : null;
  }
  setItem(k, v) {
    this.map.set(k, String(v));
  }
  removeItem(k) {
    this.map.delete(k);
  }
}

async function main() {
  const mem = new Memory();
  const projects = createProjectStore(mem);
  const settings = createSettingsStore(mem);

  // Empty state
  assert.equal(projects.list().length, 0);

  // Create
  const draft = projects.upsert(createProject({ mode: "TEXT" }));
  assert.ok(draft.id);
  assert.equal(draft.status, "DRAFT");

  // Edit idea + format
  projects.upsert({ ...projects.get(draft.id), rawIdea: "a clock that steals Tuesdays" });
  projects.upsert({ ...projects.get(draft.id), format: "SHORT_STORY" });
  assert.equal(projects.get(draft.id).rawIdea, "a clock that steals Tuesdays");

  // Generate
  const ai = new MockAiService({ stepMs: 5 });
  const stages = [];
  const generated = await ai.generate({
    idea: "a clock that steals Tuesdays",
    format: "SHORT_STORY",
    preferences: settings.get().writing,
    onProgress: (p) => stages.push(p.percent),
  });
  assert.ok(generated.fullText.includes("clock"));
  assert.ok(generated.suggestedTitle);
  assert.ok(stages.includes(100));

  // Save
  const saved = projects.upsert({
    ...projects.get(draft.id),
    generatedText: generated.fullText,
    title: generated.suggestedTitle,
    status: "SAVED",
  });
  assert.equal(saved.status, "SAVED");

  // Reopen
  const reopened = projects.get(saved.id);
  assert.equal(reopened.generatedText, generated.fullText);
  assert.equal(reopened.title, generated.suggestedTitle);

  // Rename + delete
  assert.equal(projects.rename(saved.id, "Tuesday Thief").title, "Tuesday Thief");
  projects.delete(saved.id);
  assert.equal(projects.get(saved.id), null);

  // Empty idea fails safely
  await assert.rejects(
    () => ai.generate({ idea: "  ", format: "NOVEL" }),
    /Add an idea/
  );

  // Stop / abort
  const controller = new AbortController();
  const slow = new MockAiService({ stepMs: 80 });
  const pending = slow.generate({
    idea: "x",
    format: "NOVEL",
    signal: controller.signal,
  });
  controller.abort();
  await assert.rejects(pending, (err) => err.name === "AbortError");

  // All formats produce content
  for (const id of ["NOVEL", "MOVIE_SCREENPLAY", "SHORT_STORY", "YOUTUBE_SCRIPT", "POLISHED_WRITING"]) {
    const text = compose("lost keys in a cathedral", id, settings.get().writing);
    assert.ok(text.length > 40, id);
    assert.ok(formatById(id).displayName);
    assert.ok(suggestTitle("lost keys in a cathedral", id));
  }

  assert.equal(wordCount("one two three"), 3);
  assert.ok(previewLine({ rawIdea: "hello world", generatedText: "" }).includes("hello"));

  // Settings persist
  settings.patch({ theme: "dark", writing: { ...settings.get().writing, tone: "Literary" } });
  assert.equal(settings.get().theme, "dark");
  assert.equal(settings.get().writing.tone, "Literary");

  const appSrc = readFileSync(join(dirname(fileURLToPath(import.meta.url)), "app.js"), "utf8");
  for (const screen of ["home", "input", "format", "generation", "editor", "projects", "settings"]) {
    assert.ok(appSrc.includes(`data-screen="${screen}"`) || appSrc.includes(`function ${screen}Screen`), `screen ${screen}`);
  }

  console.log("OK  create → generate → edit → save → reopen → rename → delete");
  console.log("OK  all five output formats");
  console.log("OK  empty idea + abort handling");
  console.log("OK  settings persistence");
  console.log("OK  all seven screens present");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
