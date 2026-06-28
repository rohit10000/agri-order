# Testing Guide - Order Processing Service

This guide shows you how to test the order processing service before integrating with the real database and Razorpay.

---

## What's Implemented

✅ **In-Memory Data Store** - Simulates PostgreSQL tables
✅ **Claude API Integration** - REAL integration for AI parsing
✅ **Order Processing Service** - Complete state machine
✅ **WhatsApp Webhook** - Receives messages
✅ **Test Controller** - For easy manual testing
❌ **PostgreSQL** - Not connected yet (using in-memory)
❌ **Razorpay** - Simulated (just generates fake links)
❌ **WhatsApp Send** - Simulated (just logs messages)

---

## Prerequisites

### 1. Get Claude API Key

**This is the ONLY external service you need to set up for testing!**

1. Go to https://console.anthropic.com
2. Sign up or log in
3. Navigate to **API Keys**
4. Click **Create Key**
5. Copy the key (starts with `sk-ant-api03-...`)

### 2. Create `.env` File

```bash
cd /Users/shwetasingh/Downloads/order
cp .env.example .env
```

Edit `.env` and add your Claude API key:

```env
# Claude API (REQUIRED for testing)
CLAUDE_API_KEY=sk-ant-api03-YOUR-ACTUAL-KEY-HERE
CLAUDE_MODEL=claude-sonnet-4-20250514
CLAUDE_API_URL=https://api.anthropic.com/v1/messages
CLAUDE_TIMEOUT_SECONDS=30

# Database (not needed for testing - using in-memory)
POSTGRES_DB=khet_freshness
POSTGRES_USER=khetuser
POSTGRES_PASSWORD=test123
DATABASE_URL=jdbc:postgresql://localhost:5432/khet_freshness

# Other services (not needed for testing - simulated)
RAZORPAY_KEY_ID=test_key
RAZORPAY_KEY_SECRET=test_secret
RAZORPAY_WEBHOOK_SECRET=test_webhook_secret
WA_PHONE_NUMBER_ID=test_phone_id
WA_ACCESS_TOKEN=test_token
WA_APP_SECRET=test_secret
WA_VERIFY_TOKEN=test_verify
WA_WORKER_PHONE=919876543210
```

**IMPORTANT**: Only `CLAUDE_API_KEY` needs to be real. All other values can be dummy for now.

---

## Build and Run

### 1. Build the Project

```bash
./gradlew clean build -x test
```

### 2. Run the Application

```bash
./gradlew bootRun
```

You should see:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

Started OrderApplication in X.XXX seconds
```

**The application is now running on http://localhost:8080**

---

## Testing Scenarios

### Test 1: Health Check

**Verify the application is running:**

```bash
curl http://localhost:8080/test/health
```

**Expected response:**
```json
{
  "status": "healthy",
  "service": "Khet Freshness Order Service",
  "mode": "test"
}
```

---

### Test 2: Simple Order (New Customer)

**Send a simple order message:**

```bash
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "919876543210",
    "message": "5kg gehun chahiye"
  }'
```

**Expected response:**
```json
{
  "status": "processed",
  "phone": "919876543210",
  "message": "Message processed successfully. Check logs for WhatsApp responses."
}
```

**Check the application logs**, you should see:

1. 📨 Message received and parsed
2. Claude API call to parse intent
3. 📱 WhatsApp message asking for address (simulated - logged only)
4. Customer and conversation state created

**Example log output:**
```
📨 Processing message from 919876543210: 5kg gehun chahiye
Calling Claude API with model: claude-sonnet-4-20250514
Parsed intent: item=WHEAT, quantity=5, needs_clarification=false
📱 [WhatsApp Simulated] To: 919876543210 | Message: कृपया अपना पूरा डिलीवरी पता दें...
```

---

### Test 3: Provide Address

**Now provide the address:**

```bash
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "919876543210",
    "message": "B-14, Tulsi Nagar, Lanka ke paas, Varanasi"
  }'
```

**Check logs**, you should see:

1. Address validated by Claude
2. Stock reserved (5kg of WHEAT)
3. Order created
4. Payment link generated (simulated)
5. 📱 WhatsApp message with order confirmation and payment link

**Example log output:**
```
Validating address: B-14, Tulsi Nagar, Lanka ke paas, Varanasi
Address validation: valid=true, missing=null
✅ Order created: #1 for WHEAT × 5.00 kg
💳 [Razorpay Simulated] Created payment link for order #1: https://razorpay-simulated.com/pay/pay_sim_abc12345
📱 [WhatsApp Simulated] To: 919876543210 | Message: ✅ ऑर्डर कन्फर्म हो गया!...
```

---

### Test 4: Check Data Store Status

**View current state:**

```bash
curl http://localhost:8080/test/status
```

**Expected response:**
```json
{
  "customers": 1,
  "orders": 1,
  "conversations": 0,
  "stock": {
    "WHEAT": {
      "availableKg": 500,
      "reservedKg": 5,
      "freeKg": 495
    },
    "RICE": { ... },
    "ATTA": { ... }
  }
}
```

---

### Test 5: Ambiguous Order (Clarification Flow)

**Test the clarification flow:**

```bash
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "919999999999",
    "message": "kuch chahiye"
  }'
```

**Check logs:**
- Claude detects unclear intent
- WhatsApp message asks "What would you like to order?" with product list

**Then clarify:**

```bash
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "919999999999",
    "message": "10kg chawal"
  }'
```

**Check logs:**
- Intent now clear: RICE × 10kg
- Asks for address

---

### Test 6: Returning Customer (Saved Address)

**Place another order with the same phone:**

```bash
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "919876543210",
    "message": "3kg atta chahiye"
  }'
```

**Check logs:**
- System recognizes returning customer
- Shows saved address for confirmation
- Waits for "1" (confirm) or "2" (new address)

**Confirm saved address:**

```bash
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "919876543210",
    "message": "1"
  }'
```

**Check logs:**
- Uses saved address
- Creates order immediately
- Sends payment link

---

### Test 7: English Language

**Test English language detection:**

```bash
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "918888888888",
    "message": "I need 7kg wheat"
  }'
```

**Check logs:**
- Claude detects language: "english"
- Customer language preference updated to ENGLISH
- All responses in English

---

### Test 8: Insufficient Stock

**Test stock constraint:**

```bash
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "917777777777",
    "message": "1000kg gehun"
  }'
```

**Check logs:**
- Stock check fails (only 500kg available)
- WhatsApp message offers available quantity

---

### Test 9: Reset Data

**Clear all test data:**

```bash
curl -X POST http://localhost:8080/test/reset
```

**Expected response:**
```json
{
  "status": "reset",
  "message": "All data cleared"
}
```

All customers, orders, and conversation states are deleted. Stock is reset to 500kg for each product.

---

## Testing the WhatsApp Webhook Endpoint

### Webhook Verification (GET)

**Test Meta's verification handshake:**

```bash
curl "http://localhost:8080/webhook?hub.mode=subscribe&hub.verify_token=test&hub.challenge=test123"
```

**Expected response:**
```
test123
```

### Webhook Message (POST)

**Simulate a WhatsApp message webhook:**

```bash
curl -X POST http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "object": "whatsapp_business_account",
    "entry": [{
      "id": "123",
      "changes": [{
        "value": {
          "messaging_product": "whatsapp",
          "messages": [{
            "id": "wamid_xxx",
            "from": "919876543210",
            "type": "text",
            "text": {
              "body": "5kg gehun chahiye"
            }
          }]
        }
      }]
    }]
  }'
```

**Expected:**
- HTTP 200 OK
- Message processed asynchronously
- Check logs for processing details

---

## Complete End-to-End Test

**Follow this sequence:**

```bash
# 1. Reset data
curl -X POST http://localhost:8080/test/reset

# 2. New order
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{"phone":"919876543210","message":"5kg gehun chahiye"}'

# Wait 2-3 seconds for Claude API response

# 3. Provide address
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{"phone":"919876543210","message":"B-14, Tulsi Nagar, Lanka, Varanasi"}'

# 4. Check status
curl http://localhost:8080/test/status

# 5. Place another order (returning customer)
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{"phone":"919876543210","message":"3kg chawal"}'

# 6. Confirm saved address
curl -X POST http://localhost:8080/test/message \
  -H "Content-Type: application/json" \
  -d '{"phone":"919876543210","message":"1"}'

# 7. Check final status
curl http://localhost:8080/test/status
```

**You should have:**
- 1 customer
- 2 orders
- 0 active conversations (cleared after order)
- Stock: WHEAT -5kg, RICE -3kg

---

## Troubleshooting

### Issue: "Claude API authentication failed"

**Solution:**
- Check `CLAUDE_API_KEY` in `.env`
- Ensure no extra spaces or quotes
- Test key with curl:

```bash
curl -X POST https://api.anthropic.com/v1/messages \
  -H "x-api-key: YOUR_KEY" \
  -H "Content-Type: application/json" \
  -H "anthropic-version: 2023-06-01" \
  -d '{
    "model": "claude-sonnet-4-20250514",
    "max_tokens": 100,
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

### Issue: "Application won't start"

**Solution:**
- Check for port 8080 already in use: `lsof -i :8080`
- Kill existing process or change port in `application.yml`
- Check logs for error details

### Issue: "No response from Claude"

**Solution:**
- Check internet connection
- Verify Claude API key is valid
- Check logs for timeout errors (default 30s)

### Issue: "Messages not being processed"

**Solution:**
- Check application logs
- Ensure async is enabled (@EnableAsync in AppConfig)
- Try sequential processing (remove @Async temporarily)

---

## Viewing Logs

**Watch logs in real-time:**

```bash
./gradlew bootRun | grep -E "(📨|📱|💳|✅|❌)"
```

This filters for important emoji-prefixed log messages.

---

## Next Steps

After testing successfully:

1. ✅ **You've verified the core order flow works!**
2. ✅ **Claude AI integration is working**
3. ⏳ **Next: Integrate PostgreSQL** (replace InMemoryDataStore)
4. ⏳ **Next: Integrate Razorpay** (real payment links)
5. ⏳ **Next: Integrate WhatsApp** (real message sending)

---

## API Reference

### Test Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/test/message` | Send simulated WhatsApp message |
| GET | `/test/status` | Get data store status |
| POST | `/test/reset` | Reset all data |
| GET | `/test/health` | Health check |

### WhatsApp Webhook

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/webhook` | Verification handshake |
| POST | `/webhook` | Receive inbound messages |

---

**Happy Testing! 🎉**

Questions? Check the application logs - they're very detailed!
