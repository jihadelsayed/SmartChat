import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { aiService } from "../ai/ai.service";
import { AI_CONTEXT_MESSAGE_LIMIT } from "../ai/ai.constants";
import { AiProviderError } from "../ai/ai.errors";
import type { AiChatInput } from "../ai/ai.types";
import { DEFAULT_CONVERSATION_TITLE } from "../../config/constants";
import { MessageRequestStatus } from "../../generated/prisma/client";
import { AppError } from "../../shared/errors/app-error";
import { conversationService } from "../conversations/conversation.service";
import { userService } from "../users/user.service";
import {
  AiRequestInProgressError,
  IdempotencyConflictError,
  StoredMessageRequestError
} from "./message.errors";
import { messageRepository } from "./message.repository";
import { resolveUploadPath } from "../../shared/utils/file";

type RecentMessage = Awaited<
  ReturnType<typeof messageRepository.listRecentByConversation>
>[number];

function toAiContext(recentMessages: RecentMessage[]) {
  return recentMessages.map((message) => ({
    role:
      message.sender === "USER"
        ? "user" as const
        : "assistant" as const,
    content: message.content
  }));
}

function normalizeAttachmentIds(attachmentIds: string[]): string[] {
  return [...new Set(attachmentIds)];
}

function messageRequestHash(
  content: string,
  attachmentIds: string[]
): string {
  return createHash("sha256")
    .update(JSON.stringify({
      type: "conversation-message",
      content,
      attachmentIds
    }))
    .digest("hex");
}

async function toAiImages(
  attachments: Array<{ mimeType: string; fileUrl: string }>
) {
  return Promise.all(
    attachments.map(async (attachment) => ({
      mimeType: attachment.mimeType,
      dataBase64: (
        await readFile(resolveUploadPath(attachment.fileUrl))
      ).toString("base64")
    }))
  );
}

function sanitizedAiError(error: unknown): AppError {
  return error instanceof AppError
    ? error
    : new AiProviderError();
}

async function generateAndPersistAssistant(
  conversationId: string,
  content: string,
  userMessage: Awaited<
    ReturnType<typeof messageRepository.createUserMessage>
  >,
  recentMessages: RecentMessage[]
) {
  const assistantContent = await aiService.reply(
    content,
    toAiContext(recentMessages),
    await toAiImages(userMessage.attachments)
  );
  const assistantMessage =
    await messageRepository.createAssistantMessage(
      conversationId,
      assistantContent,
      userMessage.createdAt
    );

  return {
    userMessage,
    assistantMessage
  };
}

async function sendIdempotentMessage(
  conversationId: string,
  userId: string,
  content: string,
  idempotencyKey: string,
  recentMessages: RecentMessage[],
  attachmentIds: string[]
) {
  const requestHash = messageRequestHash(content, attachmentIds);
  const claim = await messageRepository.claimMessageRequest({
    userId,
    conversationId,
    idempotencyKey,
    requestHash,
    userContent: content,
    attachmentIds,
    after: recentMessages.at(-1)?.createdAt
  });
  const request = claim.request;

  if (!claim.claimed) {
    if (request.requestHash !== requestHash) {
      throw new IdempotencyConflictError();
    }

    if (request.status === MessageRequestStatus.PROCESSING) {
      throw new AiRequestInProgressError();
    }

    if (request.status === MessageRequestStatus.SUCCEEDED) {
      if (!request.assistantMessage) {
        throw new AiProviderError();
      }

      return {
        userMessage: request.userMessage,
        assistantMessage: request.assistantMessage
      };
    }

    throw new StoredMessageRequestError(
      request.errorStatusCode ?? 502,
      request.errorCode ?? "AI_PROVIDER_ERROR",
      request.errorMessage ??
        "The AI service is temporarily unavailable. Please try again.",
      request.errorRetryable ?? false,
      request.retryAfter ?? undefined
    );
  }

  let assistantContent: string;

  try {
    assistantContent = await aiService.reply(
      content,
      toAiContext(recentMessages),
      await toAiImages(request.userMessage.attachments)
    );
  } catch (error: unknown) {
    const aiError = sanitizedAiError(error);

    await messageRepository.failMessageRequest(request.id, {
      statusCode: aiError.statusCode,
      code: aiError.code,
      message: aiError.message,
      retryable: aiError.retryable ?? false,
      retryAfter: aiError.retryAfter
    });

    throw aiError;
  }

  const assistantMessage =
    await messageRepository.completeMessageRequest(
      request.id,
      conversationId,
      assistantContent,
      request.userMessage.createdAt
    );

  return {
    userMessage: request.userMessage,
    assistantMessage
  };
}

async function sendToExistingConversation(
  conversationId: string,
  userId: string,
  content: string,
  idempotencyKey?: string,
  inputAttachmentIds: string[] = []
) {
  await conversationService.get(conversationId, userId);

  const normalizedContent = content.trim();
  const attachmentIds = normalizeAttachmentIds(inputAttachmentIds);
  const recentMessages =
    await messageRepository.listRecentByConversation(
      conversationId,
      AI_CONTEXT_MESSAGE_LIMIT - 1
    );

  if (idempotencyKey) {
    return sendIdempotentMessage(
      conversationId,
      userId,
      normalizedContent,
      idempotencyKey,
      recentMessages,
      attachmentIds
    );
  }

  const userMessage = await messageRepository.createUserMessage(
    conversationId,
    userId,
    normalizedContent,
    attachmentIds,
    recentMessages.at(-1)?.createdAt
  );

  return generateAndPersistAssistant(
    conversationId,
    normalizedContent,
    userMessage,
    recentMessages
  );
}

export const messageService = {
  async list(conversationId: string, userId: string) {
    await conversationService.get(conversationId, userId);

    return messageRepository.listByConversation(conversationId);
  },

  async send(
    conversationId: string,
    userId: string,
    content: string,
    idempotencyKey?: string,
    attachmentIds: string[] = []
  ) {
    return sendToExistingConversation(
      conversationId,
      userId,
      content,
      idempotencyKey,
      attachmentIds
    );
  },

  async sendFromAiChat(input: AiChatInput) {
    await userService.getProfile(input.userId);

    if (input.conversationId) {
      return sendToExistingConversation(
        input.conversationId,
        input.userId,
        input.message
      );
    }

    const { conversation, userMessage } =
      await messageRepository.createConversationWithFirstUserMessage(
        input.userId,
        DEFAULT_CONVERSATION_TITLE,
        input.message
      );

    return generateAndPersistAssistant(
      conversation.id,
      input.message,
      userMessage,
      []
    );
  }
};
