export type GitProvider = "github" | "gitlab" | "bitbucket";

export type CreditType =
  | "provisional"
  | "confirmed"
  | "bonus"
  | "clawback"
  | "override"
  | "manual_adjustment";

export interface CreditLedgerEntry {
  userId: string;
  deltaMinutes: number;
  creditType: CreditType;
  sourceId: string;
  reason: string;
  reversible: boolean;
  createdAt: string;
}

export interface ScoreDecision {
  subjectType: "pull_request" | "commit_batch" | "review";
  subjectId: string;
  score: number;
  creditMinutes: number;
  confidence: "low" | "medium" | "high";
  reasons: string[];
  riskFlags: string[];
  needsHumanReview: boolean;
}

export type { MobileCreditState } from "./mobile-credit.js";
