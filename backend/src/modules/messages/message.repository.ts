import { prisma } from "../../database/prisma";
import {
  MessageRequestStatus,
  MessageSender,
  AttachmentStatus,
  Prisma
} from "../../generated/prisma/client";
import { AppError } from "../../shared/errors/app-error";

const attachments = {
  attachments: true
} as const;

const messageRequestMessages = {
  userMessage: {
    include: attachments
  },
  assistantMessage: {
    include: attachments
  }
} as const;

function nextCreatedAt(after?: Date) {
  return new Date(
    Math.max(Date.now(), after ? after.getTime() + 1 : 0)
  );
}

export interface ClaimMessageRequestInput {
  userId: string;
  conversationId: string;
  idempotencyKey: string;
  requestHash: string;
  userContent: string;
  attachmentIds: string[];
  after?: Date;
}

export interface PersistedMessageRequestError {
  statusCode: number;
  code: string;
  message: string;
  retryable: boolean;
  retryAfter?: string;
}

async function associateAttachments(
  transaction: Prisma.TransactionClient,
  userId: string,
  messageId: string,
  attachmentIds: string[]
) {
  if (attachmentIds.length === 0) return;

  const attachmentsToAssociate = await transaction.attachment.findMany({
    where: {
      id: { in: attachmentIds },
      userId,
      status: AttachmentStatus.UPLOADED
    },
    select: {
      id: true,
      messageId: true
    }
  });
  if (
    attachmentsToAssociate.length !== attachmentIds.length ||
    attachmentsToAssociate.some((attachment) => attachment.messageId !== null)
  ) {
    throw new AppError(
      "One or more attachments are unavailable for this message.",
      409,
      "ATTACHMENT_UNAVAILABLE",
      undefined,
      { retryable: false }
    );
  }

  const updated = await transaction.attachment.updateMany({
    where: {
      id: { in: attachmentIds },
      userId,
      status: AttachmentStatus.UPLOADED,
      messageId: null
    },
    data: { messageId }
  });
  if (updated.count !== attachmentIds.length) {
    throw new AppError(
      "One or more attachments were already associated.",
      409,
      "ATTACHMENT_ASSOCIATION_CONFLICT",
      undefined,
      { retryable: false }
    );
  }
}

export const messageRepository = {
  listByConversation(conversationId: string) {
    return prisma.message.findMany({
      where: { conversationId },
      orderBy: [
        { createdAt: "asc" },
        { id: "asc" }
      ],
      include: attachments
    });
  },

  async listRecentByConversation(
    conversationId: string,
    limit: number
  ) {
    const messages = await prisma.message.findMany({
      where: { conversationId },
      orderBy: [
        { createdAt: "desc" },
        { id: "desc" }
      ],
      take: limit,
      select: {
        sender: true,
        content: true,
        createdAt: true
      }
    });

    return messages.reverse();
  },

  createUserMessage(
    conversationId: string,
    userId: string,
    userContent: string,
    attachmentIds: string[],
    after?: Date
  ) {
    return prisma.$transaction(async (transaction) => {
      const userCreatedAt = nextCreatedAt(after);
      const createdMessage = await transaction.message.create({
        data: {
          conversationId,
          sender: MessageSender.USER,
          content: userContent,
          createdAt: userCreatedAt
        },
      });
      await associateAttachments(
        transaction,
        userId,
        createdMessage.id,
        attachmentIds
      );

      await transaction.conversation.update({
        where: { id: conversationId },
        data: { updatedAt: userCreatedAt }
      });

      return transaction.message.findUniqueOrThrow({
        where: { id: createdMessage.id },
        include: attachments
      });
    });
  },

  createConversationWithFirstUserMessage(
    userId: string,
    title: string,
    userContent: string
  ) {
    return prisma.$transaction(async (transaction) => {
      const userCreatedAt = nextCreatedAt();
      const conversation = await transaction.conversation.create({
        data: {
          userId,
          title,
          createdAt: userCreatedAt,
          updatedAt: userCreatedAt
        }
      });
      const userMessage = await transaction.message.create({
        data: {
          conversationId: conversation.id,
          sender: MessageSender.USER,
          content: userContent,
          createdAt: userCreatedAt
        },
        include: attachments
      });

      return {
        conversation,
        userMessage
      };
    });
  },

  createAssistantMessage(
    conversationId: string,
    assistantContent: string,
    after: Date
  ) {
    return prisma.$transaction(async (transaction) => {
      const assistantCreatedAt = nextCreatedAt(after);
      const assistantMessage = await transaction.message.create({
        data: {
          conversationId,
          sender: MessageSender.ASSISTANT,
          content: assistantContent,
          createdAt: assistantCreatedAt
        },
        include: attachments
      });

      await transaction.conversation.update({
        where: { id: conversationId },
        data: { updatedAt: assistantCreatedAt }
      });

      return assistantMessage;
    });
  },

  async claimMessageRequest(input: ClaimMessageRequestInput) {
    try {
      const request = await prisma.$transaction(
        async (transaction) => {
          const userCreatedAt = nextCreatedAt(input.after);
          const userMessage =
            await transaction.message.create({
              data: {
                conversationId: input.conversationId,
                sender: MessageSender.USER,
                content: input.userContent,
                createdAt: userCreatedAt
              },
            });
          await associateAttachments(
            transaction,
            input.userId,
            userMessage.id,
            input.attachmentIds
          );

          const createdRequest =
            await transaction.messageRequest.create({
              data: {
                userId: input.userId,
                conversationId: input.conversationId,
                idempotencyKey: input.idempotencyKey,
                requestHash: input.requestHash,
                status: MessageRequestStatus.PROCESSING,
                userMessageId: userMessage.id
              },
              include: messageRequestMessages
            });

          await transaction.conversation.update({
            where: { id: input.conversationId },
            data: { updatedAt: userCreatedAt }
          });

          return createdRequest;
        }
      );

      return {
        claimed: true as const,
        request
      };
    } catch (error: unknown) {
      if (
        !(
          error instanceof
            Prisma.PrismaClientKnownRequestError
        ) ||
        error.code !== "P2002"
      ) {
        throw error;
      }

      const request = await prisma.messageRequest.findUnique({
        where: {
          userId_conversationId_idempotencyKey: {
            userId: input.userId,
            conversationId: input.conversationId,
            idempotencyKey: input.idempotencyKey
          }
        },
        include: messageRequestMessages
      });

      if (!request) {
        throw error;
      }

      return {
        claimed: false as const,
        request
      };
    }
  },

  completeMessageRequest(
    requestId: string,
    conversationId: string,
    assistantContent: string,
    after: Date
  ) {
    return prisma.$transaction(async (transaction) => {
      const assistantCreatedAt = nextCreatedAt(after);
      const assistantMessage = await transaction.message.create({
        data: {
          conversationId,
          sender: MessageSender.ASSISTANT,
          content: assistantContent,
          createdAt: assistantCreatedAt
        },
        include: attachments
      });

      await transaction.messageRequest.update({
        where: { id: requestId },
        data: {
          status: MessageRequestStatus.SUCCEEDED,
          assistantMessageId: assistantMessage.id
        }
      });

      await transaction.conversation.update({
        where: { id: conversationId },
        data: { updatedAt: assistantCreatedAt }
      });

      return assistantMessage;
    });
  },

  failMessageRequest(
    requestId: string,
    error: PersistedMessageRequestError
  ) {
    return prisma.messageRequest.update({
      where: { id: requestId },
      data: {
        status: MessageRequestStatus.FAILED,
        errorStatusCode: error.statusCode,
        errorCode: error.code,
        errorMessage: error.message,
        errorRetryable: error.retryable,
        retryAfter: error.retryAfter
      }
    });
  }
};
