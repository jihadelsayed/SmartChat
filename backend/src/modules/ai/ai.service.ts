import { environment } from "../../config/environment";
import type { AiProvider } from "./providers/ai-provider.interface";
import { MockAiProvider } from "./providers/mock.provider";
import { OpenAiProvider } from "./providers/openai.provider";

const provider: AiProvider =
  environment.AI_PROVIDER === "openai"
    ? new OpenAiProvider(environment.OPENAI_API_KEY, environment.OPENAI_MODEL)
    : new MockAiProvider();

export const aiService = {
  reply(message: string): Promise<string> {
    return provider.generateReply({ message });
  }
};
