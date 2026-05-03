export interface MobileCreditState {
  remainingMinutes: number;
  blockedTargets: string[];
  freeUntil?: string;
  strictMode: boolean;
  lastUpdatedAt: string;
}
