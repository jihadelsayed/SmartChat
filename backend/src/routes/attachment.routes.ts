import { Router } from "express";
import { requireAuthentication } from "../middleware/authentication.middleware";
import { uploadAttachment } from "../middleware/upload.middleware";
import { validate } from "../middleware/validation.middleware";
import { attachmentController } from "../modules/attachments/attachment.controller";
import {
  attachmentIdSchema,
  messageAttachmentSchema,
  stagedAttachmentSchema
} from "../modules/attachments/attachment.validation";
import { asyncHandler } from "../shared/utils/async-handler";

export const messageAttachmentRouter = Router({
  mergeParams: true
});

messageAttachmentRouter.use(requireAuthentication);

messageAttachmentRouter.get(
  "/",
  validate(messageAttachmentSchema),
  asyncHandler(attachmentController.listByMessage)
);

messageAttachmentRouter.post(
  "/",
  validate(messageAttachmentSchema),
  uploadAttachment,
  asyncHandler(attachmentController.upload)
);

export const attachmentRouter = Router();

attachmentRouter.use(requireAuthentication);

attachmentRouter.post(
  "/",
  validate(stagedAttachmentSchema),
  uploadAttachment,
  asyncHandler(attachmentController.uploadStaged)
);

attachmentRouter.get(
  "/:attachmentId",
  validate(attachmentIdSchema),
  asyncHandler(attachmentController.getById)
);

attachmentRouter.delete(
  "/:attachmentId",
  validate(attachmentIdSchema),
  asyncHandler(attachmentController.remove)
);
