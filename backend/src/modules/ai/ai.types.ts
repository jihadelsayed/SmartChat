import type { MessageSender } from "../../generated/prisma/client";

export interface AiChatInput {
  message: string;
  conversationId?: string;
  userId: string;
}

export interface AiChatMessageResponse {
  id: string;
  conversationId: string;
  role: MessageSender;
  content: string;
  createdAt: string;
}

export interface AiChatResponse {
  reply: string;
  userMessage: AiChatMessageResponse;
  assistantMessage: AiChatMessageResponse;
}

export interface AiContextMessage {
  role: "user" | "assistant";
  content: string;
}

export interface AiProviderRequest {
  message: string;
  messages: AiContextMessage[];
  systemPrompt?: string;
  model?: string;
  images?: AiImageInput[];
}

export interface AiImageInput {
  mimeType: string;
  dataBase64: string;
}
