# Quick Reference Guide

## Project Structure at a Glance

```
order/
├── .env                          # ⚠️ Your secrets (git-ignored)
├── .env.example                  # Template with placeholders
├── docker-compose.yml            # PostgreSQL setup
├── build.gradle                  # Dependencies
├── README.md                     # Project overview
├── SETUP_GUIDE.md               # Detailed setup steps
├── IMPLEMENTATION_STATUS.md      # What's done, what's pending
└── src/main/
    ├── java/in/agri/order/
    │   ├── config/              # ✅ All configuration classes
    │   ├── controller/          # ⏳ TODO: Webhook controllers
    │   ├── dto/                 # ✅ All DTOs for APIs
    │   ├── model/               # ✅ All entities & enums
    │   ├── repository/          # ✅ All Spring Data repos
    │   └── service/             # ⏳ TODO: Business logic
    └── resources/
        ├── application.yml      # ✅ App configuration
        └── db/migration/        # ✅ Flyway SQL scripts
```

---

## Essential Commands

### Docker & Database
```bash
# Start PostgreSQL
docker-compose up -d

# Stop PostgreSQL
docker-compose down

# View logs
docker logs -f khet-freshness-db

# Connect to database
docker exec -it khet-freshness-db psql -U khetuser -d khet_freshness

# Reset database (WARNING: deletes all data)
docker-compose down -v && docker-compose up -d
```

### Application
```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies
```

### ngrok (for webhooks)
```bash
# Start ngrok
ngrok http 8080

# Keep this running in a separate terminal!
```

---

## Environment Variables Checklist

Your `.env` file must have:

### Database (defaults work for local dev)
- ✅ `POSTGRES_DB=khet_freshness`
- ✅ `POSTGRES_USER=khetuser`
- ✅ `POSTGRES_PASSWORD=your_password`
- ✅ `DATABASE_URL=jdbc:postgresql://localhost:5432/khet_freshness`

### Claude API (get from console.anthropic.com)
- ⚠️ `CLAUDE_API_KEY=sk-ant-api03-...` ← **MUST SET**
- ✅ `CLAUDE_MODEL=claude-sonnet-4-20250514`

### Razorpay (get from razorpay.com dashboard)
- ⚠️ `RAZORPAY_KEY_ID=rzp_test_...` ← **MUST SET**
- ⚠️ `RAZORPAY_KEY_SECRET=...` ← **MUST SET**
- ⚠️ `RAZORPAY_WEBHOOK_SECRET=...` ← **MUST SET**

### WhatsApp Business API (get from developers.facebook.com)
- ⚠️ `WA_PHONE_NUMBER_ID=...` ← **MUST SET**
- ⚠️ `WA_ACCESS_TOKEN=...` ← **MUST SET**
- ⚠️ `WA_APP_SECRET=...` ← **MUST SET**
- ⚠️ `WA_VERIFY_TOKEN=khet_verify_token_12345` ← **Choose your own**
- ⚠️ `WA_WORKER_PHONE=919876543210` ← **Your phone number**

---

## Database Quick Queries

```sql
-- List all tables
\dt

-- Check customers
SELECT id, phone, language, created_at FROM customers;

-- Check orders
SELECT id, customer_id, item, quantity_kg, status, created_at FROM orders;

-- Check stock
SELECT product_item, available_kg, reserved_kg,
       (available_kg - reserved_kg) as free_kg FROM stock;

-- Check conversation state
SELECT phone, state, item, quantity_kg FROM order_conversation_state;

-- Check prices
SELECT product_item, sell_price, effective_date FROM price_records
ORDER BY product_item, effective_date DESC;

-- Clean up test data
TRUNCATE customers CASCADE;
TRUNCATE order_conversation_state CASCADE;

-- Exit psql
\q
```

---

## API Testing with curl

### Test Claude API
```bash
curl -X POST https://api.anthropic.com/v1/messages \
  -H "x-api-key: YOUR_CLAUDE_API_KEY" \
  -H "Content-Type: application/json" \
  -H "anthropic-version: 2023-06-01" \
  -d '{
    "model": "claude-sonnet-4-20250514",
    "max_tokens": 100,
    "messages": [{"role": "user", "content": "Say hello"}]
  }'
```

### Test WhatsApp Send
```bash
curl -X POST "https://graph.facebook.com/v21.0/YOUR_PHONE_NUMBER_ID/messages" \
  -H "Authorization: Bearer YOUR_WA_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "messaging_product": "whatsapp",
    "to": "919876543210",
    "type": "text",
    "text": {"body": "Test message from Khet Freshness"}
  }'
```

### Test Razorpay Auth
```bash
curl -X GET https://api.razorpay.com/v1/payments \
  -u YOUR_KEY_ID:YOUR_KEY_SECRET
```

---

## Webhook URLs (with ngrok)

Once ngrok is running at `https://abc123.ngrok.io`:

### WhatsApp Webhook
- **Callback URL**: `https://abc123.ngrok.io/webhook`
- **Verify Token**: Whatever you set in `WA_VERIFY_TOKEN`
- **Subscribe to**: messages

### Razorpay Webhook
- **Webhook URL**: `https://abc123.ngrok.io/webhook/razorpay`
- **Events**: `payment_link.paid`, `payment.captured`
- **Secret**: Whatever Razorpay generates (save to `.env`)

---

## Implementation Priority Order

1. **Setup environment** (1-2 hours)
   - Get all API keys
   - Configure `.env`
   - Start database
   - Verify connections

2. **ClaudeService** (2-3 hours)
   - Order intent parsing
   - Address validation
   - Error handling

3. **WhatsAppService** (2-3 hours)
   - Send text messages
   - HMAC verification
   - Message templates

4. **RazorpayService** (2 hours)
   - Create payment links
   - HMAC verification

5. **CustomerService + StockService** (2 hours)
   - Customer CRUD
   - Stock reservation logic

6. **OrderProcessingService** (4-5 hours)
   - State machine
   - Main business logic
   - Integration of all services

7. **Controllers** (2-3 hours)
   - WhatsApp webhook
   - Razorpay webhook
   - Async processing

8. **Scheduled Jobs** (1-2 hours)
   - Packing manifest
   - Cleanup jobs

9. **Testing** (2-3 hours)
   - Unit tests
   - Integration tests
   - Manual E2E testing

**Total estimated time**: 20-25 hours

---

## Common Error Solutions

### "Database connection refused"
→ Is Docker running? `docker ps`
→ Is PostgreSQL started? `docker-compose up -d`
→ Check `DATABASE_URL` in `.env`

### "Claude API authentication failed"
→ Is `CLAUDE_API_KEY` set correctly in `.env`?
→ No extra spaces or quotes around the key?
→ Test with curl (see above)

### "WhatsApp verification failed"
→ Is ngrok running?
→ Did you update the webhook URL in Meta console?
→ Does `WA_VERIFY_TOKEN` match in both places?

### "Razorpay payment link creation failed"
→ Using test keys (`rzp_test_...`)?
→ Both Key ID and Secret set?
→ Amount in paise (multiply by 100)?

### "Flyway migration failed"
→ Database not empty? Drop and recreate: `docker-compose down -v && docker-compose up -d`
→ Check migration script syntax

---

## Key Design Decisions

### Why PostgreSQL?
- ACID guarantees for atomic stock reservation
- Rich data types (JSONB, ENUMs)
- Proven at scale

### Why pessimistic locking for stock?
- Prevents overselling
- Simple to reason about
- Acceptable for Phase 1 scale

### Why return HTTP 200 always for webhooks?
- Meta/Razorpay retry on non-2xx
- Retries cause duplicate processing
- Better to log errors and handle async

### Why separate conversation state table?
- Orders are permanent, conversations are temporary
- Easy to clean up old conversations
- Clearer separation of concerns

---

## Files You WILL Create

```
service/
├── ClaudeService.java           # AI integration
├── WhatsAppService.java         # Messaging
├── RazorpayService.java         # Payments
├── CustomerService.java         # Customer management
├── StockService.java            # Stock operations
├── OrderProcessingService.java  # Main business logic
├── MessageTemplates.java        # Localization
└── ScheduledJobsService.java    # Background jobs

controller/
├── WhatsAppWebhookController.java
└── RazorpayWebhookController.java
```

---

## Useful Keyboard Shortcuts

### In psql
- `\dt` - list tables
- `\d table_name` - describe table
- `\q` - quit
- `\l` - list databases
- `\c dbname` - connect to database

### In Gradle
- `./gradlew tasks` - see all tasks
- `./gradlew dependencies --configuration runtimeClasspath` - see dependencies
- `./gradlew build --info` - verbose build

---

## Pro Tips

1. **Test APIs individually first** before integrating
2. **Use Postman** for testing webhooks locally
3. **Keep ngrok running** - don't restart or URL changes
4. **Check logs first** - most errors are logged clearly
5. **Start with stubs** - implement empty methods, fill in logic later
6. **Use @Transactional** - wrap any method that modifies data
7. **Don't forget timezones** - always use `Asia/Kolkata`
8. **Test with real WhatsApp** - send messages to yourself
9. **Use Razorpay test mode** - don't need real money
10. **Git commit often** - small, frequent commits

---

## Support Resources

📖 **Your Docs**:
- `README.md` - Project overview
- `SETUP_GUIDE.md` - Detailed setup
- `IMPLEMENTATION_STATUS.md` - What's done/pending

📋 **Official Docs**:
- Spring Boot: https://spring.io/guides
- Claude API: https://docs.anthropic.com
- WhatsApp: https://developers.facebook.com/docs/whatsapp
- Razorpay: https://razorpay.com/docs

💻 **Tools**:
- IntelliJ IDEA (recommended)
- Postman (for API testing)
- DBeaver (for database GUI)
- ngrok (for webhooks)

---

**You're ready to go! Start with SETUP_GUIDE.md and work through the implementation step by step.**

**Good luck! 🚀**
