# AI Chat Bot (Spring Boot + Spring AI)

This project is a multi-provider AI chat application built with Spring Boot and Spring AI.
It includes:
- REST APIs for basic chat and conversational chat
- Streaming chat (SSE)
- Prompt-based code review API
- Browser UIs for streaming chat and code review

## Tech Stack
- Java 17
- Spring Boot 3.5.x
- Spring AI 1.1.x
- Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Prerequisites
- JDK 17 installed
- Internet access for cloud providers (OpenAI/Gemini/Groq)
- Optional: Ollama running locally if you use provider `ollama`

## 1) Setup Environment Variables
The app reads provider keys from `src/main/resources/application.yml`.

Required/optional variables:
- `OPEN_API_KEY` (required if using OpenAI)
- `GEMINI_API_KEY` (required if using Gemini)
- `GROQ_API_KEY` (required if using Groq)
- `OLLAMA_API_KEY` (optional, blank by default)

### Windows PowerShell (current terminal session)
```powershell
$env:OPEN_API_KEY="your-openai-key"
$env:GEMINI_API_KEY="your-gemini-key"
$env:GROQ_API_KEY="your-groq-key"
$env:OLLAMA_API_KEY=""
```

### macOS/Linux (current terminal session)
```bash
export OPEN_API_KEY="your-openai-key"
export GEMINI_API_KEY="your-gemini-key"
export GROQ_API_KEY="your-groq-key"
export OLLAMA_API_KEY=""
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

## 3) API Testing in Postman

### A) BasicChatController
Controller path: `/chatclient`

#### Endpoint: `POST /chatclient/chat`
- URL: `http://localhost:8080/chatclient/chat`
- Body type: `raw` -> `Text`
- Example body:
```text
Explain dependency injection in Spring Boot.
```
- Expected response: plain text AI answer

Postman cURL preview equivalent:
```bash
curl -X POST "http://localhost:8080/chatclient/chat" \
  -H "Content-Type: text/plain" \
  --data "Explain dependency injection in Spring Boot."
```

### B) ChatModelController
Controller path: `/chatmodel`

#### Endpoint: `POST /chatmodel/chat`
- URL: `http://localhost:8080/chatmodel/chat`
- Headers:
  - `Content-Type: text/plain`
  - `ai-provider: ollama` (or `openai`, `gemini`, `groq`)
- Body type: `raw` -> `Text`
- Example body:
```text
Give me 3 tips to optimize Java code readability.
```

#### Endpoint: `POST /chatmodel/chat/conversation`
- URL (new conversation): `http://localhost:8080/chatmodel/chat/conversation`
- URL (existing conversation): `http://localhost:8080/chatmodel/chat/conversation?conversationId=1001`
- Headers:
  - `Content-Type: text/plain`
  - `ai-provider: ollama` (or `openai`, `gemini`, `groq`)
- Body type: `raw` -> `Text`
- Example body:
```text
Continue from previous answer and provide sample code.
```

Behavior:
- If `conversationId` is absent, backend creates one.
- If present, backend uses recent conversation history.

## 4) UI Usage

### A) `index.html` (StreamingChatModelController)
UI file: `src/main/resources/static/index.html`

Open in browser:
- `http://localhost:8080/index.html`

How to use:
1. Select provider (`openai`, `gemini`, `ollama`, `groq`).
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
- `POST /prompts/chat`

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

## 6) Project Structure (Key Files)
- `src/main/java/com/prashant/ai_chat_bot/controller/BasicChatController.java`
- `src/main/java/com/prashant/ai_chat_bot/controller/ChatModelController.java`
- `src/main/java/com/prashant/ai_chat_bot/controller/StreamingChatModelController.java`
- `src/main/java/com/prashant/ai_chat_bot/controller/PromptController.java`
- `src/main/resources/application.yml`
- `src/main/resources/static/index.html`
- `src/main/resources/static/code-review.html`

## 7) Sample Postman Collection (Import JSON)
Copy the JSON below into a file like `ai-chat-bot.postman_collection.json`, then import it in Postman.

```json
{
  "info": {
    "name": "AI Chat Bot API",
    "_postman_id": "8d1d29d4-6df4-4c7d-81e7-f7f0d4c6cf12",
    "description": "Sample requests for BasicChatController, ChatModelController, StreamingChatModelController, and PromptController.",
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
      "name": "PromptController - Code Review",
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
          "raw": "{{baseUrl}}/prompts/chat",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "prompts",
            "chat"
          ]
        }
      }
    }
  ]
}
```

## 8) References
- Spring AI: https://spring.io/projects/spring-ai

## 9) Credits
- Thanks to HungryCoders for the learning content and guidance:
  https://www.hungrycoders.com/course/ai-for-java-spring-boot-backend-engineers
