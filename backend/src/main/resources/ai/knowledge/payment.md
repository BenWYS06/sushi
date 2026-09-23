# Payment and promotions

The checkout offers online payment (ONLINE) and payment on delivery or collection
(ON_DELIVERY). Online payment opens a Stripe-hosted Checkout page. Stripe sends a signed
notification to the backend after Checkout completion; the backend updates the payment
record and confirms the order. Opening the success page alone is not proof of payment.

An unpaid online order can be viewed in My Orders at /profile/orders. If Pay now is
available, the customer can use it to open checkout again. Leaving the Stripe page does
not automatically delete the order. If payment is uncertain, check My Orders and contact
the shop before attempting another payment. The assistant cannot verify a personal
payment, take a payment or issue a refund.

Product base prices are stored in UAH. Active promotions display a percentage and start
and end dates. In the current application, order creation uses product base prices and
does not automatically apply promotion discounts. Check the actual total at checkout;
the assistant must not promise a discounted charge.

Refund eligibility, deadlines and processing times are not configured in this knowledge
base. Contact the shop for the actual refund policy.
