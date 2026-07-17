import jwt, { type JwtPayload, type SignOptions } from "jsonwebtoken";
import { environment } from "../../config/environment";

export interface AccessTokenPayload extends JwtPayload {
  sub: string;
  email: string;
}

export function createAccessToken(userId: string, email: string): string {
  const options: SignOptions = {
    expiresIn: environment.JWT_EXPIRES_IN as SignOptions["expiresIn"]
  };
  return jwt.sign({ email }, environment.JWT_SECRET, { ...options, subject: userId });
}

export function verifyAccessToken(token: string): AccessTokenPayload {
  return jwt.verify(token, environment.JWT_SECRET) as AccessTokenPayload;
}
