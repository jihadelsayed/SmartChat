import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import type { ApiEnvelope } from "./test-server";
import { readApiEnvelope } from "./test-server";

export interface TestUserCredentials {
  email: string;
  password: string;
  displayName: string;
}

export interface TestPublicUser {
  id: string;
  email: string;
  displayName: string;
  profileImageUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RegisteredTestUser {
  credentials: TestUserCredentials;
  accessToken: string;
  user: TestPublicUser;
}

interface AuthenticationData {
  accessToken: string;
  user: TestPublicUser;
}

export function createTestUserCredentials(): TestUserCredentials {
  return {
    email: `test-${randomUUID()}@smartchat.local`,
    password: "ValidPassword123!",
    displayName: "SmartChat Test User"
  };
}

export async function registerTestUser(
  baseUrl: string,
  credentials = createTestUserCredentials()
): Promise<RegisteredTestUser> {
  const response = await fetch(`${baseUrl}/api/v1/auth/register`, {
    method: "POST",
    headers: {
      "content-type": "application/json"
    },
    body: JSON.stringify(credentials)
  });

  assert.equal(response.status, 201);

  const responseBody = await readApiEnvelope<AuthenticationData>(response);
  assert.equal(responseBody.success, true);
  assert.ok(responseBody.data);
  assert.equal(typeof responseBody.data.accessToken, "string");

  return {
    credentials,
    accessToken: responseBody.data.accessToken,
    user: responseBody.data.user
  };
}

export function authenticatedHeaders(
  accessToken: string
): Record<string, string> {
  return {
    authorization: `Bearer ${accessToken}`
  };
}

export type AuthenticationEnvelope = ApiEnvelope<AuthenticationData>;
