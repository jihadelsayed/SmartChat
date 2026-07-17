export {};

declare global {
  namespace Express {
    interface Request {
      authenticatedUser?: {
        id: string;
        email: string;
      };
    }
  }
}
