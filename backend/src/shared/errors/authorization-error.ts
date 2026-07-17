import { AppError } from "./app-error";

export class AuthorizationError extends AppError {
  constructor(message = "You do not have permission to perform this action") {
    super(message, 403, "FORBIDDEN");
  }
}
