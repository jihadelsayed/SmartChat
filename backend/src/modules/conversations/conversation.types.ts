import type { MessageRecord, MessageResponse } from "../messages/message.types";

export interface ConversationRecord {
  id: string;
  userId: string;
  title: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface ConversationSummaryRecord extends ConversationRecord {
  _count: {
    messages: number;
  };
}

export interface ConversationDetailRecord extends ConversationRecord {
  messages: MessageRecord[];
}

export interface ConversationSummaryResponse {
  id: string;
  userId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

export interface ConversationDetailResponse {
  id: string;
  userId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: MessageResponse[];
}