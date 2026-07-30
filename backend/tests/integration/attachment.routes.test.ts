import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import {
  access,
  readdir,
  utimes,
  writeFile
} from "node:fs/promises";
import path from "node:path";
import { after, test } from "node:test";
import "../setup";
import { runAttachmentCleanup } from "../../src/jobs/attachment-cleanup.job";
import { MAXIMUM_ATTACHMENT_SIZE_BYTES } from "../../src/middleware/upload.middleware";
import { MockAiProvider } from "../../src/modules/ai/providers/mock.provider";
import type { AiProviderRequest } from "../../src/modules/ai/ai.types";
import { prisma } from "../../src/database/prisma";
import { AttachmentStatus } from "../../src/generated/prisma/client";
import {
  deleteUploadedFile,
  resolveUploadPath
} from "../../src/shared/utils/file";
import { cleanupTestUsers } from "../helpers/test-database";
import {
  authenticatedHeaders,
  registerTestUser
} from "../helpers/test-user";
import {
  readApiEnvelope,
  startTestServer
} from "../helpers/test-server";

interface ConversationData {
  id: string;
}

interface MessageData {
  id: string;
}

interface SendMessageData {
  userMessage: MessageData;
}

interface AttachmentData {
  id: string;
  messageId: string;
  fileName: string;
  mimeType: string;
  fileUrl: string;
  sizeBytes: number;
}

const testServer = await startTestServer();
const registeredUser = await registerTestUser(testServer.baseUrl);
const otherUser = await registerTestUser(testServer.baseUrl);
let uploadedFileUrl: string | undefined;
const stagedFileUrls = new Set<string>();

after(async () => {
  if (uploadedFileUrl) {
    await deleteUploadedFile(uploadedFileUrl);
  }
  for (const fileUrl of stagedFileUrls) {
    await deleteUploadedFile(fileUrl);
  }

  await cleanupTestUsers([
    registeredUser.credentials.email,
    otherUser.credentials.email
  ]);
  await testServer.stop();
});

async function createMessageForAttachment(): Promise<MessageData> {
  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );
  const jsonHeaders = {
    ...authorizationHeaders,
    "content-type": "application/json"
  };

  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ title: "Attachment route test" })
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);

  const messageResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationBody.data.id}/messages`,
    {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ content: "Message with an attachment" })
    }
  );
  const messageBody = await readApiEnvelope<SendMessageData>(messageResponse);
  assert.ok(messageBody.data);

  return messageBody.data.userMessage;
}

async function uploadStaged(
  accessToken: string,
  clientAttachmentId: string,
  mimeType: string,
  fileName: string,
  contents?: Uint8Array
) {
  const response = await fetch(`${testServer.baseUrl}/api/v1/attachments`, {
    method: "POST",
    headers: {
      ...authenticatedHeaders(accessToken),
      "x-client-attachment-id": clientAttachmentId
    },
    body: createUploadForm(mimeType, fileName, contents)
  });
  const body = await readApiEnvelope<AttachmentData>(response);
  if (body.data?.fileUrl) stagedFileUrls.add(body.data.fileUrl);
  return {
    response,
    body
  };
}

function createUploadForm(
  mimeType: string,
  fileName: string,
  contents: Uint8Array<ArrayBufferLike> = new Uint8Array([
    137, 80, 78, 71, 13, 10, 26, 10
  ])
): FormData {
  const form = new FormData();
  form.append(
    "file",
    new Blob([new Uint8Array(contents)], { type: mimeType }),
    fileName
  );
  return form;
}

test("attachment endpoints upload, list, read, and protect associated files", async () => {
  assert.equal(MAXIMUM_ATTACHMENT_SIZE_BYTES, 10 * 1024 * 1024);

  const message = await createMessageForAttachment();
  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );
  const messageAttachmentsUrl =
    `${testServer.baseUrl}/api/v1/messages/${message.id}/attachments`;

  const uploadResponse = await fetch(messageAttachmentsUrl, {
    method: "POST",
    headers: authorizationHeaders,
    body: createUploadForm("image/png", "attachment-test.png")
  });
  const uploadBody = await readApiEnvelope<AttachmentData>(uploadResponse);

  assert.equal(uploadResponse.status, 201);
  assert.ok(uploadBody.data);
  assert.equal(uploadBody.data.messageId, message.id);
  assert.equal(uploadBody.data.mimeType, "image/png");

  uploadedFileUrl = uploadBody.data.fileUrl;
  const uploadedFilePath = resolveUploadPath(uploadedFileUrl);
  await access(uploadedFilePath);

  const listResponse = await fetch(messageAttachmentsUrl, {
    headers: authorizationHeaders
  });
  const listBody = await readApiEnvelope<AttachmentData[]>(listResponse);

  assert.equal(listResponse.status, 200);
  assert.equal(listBody.data?.length, 1);
  assert.equal(listBody.data?.[0]?.id, uploadBody.data.id);

  const attachmentUrl =
    `${testServer.baseUrl}/api/v1/attachments/${uploadBody.data.id}`;
  const getResponse = await fetch(attachmentUrl, {
    headers: authorizationHeaders
  });
  const getBody = await readApiEnvelope<AttachmentData>(getResponse);

  assert.equal(getResponse.status, 200);
  assert.equal(getBody.data?.id, uploadBody.data.id);

  const deleteResponse = await fetch(attachmentUrl, {
    method: "DELETE",
    headers: authorizationHeaders
  });

  assert.equal(deleteResponse.status, 409);
  await access(uploadedFilePath);
});

test("unsupported attachment MIME types return a client error", async () => {
  const message = await createMessageForAttachment();
  const response = await fetch(
    `${testServer.baseUrl}/api/v1/messages/${message.id}/attachments`,
    {
      method: "POST",
      headers: authenticatedHeaders(registeredUser.accessToken),
      body: createUploadForm("text/plain", "attachment-test.txt")
    }
  );
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 415);
  assert.equal(responseBody.error?.code, "UNSUPPORTED_ATTACHMENT_TYPE");
});

for (const supported of [
  {
    mimeType: "image/jpeg",
    fileName: "photo.jpg",
    bytes: new Uint8Array([0xff, 0xd8, 0xff])
  },
  {
    mimeType: "image/png",
    fileName: "photo.png",
    bytes: new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10])
  },
  {
    mimeType: "image/webp",
    fileName: "photo.webp",
    bytes: new TextEncoder().encode("RIFF0000WEBP")
  }
]) {
  test(`staged ${supported.mimeType} uploads succeed`, async () => {
    const upload = await uploadStaged(
      registeredUser.accessToken,
      randomUUID(),
      supported.mimeType,
      supported.fileName,
      supported.bytes
    );
    assert.equal(upload.response.status, 201);
    assert.equal(upload.body.data?.messageId, null);
    assert.equal(upload.body.data?.mimeType, supported.mimeType);
  });
}

test("HEIC and fake image content are rejected", async () => {
  const heic = await uploadStaged(
    registeredUser.accessToken,
    randomUUID(),
    "image/heic",
    "photo.heic",
    new Uint8Array([0, 0, 0, 0])
  );
  assert.equal(heic.response.status, 415);
  assert.equal(heic.body.error?.code, "UNSUPPORTED_ATTACHMENT_TYPE");

  const fakePng = await uploadStaged(
    registeredUser.accessToken,
    randomUUID(),
    "image/png",
    "fake.png",
    new TextEncoder().encode("not an image")
  );
  assert.equal(fakePng.response.status, 415);
  assert.equal(fakePng.body.error?.code, "INVALID_ATTACHMENT_CONTENT");
});

test("staged upload idempotency is scoped and rejects changed content", async () => {
  const clientAttachmentId = randomUUID();
  const bytes = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]);
  const [first, concurrent] = await Promise.all([
    uploadStaged(
      registeredUser.accessToken,
      clientAttachmentId,
      "image/png",
      "safe.png",
      bytes
    ),
    uploadStaged(
      registeredUser.accessToken,
      clientAttachmentId,
      "image/png",
      "safe.png",
      bytes
    )
  ]);
  assert.deepEqual(
    [first.response.status, concurrent.response.status].sort(),
    [200, 201]
  );
  assert.equal(first.body.data?.id, concurrent.body.data?.id);
  assert.equal(
    await prisma.attachment.count({
      where: {
        userId: registeredUser.user.id,
        clientAttachmentId
      }
    }),
    1
  );

  const conflict = await uploadStaged(
    registeredUser.accessToken,
    clientAttachmentId,
    "image/png",
    "changed.png",
    new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10, 1])
  );
  assert.equal(conflict.response.status, 409);
  assert.equal(conflict.body.error?.code, "ATTACHMENT_ID_CONFLICT");

  const privateRead = await fetch(
    `${testServer.baseUrl}/api/v1/attachments/${first.body.data?.id}`,
    { headers: authenticatedHeaders(otherUser.accessToken) }
  );
  assert.equal(privateRead.status, 404);
});

test("message association is atomic and image input reaches the provider", async (testContext) => {
  const upload = await uploadStaged(
    registeredUser.accessToken,
    randomUUID(),
    "image/png",
    "../../unsafe.png",
    new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10])
  );
  assert.equal(upload.response.status, 201);
  assert.ok(upload.body.data);
  assert.doesNotMatch(upload.body.data.fileUrl, /\.\.|unsafe/);

  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: {
        ...authenticatedHeaders(registeredUser.accessToken),
        "content-type": "application/json"
      },
      body: JSON.stringify({ title: "Image message" })
    }
  );
  const conversation =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversation.data);
  let providerImages = 0;
  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async (input: AiProviderRequest) => {
      providerImages = input.images?.length ?? 0;
      return "Image received";
    }
  );

  const messageResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversation.data.id}/messages`,
    {
      method: "POST",
      headers: {
        ...authenticatedHeaders(registeredUser.accessToken),
        "content-type": "application/json",
        "idempotency-key": randomUUID()
      },
      body: JSON.stringify({
        content: "",
        attachmentIds: [upload.body.data.id, upload.body.data.id]
      })
    }
  );
  const messageBody = await readApiEnvelope<{
    userMessage: { attachments: AttachmentData[] };
  }>(messageResponse);
  assert.equal(messageResponse.status, 201);
  assert.equal(providerImages, 1);
  assert.equal(messageBody.data?.userMessage.attachments.length, 1);
  assert.equal(
    messageBody.data?.userMessage.attachments[0]?.id,
    upload.body.data.id
  );
});

test("another user's or incomplete attachment cannot be associated", async (testContext) => {
  const upload = await uploadStaged(
    otherUser.accessToken,
    randomUUID(),
    "image/png",
    "private.png",
    new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10])
  );
  assert.ok(upload.body.data);
  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: {
        ...authenticatedHeaders(registeredUser.accessToken),
        "content-type": "application/json"
      },
      body: JSON.stringify({ title: "Rejected attachments" })
    }
  );
  const conversation =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversation.data);
  let providerCalls = 0;
  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async () => {
      providerCalls += 1;
      return "Must not run";
    }
  );

  const response = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversation.data.id}/messages`,
    {
      method: "POST",
      headers: {
        ...authenticatedHeaders(registeredUser.accessToken),
        "content-type": "application/json"
      },
      body: JSON.stringify({
        content: "Private",
        attachmentIds: [upload.body.data.id]
      })
    }
  );
  assert.equal(response.status, 409);
  assert.equal(providerCalls, 0);

  const incomplete = await prisma.attachment.create({
    data: {
      userId: registeredUser.user.id,
      clientAttachmentId: randomUUID(),
      status: AttachmentStatus.PROCESSING,
      fileName: "incomplete.png",
      mimeType: "image/png",
      fileUrl: `/uploads/${randomUUID()}`,
      sizeBytes: 8,
      contentHash: randomUUID()
    }
  });
  const incompleteResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversation.data.id}/messages`,
    {
      method: "POST",
      headers: {
        ...authenticatedHeaders(registeredUser.accessToken),
        "content-type": "application/json"
      },
      body: JSON.stringify({
        content: "Incomplete",
        attachmentIds: [incomplete.id]
      })
    }
  );
  assert.equal(incompleteResponse.status, 409);
  assert.equal(providerCalls, 0);
});

test("attachments larger than 10 MB are rejected and removed", async () => {
  const uploadsDirectory = path.dirname(
    resolveUploadPath("/uploads/directory-probe")
  );
  const filesBeforeUpload = (await readdir(uploadsDirectory)).sort();

  const response = await fetch(
    `${testServer.baseUrl}/api/v1/messages/${randomUUID()}/attachments`,
    {
      method: "POST",
      headers: authenticatedHeaders(registeredUser.accessToken),
      body: createUploadForm(
        "image/png",
        "oversized.png",
        new Uint8Array(MAXIMUM_ATTACHMENT_SIZE_BYTES + 1)
      )
    }
  );
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 413);
  assert.equal(responseBody.error?.code, "ATTACHMENT_TOO_LARGE");
  assert.deepEqual((await readdir(uploadsDirectory)).sort(), filesBeforeUpload);
});

test("a failed attachment database write removes the uploaded file", async () => {
  const uploadsDirectory = path.dirname(
    resolveUploadPath("/uploads/directory-probe")
  );
  const filesBeforeUpload = (await readdir(uploadsDirectory)).sort();

  const response = await fetch(
    `${testServer.baseUrl}/api/v1/messages/${randomUUID()}/attachments`,
    {
      method: "POST",
      headers: authenticatedHeaders(registeredUser.accessToken),
      body: createUploadForm("image/png", "failed-write.png")
    }
  );

  assert.equal(response.status, 404);
  assert.deepEqual((await readdir(uploadsDirectory)).sort(), filesBeforeUpload);
});

test("the orphan cleanup job removes unreferenced files after its grace period", async () => {
  const orphanFileUrl = `/uploads/orphan-${randomUUID()}.png`;
  const orphanFilePath = resolveUploadPath(orphanFileUrl);
  const oldTimestamp = new Date(Date.now() - 8 * 24 * 60 * 60 * 1000);

  await writeFile(orphanFilePath, new Uint8Array([137, 80, 78, 71]));
  await utimes(orphanFilePath, oldTimestamp, oldTimestamp);

  const cleanupResult = await runAttachmentCleanup();

  assert.ok(cleanupResult.deletedFiles >= 1);
  await assert.rejects(access(orphanFilePath));
});
