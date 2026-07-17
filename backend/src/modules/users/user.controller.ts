import type { Request, Response } from "express";
import { successResponse } from "../../shared/responses/api-response";
import { userService } from "./user.service";

function authenticatedUserId(request: Request): string {
  return request.authenticatedUser!.id;
}

export const userController = {
  async getProfile(request: Request, response: Response) {
    const user = await userService.getProfile(authenticatedUserId(request));
    response.json(successResponse(user));
  },

  async updateProfile(request: Request, response: Response) {
    const user = await userService.updateProfile(authenticatedUserId(request), request.body);
    response.json(successResponse(user));
  }
};
