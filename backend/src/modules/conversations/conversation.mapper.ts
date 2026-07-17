import { mapMessagesToResponse } from "../messages/message.mapper";
import type {
  ConversationDetailRecord,
  ConversationDetailResponse,
  ConversationSummaryRecord,
  ConversationSummaryResponse
} from "./conversation.types";

export function mapConversationSummaryToResponse(
  conversation: ConversationSummaryRecord
): ConversationSummaryResponse {
  return {
    id: conversation.id,
    userId: conversation.userId,
    title: conversation.title,
    createdAt: conversation.createdAt.toISOString(),
    updatedAt: conversation.updatedAt.toISOString(),
    messageCount: conversation._count.messages
  };
}

export function mapConversationSummariesToResponse(
  conversations: ConversationSummaryRecord[]
): ConversationSummaryResponse[] {
  return conversations.map(mapConversationSummaryToResponse);
}

export function mapConversationDetailToResponse(
  conversation: ConversationDetailRecord
): ConversationDetailResponse {
  return {
    id: conversation.id,
    userId: conversation.userId,
    title: conversation.title,
    createdAt: conversation.createdAt.toISOString(),
    updatedAt: conversation.updatedAt.toISOString(),
    messages: mapMessagesToResponse(conversation.messages)
  };
}