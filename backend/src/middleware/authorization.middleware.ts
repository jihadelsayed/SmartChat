import type {
  NextFunction,
  Request,
  RequestHandler,
  Response
} from "express";
import { AuthenticationError } from "../shared/errors/authentication-error";
import { AuthorizationError } from "../shared/errors/authorization-error";

export type ResourceOwnerResolver = (
  request: Request
) =>
  | string
  | null
  | undefined
  | Promise<string | null | undefined>;

export function requireResourceOwnership(
  resolveOwnerId: ResourceOwnerResolver
): RequestHandler {
  return async (
    request: Request,
    _response: Response,
    next: NextFunction
  ): Promise<void> => {
    try {
      const authenticatedUser = request.authenticatedUser;

      if (!authenticatedUser) {
        next(new AuthenticationError());
        return;
      }

      const ownerId = await resolveOwnerId(request);

      if (!ownerId || ownerId !== authenticatedUser.id) {
        next(new AuthorizationError());
        return;
      }

      next();
    } catch (error: unknown) {
      next(error);
    }
  };
}

export function requireCurrentUserParameter(
  parameterName = "userId"
): RequestHandler {
  return (
    request: Request,
    _response: Response,
    next: NextFunction
  ): void => {
    const authenticatedUser = request.authenticatedUser;

    if (!authenticatedUser) {
      next(new AuthenticationError());
      return;
    }

    if (request.params[parameterName] !== authenticatedUser.id) {
      next(new AuthorizationError());
      return;
    }

    next();
  };
}