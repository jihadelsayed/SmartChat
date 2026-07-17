import type { AiProvider } from "./ai-provider.interface";

export class MockAiProvider implements AiProvider {
  async generateReply(input: { message: string }): Promise<string> {
    return `SmartChat received: "${input.message}". This is a mock AI response for the course MVP.`;
  }
}
