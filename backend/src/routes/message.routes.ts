import { Router } from "express";
import { requireAuthentication } from "../middleware/authentication.middleware";
import { validate } from "../middleware/validation.middleware";
import { messageController } from "../modules/messages/message.controller";
import { conversationIdSchema } from "../modules/conversations/conversation.validation";
import { createMessageSchema } from "../modules/messages/message.validation";
import { asyncHandler } from "../shared/utils/async-handler";

export const messageRouter = Router({
  mergeParams: true
});

messageRouter.use(requireAuthentication);

messageRouter.get(
  "/",
  validate(conversationIdSchema),
  asyncHandler(messageController.list)
);

messageRouter.post(
  "/",
  validate(createMessageSchema),
  asyncHandler(messageController.create)
);