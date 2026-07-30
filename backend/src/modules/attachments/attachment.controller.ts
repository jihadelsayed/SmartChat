import { randomUUID } from "node:crypto";
import type { Request, Response } from "express";
import { AppError } from "../../shared/errors/app-error";
import { successResponse } from "../../shared/responses/api-response";
import {
  createUploadUrl,
  deleteUploadedFile
} from "../../shared/utils/file";
import {
  mapAttachmentToResponse,
  mapAttachmentsToResponse
} from "./attachment.mapper";
import { attachmentService } from "./attachment.service";
import { validateAttachmentFile } from "./attachment-file";

async function persistUpload(
  request: Request,
  response: Response,
  messageId?: string
) {
  if (!request.file) {
    throw new AppError(
      "An attachment file is required",
      400,
      "ATTACHMENT_FILE_REQUIRED"
    );
  }

  const fileUrl = createUploadUrl(request.file.filename);

  try {
    const validated = await validateAttachmentFile(
      request.file.path,
      request.file.mimetype
    );
    const result = await attachmentService.create(
      request.authenticatedUser!.id,
      {
        messageId,
        clientAttachmentId:
          request.header("x-client-attachment-id") ?? randomUUID(),
        fileName: request.file.originalname,
        mimeType: request.file.mimetype,
        fileUrl,
        sizeBytes: request.file.size,
        contentHash: validated.contentHash
      }
    );

    if (!result.created) {
      await deleteUploadedFile(fileUrl);
    }

    response
      .status(result.created ? 201 : 200)
      .json(successResponse(mapAttachmentToResponse(result.attachment)));
  } catch (error: unknown) {
    try {
      await deleteUploadedFile(fileUrl);
    } catch (cleanupError: unknown) {
      console.error("Failed to clean up rejected attachment upload", cleanupError);
    }
    throw error;
  }
}

export const attachmentController = {
  async listByMessage(request: Request, response: Response) {
    const attachments = await attachmentService.listByMessage(
      String(request.params.messageId),
      request.authenticatedUser!.id
    );

    response
      .status(200)
      .json(successResponse(mapAttachmentsToResponse(attachments)));
  },

  async upload(request: Request, response: Response) {
    await persistUpload(request, response, String(request.params.messageId));
  },

  async uploadStaged(request: Request, response: Response) {
    await persistUpload(request, response);
  },

  async getById(request: Request, response: Response) {
    const attachment = await attachmentService.getById(
      String(request.params.attachmentId),
      request.authenticatedUser!.id
    );

    response
      .status(200)
      .json(successResponse(mapAttachmentToResponse(attachment)));
  },

  async remove(request: Request, response: Response) {
    const attachment = await attachmentService.remove(
      String(request.params.attachmentId),
      request.authenticatedUser!.id
    );

    response
      .status(200)
      .json(successResponse(mapAttachmentToResponse(attachment)));
  }
};
