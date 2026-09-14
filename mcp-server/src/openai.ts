import { AzureOpenAI } from 'openai';
import dotenv from 'dotenv';
dotenv.config();

const endpoint = process.env.AZURE_OPENAI_ENDPOINT || "https://mock.azure.com";
const apiKey = process.env.AZURE_OPENAI_API_KEY || "mock-openai-key";
const deployment = process.env.AZURE_OPENAI_DEPLOYMENT_NAME || "gpt-4o";

export async function generateAngularCodeWithLLM(prompt: string): Promise<Record<string, string>> {
  if (apiKey === "mock-openai-key" || endpoint.includes("mock")) {
    console.log("Mock OpenAI mode enabled. Returning empty files map.");
    return {};
  }

  const client = new AzureOpenAI({ endpoint, apiKey, deployment, apiVersion: "2024-02-01" });
  
  console.log(`Calling Azure OpenAI at ${endpoint} (Deployment: ${deployment})`);
  
  try {
    const response = await client.chat.completions.create({
      model: deployment,
      messages: [{ role: "user", content: prompt }],
      max_tokens: 16000,
      temperature: 0.2,
    });

    const content = response.choices[0]?.message?.content;
    if (!content) return {};

    // Extract JSON block from markdown code fence if present
    const start = content.indexOf('{');
    const end = content.lastIndexOf('}');
    
    if (start >= 0 && end > start) {
      const jsonPart = content.substring(start, end + 1);
      return JSON.parse(jsonPart);
    }
  } catch (error: any) {
    console.error("Azure OpenAI call failed:", error.message);
  }
  
  return {};
}
