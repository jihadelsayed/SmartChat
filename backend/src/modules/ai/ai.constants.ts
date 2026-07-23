export const AI_PROVIDERS = {
  MOCK: "mock",
  OPENAI: "openai",
  GEMINI: "gemini"
} as const;

export type AiProviderName =
  (typeof AI_PROVIDERS)[keyof typeof AI_PROVIDERS];

export const DEFAULT_AI_PROVIDER: AiProviderName =
  AI_PROVIDERS.MOCK;

export const MOCK_AI_MODEL = "smartchat-mock-v1";

export const AI_REQUEST_TIMEOUT_MS = 30_000;

export const AI_MAXIMUM_RETRIES = 2;