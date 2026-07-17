import {
  mapAttachmentsToResponse
} from "../attachments/attachment.mapper";
import type {
  MessageRecord,
  MessageResponse
} from "./message.types";

export function mapMessageToResponse(
  message: MessageRecord
): MessageResponse {
  return {
    id: message.id,
    conversationId: message.conversationId,
    sender: message.sender,
    content: message.content,
    createdAt: message.createdAt.toISOString(),
    attachments: mapAttachmentsToResponse(message.attachments)
  };
}

export function mapMessagesToResponse(
  messages: MessageRecord[]
): MessageResponse[] {
  return messages.map(mapMessageToResponse);
}