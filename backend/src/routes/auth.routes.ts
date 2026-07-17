import { Router } from "express";
import { authController } from "../modules/auth/auth.controller";
import { loginSchema, registerSchema } from "../modules/auth/auth.validation";
import { validate } from "../middleware/validation.middleware";
import { asyncHandler } from "../shared/utils/async-handler";

export const authRouter = Router();
authRouter.post("/register", validate(registerSchema), asyncHandler(authController.register));
authRouter.post("/login", validate(loginSchema), asyncHandler(authController.login));
