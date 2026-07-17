import { aiService } from "../ai/ai.service";
import { conversationService } from "../conversations/conversation.service";
import { messageRepository } from "./message.repository";

export const messageService = {
  async list(conversationId: string, userId: string) {
    await conversationService.get(conversationId, userId);

    return messageRepository.listByConversation(conversationId);
  },

  async send(conversationId: string, userId: string, content: string) {
    await conversationService.get(conversationId, userId);

    const assistantContent = await aiService.reply(content);

    return messageRepository.createExchange(
      conversationId,
      content,
      assistantContent
    );
  }
};
