import { Router } from "express";
import { requireAuthentication } from "../middleware/authentication.middleware";
import { validate } from "../middleware/validation.middleware";
import { conversationController } from "../modules/conversations/conversation.controller";
import {
  conversationIdSchema,
  createConversationSchema,
  updateConversationSchema
} from "../modules/conversations/conversation.validation";
import { asyncHandler } from "../shared/utils/async-handler";

export const conversationRouter = Router();

conversationRouter.use(requireAuthentication);

conversationRouter.get(
  "/",
  asyncHandler(conversationController.list)
);

conversationRouter.post(
  "/",
  validate(createConversationSchema),
  asyncHandler(conversationController.create)
);

conversationRouter.get(
  "/:conversationId",
  validate(conversationIdSchema),
  asyncHandler(conversationController.get)
);

conversationRouter.patch(
  "/:conversationId",
  validate(updateConversationSchema),
  asyncHandler(conversationController.update)
);

conversationRouter.delete(
  "/:conversationId",
  validate(conversationIdSchema),
  asyncHandler(conversationController.remove)
);
