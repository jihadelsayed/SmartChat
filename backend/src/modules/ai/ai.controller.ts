import type { Request, Response } from "express";
import { successResponse } from "../../shared/responses/api-response";
import { messageService } from "../messages/message.service";
import { mapAiChatResponse } from "./ai.mapper";

export const aiController = {
  async chat(request: Request, response: Response) {
    const { userMessage, assistantMessage } =
      await messageService.sendFromAiChat({
        userId: request.authenticatedUser!.id,
        message: request.body.message,
        conversationId: request.body.conversationId
      });

    response.json(
      successResponse(mapAiChatResponse(userMessage, assistantMessage))
    );
  }
};
