import { Router } from "express";
import { requireAuthentication } from "../middleware/authentication.middleware";
import { validate } from "../middleware/validation.middleware";
import { aiController } from "../modules/ai/ai.controller";
import { standaloneChatSchema } from "../modules/ai/ai.validation";
import { asyncHandler } from "../shared/utils/async-handler";

export const aiRouter = Router();
aiRouter.use(requireAuthentication);
aiRouter.post("/chat", validate(standaloneChatSchema), asyncHandler(aiController.chat));
