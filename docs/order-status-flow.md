# Order status flow

How `Order.status` changes depending on `paymentMethod`, Stripe, and `deliveryMethod`. Traced from
`OrderCreationService`, `OrderService`, `PaymentService`, `StripeService`, and `OrderStatus.validateTransition`.

## Enums involved

- `OrderStatus`: `NEW, CONFIRMED, COOKING, READY, DELIVERING, DELIVERED, CANCELLED`
- `DeliveryMethod`: `DELIVERY, PICKUP`
- `PaymentMethod`: `ONLINE, ON_DELIVERY`
- `PaymentStatus` (on the `Payment` entity, separate from `OrderStatus`): `PENDING, PAID, FAILED, REFUNDED`

## 1. Order creation branches on paymentMethod

```text
Customer creates order
    |
    v
paymentMethod?
    |
    +-- ON_DELIVERY --> status = CONFIRMED immediately (no Stripe step)
    |
    +-- ONLINE      --> status = NEW (default), waits for Stripe payment
```

Source: `OrderCreationService.java:50-52`.

## 2. Stripe payment flow (ONLINE only)

```text
status = NEW
    |
    | POST /api/payments/order/{orderId}
    v
Stripe Checkout Session created
Payment row created, status = PENDING
    |
    | customer pays on Stripe hosted page
    v
Stripe sends webhook: checkout.session.completed
    |
    | POST /api/payments/webhook
    v
PaymentService.confirmPayment
Payment.status = PAID
    |
    v
PaymentConfirmedEvent published
    |
    v
OrderService.confirmOrder
Order.status = CONFIRMED   <-- set directly, does NOT run validateTransition
```

If payment never completes, the order just stays at `NEW` (or gets cancelled manually). The webhook path only
ever produces `PENDING` -> `PAID`; there's no automatic `FAILED` handling wired up.

## 3. After CONFIRMED, deliveryMethod decides the branch

```text
                         CONFIRMED
                             |
                          COOKING
                             |
                           READY
                             |
              +--------------+--------------+
              |                             |
          PICKUP                        DELIVERY
              |                             |
          DELIVERED                    DELIVERING
      (admin sets directly)      (admin calls /dispatch,
                                   assigns a courier)
                                             |
                                        DELIVERED
                                   (only that courier)
```

| Delivery method | Allowed path                                             | Who can move it |
|---|---|---|
| `PICKUP`   | `CONFIRMED -> COOKING -> READY -> DELIVERED`                        | Admin only. `DELIVERING` is blocked entirely. |
| `DELIVERY` | `CONFIRMED -> COOKING -> READY -> DELIVERING -> DELIVERED`          | Admin drives up to `READY`; only `dispatch()` sets `DELIVERING`; only the assigned courier sets `DELIVERED`. |

Blocked cases enforced in `OrderService` / `OrderStatus.validateTransition`:

- Admin sets `DELIVERING` via the normal status endpoint -> rejected, must use `/dispatch`.
- Admin sets `DELIVERED` on a `DELIVERY` order -> forbidden, only the assigned courier can.
- Courier touches a `PICKUP` order, an order not assigned to them, or sets anything other than `DELIVERED` -> forbidden.
- Anyone sets `DELIVERING` on a `PICKUP` order -> rejected regardless of role.

## 4. Cancellation

`CANCELLED` is only reachable from `NEW`. Since `ON_DELIVERY` orders skip `NEW` entirely (they start at
`CONFIRMED`), they can never be cancelled through this transition — only unpaid `ONLINE` orders (still sitting at
`NEW`) can.
