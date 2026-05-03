import type { FastifyInstance, FastifyRequest } from "fastify";
import { scorePullRequest, type ChangedFile, type PullRequestSignals } from "@commit-to-unlock/scoring";
import { isBotLogin, verifyGitHubSignature } from "../github/webhook.js";

interface GitHubPullRequestWebhook {
  action: string;
  pull_request?: {
    id: number;
    number: number;
    merged?: boolean;
    html_url?: string;
    body?: string | null;
    user?: {
      login?: string;
      type?: string;
    };
  };
  repository?: {
    full_name?: string;
  };
}

export async function registerGitHubWebhookRoutes(
  app: FastifyInstance,
  options: { webhookSecret: string }
): Promise<void> {
  app.post(
    "/webhooks/github",
    {
      config: {
        rawBody: true
      }
    },
    async (request, reply) => {
      const rawBody = getRawBody(request);
      const signatureValid = verifyGitHubSignature({
        payload: rawBody,
        signatureHeader: request.headers["x-hub-signature-256"] as string | undefined,
        secret: options.webhookSecret
      });

      if (!signatureValid) {
        return reply.code(401).send({ error: "invalid_signature" });
      }

      const eventType = request.headers["x-github-event"];
      const deliveryId = request.headers["x-github-delivery"];

      if (eventType !== "pull_request") {
        return reply.code(202).send({ accepted: true, deliveryId, ignored: eventType });
      }

      const body = request.body as GitHubPullRequestWebhook;

      if (!body.pull_request || !body.repository) {
        return reply.code(400).send({ error: "invalid_pull_request_payload" });
      }

      const signals = mapPullRequestWebhook(body);
      const decision = scorePullRequest(signals);

      // This is intentionally in-memory for the first scaffold. The next step is
      // persisting inbound events, feature vectors, score decisions, and ledger rows.
      return reply.code(202).send({
        accepted: true,
        deliveryId,
        repository: body.repository.full_name,
        pullRequest: body.pull_request.number,
        decision
      });
    }
  );
}

function mapPullRequestWebhook(body: GitHubPullRequestWebhook): PullRequestSignals {
  const pullRequest = body.pull_request;
  if (!pullRequest) {
    throw new Error("pull_request is required");
  }

  return {
    subjectId: `github:${pullRequest.id}`,
    eventType: pullRequest.merged ? "pull_request_merged" : "pull_request_updated",
    changedFiles: placeholderChangedFiles(),
    issueLinked: hasIssueLink(pullRequest.body),
    ciPassed: false,
    approvals: 0,
    reviewComments: 0,
    discussionsResolved: 0,
    revertDetected: false,
    duplicatePatchRisk: "low",
    authorIsBot: pullRequest.user?.type === "Bot" || isBotLogin(pullRequest.user?.login)
  };
}

function placeholderChangedFiles(): ChangedFile[] {
  return [
    {
      path: "unknown",
      additions: 0,
      deletions: 0,
      changes: 0,
      status: "modified"
    }
  ];
}

function hasIssueLink(body: string | null | undefined): boolean {
  if (!body) return false;
  return /(close[sd]?|fix(e[sd])?|resolve[sd]?)\s+#\d+/i.test(body) || /#\d+/.test(body);
}

function getRawBody(request: FastifyRequest): Buffer {
  const rawBody = (request as FastifyRequest & { rawBody?: Buffer }).rawBody;
  if (rawBody) return rawBody;
  return Buffer.from(JSON.stringify(request.body ?? {}));
}
