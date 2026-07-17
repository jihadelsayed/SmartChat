import assert from "node:assert/strict";
import { after, test } from "node:test";
import { cleanupTestUsers } from "../helpers/test-database";
import {
  authenticatedHeaders,
  registerTestUser,
  type TestPublicUser
} from "../helpers/test-user";
import {
  readApiEnvelope,
  startTestServer
} from "../helpers/test-server";

const testServer = await startTestServer();
const createdEmails: string[] = [];

after(async () => {
  await cleanupTestUsers(createdEmails);
  await testServer.stop();
});

test("GET /api/v1/users/me rejects unauthenticated requests", async () => {
  const response = await fetch(`${testServer.baseUrl}/api/v1/users/me`);
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 401);
  assert.equal(responseBody.error?.code, "AUTHENTICATION_REQUIRED");
});

test("GET /api/v1/users/me returns the authenticated profile", async () => {
  const registeredUser = await registerTestUser(testServer.baseUrl);
  createdEmails.push(registeredUser.credentials.email);

  const response = await fetch(`${testServer.baseUrl}/api/v1/users/me`, {
    headers: authenticatedHeaders(registeredUser.accessToken)
  });
  const responseBody = await readApiEnvelope<TestPublicUser>(response);

  assert.equal(response.status, 200);
  assert.equal(responseBody.data?.id, registeredUser.user.id);
  assert.equal(responseBody.data?.email, registeredUser.credentials.email);
});
