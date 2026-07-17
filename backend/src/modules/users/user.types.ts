export interface PublicUser {
  id: string;
  email: string;
  displayName: string;
  profileImageUrl: string | null;
  createdAt: Date;
  updatedAt: Date;
}
