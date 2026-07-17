import type { User } from "../../generated/prisma/client";
import type { PublicUser } from "./user.types";

export function toPublicUser(user: User): PublicUser {
  return {
    id: user.id,
    email: user.email,
    displayName: user.displayName,
    profileImageUrl: user.profileImageUrl,
    createdAt: user.createdAt,
    updatedAt: user.updatedAt
  };
}
