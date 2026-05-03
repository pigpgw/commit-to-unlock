#!/usr/bin/env tsx
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { basename, join, resolve } from "node:path";

interface DogfoodEvent {
  timestamp: Date;
  timestampRaw: string;
  type: string;
  target: string | null;
  policyReason: string | null;
  creditRemaining: number | null;
  detail: string;
}

interface DailySummary {
  date: string;
  events: number;
  blockedAttempts: number;
  policyBlocks: number;
  emergencyUnlocks: number;
  freeDays: number;
  dailyQuestAdds: number;
  dailyQuestMockCompletions: number;
  autoCreditSpends: number;
}

type GateStatus = "pass" | "fail" | "needs_data";

interface GateCheck {
  label: string;
  passed: boolean;
  actual: number | boolean;
  target: string;
}

interface GateDecision {
  id: "A" | "B" | "C";
  title: string;
  status: GateStatus;
  checks: GateCheck[];
  summary: string;
}

interface DataQualityMetric {
  metric: string;
  events: number;
  populated: number;
  coverage: number;
}

interface DogfoodAnalysis {
  file: string;
  eventCount: number;
  firstEventAt: string | null;
  lastEventAt: string | null;
  activeDays: number;
  counts: Record<string, number>;
  metrics: Record<string, number>;
  policyReasons: Record<string, number>;
  targets: Record<string, number>;
  daily: DailySummary[];
  gates: GateDecision[];
  dataQuality: DataQualityMetric[];
  recommendations: string[];
}

interface CliOptions {
  inputPath?: string;
  json: boolean;
  help: boolean;
}

const eventTypes = {
  blockedAttempts: ["blocked_attempt"],
  policyBlocks: ["policy_blocked"],
  permissionFailures: ["permission_missing"],
  overlayOpens: ["overlay_open_app"],
  overlayCreditAdds: ["overlay_add_credit"],
  autoCreditSpends: ["credit_auto_spent"],
  manualCreditChanges: ["credit_added", "credit_spent", "credit_reset"],
  freeDays: ["free_day_set"],
  emergencyUnlocks: ["emergency_unlock_started"],
  dailyQuestAdds: ["daily_quest_added"],
  dailyQuestMockCompletions: ["daily_quest_mock_completed"],
  foregroundChanges: ["foreground_changed"],
  monitorEnabledSignals: ["monitor_started", "monitor_heartbeat"],
  overlayShows: ["overlay_shown"]
} as const;

const targetLikeEvents = new Set([
  "blocked_attempt",
  "foreground_changed",
  "overlay_open_app",
  "overlay_shown",
  "policy_allowed",
  "policy_blocked",
  "target_added",
  "target_matched",
  "target_use_started",
  "target_use_stopped"
]);

const policyReasonEvents = new Set([
  "blocked_attempt",
  "credit_auto_spent",
  "overlay_add_credit",
  "overlay_hidden",
  "overlay_open_app",
  "overlay_shown",
  "policy_allowed",
  "policy_blocked",
  "target_matched",
  "target_use_started",
  "target_use_stopped"
]);

const creditRemainingEvents = new Set([
  "blocked_attempt",
  "credit_added",
  "credit_auto_spent",
  "credit_reset",
  "credit_spent",
  "overlay_add_credit",
  "overlay_hidden",
  "overlay_open_app",
  "overlay_shown",
  "policy_allowed",
  "policy_blocked",
  "target_matched",
  "target_use_started"
]);

function usage(): string {
  return `Usage: pnpm android:dogfood:analyze [dogfood-export.tsv] [--json]

If no file is provided, the newest artifacts/android-dogfood/*.tsv export is used.

Options:
  --json      Print machine-readable JSON instead of Markdown.
  -h, --help  Show this help text.
`;
}

function parseArgs(argv: string[]): CliOptions {
  const options: CliOptions = { json: false, help: false };

  for (const arg of argv) {
    if (arg === "--") continue;
    if (arg === "--json") {
      options.json = true;
      continue;
    }
    if (arg === "-h" || arg === "--help") {
      options.help = true;
      continue;
    }
    if (arg.startsWith("-")) {
      throw new Error(`Unknown option: ${arg}`);
    }
    if (options.inputPath) {
      throw new Error(`Too many input files: ${arg}`);
    }
    options.inputPath = arg;
  }

  return options;
}

function newestDogfoodExport(): string {
  const artifactDir = resolve("artifacts/android-dogfood");
  if (!existsSync(artifactDir)) {
    throw new Error("No artifacts/android-dogfood directory found. Run pnpm android:dogfood:export first.");
  }

  const candidates = readdirSync(artifactDir)
    .filter((file) => file.endsWith(".tsv"))
    .map((file) => join(artifactDir, file))
    .filter((file) => statSync(file).isFile())
    .sort((left, right) => statSync(right).mtimeMs - statSync(left).mtimeMs);

  const newest = candidates.at(0);
  if (!newest) {
    throw new Error("No dogfood TSV exports found under artifacts/android-dogfood.");
  }
  return newest;
}

function parseTsv(filePath: string): DogfoodEvent[] {
  const raw = readFileSync(filePath, "utf8").trim();
  if (!raw) return [];

  const lines = raw.split(/\r?\n/);
  const header = lines.shift()?.split("\t") ?? [];
  const timestampIndex = header.indexOf("timestamp");
  const typeIndex = header.indexOf("type");
  const targetIndex = header.indexOf("target");
  const policyReasonIndex = header.indexOf("policy_reason");
  const creditRemainingIndex = header.indexOf("credit_remaining");
  const detailIndex = header.indexOf("detail");

  if (timestampIndex < 0 || typeIndex < 0 || detailIndex < 0) {
    throw new Error("Expected TSV header with timestamp, type, and detail columns.");
  }

  return lines
    .map((line, index) => {
      const columns = line.split("\t");
      const timestampRaw = columns[timestampIndex]?.trim() ?? "";
      const type = columns[typeIndex]?.trim() ?? "";
      const target = optionalColumn(columns, targetIndex);
      const policyReason = optionalColumn(columns, policyReasonIndex);
      const creditRemainingRaw = optionalColumn(columns, creditRemainingIndex);
      const creditRemaining = creditRemainingRaw === null ? null : Number.parseInt(creditRemainingRaw, 10);
      const detail = columns.slice(detailIndex).join("\t").trim();
      const timestamp = new Date(timestampRaw);

      if (!timestampRaw || Number.isNaN(timestamp.getTime()) || !type) {
        throw new Error(`Invalid dogfood row at line ${index + 2}: ${line}`);
      }
      if (creditRemainingRaw !== null && !Number.isInteger(creditRemaining)) {
        throw new Error(`Invalid credit_remaining value at line ${index + 2}: ${line}`);
      }

      return {
        timestamp,
        timestampRaw,
        type,
        target,
        policyReason,
        creditRemaining,
        detail
      };
    })
    .sort((left, right) => left.timestamp.getTime() - right.timestamp.getTime());
}

function optionalColumn(columns: string[], index: number): string | null {
  if (index < 0) return null;
  const value = columns[index]?.trim() ?? "";
  return value.length > 0 ? value : null;
}

function analyze(filePath: string, events: DogfoodEvent[]): DogfoodAnalysis {
  const counts = countBy(events, (event) => event.type);
  const policyReasons = countPolicyReasons(events);
  const targets = targetCounts(events);
  const daily = dailySummaries(events);
  const activeDays = new Set(events.map((event) => dayKey(event.timestamp))).size;
  const metrics = Object.fromEntries(
    Object.entries(eventTypes).map(([metric, types]) => [
      metric,
      events.filter((event) => (types as readonly string[]).includes(event.type)).length
    ])
  );

  const analysis: DogfoodAnalysis = {
    file: filePath,
    eventCount: events.length,
    firstEventAt: events.at(0)?.timestamp.toISOString() ?? null,
    lastEventAt: events.at(-1)?.timestamp.toISOString() ?? null,
    activeDays,
    counts,
    metrics,
    policyReasons,
    targets,
    daily,
    gates: [],
    dataQuality: dataQuality(events),
    recommendations: []
  };
  analysis.gates = gateDecisions(analysis, events);
  analysis.recommendations = recommendations(analysis);
  return analysis;
}

function countBy<T>(items: T[], key: (item: T) => string): Record<string, number> {
  return items.reduce<Record<string, number>>((accumulator, item) => {
    const value = key(item);
    accumulator[value] = (accumulator[value] ?? 0) + 1;
    return accumulator;
  }, {});
}

function countPolicyReasons(events: DogfoodEvent[]): Record<string, number> {
  return countByValues(events.filter((event) => policyReasonEvents.has(event.type)), eventPolicyReason);
}

function countByValues(events: DogfoodEvent[], value: (event: DogfoodEvent) => string | null): Record<string, number> {
  return events.reduce<Record<string, number>>((accumulator, event) => {
    const item = value(event);
    if (!item) return accumulator;
    accumulator[item] = (accumulator[item] ?? 0) + 1;
    return accumulator;
  }, {});
}

function targetCounts(events: DogfoodEvent[]): Record<string, number> {
  const targets: Record<string, number> = {};
  for (const event of events) {
    if (!targetLikeEvents.has(event.type)) continue;
    const target = eventTarget(event);
    if (!target) continue;
    targets[target] = (targets[target] ?? 0) + 1;
  }
  return sortRecord(targets);
}

function dataQuality(events: DogfoodEvent[]): DataQualityMetric[] {
  const targetEvents = events.filter((event) => targetLikeEvents.has(event.type));
  const reasonEvents = events.filter((event) => policyReasonEvents.has(event.type));
  const creditEvents = events.filter((event) => creditRemainingEvents.has(event.type));

  return [
    qualityMetric("target coverage", targetEvents.length, targetEvents.filter((event) => eventTarget(event)).length),
    qualityMetric("policy reason coverage", reasonEvents.length, reasonEvents.filter((event) => eventPolicyReason(event)).length),
    qualityMetric("credit remaining coverage", creditEvents.length, creditEvents.filter((event) => event.creditRemaining !== null).length)
  ];
}

function qualityMetric(metric: string, events: number, populated: number): DataQualityMetric {
  return {
    metric,
    events,
    populated,
    coverage: events === 0 ? 1 : Number((populated / events).toFixed(3))
  };
}

function eventTarget(event: DogfoodEvent): string | null {
  return event.target ??
    detailValue(event.detail, "target") ??
    detailValue(event.detail, "package") ??
    (event.detail.includes("=") ? null : event.detail.trim() || null);
}

function eventPolicyReason(event: DogfoodEvent): string | null {
  return event.policyReason ?? detailValue(event.detail, "reason");
}

function detailValue(detail: string, key: string): string | null {
  const match = detail.match(new RegExp(`(?:^|\\s)${escapeRegExp(key)}=([^\\s]+)`));
  return match?.[1] ?? null;
}

function dailySummaries(events: DogfoodEvent[]): DailySummary[] {
  const byDay = new Map<string, DogfoodEvent[]>();
  for (const event of events) {
    const key = dayKey(event.timestamp);
    byDay.set(key, [...(byDay.get(key) ?? []), event]);
  }

  return [...byDay.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([date, dayEvents]) => ({
      date,
      events: dayEvents.length,
      blockedAttempts: countTypes(dayEvents, eventTypes.blockedAttempts),
      policyBlocks: countTypes(dayEvents, eventTypes.policyBlocks),
      emergencyUnlocks: countTypes(dayEvents, eventTypes.emergencyUnlocks),
      freeDays: countTypes(dayEvents, eventTypes.freeDays),
      dailyQuestAdds: countTypes(dayEvents, eventTypes.dailyQuestAdds),
      dailyQuestMockCompletions: countTypes(dayEvents, eventTypes.dailyQuestMockCompletions),
      autoCreditSpends: countTypes(dayEvents, eventTypes.autoCreditSpends)
    }));
}

function countTypes(events: DogfoodEvent[], types: readonly string[]): number {
  return events.filter((event) => types.includes(event.type)).length;
}

function dayKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function recommendations(analysis: DogfoodAnalysis): string[] {
  const metrics = analysis.metrics;
  const notes: string[] = [];

  if (analysis.eventCount === 0) {
    return ["No events found. Run a device dogfood session before making product calls."];
  }

  if (analysis.activeDays < 3) {
    notes.push("Collect at least 3 active dogfood days before deciding whether the blocker loop is sticky.");
  }
  if ((metrics.blockedAttempts ?? 0) < 8) {
    notes.push("Blocked attempts are below the current 8-attempt signal target; keep testing selected-app blocking.");
  }
  if ((metrics.permissionFailures ?? 0) > (metrics.blockedAttempts ?? 0)) {
    notes.push("Permission failures exceed blocked attempts; improve onboarding and permission recovery before adding more features.");
  }
  if ((metrics.dailyQuestAdds ?? 0) > 0 && (metrics.dailyQuestMockCompletions ?? 0) === 0) {
    notes.push("Daily quests are being planned but not completed with proof; test the mock proof loop before GitHub scoring.");
  }
  if ((metrics.emergencyUnlocks ?? 0) > (metrics.freeDays ?? 0) + (metrics.autoCreditSpends ?? 0)) {
    notes.push("Emergency unlocks dominate earned/free usage; policy may be too strict or credit earning may be too slow.");
  }
  if ((metrics.overlayCreditAdds ?? 0) > (metrics.autoCreditSpends ?? 0) + (metrics.dailyQuestMockCompletions ?? 0)) {
    notes.push("Overlay test-credit unlocks dominate proof/usage; consider enabling strict mode during dogfood.");
  }
  for (const gate of analysis.gates) {
    if (gate.status === "fail") {
      notes.push(`Gate ${gate.id} is failing: ${gate.summary}`);
    } else if (gate.status === "needs_data") {
      notes.push(`Gate ${gate.id} needs more data: ${gate.summary}`);
    }
  }

  if (notes.length === 0) {
    notes.push("No obvious dogfood risk flags. Continue collecting sessions and inspect top policy reasons.");
  }
  return notes;
}

function gateDecisions(analysis: DogfoodAnalysis, events: DogfoodEvent[]): GateDecision[] {
  const metrics = analysis.metrics;
  const dogfoodSpanDays = spanDays(analysis.firstEventAt, analysis.lastEventAt);
  const monitorEnabledDays = distinctEventDays(events, eventTypes.monitorEnabledSignals);
  const blockedAttempts = metrics.blockedAttempts ?? 0;
  const emergencyUnlocks = metrics.emergencyUnlocks ?? 0;
  const mockProofCompletions = metrics.dailyQuestMockCompletions ?? 0;
  const foregroundChanges = metrics.foregroundChanges ?? 0;
  const overlayShows = metrics.overlayShows ?? 0;
  const permissionFailures = metrics.permissionFailures ?? 0;

  const gateAChecks: GateCheck[] = [
    {
      label: "Foreground app was observed",
      passed: foregroundChanges > 0,
      actual: foregroundChanges,
      target: "> 0 foreground_changed events"
    },
    {
      label: "Blocking overlay was observed",
      passed: overlayShows > 0 || blockedAttempts > 0,
      actual: overlayShows || blockedAttempts,
      target: "> 0 overlay_shown or blocked_attempt events"
    },
    {
      label: "Permission/service failures are logged when they happen",
      passed: permissionFailures > 0 || analysis.eventCount > 0,
      actual: permissionFailures,
      target: "event log exists; permission_missing should appear if permissions fail"
    }
  ];

  const gateBChecks: GateCheck[] = [
    {
      label: "Monitor enabled days",
      passed: monitorEnabledDays >= 8,
      actual: monitorEnabledDays,
      target: ">= 8 days in a 14-day dogfood window"
    },
    {
      label: "Blocked attempts",
      passed: blockedAttempts >= 8,
      actual: blockedAttempts,
      target: ">= 8 attempts in 14 days (4/week)"
    },
    {
      label: "Emergency unlocks",
      passed: emergencyUnlocks <= 6,
      actual: emergencyUnlocks,
      target: "<= 6 in 14 days (3/week)"
    }
  ];

  const gateCChecks: GateCheck[] = [
    {
      label: "Mock proof completions",
      passed: mockProofCompletions >= 5,
      actual: mockProofCompletions,
      target: ">= 5 local proof completions before real GitHub proof"
    }
  ];

  const gateA = makeGate({
    id: "A",
    title: "Enforcement Viability",
    checks: gateAChecks,
    hasEnoughData: analysis.eventCount > 0,
    passingSummary: "local enforcement signals are present",
    failingSummary: "foreground or overlay evidence is missing"
  });

  const gateB = makeGate({
    id: "B",
    title: "Dogfood Need",
    checks: gateBChecks,
    hasEnoughData: dogfoodSpanDays >= 14 || monitorEnabledDays >= 8,
    passingSummary: "14-day dogfood need signal is strong enough to keep mobile-first",
    failingSummary: "14-day dogfood need signal is weak; consider desktop/browser-first"
  });

  const gateC = makeGate({
    id: "C",
    title: "Developer Proof Supply",
    checks: gateCChecks,
    hasEnoughData: dogfoodSpanDays >= 14 || mockProofCompletions >= 5,
    passingSummary: "local proof behavior is frequent enough to test real GitHub/IDE proof",
    failingSummary: "proof events are too sparse; widen beyond PR-only before Sprint 4"
  });

  if (gateC.status === "pass") {
    gateC.summary = `${gateC.summary}; still requires real GitHub/WakaTime/IDE proof validation`;
  }

  return [gateA, gateB, gateC];
}

function makeGate(input: {
  id: GateDecision["id"];
  title: string;
  checks: GateCheck[];
  hasEnoughData: boolean;
  passingSummary: string;
  failingSummary: string;
}): GateDecision {
  const allPassed = input.checks.every((check) => check.passed);
  const status: GateStatus = allPassed ? "pass" : input.hasEnoughData ? "fail" : "needs_data";
  return {
    id: input.id,
    title: input.title,
    status,
    checks: input.checks,
    summary: status === "pass" ? input.passingSummary :
      status === "fail" ? input.failingSummary :
        "not enough dogfood data yet"
  };
}

function distinctEventDays(events: DogfoodEvent[], types: readonly string[]): number {
  return new Set(
    events
      .filter((event) => types.includes(event.type))
      .map((event) => dayKey(event.timestamp))
  ).size;
}

function spanDays(firstEventAt: string | null, lastEventAt: string | null): number {
  if (!firstEventAt || !lastEventAt) return 0;
  const first = Date.parse(firstEventAt);
  const last = Date.parse(lastEventAt);
  if (Number.isNaN(first) || Number.isNaN(last)) return 0;
  return Math.floor((last - first) / (24 * 60 * 60 * 1000)) + 1;
}

function renderMarkdown(analysis: DogfoodAnalysis): string {
  const lines = [
    "# Android Dogfood Analysis",
    "",
    `File: \`${analysis.file}\``,
    `Events: ${analysis.eventCount}`,
    `Window: ${analysis.firstEventAt ?? "n/a"} -> ${analysis.lastEventAt ?? "n/a"}`,
    `Active days: ${analysis.activeDays}`,
    "",
    "## Core Metrics",
    "",
    table(
      ["Metric", "Count"],
      Object.entries(analysis.metrics).map(([key, value]) => [key, String(value)])
    ),
    "",
    "## Data Quality",
    "",
    table(
      ["Metric", "Events", "Populated", "Coverage"],
      analysis.dataQuality.map((metric) => [
        metric.metric,
        String(metric.events),
        String(metric.populated),
        `${Math.round(metric.coverage * 100)}%`
      ])
    ),
    "",
    "## Gate Snapshot",
    "",
    table(
      ["Gate", "Status", "Summary"],
      analysis.gates.map((gate) => [`Gate ${gate.id}: ${gate.title}`, gate.status, gate.summary])
    ),
    "",
    ...analysis.gates.flatMap((gate) => [
      `### Gate ${gate.id} Checks`,
      "",
      table(
        ["Check", "Actual", "Target", "Pass"],
        gate.checks.map((check) => [
          check.label,
          String(check.actual),
          check.target,
          check.passed ? "yes" : "no"
        ])
      ),
      ""
    ]),
    "## Policy Reasons",
    "",
    recordTable(analysis.policyReasons),
    "",
    "## Top Targets",
    "",
    recordTable(analysis.targets, 10),
    "",
    "## Daily Summary",
    "",
    table(
      ["Date", "Events", "Blocks", "Policy Blocks", "Free Days", "Emergency", "Quest Adds", "Quest Proofs", "Auto Spends"],
      analysis.daily.map((day) => [
        day.date,
        String(day.events),
        String(day.blockedAttempts),
        String(day.policyBlocks),
        String(day.freeDays),
        String(day.emergencyUnlocks),
        String(day.dailyQuestAdds),
        String(day.dailyQuestMockCompletions),
        String(day.autoCreditSpends)
      ])
    ),
    "",
    "## Recommendations",
    "",
    ...analysis.recommendations.map((item) => `- ${item}`)
  ];

  return lines.join("\n");
}

function recordTable(record: Record<string, number>, limit = 20): string {
  const rows = Object.entries(sortRecord(record))
    .slice(0, limit)
    .map(([key, value]) => [key, String(value)]);
  return table(["Value", "Count"], rows);
}

function table(headers: string[], rows: string[][]): string {
  if (rows.length === 0) {
    return "_No data._";
  }

  return [
    `| ${headers.join(" | ")} |`,
    `| ${headers.map(() => "---").join(" | ")} |`,
    ...rows.map((row) => `| ${row.join(" | ")} |`)
  ].join("\n");
}

function sortRecord(record: Record<string, number>): Record<string, number> {
  return Object.fromEntries(
    Object.entries(record).sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]))
  );
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function main(): void {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    process.stdout.write(usage());
    return;
  }

  const inputPath = resolve(options.inputPath ?? newestDogfoodExport());
  if (!existsSync(inputPath)) {
    throw new Error(`Dogfood export not found: ${inputPath}`);
  }

  const events = parseTsv(inputPath);
  const analysis = analyze(basename(inputPath), events);
  process.stdout.write(options.json ? `${JSON.stringify(analysis, null, 2)}\n` : `${renderMarkdown(analysis)}\n`);
}

try {
  main();
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  process.stderr.write(`android-dogfood-analyze: ${message}\n`);
  process.stderr.write(usage());
  process.exit(1);
}
