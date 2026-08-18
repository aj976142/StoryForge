import { defaultSettings } from "../domain/models.js";

const PROJECTS_KEY = "storyforge.projects.v1";
const SETTINGS_KEY = "storyforge.settings.v1";

function memoryFallback() {
  const map = new Map();
  return {
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: (k) => map.delete(k),
  };
}

export function createProjectStore(storage) {
  const db = storage || (typeof localStorage !== "undefined" ? localStorage : memoryFallback());

  function readAll() {
    try {
      const raw = db.getItem(PROJECTS_KEY);
      const parsed = raw ? JSON.parse(raw) : [];
      if (!Array.isArray(parsed)) return [];
      return parsed.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));
    } catch {
      return [];
    }
  }

  function writeAll(list) {
    db.setItem(PROJECTS_KEY, JSON.stringify(list));
    return list.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));
  }

  return {
    list() {
      return readAll();
    },
    recent(limit = 8) {
      return readAll().slice(0, limit);
    },
    get(id) {
      return readAll().find((p) => p.id === id) || null;
    },
    upsert(project) {
      if (!project || !project.id) throw new Error("Project is missing an id.");
      const stamped = { ...project, updatedAt: Date.now() };
      const next = readAll().filter((p) => p.id !== stamped.id);
      next.push(stamped);
      writeAll(next);
      return stamped;
    },
    rename(id, title) {
      const current = this.get(id);
      if (!current) return null;
      const name = String(title || "").trim();
      if (!name) return current;
      return this.upsert({ ...current, title: name });
    },
    delete(id) {
      writeAll(readAll().filter((p) => p.id !== id));
    },
    clear() {
      writeAll([]);
    },
  };
}

export function createSettingsStore(storage) {
  const db = storage || (typeof localStorage !== "undefined" ? localStorage : memoryFallback());

  return {
    get() {
      try {
        const raw = db.getItem(SETTINGS_KEY);
        if (!raw) return defaultSettings();
        return { ...defaultSettings(), ...JSON.parse(raw), writing: { ...defaultSettings().writing, ...(JSON.parse(raw).writing || {}) }, ai: { ...defaultSettings().ai, ...(JSON.parse(raw).ai || {}) } };
      } catch {
        return defaultSettings();
      }
    },
    save(next) {
      const merged = { ...defaultSettings(), ...next };
      db.setItem(SETTINGS_KEY, JSON.stringify(merged));
      return merged;
    },
    patch(partial) {
      return this.save({ ...this.get(), ...partial });
    },
  };
}
