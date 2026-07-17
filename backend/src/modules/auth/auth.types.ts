import type { PublicUser } from "../users/user.types";

export interface AuthResult {
  accessToken: string;
  user: PublicUser;
}
