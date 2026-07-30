import { Prisma } from "../../generated/prisma/client";
import { AppError } from "../../shared/errors/app-error";
import { NotFoundError } from "../../shared/errors/not-found-error";
import { deleteUploadedFile } from "../../shared/utils/file";
import { attachmentRepository } from "./attachment.repository";
import type { CreateAttachmentInput } from "./attachment.types";

export const attachmentService = {
  async listByMessage(messageId: string, userId: string) {
    const message = await attachmentRepository.findMessageForUser(
      messageId,
      userId
    );

    if (!message) {
      throw new NotFoundError("Message");
    }

    return attachmentRepository.listByMessage(messageId);
  },

  async create(userId: string, input: CreateAttachmentInput) {
    if (input.messageId) {
      const message = await attachmentRepository.findMessageForUser(
        input.messageId,
        userId
      );

      if (!message) {
        throw new NotFoundError("Message");
      }
    }

    const existing = await attachmentRepository.findByClientAttachmentId(
      userId,
      input.clientAttachmentId
    );
    if (existing) {
      return {
        attachment: validateReplay(existing, input),
        created: false
      };
    }

    try {
      return {
        attachment: await attachmentRepository.create(userId, input),
        created: true
      };
    } catch (error: unknown) {
      if (
        !(error instanceof Prisma.PrismaClientKnownRequestError) ||
        error.code !== "P2002"
      ) {
        throw error;
      }
      const concurrent =
        await attachmentRepository.findByClientAttachmentId(
          userId,
          input.clientAttachmentId
        );
      if (!concurrent) throw error;
      return {
        attachment: validateReplay(concurrent, input),
        created: false
      };
    }
  },

  async getById(attachmentId: string, userId: string) {
    const attachment = await attachmentRepository.findByIdForUser(
      attachmentId,
      userId
    );

    if (!attachment) {
      throw new NotFoundError("Attachment");
    }

    return attachment;
  },

  async remove(attachmentId: string, userId: string) {
    const attachment = await attachmentRepository.findByIdForUser(
      attachmentId,
      userId
    );

    if (!attachment) {
      throw new NotFoundError("Attachment");
    }

    if (attachment.messageId) {
      throw new AppError(
        "An attachment already associated with a message cannot be deleted.",
        409,
        "ATTACHMENT_ALREADY_ASSOCIATED",
        undefined,
        { retryable: false }
      );
    }

    await attachmentRepository.deleteById(attachment.id);

    try {
      await deleteUploadedFile(attachment.fileUrl);
    } catch (error: unknown) {
      console.error(
        `Failed to delete attachment file ${attachment.fileUrl}`,
        error
      );
    }

    return attachment;
  }
};

function validateReplay(
  existing: Awaited<
    ReturnType<typeof attachmentRepository.findByClientAttachmentId>
  > & {},
  input: CreateAttachmentInput
) {
  if (
    existing.contentHash !== input.contentHash ||
    existing.mimeType !== input.mimeType ||
    existing.sizeBytes !== input.sizeBytes ||
    existing.fileName !== input.fileName
  ) {
    throw new AppError(
      "The client attachment ID was already used for a different file.",
      409,
      "ATTACHMENT_ID_CONFLICT",
      undefined,
      { retryable: false }
    );
  }
  return existing;
}
