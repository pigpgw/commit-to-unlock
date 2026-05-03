const SOURCE_EXTENSIONS = new Set([
  ".c",
  ".cc",
  ".cpp",
  ".cs",
  ".css",
  ".go",
  ".html",
  ".java",
  ".js",
  ".jsx",
  ".kt",
  ".m",
  ".mm",
  ".php",
  ".py",
  ".rb",
  ".rs",
  ".scala",
  ".scss",
  ".swift",
  ".ts",
  ".tsx",
  ".vue"
]);

const DOC_EXTENSIONS = new Set([".md", ".mdx", ".rst", ".txt", ".adoc"]);
const CONFIG_FILENAMES = new Set([
  ".eslintrc",
  ".prettierrc",
  "dockerfile",
  "compose.yaml",
  "docker-compose.yml",
  "tsconfig.json",
  "package.json",
  "vite.config.ts",
  "next.config.js"
]);
const LOCK_FILENAMES = new Set([
  "package-lock.json",
  "pnpm-lock.yaml",
  "yarn.lock",
  "bun.lockb",
  "poetry.lock",
  "pipfile.lock",
  "gemfile.lock",
  "cargo.lock",
  "go.sum"
]);

export interface FileClassification {
  isSource: boolean;
  isTest: boolean;
  isDocs: boolean;
  isConfig: boolean;
  isLockfile: boolean;
  isGenerated: boolean;
  isVendor: boolean;
}

export function classifyPath(path: string): FileClassification {
  const normalized = path.toLowerCase();
  const filename = normalized.split("/").at(-1) ?? normalized;
  const extension = getExtension(filename);

  const isVendor =
    normalized.startsWith("vendor/") ||
    normalized.startsWith("third_party/") ||
    normalized.includes("/vendor/") ||
    normalized.includes("/node_modules/") ||
    normalized.includes("/third_party/");
  const isGenerated =
    normalized.startsWith("generated/") ||
    normalized.includes("generated/") ||
    normalized.includes("/generated/") ||
    normalized.includes(".generated.") ||
    normalized.endsWith(".gen.ts") ||
    normalized.endsWith(".pb.go") ||
    normalized.endsWith(".min.js");
  const isLockfile = LOCK_FILENAMES.has(filename);
  const isTest =
    normalized.includes("__tests__/") ||
    normalized.includes("/test/") ||
    normalized.includes("/tests/") ||
    normalized.includes("/spec/") ||
    filename.includes(".test.") ||
    filename.includes(".spec.");
  const isDocs = DOC_EXTENSIONS.has(extension) || normalized.startsWith("docs/");
  const isConfig = CONFIG_FILENAMES.has(filename) || normalized.startsWith(".github/");
  const isSource = SOURCE_EXTENSIONS.has(extension) && !isVendor && !isGenerated;

  return { isSource, isTest, isDocs, isConfig, isLockfile, isGenerated, isVendor };
}

function getExtension(filename: string): string {
  const dotIndex = filename.lastIndexOf(".");
  return dotIndex >= 0 ? filename.slice(dotIndex) : "";
}
