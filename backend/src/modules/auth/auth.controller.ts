import type { Request, Response } from "express";
import { successResponse } from "../../shared/responses/api-response";
import { authService } from "./auth.service";

export const authController = {
  async register(request: Request, response: Response) {
    const result = await authService.register(request.body);
    response.status(201).json(successResponse(result));
  },

  async login(request: Request, response: Response) {
    const result = await authService.login(request.body);
    response.json(successResponse(result));
  }
};
