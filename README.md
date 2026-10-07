# E-Commerce Microservices Assessment

## Architecture & Design

### System Design

- **order-service** owns the order lifecycle and maintains system of record for orders.
- **notification-service** consumes the order events and keeps an audit record in PostgreSQL of what was notified.
- These 2 services communicate over REST, but the notification call is decoupled from the order transaction. When an order is created, updated, or cancelled, order-service publishes an internal domain event. After the database transaction commits, a listener calls notification-service over REST, forwarding the tenant context.

Since this system includes only two services I decided to implement REST, which keeps the system simple to run and operate. The notification call is deliberately decoupled from the order transaction so order persistence never depends on notification-service being available. If this grew beyond two services or needed guaranteed fan-out, I would implement an event broker. The current design keeps that migration path available by already modeling order changes as events.

### Multi-Tenancy Design

- Tenancy is enforced consistently throughout the service.

- Tenant header,`X-Tenant-ID`, is mandatory and validated in the TenantInterceptor before any business logic runs and is rejected with a 400 Bad Request error. This way the service will never process code for any unknown client.
- Between services, when the order service calls the notification service, it forwards the `X-Tenant-ID` header. This passes the tenant identity along so isolation is preserved.
- Every GET operation is tenant-scoped (`findByIdAndTenantId`, `findByTenantId`) and `tenant_id` is non null and indexed on every table. Since queries are tenant scoped, this leads to less data leaking and since it must be non null, no row can exist without a tenant.

### Tenant Context Propagation

- The tenant context flows inbound through the request carrying the `X-Tenant-ID` header.
- A TenantInterceptor runs before the controllers which validates the header and puts it in a TenantContext, which is backed by a ThreadLocal and cleared afterCompletion. This way a pooled thread never carries one requests tenant into the next.
- The service and the data layers read the tenant from the TenantContext.
- When order service calls notification service, the tenant is published on the event and sent to the notification service's TenantInterceptor.

### Resilience and failure handling

This system is designed in such a way to prioritize that **an order write must never fail because of notification-service**, and no event may be silently lost.

- Notifications are sent on a transaction-synchronized hook (`@TransactionalEventListener(AFTER_COMMIT)`) that fires only after the order commits. If the order transaction fails, no notification is sent.
- The HTTP client has explicit connect and read timeouts, so a slow or hung notification-service cannot exhaust caller threads.
- If notification-service is down or returns an error, the attempt is recorded in a `failed_notifications` table rather than swallowed in a log line. That makes the failure reconcilable, or atleast adds observability.

### Industry Standards Applied

- Each service has a single responsibility. The order service manages orders and the notification service manages notifications.
- Each service never reads or writes into/from another service's table. Even though there is 1 postgres instance, each service has its own seperate tables and no cross access.
- Domain events and transactional event listener decouples the notification from saving the order
- State machine for order lifecycle means there is an explicit path for the status to follow and invalid updates return conflicts.
- DTO (data transfer object) are created to keep the API contract seperate from the internal data model.

## Production Readiness

- In the docker compose, I would add health checks and gate the app startup on dependency readiness for reliable cold starts.
- For the notification delivery, I would implement a message broker such as SNS/SQS. The order service would publish messages to a SNS topic and the notification service would consume from the SQS queue subscribed to it. I would also implement a DLQ which would catch failed messages with a redrive policy.
- I would add more observability such as metrics and alarms on the number of messages in the DLQ and delivery success/failure rate.
- I would add idempotency. SNS and SQS delivers at least once so there may be duplicates, so I would ensure that there is unique key (tenant_id, order_id, event_type, or another unique id) so that if a message delivers twice it does not notify the customer again.
- When retrying after a transient failure, it would be retried via SQS visibility timeout backoff using exponential intervals with jitter. After the retries are exhausted, it would go to the DLQ.

## Assumptions

- Notifications have eventual consistency and may be delivered slightly after the order responds. It is assumed that the delivery time does not need to be immedediate.
- Assuming that the order does not matter for the notifications sent from the order service.
- There is a single shared PostgreSQL instance with a table per service, representing the logical database per service boundary without the operational cost of two databases for the assessment.
- IDs are server generated UUIDs.
- Tenant identity is provided by a trusted upstream (API gateway / auth layer) via `X-Tenant-ID`. Authentication is out of scope for this assessment.
- Order fields are intentionally minimal, `customerEmail` and `totalAmount`. In production, there would likely be additional fields such as quantities, currency, addresses, and payment. These are all are out of scope.
- Notifications are simulated (structured log + persisted audit record), with `simulation_status` always `SIMULATED_SENT` today, which is modeled as the extension point for real delivery states (sent/failed/bounced).

---

### Order lifecycle

The following statuses are implemented to represent different stages of an order: `PENDING`, `PROCESSING`, `COMPLETED`, `CANCELLED`.

| From       | Allowed to Update to             |
| ---------- | -------------------------------- |
| PENDING    | PROCESSING, COMPLETED, CANCELLED |
| PROCESSING | COMPLETED, CANCELLED             |
| COMPLETED  | N/A.                             |
| CANCELLED  | N/A.                             |

Illegal updates (updating an already cancelled order or cancelling an already completed order) are rejected with `409 Conflict` error. An order already processing can also not be updated to pending. Each change emits an event type: `ORDER_CREATED`, `ORDER_UPDATED`, `ORDER_COMPLETED`, `ORDER_CANCELLED`.

---

## Running the System

Both images are published to Docker Hub and referenced from `docker-compose.yml`:

```bash
docker compose up
```

| Service                 | URL                                   |
| ----------------------- | ------------------------------------- |
| order-service           | http://localhost:8080                 |
| notification-service    | http://localhost:8081                 |
| Swagger (orders)        | http://localhost:8080/swagger-ui.html |
| Swagger (notifications) | http://localhost:8081/swagger-ui.html |

PostgreSQL listens on `localhost:5432` (`ecommerce_db` / `postgres` / `password`).

### Running the tests:

```bash
cd order_service && ./mvnw test
cd notification_service && ./mvnw test
```

---

## API

All endpoints require the `X-Tenant-ID` header.

### order-service - `/api/orders`

| Method | Path                              | Description              | Success |
| ------ | --------------------------------- | ------------------------ | ------- |
| POST   | `/api/orders`                     | Create an order          | `201`   |
| GET    | `/api/orders`                     | List orders for a tenant | `200`   |
| GET    | `/api/orders/{id}`                | Get one order            | `200`   |
| PUT    | `/api/orders/{id}/status?status=` | Update order status      | `200`   |
| POST   | `/api/orders/{id}/cancel`         | Cancel an order          | `200`   |

Create an order:

```bash
curl -X 'POST' \
  'http://localhost:8080/api/orders' \
  -H 'accept: */*' \
  -H 'X-Tenant-ID: default-tenant' \
  -H 'Content-Type: application/json' \
  -d '{
  "customerEmail": "anushka_chaudhari@yahoo.com",
  "totalAmount": 5.00
}'
```

Update status:

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/orders/b8dc1037-33ef-436a-a7b3-fbfbbc885a20/status?status=PROCESSING' \
  -H 'accept: */*' \
  -H 'X-Tenant-ID: default-tenant'
```

Cancel:

```bash
curl -X 'POST' \
  'http://localhost:8080/api/orders/29e869a0-1d38-4eb4-9de0-aea495b99fce/cancel' \
  -H 'accept: */*' \
  -H 'X-Tenant-ID: default-tenant' \
  -d ''
```

### notification-service - `/api/notifications`

| Method | Path                 | Description                                          | Success |
| ------ | -------------------- | ---------------------------------------------------- | ------- |
| POST   | `/api/notifications` | Receive an order event, simulate send, persist a log | `201`   |
| GET    | `/api/notifications` | List notification logs for the tenant                | `200`   |

### Error responses

- Errors return a structured JSON body (`timestamp`, `status`, `error`, `message`, `path`)
- Validation errors return a `messages` map of field → reason.
- Status codes: `400` (validation / bad request / missing tenant), `404` (not found), `409` (illegal status transition), `500` (unexpected).

---

## Data Model

**orders**: `id` (UUID), `tenant_id`, `customer_email`, `total_amount`, `status`, `created_at`, `updated_at`. Indexed on `tenant_id`.

**notification_logs**: `id` (UUID), `tenant_id`, `order_id`, `event_type`, `customer_email`, `amount`, `simulation_status`, `sent_at`. Indexed on `tenant_id`. `simulation_status` records the outcome of the simulated send (currently always `SIMULATED_SENT`; for production we would use real delivery states such as sent/failed/bounced).

**failed_notifications**: `id` (UUID), `tenant_id`, `order_id`, `event_type`, `customer_email`, `amount`, `failure_reason`, `failed_at`. Maintains record of undelivered notifications for reconciliation.

---
