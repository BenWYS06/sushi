# AI shopping agent (backend)

The agent builds on the existing RAG phase. It is available only when the `ai` Spring profile is active.

## Workflow

```text
JWT customer
    |
    | POST session / POST message
    v
AiChatController
    |
    v
AiChatService ---- RAG search ---- pgvector
    |
    | ChatClient + ToolContext(user email, session ID)
    v
Ollama chooses a tool when needed
    |
    +-- searchProducts
    +-- getProductDetails
    +-- getActivePromotions
    +-- getMyOrderStatus
    +-- draftOrder --------> stores a priced PENDING draft only

Explicit POST confirm-order
    |
    +-- locks the draft (prevents double confirm)
    +-- checks ownership, product availability and price again
    +-- calls the existing OrderCreationService
    v
Real order
```

The email and session ID in `ToolContext` come from the authenticated backend request. They are not model
arguments, so the model cannot request another customer's orders or draft an order in another session.

## API test order

Use the same Docker/Ollama setup from `docs/ai-rag.md`, index the RAG documents once, then log in and set the JWT
in Swagger.

1. Create a session:

```http
POST /api/ai/chat/sessions
```

Save the returned UUID.

2. Ask the agent to use live tools:

```http
POST /api/ai/chat/sessions/{sessionId}/messages
Content-Type: application/json

{
  "message": "Find up to three available rolls and show their current base prices."
}
```

3. Ask for a draft using a real product ID returned by the catalog tool:

```http
POST /api/ai/chat/sessions/{sessionId}/messages
Content-Type: application/json

{
  "message": "Create a pickup draft for 2 of product 1, paid on delivery."
}
```

The response includes the stored draft. A tool call never creates a real order.

4. Inspect persisted history and the current draft:

```http
GET /api/ai/chat/sessions/{sessionId}
```

5. Confirm explicitly:

```http
POST /api/ai/chat/sessions/{sessionId}/confirm-order
```

The confirm endpoint returns `409 Conflict` if the draft was already confirmed, a product became unavailable,
or a product price changed. Create a new draft in that case.

For `ONLINE` payment, confirmation creates the order with the same behavior as normal checkout; it does not charge
the customer. The existing Stripe checkout step still happens separately after the order is created.

## Main files to read

Read these in this order:

1. `AiChatController` - HTTP boundary and authenticated user.
2. `AiChatService` - RAG context, conversation history and the ChatClient call.
3. `AiTools` - tool definitions visible to the model.
4. `AiOrderDraftService` - pricing, draft persistence and safe confirmation.
5. `ChatSession`, `ChatMessage`, `OrderDraft` - persisted state.
6. `V26__create_ai_chat_tables.sql` - database schema.

The message endpoint intentionally returns normal JSON. SSE was not added because there is no AI frontend in this
phase. Streaming can be added later without changing tools, draft validation or confirmation logic.
