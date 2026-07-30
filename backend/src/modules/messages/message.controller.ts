import type { Request, Response } from "express";
import { successResponse } from "../../shared/responses/api-response";
import { messageService } from "./message.service";

export const messageController = {
  async list(request: Request, response: Response) {
    const messages = await messageService.list(
      String(request.params.conversationId),
      request.authenticatedUser!.id
    );

    response.status(200).json(successResponse(messages));
  },

  async create(request: Request, response: Response) {
    const idempotencyKey = (
      request.header("idempotency-key") ??
      request.body.clientRequestId
    )?.toLowerCase();
    const result = await messageService.send(
      String(request.params.conversationId),
      request.authenticatedUser!.id,
      request.body.content,
      idempotencyKey,
      request.body.attachmentIds
    );

    response.status(201).json(successResponse(result));
  }
};
