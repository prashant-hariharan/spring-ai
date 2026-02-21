# AI Chat Bot (Spring Boot + Spring AI)

This project is a multi-provider AI chat application built with Spring Boot and Spring AI.
It includes:
- REST APIs for basic chat and conversational chat
- Streaming chat (SSE)
- Prompt-based code review and ticket analysis APIs
- Browser UIs for streaming chat and code review

## Tech Stack
- Java 17
- Spring Boot 3.5.x
- Spring AI 1.1.x
- Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Prerequisites
- JDK 17 installed
- Internet access for cloud providers (OpenAI/Gemini/Groq/Cohere/Mistral)
- Optional: Ollama running locally if you use provider `ollama`

## 1) Setup Environment Variables
The app reads provider keys from `src/main/resources/application.yml`.

Required/optional variables:
- `OPEN_API_KEY` (required if using OpenAI)
- `GEMINI_API_KEY` (required if using Gemini)
- `GROQ_API_KEY` (required if using Groq)
- `COHERE_API_KEY` (required if using Cohere)
- `MISTRAL_API_KEY` (required if using Mistral)
- `OLLAMA_API_KEY` (optional, blank by default)

### Windows PowerShell (current terminal session)
```powershell
$env:OPEN_API_KEY="your-openai-key"
$env:GEMINI_API_KEY="your-gemini-key"
$env:GROQ_API_KEY="your-groq-key"
$env:COHERE_API_KEY="your-cohere-key"
$env:MISTRAL_API_KEY="your-mistral-key"
$env:OLLAMA_API_KEY=""
```

### macOS/Linux (current terminal session)
```bash
export OPEN_API_KEY="your-openai-key"
export GEMINI_API_KEY="your-gemini-key"
export GROQ_API_KEY="your-groq-key"
export COHERE_API_KEY="your-cohere-key"
export MISTRAL_API_KEY="your-mistral-key"
export OLLAMA_API_KEY=""
```

Sample AI provider entry in `application.yml`:
```yaml
spring:
  ai:
    providers:
      cohere:
        api-key: ${COHERE_API_KEY:}
        model: command-r
        temperature: 0.7
        max-tokens: 500
        base-url: https://api.cohere.ai/compatibility/v1
        completion-path: /chat/completions
      mistral:
        api-key: ${MISTRAL_API_KEY:}
        model: mistral-small-latest
        temperature: 0.7
        max-tokens: 500
        base-url: https://api.mistral.ai/v1
        completion-path: /chat/completions
```

Notes:
- Default provider in controllers is `ollama` when header `ai-provider` is not sent.
- If you use Ollama, ensure Ollama server is running at `http://localhost:11434`.

## 2) Run the Application
From the project root:

### Windows
```powershell
.\mvnw.cmd spring-boot:run
```

### macOS/Linux
```bash
./mvnw spring-boot:run
```

Default app URL:
- `http://localhost:8080`

## 3) API Quick Reference
Use this section for fast lookup. For runnable requests, import the Postman collection in Section 6.

Common provider header values:
- `ai-provider: openai | gemini | ollama | groq | cohere | mistral`

| Controller | Endpoint | Content-Type | Body | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `BasicChatController` | `POST /chatclient/chat` | `text/plain` | plain text message | plain text | No `ai-provider` header required. |
| `ChatModelController` | `POST /chatmodel/chat` | `text/plain` | plain text message | plain text | Uses selected provider from header. |
| `ChatModelController` | `POST /chatmodel/chat/conversation` | `text/plain` | plain text message | plain text | Optional query param: `conversationId=1001`. |
| `StreamingChatModelController` | `POST /chatmodel/streaming/chat` | `text/plain` | plain text message | `text/event-stream` | Streaming response (SSE). |
| `StreamingChatModelController` | `POST /chatmodel/streaming/chat/conversation` | `text/plain` | plain text message | `text/event-stream` | Optional query param: `conversationId=1001`. |
| `PromptController` | `POST /prompts/analyze-code` | `application/json` | `CodeReviewDTO` JSON | plain text | Request fields: `language`, `code`, `businessRequirements` (optional). |
| `PromptController` | `POST /prompts/analyze-ticket` | `text/plain` | ticket description text | `TicketAnalysisResponse` JSON | Returns `ticketAnalysis` + optional `bespokeResponses` when priority is `HIGH`/`URGENT`. |

`/prompts/analyze-ticket` example bodies:
- Normal case: `Customer reports checkout failure with payment timeout after entering card details.`
- Urgent trigger case: `P0 INCIDENT: All customers are unable to complete checkout globally for the last 30 minutes. Revenue impact is critical, payment attempts are failing with timeout errors, and support volume is spiking. Immediate rollback/escalation required.`

## 4) UI Usage

### A) `index.html` (StreamingChatModelController)
UI file: `src/main/resources/static/index.html`

Open in browser:
- `http://localhost:8080/index.html`

How to use:
1. Select provider (`openai`, `gemini`, `ollama`, `groq`, `cohere`, `mistral`).
2. Optional: enter `Conversation ID` to continue existing chat.
3. Enter message.
4. Click `Start Streaming`.
5. Response appears progressively (streaming/SSE).

Backend endpoints used by this UI:
- `POST /chatmodel/streaming/chat`
- `POST /chatmodel/streaming/chat/conversation?conversationId=...` (when ID is provided)

### B) `code-review.html` (PromptController)
UI file: `src/main/resources/static/code-review.html`

Open in browser:
- `http://localhost:8080/code-review.html`

How to use:
1. Select programming language.
2. Select provider.
3. (Optional) Enter business requirements.
4. Paste code.
5. Click `Review Code`.

Backend endpoint used by this UI:
- `POST /prompts/analyze-code`

Request payload sent by UI:
```json
{
  "code": "public class User { ... }",
  "language": "Java",
  "businessRequirements": "Validate password strength"
}
```

Header sent by UI:
- `ai-provider: ollama` (or selected provider)

## 5) Quick Troubleshooting
- `401/403` from provider: verify API keys are set in environment variables.
- Empty/failed response: check Spring Boot logs and provider availability.
- Ollama errors: ensure local Ollama is running and model is available.
- UI not loading: verify app is running on `http://localhost:8080`.

## 6) Sample Postman Collection (Import JSON)
Copy the JSON below into a file like `ai-chat-bot.postman_collection.json`, then import it in Postman.

```json
{
  "info": {
    "name": "AI Chat Bot API",
    "_postman_id": "8d1d29d4-6df4-4c7d-81e7-f7f0d4c6cf12",
    "description": "Sample requests for BasicChatController, ChatModelController, StreamingChatModelController, and PromptController (code and ticket analysis).",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    },
    {
      "key": "provider",
      "value": "ollama"
    },
    {
      "key": "conversationId",
      "value": "1001"
    }
  ],
  "item": [
    {
      "name": "BasicChatController - Chat",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "text/plain"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "Explain dependency injection in Spring Boot."
        },
        "url": {
          "raw": "{{baseUrl}}/chatclient/chat",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "chatclient",
            "chat"
          ]
        }
      }
    },
    {
      "name": "ChatModelController - Chat",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "text/plain"
          },
          {
            "key": "ai-provider",
            "value": "{{provider}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "Give me 3 tips to optimize Java code readability."
        },
        "url": {
          "raw": "{{baseUrl}}/chatmodel/chat",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "chatmodel",
            "chat"
          ]
        }
      }
    },
    {
      "name": "ChatModelController - Conversation (New)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "text/plain"
          },
          {
            "key": "ai-provider",
            "value": "{{provider}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "Start a new conversation and explain Spring profiles."
        },
        "url": {
          "raw": "{{baseUrl}}/chatmodel/chat/conversation",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "chatmodel",
            "chat",
            "conversation"
          ]
        }
      }
    },
    {
      "name": "ChatModelController - Conversation (Existing)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "text/plain"
          },
          {
            "key": "ai-provider",
            "value": "{{provider}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "Continue from previous answer and add a code example."
        },
        "url": {
          "raw": "{{baseUrl}}/chatmodel/chat/conversation?conversationId={{conversationId}}",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "chatmodel",
            "chat",
            "conversation"
          ],
          "query": [
            {
              "key": "conversationId",
              "value": "{{conversationId}}"
            }
          ]
        }
      }
    },
    {
      "name": "StreamingChatModelController - Streaming Chat",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "text/plain"
          },
          {
            "key": "ai-provider",
            "value": "{{provider}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "Explain what server-sent events are."
        },
        "url": {
          "raw": "{{baseUrl}}/chatmodel/streaming/chat",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "chatmodel",
            "streaming",
            "chat"
          ]
        }
      }
    },
    {
      "name": "StreamingChatModelController - Streaming Conversation",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "text/plain"
          },
          {
            "key": "ai-provider",
            "value": "{{provider}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "Continue with more details about SSE tradeoffs."
        },
        "url": {
          "raw": "{{baseUrl}}/chatmodel/streaming/chat/conversation?conversationId={{conversationId}}",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "chatmodel",
            "streaming",
            "chat",
            "conversation"
          ],
          "query": [
            {
              "key": "conversationId",
              "value": "{{conversationId}}"
            }
          ]
        }
      }
    },
    {
      "name": "PromptController - Analyze Code",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "ai-provider",
            "value": "{{provider}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"language\": \"Java\",\n  \"code\": \"public class User { private String password; public String getPassword(){ return password; } }\",\n  \"businessRequirements\": \"Password must be at least 8 characters and include one special character\"\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/prompts/analyze-code",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "prompts",
            "analyze-code"
          ]
        }
      }
    },
    {
      "name": "PromptController - Analyze Ticket",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "text/plain"
          },
          {
            "key": "ai-provider",
            "value": "{{provider}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "Customer reports checkout failure with payment timeout after entering card details."
        },
        "url": {
          "raw": "{{baseUrl}}/prompts/analyze-ticket",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "prompts",
            "analyze-ticket"
          ]
        }
      }
    },
    {
      "name": "PromptController - Analyze Ticket (Urgent P0)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "text/plain"
          },
          {
            "key": "ai-provider",
            "value": "{{provider}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "P0 INCIDENT: All customers are unable to complete checkout globally for the last 30 minutes. Revenue impact is critical, payment attempts are failing with timeout errors, and support volume is spiking. Immediate rollback/escalation required."
        },
        "url": {
          "raw": "{{baseUrl}}/prompts/analyze-ticket",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "prompts",
            "analyze-ticket"
          ]
        }
      }
    }
  ]
}
```

## 7) References
- Spring AI: https://spring.io/projects/spring-ai

## 8) Credits
- Thanks to HungryCoders for the learning content and guidance:
  https://www.hungrycoders.com/course/ai-for-java-spring-boot-backend-engineers
