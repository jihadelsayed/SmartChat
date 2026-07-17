import type {
  Attachment,
  MessageSender
} from "../../generated/prisma/client";
import type { AttachmentResponse } from "../attachments/attachment.mapper";

export interface MessageRecord {
  id: string;
  conversationId: string;
  sender: MessageSender;
  content: string;
  createdAt: Date;
  attachments: Attachment[];
}

export interface MessageResponse {
  id: string;
  conversationId: string;
  sender: MessageSender;
  content: string;
  createdAt: string;
  attachments: AttachmentResponse[];
}

export interface SendMessageResponse {
  userMessage: MessageResponse;
  assistantMessage: MessageResponse;
}