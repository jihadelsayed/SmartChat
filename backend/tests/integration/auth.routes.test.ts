import assert from "node:assert/strict";
import { after, test } from "node:test";
import { cleanupTestUsers } from "../helpers/test-database";
import {
  createTestUserCredentials,
  registerTestUser,
  type AuthenticationEnvelope
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

test("POST /api/v1/auth/register creates an account", async () => {
  const registeredUser = await registerTestUser(testServer.baseUrl);
  createdEmails.push(registeredUser.credentials.email);

  assert.equal(registeredUser.user.email, registeredUser.credentials.email);
  assert.equal(
    registeredUser.user.displayName,
    registeredUser.credentials.displayName
  );
});

test("POST /api/v1/auth/login authenticates valid credentials", async () => {
  const registeredUser = await registerTestUser(testServer.baseUrl);
  createdEmails.push(registeredUser.credentials.email);

  const response = await fetch(`${testServer.baseUrl}/api/v1/auth/login`, {
    method: "POST",
    headers: {
      "content-type": "application/json"
    },
    body: JSON.stringify({
      email: registeredUser.credentials.email,
      password: registeredUser.credentials.password
    })
  });

  const responseBody =
    await readApiEnvelope<AuthenticationEnvelope["data"]>(response);

  assert.equal(response.status, 200);
  assert.equal(responseBody.success, true);
  assert.equal(responseBody.data?.user.id, registeredUser.user.id);
  assert.equal(typeof responseBody.data?.accessToken, "string");
});

test("invalid registration data returns a validation error", async () => {
  const response = await fetch(`${testServer.baseUrl}/api/v1/auth/register`, {
    method: "POST",
    headers: {
      "content-type": "application/json"
    },
    body: JSON.stringify({
      email: "not-an-email",
      password: "short",
      displayName: "A"
    })
  });

  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 400);
  assert.equal(responseBody.error?.code, "VALIDATION_ERROR");
});

test("malformed JSON returns a client error", async () => {
  const response = await fetch(`${testServer.baseUrl}/api/v1/auth/login`, {
    method: "POST",
    headers: {
      "content-type": "application/json"
    },
    body: "{"
  });

  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 400);
  assert.equal(responseBody.error?.code, "INVALID_JSON");
});

test("incorrect credentials return an authentication error", async () => {
  const credentials = createTestUserCredentials();
  const registeredUser = await registerTestUser(
    testServer.baseUrl,
    credentials
  );
  createdEmails.push(registeredUser.credentials.email);

  const response = await fetch(`${testServer.baseUrl}/api/v1/auth/login`, {
    method: "POST",
    headers: {
      "content-type": "application/json"
    },
    body: JSON.stringify({
      email: credentials.email,
      password: "IncorrectPassword123!"
    })
  });

  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 401);
  assert.equal(responseBody.error?.code, "AUTHENTICATION_REQUIRED");
});
