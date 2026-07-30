CREATE TYPE "AttachmentStatus" AS ENUM ('PROCESSING', 'UPLOADED');

ALTER TABLE "Attachment"
ADD COLUMN "userId" TEXT,
ADD COLUMN "clientAttachmentId" TEXT,
ADD COLUMN "status" "AttachmentStatus" NOT NULL DEFAULT 'UPLOADED',
ADD COLUMN "contentHash" TEXT;

UPDATE "Attachment" AS attachment
SET
  "userId" = conversation."userId",
  "clientAttachmentId" = attachment."id",
  "contentHash" = attachment."id"
FROM "Message" AS message
JOIN "Conversation" AS conversation
  ON conversation."id" = message."conversationId"
WHERE message."id" = attachment."messageId";

ALTER TABLE "Attachment"
ALTER COLUMN "userId" SET NOT NULL,
ALTER COLUMN "clientAttachmentId" SET NOT NULL,
ALTER COLUMN "contentHash" SET NOT NULL,
ALTER COLUMN "messageId" DROP NOT NULL;

ALTER TABLE "Attachment"
ADD CONSTRAINT "Attachment_userId_fkey"
FOREIGN KEY ("userId") REFERENCES "User"("id")
ON DELETE CASCADE ON UPDATE CASCADE;

CREATE INDEX "Attachment_userId_createdAt_idx"
ON "Attachment"("userId", "createdAt");

CREATE UNIQUE INDEX "Attachment_userId_clientAttachmentId_key"
ON "Attachment"("userId", "clientAttachmentId");
