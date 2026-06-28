# Khet Freshness - Order Service

**WhatsApp-based AI-native order management system for farm-to-consumer direct sales**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)

---

## Overview

Khet Freshness connects farmers near Varanasi directly with city consumers through WhatsApp, eliminating middlemen and ensuring farmers get fair prices while consumers get fresh, traceable produce.

### Key Features

- 📱 **WhatsApp Interface**: Natural language ordering in Hindi, English, and Hinglish
- 🤖 **AI-Powered**: Claude AI for intent parsing and address validation
- 💳 **Seamless Payments**: Razorpay UPI payment links
- 📦 **Real-time Stock Management**: Atomic stock reservation prevents overselling
- 🚚 **Delivery Tracking**: Automated packing manifests and order lifecycle management
- 🌍 **Localized**: Hindi-first design with IST timezone support

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Backend Framework | Spring Boot 3.2 |
| Language | Java 21 |
| Database | PostgreSQL 16 |
| AI/LLM | Claude API (Anthropic) |
| Payments | Razorpay |
| Messaging | WhatsApp Business Cloud API |
| Migration | Flyway |
| Build Tool | Gradle |

---

## Quick Start

### Prerequisites

- Java 21
- Docker Desktop
- ngrok (for webhook testing)

### Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd order
```

2. **Configure environment**
```bash
cp .env.example .env
# Edit .env and add your API keys (see SETUP_GUIDE.md)
```

3. **Start PostgreSQL**
```bash
docker-compose up -d
```

4. **Run the application**
```bash
./gradlew bootRun
```

5. **Setup webhooks with ngrok**
```bash
ngrok http 8080
# Copy the HTTPS URL and configure in WhatsApp/Razorpay consoles
```

📖 **Detailed setup instructions**: See [SETUP_GUIDE.md](./SETUP_GUIDE.md)

---

## Architecture

```
┌─────────────┐
│  Customer   │
│  (WhatsApp) │
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────────────┐
│     WhatsApp Business Cloud API          │
└──────┬───────────────────────────────────┘
       │ Webhook
       ▼
┌──────────────────────────────────────────┐
│    Spring Boot Order Service             │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │  Order Processing Agent            │ │
│  │  - State Machine                   │ │
│  │  - Intent Parsing                  │ │
│  │  - Address Validation              │ │
│  └────────┬───────────────────┬────────┘ │
│           │                   │          │
│           ▼                   ▼          │
│  ┌─────────────────┐  ┌──────────────┐  │
│  │  Claude API     │  │  Razorpay    │  │
│  │  Service        │  │  Service     │  │
│  └─────────────────┘  └──────────────┘  │
│           │                   │          │
│           ▼                   ▼          │
│  ┌────────────────────────────────────┐ │
│  │       PostgreSQL Database          │ │
│  │  - Customers, Orders, Stock        │ │
│  │  - Conversation State              │ │
│  └────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

---

## Project Structure

```
src/main/java/in/agri/order/
├── config/          # Configuration classes
├── controller/      # REST controllers & webhooks
├── dto/             # Data transfer objects
├── model/           # JPA entities
├── repository/      # Spring Data repositories
└── service/         # Business logic services

src/main/resources/
├── db/migration/    # Flyway SQL migrations
└── application.yml  # Application configuration
```

---

## API Endpoints

### Webhooks

- `GET /webhook` - WhatsApp verification handshake
- `POST /webhook` - Inbound WhatsApp messages
- `POST /webhook/razorpay` - Razorpay payment confirmation

### Future Endpoints (Phase 2+)

- Order status queries
- Admin dashboard
- Analytics APIs

---

## Database Schema

### Core Tables

- **customers**: Customer records with phone as primary identifier
- **customer_addresses**: Delivery addresses (supports multiple per customer)
- **products**: Product catalog (Wheat, Rice, Atta)
- **price_records**: Historical pricing with effective dates
- **stock**: Real-time inventory with atomic reservation
- **orders**: Complete order lifecycle tracking
- **order_conversation_state**: Temporary state for in-progress conversations
- **payment_webhooks**: Audit log for payment events

See [database schema](./src/main/resources/db/migration/V1__initial_schema.sql) for details.

---

## Environment Variables

All configuration is done via environment variables loaded from `.env` file:

### Database
- `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`

### Claude AI
- `CLAUDE_API_KEY`, `CLAUDE_MODEL`

### Razorpay
- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`

### WhatsApp
- `WA_PHONE_NUMBER_ID`, `WA_ACCESS_TOKEN`, `WA_APP_SECRET`, `WA_VERIFY_TOKEN`

See [.env.example](./.env.example) for complete list.

---

## Development

### Build

```bash
./gradlew clean build
```

### Test

```bash
./gradlew test
```

### Run

```bash
./gradlew bootRun
```

### Database Management

```bash
# Connect to database
docker exec -it khet-freshness-db psql -U khetuser -d khet_freshness

# View tables
\dt

# Query customers
SELECT * FROM customers;

# Exit
\q
```

---

## Testing Order Flow

### 1. Send WhatsApp Message

Send to your WhatsApp test number:
```
5kg gehun chahiye
```

### 2. Provide Address

When prompted:
```
B-14, Tulsi Nagar, Lanka ke paas, Varanasi
```

### 3. Complete Payment

Click the Razorpay payment link received via WhatsApp.

### 4. Receive Confirmation

You'll get delivery date and order confirmation.

---

## Deployment

**Coming Soon**: Production deployment guide for AWS/GCP/Azure

---

## Roadmap

### Phase 1 (Current) ✅
- WhatsApp order intake
- Claude AI integration
- Razorpay payments
- Order lifecycle management

### Phase 2 (Planned)
- Dynamic pricing agent
- Marketing agent
- Customer analytics

### Phase 3 (Future)
- Multi-product catalog expansion
- B2B bulk orders
- Delivery tracking integration

---

## Contributing

This is an internal project. For questions or suggestions, contact the engineering team.

---

## License

Proprietary - Internal Use Only

---

## Support

📖 **Documentation**: [SETUP_GUIDE.md](./SETUP_GUIDE.md)
📋 **Technical Spec**: [order_service_spec.docx](./order_service_spec.docx)
🐛 **Issues**: Contact engineering team

---

**Built with ❤️ for Indian farmers**
