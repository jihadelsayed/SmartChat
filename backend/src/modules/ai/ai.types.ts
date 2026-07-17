import type { AiProviderName } from "./ai.constants";

export interface AiChatInput {
  message: string;
  conversationId?: string;
  userId?: string;
}

export interface AiProviderRequest {
  message: string;
  systemPrompt?: string;
  model?: string;
}

export interface AiProviderResponse {
  reply: string;
  provider: AiProviderName;
  model: string;
}

export interface AiErrorDetails {
  provider: AiProviderName;
  statusCode?: number;
  retryable: boolean;
  originalMessage?: string;
}