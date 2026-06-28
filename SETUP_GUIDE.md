# Khet Freshness Order Service - Setup Guide

## Overview
This guide will help you set up the development environment and configure all required API integrations for the Khet Freshness Order Service.

---

## Prerequisites

- Java 21 installed
- Docker Desktop installed and running
- Git
- A text editor (VS Code, IntelliJ IDEA, etc.)

---

## Step 1: Database Setup with Docker

### 1.1 Create `.env` file from template

```bash
cp .env.example .env
```

### 1.2 Edit `.env` and update database credentials (optional, defaults are fine for local dev)

```env
POSTGRES_DB=khet_freshness
POSTGRES_USER=khetuser
POSTGRES_PASSWORD=your_secure_password_here
DATABASE_URL=jdbc:postgresql://localhost:5432/khet_freshness
```

### 1.3 Start PostgreSQL with Docker Compose

```bash
docker-compose up -d
```

### 1.4 Verify database is running

```bash
docker ps
# Should show khet-freshness-db container running

docker logs khet-freshness-db
# Should show PostgreSQL ready to accept connections
```

### 1.5 Test connection (optional)

```bash
docker exec -it khet-freshness-db psql -U khetuser -d khet_freshness
# Type \dt to list tables
# Type \q to quit
```

---

## Step 2: Claude API Setup

### 2.1 Get API Key

1. Go to [https://console.anthropic.com](https://console.anthropic.com)
2. Sign up or log in
3. Navigate to **API Keys** section
4. Click **Create Key**
5. Copy the API key (starts with `sk-ant-api03-...`)

### 2.2 Update `.env` file

```env
CLAUDE_API_KEY=sk-ant-api03-your-actual-key-here
CLAUDE_MODEL=claude-sonnet-4-20250514
CLAUDE_API_URL=https://api.anthropic.com/v1/messages
CLAUDE_TIMEOUT_SECONDS=30
```

### 2.3 Test API Key (optional)

```bash
curl https://api.anthropic.com/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: $CLAUDE_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -d '{
    "model": "claude-sonnet-4-20250514",
    "max_tokens": 100,
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

---

## Step 3: Razorpay Setup

### 3.1 Create Razorpay Test Account

1. Go to [https://razorpay.com](https://razorpay.com)
2. Sign up for a new account
3. You'll be in **Test Mode** by default (perfect for development)

### 3.2 Get API Keys

1. Go to **Settings** → **API Keys**
2. Click **Generate Test Keys**
3. Copy both:
   - **Key ID** (starts with `rzp_test_`)
   - **Key Secret** (keep this secure)

### 3.3 Setup Webhook

1. Go to **Settings** → **Webhooks**
2. Click **Add New Webhook**
3. Enter URL: `https://your-ngrok-url.ngrok.io/webhook/razorpay` (see Step 6 for ngrok setup)
4. Select events:
   - `payment_link.paid`
   - `payment.captured`
5. Copy the **Webhook Secret**

### 3.4 Update `.env` file

```env
RAZORPAY_KEY_ID=rzp_test_your_key_id_here
RAZORPAY_KEY_SECRET=your_key_secret_here
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret_here
RAZORPAY_API_URL=https://api.razorpay.com/v1
```

---

## Step 4: WhatsApp Business Cloud API Setup

This is the most complex setup. Follow carefully.

### 4.1 Create Meta Developer Account

1. Go to [https://developers.facebook.com](https://developers.facebook.com)
2. Sign up or log in with your Facebook account
3. Click **My Apps** → **Create App**

### 4.2 Create Business App

1. Select **Business** as the app type
2. Fill in:
   - **App Name**: Khet Freshness Order Service
   - **App Contact Email**: your email
   - **Business Portfolio**: Create new or select existing
3. Click **Create App**

### 4.3 Add WhatsApp Product

1. In your app dashboard, find **WhatsApp** under **Add Products**
2. Click **Set Up**
3. You'll see the WhatsApp Getting Started page

### 4.4 Get Phone Number ID

1. On the Getting Started page, you'll see a **Test Number** provided by Meta
2. Copy the **Phone Number ID** (looks like `123456789012345`)
3. This test number can send messages to up to 5 verified numbers

### 4.5 Get Access Token (Temporary for Testing)

1. On the same page, you'll see a **Temporary Access Token**
2. Copy this token (valid for 24 hours)
3. For production, you'll need to generate a **Permanent Token** (see Section 4.8)

### 4.6 Get App Secret

1. Go to **Settings** → **Basic**
2. Copy the **App Secret** (click **Show** to reveal)

### 4.7 Setup Webhook

1. Go to **WhatsApp** → **Configuration**
2. Click **Edit** next to **Webhook**
3. Enter:
   - **Callback URL**: `https://your-ngrok-url.ngrok.io/webhook`
   - **Verify Token**: `khet_verify_token_12345` (choose your own secure token)
4. Click **Verify and Save**
5. Subscribe to **messages** field

### 4.8 Generate Permanent Access Token (for Production)

1. Go to **Settings** → **Basic**
2. Copy **App ID** and **App Secret**
3. Go to **WhatsApp** → **API Setup**
4. Follow the **System User Token** guide to create a permanent token
5. Alternatively, use the temporary token for development

### 4.9 Add Test Phone Numbers

1. Go to **WhatsApp** → **API Setup**
2. Under **To** section, click **Manage phone number list**
3. Add your personal WhatsApp number (with country code, e.g., +919876543210)
4. You'll receive a verification code on WhatsApp
5. Enter the code to verify

### 4.10 Update `.env` file

```env
WA_PHONE_NUMBER_ID=your_phone_number_id_here
WA_ACCESS_TOKEN=your_temporary_or_permanent_token_here
WA_APP_SECRET=your_app_secret_here
WA_VERIFY_TOKEN=khet_verify_token_12345
WA_API_URL=https://graph.facebook.com/v21.0
WA_WORKER_PHONE=919876543210
```

---

## Step 5: Application Configuration

### 5.1 Verify all environment variables in `.env`

Run this check:

```bash
cat .env
```

Ensure all the following are set:
- Database: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DATABASE_URL`
- Claude: `CLAUDE_API_KEY`
- Razorpay: `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`
- WhatsApp: `WA_PHONE_NUMBER_ID`, `WA_ACCESS_TOKEN`, `WA_APP_SECRET`, `WA_VERIFY_TOKEN`, `WA_WORKER_PHONE`

---

## Step 6: Expose Local Server with ngrok (for Webhooks)

Webhooks require a publicly accessible HTTPS URL. Use ngrok for local development.

### 6.1 Install ngrok

```bash
# macOS
brew install ngrok

# Or download from https://ngrok.com/download
```

### 6.2 Start ngrok

```bash
ngrok http 8080
```

### 6.3 Copy the HTTPS URL

You'll see output like:
```
Forwarding   https://abc123.ngrok.io -> http://localhost:8080
```

Copy the `https://abc123.ngrok.io` URL.

### 6.4 Update Webhook URLs

**For WhatsApp:**
1. Go to Meta Developer Console → WhatsApp → Configuration
2. Update Callback URL to: `https://abc123.ngrok.io/webhook`

**For Razorpay:**
1. Go to Razorpay Dashboard → Settings → Webhooks
2. Update URL to: `https://abc123.ngrok.io/webhook/razorpay`

---

## Step 7: Build and Run the Application

### 7.1 Build the project

```bash
./gradlew clean build
```

### 7.2 Run the application

```bash
./gradlew bootRun
```

Or run from your IDE.

### 7.3 Verify startup

Check logs for:
- ✅ Database connection successful
- ✅ Flyway migrations applied
- ✅ Application started on port 8080

---

## Step 8: Test the Setup

### 8.1 Test Webhook Verification (WhatsApp)

Meta will automatically send a verification request when you set up the webhook.

### 8.2 Test Sending a Message

Once the application is running, send a WhatsApp message to your Meta test number:

```
5kg gehun chahiye
```

You should receive a reply from the bot!

### 8.3 Check Database

```bash
docker exec -it khet-freshness-db psql -U khetuser -d khet_freshness

# Check customers
SELECT * FROM customers;

# Check conversation state
SELECT * FROM order_conversation_state;

# Exit
\q
```

---

## Common Issues

### Issue: Database connection failed

**Solution:**
- Ensure Docker is running: `docker ps`
- Check database logs: `docker logs khet-freshness-db`
- Verify DATABASE_URL in `.env` matches your PostgreSQL config

### Issue: Claude API authentication failed

**Solution:**
- Verify your API key is correct in `.env`
- Test the key with curl (see Step 2.3)
- Ensure no extra spaces in the `.env` file

### Issue: WhatsApp webhook verification failed

**Solution:**
- Ensure ngrok is running and URL is correct
- Verify `WA_VERIFY_TOKEN` in `.env` matches what you entered in Meta console
- Check application logs for webhook verification errors

### Issue: Razorpay payment link creation failed

**Solution:**
- Verify you're using Test Mode keys (start with `rzp_test_`)
- Check that both Key ID and Key Secret are set
- Ensure Basic Auth is working (test with Razorpay API docs)

---

## Next Steps

After successful setup:

1. ✅ Send test WhatsApp messages to test order flow
2. ✅ Monitor database to see customer and order creation
3. ✅ Test payment flow with Razorpay test cards
4. ✅ Review logs for any errors or warnings
5. ✅ Start customizing the code for your specific needs

---

## Development Tips

### Useful Commands

```bash
# Restart database
docker-compose restart

# View database logs
docker logs -f khet-freshness-db

# Stop database
docker-compose down

# Remove database and start fresh
docker-compose down -v

# Rebuild and run application
./gradlew clean build && ./gradlew bootRun
```

### Debugging

- Enable SQL logging in `application.yml`: `spring.jpa.show-sql: true`
- Check application logs for detailed error messages
- Use `docker exec -it khet-freshness-db psql -U khetuser -d khet_freshness` to inspect database

---

## Security Notes

- **Never commit `.env` file** to Git (already in `.gitignore`)
- **Use separate credentials for production**
- **Rotate API keys periodically**
- **Use environment variables for all secrets**
- **Enable webhook signature verification in production**

---

## Support

For issues or questions:
- Check application logs first
- Review this setup guide
- Check the technical specification document
- Review API documentation:
  - Claude: https://docs.anthropic.com
  - WhatsApp: https://developers.facebook.com/docs/whatsapp
  - Razorpay: https://razorpay.com/docs

---

**Happy Coding! 🚀**
