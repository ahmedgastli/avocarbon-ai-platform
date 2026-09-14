import express from 'express';
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { SSEServerTransport } from '@modelcontextprotocol/sdk/server/sse.js';
import { CallToolRequestSchema, ListToolsRequestSchema } from '@modelcontextprotocol/sdk/types.js';
import { generateAngularCodeWithLLM } from './openai.js';

const app = express();
const port = process.env.PORT || 8081;

// The MCP Server instance
const server = new Server(
  {
    name: 'avocarbon-angular-generator',
    version: '1.0.0',
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// Register available tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: 'generate_angular_project',
        description: 'Generates custom non-structural Angular files and HTML templates based on a provided prompt and generation type.',
        inputSchema: {
          type: 'object',
          properties: {
            prompt: {
              type: 'string',
              description: 'The detailed instructions and OpenAPI context.',
            },
            generationType: {
              type: 'string',
              enum: ['ANGULAR_FULL', 'ANGULAR_COMPONENTS'],
              description: 'The scope of the generation.',
            },
          },
          required: ['prompt', 'generationType'],
        },
      },
    ],
  };
});

// Handle tool execution
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  if (request.params.name === 'generate_angular_project') {
    const args = request.params.arguments;
    if (!args || typeof args.prompt !== 'string') {
      throw new Error("Invalid arguments: 'prompt' is required");
    }

    try {
      const filesMap = await generateAngularCodeWithLLM(args.prompt);
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(filesMap),
          },
        ],
      };
    } catch (e: any) {
      return {
        content: [
          {
            type: 'text',
            text: `Error during LLM generation: ${e.message}`,
          },
        ],
        isError: true,
      };
    }
  }

  throw new Error(`Unknown tool: ${request.params.name}`);
});

let transport: SSEServerTransport;

// Endpoint to establish SSE connection
app.get('/sse', async (req, res) => {
  transport = new SSEServerTransport('/message', res);
  await server.connect(transport);
});

// Endpoint to receive JSON-RPC messages (CallToolRequest, etc.)
app.post('/message', async (req, res) => {
  if (!transport) {
    res.status(400).send('SSE Connection not established yet');
    return;
  }
  await transport.handlePostMessage(req, res);
});

app.listen(port, () => {
  console.log(`MCP Server running on http://localhost:${port}`);
  console.log(`SSE endpoint: http://localhost:${port}/sse`);
});
