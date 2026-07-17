import type { Request, Response } from "express";
import { successResponse } from "../../shared/responses/api-response";
import { aiService } from "./ai.service";

export const aiController = {
  async chat(request: Request, response: Response) {
    response.json(successResponse({ reply: await aiService.reply(request.body.message) }));
  }
};
