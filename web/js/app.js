import {
  FORMAT_LIST,
  createProject,
  formatById,
  formatModified,
  previewLine,
  wordCount,
} from "./domain/models.js";
import { createProjectStore, createSettingsStore } from "./services/storage.js";
import { MockAiService, createAiService } from "./services/ai.js";

const projects = createProjectStore();
const settingsStore = createSettingsStore();
const root = document.getElementById("app");
const themeRoot = document.documentElement;

let route = parseHash();
let toast = "";
let dialog = null;
let generation = {
  running: false,
  percent: 0,
  stage: "Preparing",
  error: null,
  retryable: true,
  controller: null,
};
let voice = { listening: false, rec: null };
let saveHint = "";

function parseHash() {
  const raw = (location.hash || "#/home").replace(/^#/, "");
  const [path, query = ""] = raw.split("?");
  const parts = path.split("/").filter(Boolean);
  const params = Object.fromEntries(new URLSearchParams(query));
  return { name: parts[0] || "home", parts, params };
}

window.addEventListener("hashchange", () => {
  stopVoice();
  route = parseHash();
  render();
});

function go(hash) {
  const next = hash.startsWith("#") ? hash : `#${hash}`;
  if (location.hash !== next) location.hash = next;
  route = parseHash();
  render();
}

function applyTheme() {
  const mode = settingsStore.get().theme || "system";
  const prefersDark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
  const dark = mode === "dark" || (mode === "system" && prefersDark);
  themeRoot.dataset.theme = dark ? "dark" : "light";
}

function clock() {
  return new Date().toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

function icon(name) {
  const paths = {
    flame: '<path d="M12 2s4 4.2 4 8a4 4 0 0 1-8 0c0-2 1-4 2-5-2 2-4 5-4 8a6 6 0 0 0 12 0c0-5-6-11-6-11z"/>',
    home: '<path d="M4 11.5 12 4l8 7.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1z"/>',
    folder: '<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
    gear: '<path d="M10 2h4l.6 2.4a7 7 0 0 1 1.8.9L19 4.6 21.4 7l-1.7 1.6c.2.6.3 1.2.3 1.9s-.1 1.3-.3 1.9L21.4 14 19 16.4l-2.6-.7a7 7 0 0 1-1.8.9L14 19h-4l-.6-2.4a7 7 0 0 1-1.8-.9L5 16.4 2.6 14l1.7-1.6A7 7 0 0 1 4 10c0-.7.1-1.3.3-1.9L2.6 6.5 5 4.1l2.6.7a7 7 0 0 1 1.8-.9z"/><circle cx="12" cy="10.5" r="2.4" fill="none" stroke="currentColor"/>',
    mic: '<path d="M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3zm-7 9a7 7 0 0 0 14 0M12 19v3"/>',
    stop: '<rect x="7" y="7" width="10" height="10" rx="2"/>',
    back: '<path d="M15 5 7 12l8 7" fill="none" stroke="currentColor" stroke-width="2"/>',
    close: '<path d="M6 6l12 12M18 6 6 18" fill="none" stroke="currentColor" stroke-width="2"/>',
    book: '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5z"/>',
    play: '<path d="M8 5v14l11-7z"/>',
  };
  return `<svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true">${paths[name] || ""}</svg>`;
}

function nav(active) {
  return `
    <nav class="nav" aria-label="Primary">
      <button data-go="#/home" class="${active === "home" ? "active" : ""}">${icon("home")}<span>Home</span></button>
      <button data-go="#/projects" class="${active === "projects" ? "active" : ""}">${icon("folder")}<span>Projects</span></button>
      <button data-go="#/settings" class="${active === "settings" ? "active" : ""}">${icon("gear")}<span>Settings</span></button>
    </nav>`;
}

function startDraft(mode) {
  try {
    const project = projects.upsert(createProject({ mode }));
    go(`#/input/${project.id}?mode=${mode}`);
  } catch (err) {
    toast = err.message || "Could not create a project.";
    render();
  }
}

function openExisting(id) {
  const project = projects.get(id);
  if (!project) {
    toast = "That project could not be opened.";
    go("#/projects");
    return;
  }
  if (project.generatedText) go(`#/editor/${id}`);
  else if (project.rawIdea) go(`#/format/${id}`);
  else go(`#/input/${id}?mode=${project.inputMode || "TEXT"}`);
}

function homeScreen() {
  const recent = projects.recent();
  return `
    <section class="screen" data-screen="home">
      <div class="brand">
        <div class="mark">${icon("flame")}</div>
        <div>
          <h1>StoryForge AI</h1>
          <p>Speak a spark. Leave with a story.</p>
        </div>
      </div>
      <button class="btn btn-primary" data-action="new-text">New Project</button>
      <div class="row" style="margin-top:12px">
        <button class="btn btn-ghost" data-action="new-voice">${icon("mic")} Voice Input</button>
        <button class="btn btn-ghost" data-action="new-text">Type Idea</button>
      </div>
      ${toast ? `<div class="banner error">${escapeHtml(toast)}</div>` : ""}
      <h2 class="section-title">Recent Projects</h2>
      ${
        recent.length
          ? recent.map(projectCard).join("")
          : `<div class="empty"><strong>No stories yet</strong>Start with a messy thought. We'll shape it into a novel, script, or clean prose.</div>`
      }
    </section>
    ${nav("home")}`;
}

function projectCard(p) {
  return `
    <article class="card clickable" style="margin-bottom:10px" data-open="${p.id}">
      <h3>${escapeHtml(p.title || "Untitled")}</h3>
      <div class="excerpt">${escapeHtml(previewLine(p))}</div>
      <div class="meta">${escapeHtml(formatById(p.format).displayName)} · ${escapeHtml(formatModified(p.updatedAt))}</div>
    </article>`;
}

function inputScreen(id, mode) {
  const project = projects.get(id);
  if (!project) return missing();
  const text = project.rawIdea || "";
  return `
    <section class="screen" data-screen="input">
      <div class="topbar">
        <button class="icon-btn" data-go="#/home">${icon("back")}</button>
        <h1>${mode === "VOICE" ? "Voice your idea" : "Type your idea"}</h1>
      </div>
      <p class="hint">Dump the raw thought. Structure comes next.</p>
      <textarea class="editor" id="idea" placeholder="A detective who can only remember other people's dreams…">${escapeHtml(text)}</textarea>
      <div class="counts">
        <span id="counts">${wordCount(text)} words · ${text.length} characters</span>
        <span id="save-hint">${saveHint}</span>
      </div>
      ${toast ? `<div class="banner error">${escapeHtml(toast)}</div>` : ""}
      <div class="row">
        <button class="icon-btn ${voice.listening ? "mic on" : ""}" id="mic" aria-label="Microphone">${icon(voice.listening ? "stop" : "mic")}</button>
        <button class="icon-btn" id="clear" aria-label="Clear">${icon("close")}</button>
        <button class="btn btn-primary grow" id="continue-input" ${text.trim() ? "" : "disabled"}>Continue</button>
      </div>
    </section>
    ${nav("home")}`;
}

function formatScreen(id) {
  const project = projects.get(id);
  if (!project) return missing();
  return `
    <section class="screen" data-screen="format">
      <div class="topbar">
        <button class="icon-btn" data-go="#/input/${id}">${icon("back")}</button>
        <h1>Choose a form</h1>
      </div>
      <p class="hint">How should StoryForge shape this idea?</p>
      <div class="format-grid">
        ${FORMAT_LIST.map((f) => `
          <button class="card format-card ${project.format === f.id ? "selected" : ""}" data-format="${f.id}">
            <div class="tag">${escapeHtml(f.tagline)}</div>
            <h3>${escapeHtml(f.displayName)}</h3>
            <div class="excerpt">${escapeHtml(f.description)}</div>
          </button>
        `).join("")}
      </div>
      ${toast ? `<div class="banner error">${escapeHtml(toast)}</div>` : ""}
      <div style="height:12px"></div>
      <button class="btn btn-primary" id="transform">Transform idea</button>
    </section>
    ${nav("home")}`;
}

function generationScreen(id, continueWrite) {
  return `
    <section class="screen" data-screen="generation">
      <div class="progress-wrap">
        <div class="forge-icon">${icon("flame")}</div>
        <h1 style="font-family:Fraunces,serif">Forging your draft</h1>
        <p class="muted" id="gen-stage">${escapeHtml(generation.stage)}</p>
        <div class="progress-bar"><span id="gen-bar" style="width:${generation.percent}%"></span></div>
        <div id="gen-pct">${generation.percent}%</div>
        ${generation.error ? `<div class="banner error">${escapeHtml(generation.error)}</div>` : ""}
        <div style="height:18px"></div>
        ${
          generation.error
            ? `<div class="row">
                ${generation.retryable ? `<button class="btn btn-primary" id="retry">Retry</button>` : ""}
                <button class="btn btn-outline" data-go="#/format/${id}">Back</button>
              </div>`
            : `<button class="btn btn-danger" id="stop">Stop</button>`
        }
      </div>
    </section>
    ${nav("home")}`;
}

function editorScreen(id) {
  const project = projects.get(id);
  if (!project) return missing();
  const text = project.generatedText || project.rawIdea || "";
  return `
    <section class="screen" data-screen="editor">
      <div class="topbar">
        <button class="icon-btn" data-go="#/home">${icon("back")}</button>
        <h1>Editor</h1>
      </div>
      <input class="field" id="title" value="${escapeAttr(project.title)}" placeholder="Title" />
      <div style="height:10px"></div>
      <textarea class="editor" id="draft">${escapeHtml(text)}</textarea>
      <div class="counts"><span>${escapeHtml(formatById(project.format).displayName)}</span><span id="save-hint">${saveHint}</span></div>
      ${toast ? `<div class="banner error">${escapeHtml(toast)}</div>` : ""}
      <div class="actions-grid">
        <button class="btn btn-outline" id="regen">Regenerate</button>
        <button class="btn btn-outline" id="continue-write">Continue Writing</button>
        <button class="btn btn-outline" id="copy">Copy</button>
        <button class="btn btn-primary" id="save">Save</button>
      </div>
    </section>
    ${nav("home")}`;
}

function projectsScreen() {
  const items = projects.list();
  return `
    <section class="screen" data-screen="projects">
      <div class="topbar"><h1>Projects</h1></div>
      ${
        items.length
          ? items
              .map(
                (p) => `
        <article class="card" style="margin-bottom:10px">
          <h3>${escapeHtml(p.title)}</h3>
          <div class="meta">${escapeHtml(formatById(p.format).displayName)} · ${escapeHtml(formatModified(p.updatedAt))}</div>
          <div class="project-actions">
            <button data-open="${p.id}">Open</button>
            <button data-rename="${p.id}">Rename</button>
            <button data-delete="${p.id}">Delete</button>
          </div>
        </article>`
              )
              .join("")
          : `<div class="empty"><strong>Nothing saved yet</strong>Finished drafts will live here. You can rename or delete them anytime.</div>
             <button class="btn btn-primary" data-action="new-text">New Project</button>`
      }
      ${dialogMarkup()}
    </section>
    ${nav("projects")}`;
}

function settingsScreen() {
  const s = settingsStore.get();
  const chip = (group, value, label, current) =>
    `<button class="chip ${current === value ? "active" : ""}" data-set="${group}:${value}">${label}</button>`;
  return `
    <section class="screen" data-screen="settings">
      <div class="topbar"><h1>Settings</h1></div>
      <h2 class="section-title">Theme</h2>
      <div class="chips">
        ${chip("theme", "system", "System", s.theme)}
        ${chip("theme", "light", "Light", s.theme)}
        ${chip("theme", "dark", "Dark", s.theme)}
      </div>
      <h2 class="section-title">AI provider</h2>
      <p class="hint">A real provider can be wired through AiService. V1 ships with a local mock writer so the app runs without an API key.</p>
      <div class="chips">
        ${chip("provider", "mock", "Mock (on-device)", s.ai.provider)}
        ${chip("provider", "openai", "OpenAI", s.ai.provider)}
        ${chip("provider", "gemini", "Gemini", s.ai.provider)}
        ${chip("provider", "anthropic", "Claude", s.ai.provider)}
      </div>
      <input class="field" disabled placeholder="API key — coming in a later release" />
      <p class="hint" style="margin-top:8px">Active model: ${escapeHtml(s.ai.model)}</p>
      <h2 class="section-title">Writing preferences</h2>
      <p class="hint">Tone</p>
      <div class="chips">
        ${["Cinematic", "Literary", "Casual", "Dramatic"].map((t) => chip("tone", t, t, s.writing.tone)).join("")}
      </div>
      <p class="hint">Length</p>
      <div class="chips">
        ${["Short", "Medium", "Long"].map((t) => chip("length", t, t, s.writing.length)).join("")}
      </div>
      <p class="hint">Point of view</p>
      <div class="chips">
        ${["First person", "Second person", "Third person"].map((t) => chip("pov", t, t, s.writing.pov)).join("")}
      </div>
      <p class="hint">Language</p>
      <div class="chips">
        ${["English", "Spanish", "French"].map((t) => chip("language", t, t, s.writing.language)).join("")}
      </div>
    </section>
    ${nav("settings")}`;
}

function dialogMarkup() {
  if (!dialog) return "";
  if (dialog.type === "rename") {
    return `
      <div class="dialog-back">
        <div class="dialog">
          <h3>Rename project</h3>
          <input class="field" id="rename-input" value="${escapeAttr(dialog.title)}" />
          <div class="row" style="margin-top:12px">
            <button class="btn btn-outline" id="dialog-cancel">Cancel</button>
            <button class="btn btn-primary" id="dialog-ok">Save</button>
          </div>
        </div>
      </div>`;
  }
  if (dialog.type === "delete") {
    return `
      <div class="dialog-back">
        <div class="dialog">
          <h3>Delete project?</h3>
          <p class="hint">This cannot be undone.</p>
          <div class="row">
            <button class="btn btn-outline" id="dialog-cancel">Cancel</button>
            <button class="btn btn-danger" id="dialog-ok">Delete</button>
          </div>
        </div>
      </div>`;
  }
  return "";
}

function missing() {
  return `
    <section class="screen">
      <div class="empty"><strong>Project missing</strong>It may have been deleted.</div>
      <button class="btn btn-primary" data-go="#/home">Back home</button>
    </section>${nav("home")}`;
}

function render() {
  applyTheme();
  const name = route.name;
  const id = route.parts[1];
  toast = name === route.name ? toast : "";
  let html = "";
  if (name === "home") html = homeScreen();
  else if (name === "input") html = inputScreen(id, (route.params.mode || "TEXT").toUpperCase());
  else if (name === "format") html = formatScreen(id);
  else if (name === "generation") html = generationScreen(id, route.params.continueWrite === "1");
  else if (name === "editor") html = editorScreen(id);
  else if (name === "projects") html = projectsScreen();
  else if (name === "settings") html = settingsScreen();
  else html = homeScreen();
  root.innerHTML = html;
  bind();
  const time = document.getElementById("clock");
  if (time) time.textContent = clock();
}

function bind() {
  root.querySelectorAll("[data-go]").forEach((el) => {
    el.addEventListener("click", () => go(el.getAttribute("data-go")));
  });
  root.querySelectorAll("[data-action='new-text']").forEach((el) => {
    el.addEventListener("click", () => startDraft("TEXT"));
  });
  root.querySelectorAll("[data-action='new-voice']").forEach((el) => {
    el.addEventListener("click", () => startDraft("VOICE"));
  });
  root.querySelectorAll("[data-open]").forEach((el) => {
    el.addEventListener("click", () => openExisting(el.getAttribute("data-open")));
  });
  root.querySelectorAll("[data-rename]").forEach((el) => {
    el.addEventListener("click", () => {
      const p = projects.get(el.getAttribute("data-rename"));
      if (!p) return;
      dialog = { type: "rename", id: p.id, title: p.title };
      render();
    });
  });
  root.querySelectorAll("[data-delete]").forEach((el) => {
    el.addEventListener("click", () => {
      dialog = { type: "delete", id: el.getAttribute("data-delete") };
      render();
    });
  });
  root.querySelectorAll("[data-set]").forEach((el) => {
    el.addEventListener("click", () => {
      const [group, ...rest] = el.getAttribute("data-set").split(":");
      const value = rest.join(":");
      const s = settingsStore.get();
      if (group === "theme") settingsStore.patch({ theme: value });
      else if (group === "provider") settingsStore.patch({ ai: { ...s.ai, provider: value } });
      else settingsStore.patch({ writing: { ...s.writing, [group]: value } });
      render();
    });
  });
  root.querySelectorAll("[data-format]").forEach((el) => {
    el.addEventListener("click", () => {
      const id = route.parts[1];
      const p = projects.get(id);
      if (!p) return;
      projects.upsert({ ...p, format: el.getAttribute("data-format") });
      render();
    });
  });

  const idea = document.getElementById("idea");
  if (idea) {
    idea.addEventListener("input", () => {
      const id = route.parts[1];
      const p = projects.get(id);
      if (!p) return;
      projects.upsert({ ...p, rawIdea: idea.value });
      const counts = document.getElementById("counts");
      if (counts) counts.textContent = `${wordCount(idea.value)} words · ${idea.value.length} characters`;
      const hint = document.getElementById("save-hint");
      if (hint) hint.textContent = "Saved";
      const cont = document.getElementById("continue-input");
      if (cont) cont.disabled = !idea.value.trim();
    });
    if ((route.params.mode || "").toUpperCase() === "VOICE" && !idea.value.trim()) {
      setTimeout(() => startVoice(idea), 250);
    }
  }
  document.getElementById("clear")?.addEventListener("click", () => {
    if (!idea) return;
    idea.value = "";
    idea.dispatchEvent(new Event("input"));
  });
  document.getElementById("mic")?.addEventListener("click", () => {
    if (voice.listening) stopVoice();
    else startVoice(idea);
  });
  document.getElementById("continue-input")?.addEventListener("click", () => {
    const id = route.parts[1];
    const p = projects.get(id);
    if (!p || !p.rawIdea.trim()) {
      toast = "Write or speak an idea first.";
      render();
      return;
    }
    go(`#/format/${id}`);
  });
  document.getElementById("transform")?.addEventListener("click", () => {
    go(`#/generation/${route.parts[1]}`);
  });
  document.getElementById("stop")?.addEventListener("click", () => {
    generation.controller?.abort();
  });
  document.getElementById("retry")?.addEventListener("click", () => {
    generation.error = null;
    beginGeneration();
    render();
  });

  const title = document.getElementById("title");
  const draft = document.getElementById("draft");
  const persistEditor = () => {
    const id = route.parts[1];
    const p = projects.get(id);
    if (!p) return;
    projects.upsert({
      ...p,
      title: title?.value || p.title,
      generatedText: draft?.value ?? p.generatedText,
      status: "SAVED",
    });
    saveHint = "Saved";
    const hint = document.getElementById("save-hint");
    if (hint) hint.textContent = "Saved";
  };
  title?.addEventListener("input", persistEditor);
  draft?.addEventListener("input", persistEditor);
  document.getElementById("save")?.addEventListener("click", persistEditor);
  document.getElementById("copy")?.addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText(draft?.value || "");
      saveHint = "Copied";
      const hint = document.getElementById("save-hint");
      if (hint) hint.textContent = "Copied";
    } catch {
      toast = "Copy isn't available in this browser. Select the text instead.";
      render();
    }
  });
  document.getElementById("regen")?.addEventListener("click", () => {
    persistEditor();
    go(`#/generation/${route.parts[1]}`);
  });
  document.getElementById("continue-write")?.addEventListener("click", () => {
    persistEditor();
    go(`#/generation/${route.parts[1]}?continueWrite=1`);
  });

  document.getElementById("dialog-cancel")?.addEventListener("click", () => {
    dialog = null;
    render();
  });
  document.getElementById("dialog-ok")?.addEventListener("click", () => {
    if (dialog?.type === "rename") {
      const value = document.getElementById("rename-input")?.value || "";
      projects.rename(dialog.id, value);
    }
    if (dialog?.type === "delete") projects.delete(dialog.id);
    dialog = null;
    render();
  });

  if (route.name === "generation" && !generation.running && !generation.error) {
    beginGeneration();
  }
}

function startVoice(textarea) {
  const Speech = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!Speech) {
    toast = "Voice input isn't available here. Type your idea instead.";
    render();
    return;
  }
  try {
    stopVoice();
    const rec = new Speech();
    rec.continuous = false;
    rec.interimResults = true;
    rec.onresult = (event) => {
      const spoken = Array.from(event.results).map((r) => r[0].transcript).join(" ");
      if (textarea) {
        const merged = [textarea.value.trim(), spoken.trim()].filter(Boolean).join(" ");
        textarea.value = merged;
        textarea.dispatchEvent(new Event("input"));
      }
    };
    rec.onerror = () => {
      voice.listening = false;
      toast = "Voice input failed. You can keep typing.";
      render();
    };
    rec.onend = () => {
      voice.listening = false;
      const mic = document.getElementById("mic");
      if (mic) mic.classList.remove("on");
    };
    rec.start();
    voice = { listening: true, rec };
    const mic = document.getElementById("mic");
    if (mic) mic.classList.add("on");
  } catch {
    toast = "Could not start the microphone.";
    render();
  }
}

function stopVoice() {
  try {
    voice.rec?.stop();
  } catch {
    /* ignore */
  }
  voice = { listening: false, rec: null };
}

async function beginGeneration() {
  const id = route.parts[1];
  const project = projects.get(id);
  if (!project) {
    generation = { ...generation, running: false, error: "Project missing.", retryable: false };
    render();
    return;
  }
  const controller = new AbortController();
  generation = { running: true, percent: 0, stage: "Preparing", error: null, retryable: true, controller };
  render();
  const ai = createAiService(settingsStore.get()) || new MockAiService();
  try {
    const result = await ai.generate({
      idea: project.rawIdea,
      format: project.format,
      preferences: settingsStore.get().writing,
      existingText: project.generatedText,
      continueFrom: route.params.continueWrite === "1",
      signal: controller.signal,
      onProgress: ({ percent, stage }) => {
        generation.percent = percent;
        generation.stage = stage;
        const bar = document.getElementById("gen-bar");
        const pct = document.getElementById("gen-pct");
        const st = document.getElementById("gen-stage");
        if (bar) bar.style.width = `${percent}%`;
        if (pct) pct.textContent = `${percent}%`;
        if (st) st.textContent = stage;
      },
    });
    projects.upsert({
      ...project,
      generatedText: result.fullText,
      title: project.title === "Untitled draft" ? result.suggestedTitle : project.title,
      status: "GENERATED",
    });
    generation.running = false;
    go(`#/editor/${id}`);
  } catch (err) {
    const stopped = err?.name === "AbortError";
    generation = {
      running: false,
      percent: generation.percent,
      stage: stopped ? "Stopped" : "Failed",
      error: stopped ? "Generation stopped." : err.message || "Generation failed.",
      retryable: stopped ? true : err.retryable !== false,
      controller: null,
    };
    render();
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function escapeAttr(value) {
  return escapeHtml(value).replace(/'/g, "&#39;");
}

applyTheme();
if (!location.hash) location.hash = "#/home";
render();
setInterval(() => {
  const el = document.getElementById("clock");
  if (el) el.textContent = clock();
}, 30000);

if (window.matchMedia) {
  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", applyTheme);
}

export const __test = { projects, settingsStore, startDraft, openExisting };
