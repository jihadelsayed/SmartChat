import { AppError } from "./app-error";

export class AuthenticationError extends AppError {
  constructor(message = "Authentication required") {
    super(message, 401, "AUTHENTICATION_REQUIRED");
  }
}
