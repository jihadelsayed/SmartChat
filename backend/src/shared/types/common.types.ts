export type Nullable<T> = T | null;

export type Optional<T> = T | undefined;

export interface Identifiable {
  id: string;
}

export interface Timestamped {
  createdAt: Date;
  updatedAt: Date;
}

export interface AuthenticatedUser {
  id: string;
  email: string;
}