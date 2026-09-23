# AI RAG phase

This phase keeps the RAG workflow small and visible:

```text
Products + active promotions + public reviews + verified FAQ files
                              |
                              v
                    AiDocumentService
                              |
                    Ollama embeddings
                              |
                              v
                    PostgreSQL + pgvector
                              |
                  semantic similarity search
                              |
                              v
              current DB facts + customer question
                              |
                    Ollama chat model
                              |
                              v
                 grounded answer + sources
```

Customer, order, payment, address and authentication data are never copied into the shared vector index.
Review text is public, but reviewer identity is omitted.

## Why the `ai` profile exists

The normal application still starts without Ollama or pgvector. AI beans and the AI Flyway migration load only
when the `ai` Spring profile is active.

The project uses Spring Boot 4, so this implementation uses Spring AI 2.0.1. The embedding model is
`nomic-embed-text`, which produces 768-dimensional vectors. If the embedding model or dimensions change,
recreate the vector table and rebuild the index.

## One-time setup with Docker

1. Add these values to `.env`:

```env
SPRING_PROFILES_ACTIVE=docker,ai
OLLAMA_CHAT_MODEL=llama3.2:3b
AI_INDEXING_CRON=-
```

2. Start pgvector, Redis and Ollama:

```bash
docker compose --profile ai up -d postgres redis ollama
```

The pgvector image uses the existing `postgres_data` volume. Changing the image does not delete that volume.

3. Pull the two local models once:

```bash
docker exec sushi-ollama ollama pull nomic-embed-text
docker exec sushi-ollama ollama pull llama3.2:3b
```

4. Build and start the backend:

```bash
docker compose --profile ai up -d --build backend
```

No AI API key is required because Ollama runs locally.

## Test from Swagger

Open `http://localhost:8080/swagger-ui.html`, log in through the normal API, and paste the JWT into Swagger's
Authorize dialog.

Call these endpoints in order:

1. `POST /api/ai/rag/index` as an admin. This recreates the shop vector index.
2. `GET /api/ai/rag/search?query=Which rolls have salmon?` as any signed-in user. This shows retrieval only.
3. `POST /api/ai/rag/ask` as any signed-in user:

```json
{
  "question": "Which available rolls have salmon, and what are their current base prices?"
}
```

The response contains the answer and source records. Start with English queries because `nomic-embed-text` is
English-focused.

## Updating knowledge

The editable FAQ files are in `backend/src/main/resources/ai/knowledge`. They deliberately do not invent opening
hours, address, delivery fees, delivery time, dietary guarantees or refund rules. Add those values only after the
shop confirms them, then run the index endpoint again.

Product prices and availability are re-read from PostgreSQL after retrieval, so an answer does not reuse an old
indexed price. Rebuild the index after product, promotion or review changes to improve future retrieval.

Scheduled refresh is optional. This example runs hourly:

```env
AI_INDEXING_CRON=0 0 * * * *
```
