import type { Request, Response } from "express";
import { successResponse } from "../../shared/responses/api-response";
import { conversationService } from "./conversation.service";

const userId = (request: Request) => request.authenticatedUser!.id;

export const conversationController = {
  async list(request: Request, response: Response) {
    response.json(successResponse(await conversationService.list(userId(request))));
  },

  async get(request: Request, response: Response) {
    response.json(successResponse(
      await conversationService.get(String(request.params.conversationId), userId(request))
    ));
  },

  async create(request: Request, response: Response) {
    const conversation = await conversationService.create(userId(request), request.body.title);
    response.status(201).json(successResponse(conversation));
  },

  async update(request: Request, response: Response) {
    const conversation = await conversationService.update(
      String(request.params.conversationId),
      userId(request),
      request.body.title
    );
    response.json(successResponse(conversation));
  },

  async remove(request: Request, response: Response) {
    await conversationService.remove(String(request.params.conversationId), userId(request));
    response.status(204).send();
  }
};
