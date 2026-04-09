# CodeHelper

IntelliJ plugin that sends selected code to GPT-4o-mini and shows the explanation in a side panel. You can keep asking follow-up questions from there.

## Stack

Kotlin plugin → Flask backend → OpenAI API. The backend logs the conversation to `chat.log` so context carries over between sessions. Everything runs through Docker Compose.

## Setup

Add your key to `backend/.env`:
```
OPENAI_API_KEY=sk-...
```

```bash
docker compose up --build
./gradlew runIde
```

## Structure

```
├── backend/        
├── src/            
└── docker-compose.yml
```
