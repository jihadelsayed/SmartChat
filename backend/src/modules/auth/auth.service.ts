import { Prisma, type User } from "../../generated/prisma/client";
import { AppError } from "../../shared/errors/app-error";
import { AuthenticationError } from "../../shared/errors/authentication-error";
import { createAccessToken } from "../../shared/utils/jwt";
import { hashPassword, verifyPassword } from "../../shared/utils/password";
import { toPublicUser } from "../users/user.mapper";
import { authRepository } from "./auth.repository";
import type { AuthResult } from "./auth.types";

export const authService = {
  async register(input: {
    email: string;
    password: string;
    displayName: string;
  }): Promise<AuthResult> {
    const email = input.email.toLowerCase();
    const existingUser = await authRepository.findByEmail(email);
    if (existingUser) {
      throw new AppError("An account with this email already exists", 409, "EMAIL_ALREADY_EXISTS");
    }

    let user: User;

    try {
      user = await authRepository.createUser({
        email,
        displayName: input.displayName,
        passwordHash: await hashPassword(input.password)
      });
    } catch (error: unknown) {
      if (
        error instanceof Prisma.PrismaClientKnownRequestError &&
        error.code === "P2002"
      ) {
        throw new AppError(
          "An account with this email already exists",
          409,
          "EMAIL_ALREADY_EXISTS"
        );
      }

      throw error;
    }

    return {
      accessToken: createAccessToken(user.id, user.email),
      user: toPublicUser(user)
    };
  },

  async login(input: { email: string; password: string }): Promise<AuthResult> {
    const user = await authRepository.findByEmail(input.email.toLowerCase());
    if (!user || !(await verifyPassword(input.password, user.passwordHash))) {
      throw new AuthenticationError("Incorrect email or password");
    }

    return {
      accessToken: createAccessToken(user.id, user.email),
      user: toPublicUser(user)
    };
  }
};
