import type { Role } from "./auth";

// Mirrors com.identity.identitysoft.dto.UserSummaryResponse
export interface AdminUser {
  id: number;
  username: string;
  email: string;
  active: boolean;
  roles: Role[];
}

// Mirrors com.identity.identitysoft.entity.AuditLog
export interface AuditLogEntry {
  id: number;
  username: string;
  action: string;
  timestamp: string; // ISO instant string as serialized by the backend
}
