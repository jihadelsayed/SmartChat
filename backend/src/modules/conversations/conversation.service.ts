import { DEFAULT_CONVERSATION_TITLE } from "../../config/constants";
import { NotFoundError } from "../../shared/errors/not-found-error";
import { conversationRepository } from "./conversation.repository";

export const conversationService = {
  list(userId: string) {
    return conversationRepository.listForUser(userId);
  },

  async get(conversationId: string, userId: string) {
    const conversation = await conversationRepository.findOwned(conversationId, userId);
    if (!conversation) throw new NotFoundError("Conversation");
    return conversation;
  },

  create(userId: string, title?: string) {
    return conversationRepository.create(userId, title?.trim() || DEFAULT_CONVERSATION_TITLE);
  },

  async update(conversationId: string, userId: string, title: string) {
    const result = await conversationRepository.update(conversationId, userId, title);
    if (result.count === 0) throw new NotFoundError("Conversation");
    return this.get(conversationId, userId);
  },

  async remove(conversationId: string, userId: string) {
    const result = await conversationRepository.delete(conversationId, userId);
    if (result.count === 0) throw new NotFoundError("Conversation");
  }
};
