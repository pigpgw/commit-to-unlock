export interface PolicyCreditState {
  remainingMinutes: number;
  freeUntil?: string;
  strictMode: boolean;
  lastUpdatedAt: string;
}

export interface PolicyState {
  blockedTargets: string[];
  activeWeekdays: number[];
  activeFrom?: string;
  activeUntil?: string;
  applyOnPublicHolidays: boolean;
  manualHolidayToday: boolean;
  timezone: string;
}

export interface EmergencyUnlock {
  id: string;
  durationMinutes: 5 | 15 | 30;
  reason: string;
  startedAt: string;
  expiresAt: string;
}

export type PolicyDecisionReason =
  | "own_app"
  | "target_not_blocked"
  | "inactive_weekday"
  | "outside_active_time"
  | "manual_holiday"
  | "public_holiday"
  | "free_day"
  | "emergency_unlock"
  | "credit_available"
  | "credit_empty";

export interface PolicyDecision {
  allowed: boolean;
  reason: PolicyDecisionReason;
  shouldSpendCredit: boolean;
  matchedTarget?: string;
  activeEmergencyUnlockId?: string;
}

export interface PolicyDecisionInput {
  currentPackage: string | null;
  ownPackage: string;
  now: Date;
  creditState: PolicyCreditState;
  policyState: PolicyState;
  activeEmergencyUnlocks?: EmergencyUnlock[];
  isPublicHoliday?: boolean;
}

export function evaluatePolicyDecision(input: PolicyDecisionInput): PolicyDecision {
  const currentPackage = input.currentPackage;

  if (!currentPackage || currentPackage === input.ownPackage) {
    return allow("own_app");
  }

  if (!input.policyState.blockedTargets.includes(currentPackage)) {
    return allow("target_not_blocked");
  }

  const localNow = localDateTime(input.now, input.policyState.timezone);

  if (!isActiveWeekday(localNow, input.policyState.activeWeekdays)) {
    return allow("inactive_weekday", currentPackage);
  }

  if (!isWithinActiveTime(localNow, input.policyState)) {
    return allow("outside_active_time", currentPackage);
  }

  if (input.policyState.manualHolidayToday) {
    return allow("manual_holiday", currentPackage);
  }

  if (input.isPublicHoliday && !input.policyState.applyOnPublicHolidays) {
    return allow("public_holiday", currentPackage);
  }

  if (isFutureIso(input.creditState.freeUntil, input.now)) {
    return allow("free_day", currentPackage);
  }

  const activeEmergencyUnlock = input.activeEmergencyUnlocks?.find((unlock) =>
    isEmergencyUnlockActive(unlock, input.now)
  );
  if (activeEmergencyUnlock) {
    return {
      allowed: true,
      reason: "emergency_unlock",
      shouldSpendCredit: false,
      matchedTarget: currentPackage,
      activeEmergencyUnlockId: activeEmergencyUnlock.id
    };
  }

  if (input.creditState.remainingMinutes > 0) {
    return {
      allowed: true,
      reason: "credit_available",
      shouldSpendCredit: true,
      matchedTarget: currentPackage
    };
  }

  return {
    allowed: false,
    reason: "credit_empty",
    shouldSpendCredit: false,
    matchedTarget: currentPackage
  };
}

function allow(reason: PolicyDecisionReason, matchedTarget?: string): PolicyDecision {
  return {
    allowed: true,
    reason,
    shouldSpendCredit: false,
    matchedTarget
  };
}

interface LocalDateTime {
  weekday: number;
  hour: number;
  minute: number;
}

function isActiveWeekday(now: LocalDateTime, activeWeekdays: number[]): boolean {
  if (activeWeekdays.length === 0) return false;
  return activeWeekdays.includes(now.weekday);
}

function isWithinActiveTime(now: LocalDateTime, policy: PolicyState): boolean {
  if (!policy.activeFrom && !policy.activeUntil) return true;

  const currentMinutes = now.hour * 60 + now.minute;
  const fromMinutes = policy.activeFrom ? parseTimeToMinutes(policy.activeFrom) : 0;
  const untilMinutes = policy.activeUntil ? parseTimeToMinutes(policy.activeUntil) : 24 * 60;

  if (fromMinutes === untilMinutes) return true;
  if (fromMinutes < untilMinutes) {
    return currentMinutes >= fromMinutes && currentMinutes < untilMinutes;
  }

  return currentMinutes >= fromMinutes || currentMinutes < untilMinutes;
}

function localDateTime(now: Date, timezone: string): LocalDateTime {
  const resolvedTimeZone = resolveTimeZone(timezone);
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: resolvedTimeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23"
  }).formatToParts(now);

  const value = (type: string): number => {
    const part = parts.find((item) => item.type === type)?.value;
    const parsed = Number.parseInt(part ?? "", 10);
    if (!Number.isInteger(parsed)) {
      throw new Error(`Unable to resolve ${type} for timezone: ${timezone}`);
    }
    return parsed;
  };

  const year = value("year");
  const month = value("month");
  const day = value("day");
  const jsDay = new Date(Date.UTC(year, month - 1, day)).getUTCDay();

  return {
    weekday: jsDay === 0 ? 7 : jsDay,
    hour: value("hour"),
    minute: value("minute")
  };
}

function resolveTimeZone(timezone: string): string {
  try {
    Intl.DateTimeFormat(undefined, { timeZone: timezone }).format();
    return timezone;
  } catch {
    return "UTC";
  }
}

function parseTimeToMinutes(value: string): number {
  const [hourPart, minutePart] = value.split(":");
  const hours = Number.parseInt(hourPart ?? "", 10);
  const minutes = Number.parseInt(minutePart ?? "", 10);

  if (
    !Number.isInteger(hours) ||
    !Number.isInteger(minutes) ||
    hours < 0 ||
    hours > 23 ||
    minutes < 0 ||
    minutes > 59
  ) {
    throw new Error(`Invalid HH:mm time value: ${value}`);
  }

  return hours * 60 + minutes;
}

function isFutureIso(value: string | undefined, now: Date): boolean {
  if (!value) return false;
  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) return false;
  return timestamp > now.getTime();
}

function isEmergencyUnlockActive(unlock: EmergencyUnlock, now: Date): boolean {
  return Date.parse(unlock.startedAt) <= now.getTime() &&
    Date.parse(unlock.expiresAt) > now.getTime();
}
