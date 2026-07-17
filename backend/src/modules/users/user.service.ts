import { NotFoundError } from "../../shared/errors/not-found-error";
import { toPublicUser } from "./user.mapper";
import { userRepository } from "./user.repository";

export const userService = {
  async getProfile(userId: string) {
    const user = await userRepository.findById(userId);
    if (!user) throw new NotFoundError("User");
    return toPublicUser(user);
  },

  async updateProfile(
    userId: string,
    data: { displayName?: string; profileImageUrl?: string | null }
  ) {
    const user = await userRepository.updateProfile(userId, data);
    return toPublicUser(user);
  }
};
