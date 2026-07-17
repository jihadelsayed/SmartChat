import { Router } from "express";
import { aiRouter } from "./ai.routes";
import {
  attachmentRouter,
  messageAttachmentRouter
} from "./attachment.routes";
import { authRouter } from "./auth.routes";
import { conversationRouter } from "./conversation.routes";
import { messageRouter } from "./message.routes";
import { userRouter } from "./user.routes";

export const apiRouter = Router();

apiRouter.use("/auth", authRouter);
apiRouter.use("/users", userRouter);

apiRouter.use(
  "/conversations/:conversationId/messages",
  messageRouter
);

apiRouter.use(
  "/messages/:messageId/attachments",
  messageAttachmentRouter
);

apiRouter.use("/attachments", attachmentRouter);
apiRouter.use("/conversations", conversationRouter);
apiRouter.use("/ai", aiRouter);