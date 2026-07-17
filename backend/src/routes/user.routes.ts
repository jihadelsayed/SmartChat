import { Router } from "express";
import { requireAuthentication } from "../middleware/authentication.middleware";
import { validate } from "../middleware/validation.middleware";
import { userController } from "../modules/users/user.controller";
import { updateProfileSchema } from "../modules/users/user.validation";
import { asyncHandler } from "../shared/utils/async-handler";

export const userRouter = Router();
userRouter.use(requireAuthentication);
userRouter.get("/me", asyncHandler(userController.getProfile));
userRouter.patch("/me", validate(updateProfileSchema), asyncHandler(userController.updateProfile));
