import type { MessageRecord } from "../messages/message.types";
import type { AiChatMessageResponse, AiChatResponse } from "./ai.types";

function mapAiChatMessage(message: MessageRecord): AiChatMessageResponse {
  return {
    id: message.id,
    conversationId: message.conversationId,
    role: message.sender,
    content: message.content,
    createdAt: message.createdAt.toISOString()
  };
}

export function mapAiChatResponse(
  userMessage: MessageRecord,
  assistantMessage: MessageRecord
): AiChatResponse {
  return {
    reply: assistantMessage.content,
    userMessage: mapAiChatMessage(userMessage),
    assistantMessage: mapAiChatMessage(assistantMessage)
  };
}
