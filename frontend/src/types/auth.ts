// Mirrors com.identity.identitysoft.entity.Role on the backend.
export type Role = "ROLE_USER" | "ROLE_ADMIN";

// Shape returned by /auth/login, /auth/register, and /auth/refresh.
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  username: string;
  roles: Role[];
}

// Session data kept in AuthContext state (and mirrored into localStorage).
export interface AuthUser {
  username: string;
  roles: Role[];
}
