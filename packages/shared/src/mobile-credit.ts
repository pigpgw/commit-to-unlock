export interface MobileCreditState {
  remainingMinutes: number;
  blockedTargets: string[];
  freeUntil?: string;
  strictMode: boolean;
  lastUpdatedAt: string;
}

export type MobileDailyQuestStatus = "planned" | "proof_seen" | "completed" | "rejected";

export type MobileDailyQuestProofType = "commit" | "pull_request" | "review" | "mock";

export interface MobileDailyQuest {
  id: string;
  title: string;
  required: boolean;
  proofType?: MobileDailyQuestProofType;
  proofRef?: string;
  status: MobileDailyQuestStatus;
  createdAt: string;
  completedAt?: string;
}
