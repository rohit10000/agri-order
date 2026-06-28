package in.agri.order.service;

import in.agri.order.dto.AddressValidation;
import in.agri.order.dto.OrderIntent;
import in.agri.order.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Core order processing service with state machine.
 * Handles the complete order flow from message to payment.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessingService {

    private final ClaudeService claudeService;
    private final WhatsAppService whatsAppService;
    private final RazorpayService razorpayService;
    private final InMemoryDataStore dataStore;

    /**
     * Main entry point: Process incoming message from customer.
     */
    public void processIncomingMessage(String phone, String messageText) {
        log.info("📨 Processing message from {}: {}", phone, messageText);

        // Get or create customer
        Customer customer = dataStore.findCustomerByPhone(phone)
                .orElseGet(() -> createNewCustomer(phone));

        // Get conversation state
        Optional<OrderConversationState> stateOpt = dataStore.findConversationState(phone);

        if (stateOpt.isEmpty()) {
            // New conversation - parse order intent
            handleNewOrder(customer, messageText);
        } else {
            // Continue existing conversation
            handleExistingConversation(customer, stateOpt.get(), messageText);
        }
    }

    /**
     * Handle new order - parse intent from message.
     */
    private void handleNewOrder(Customer customer, String messageText) {
        log.debug("New order conversation for customer {}", customer.getPhone());

        // Parse message with Claude
        OrderIntent intent = claudeService.parseOrderIntent(
                messageText,
                customer.getLanguage().getCode()
        );

        // Update customer language if detected
        if (intent.getLanguage() != null) {
            CustomerLanguage detectedLang = CustomerLanguage.fromCode(intent.getLanguage());
            if (detectedLang != customer.getLanguage()) {
                customer.setLanguage(detectedLang);
                log.info("Updated customer language to: {}", detectedLang);
            }
        }

        // Handle based on intent
        if (Boolean.TRUE.equals(intent.getNeedsClarification())) {
            handleClarificationNeeded(customer, intent);
        } else {
            // Intent is clear - check stock and proceed
            handleClearIntent(customer, intent);
        }
    }

    /**
     * Handle clarification needed.
     */
    private void handleClarificationNeeded(Customer customer, OrderIntent intent) {
        log.debug("Clarification needed: {}", intent.getClarificationReason());

        String response = switch (intent.getClarificationReason()) {
            case "item_unclear" -> buildItemClarificationMessage(customer.getLanguage());
            case "quantity_unclear" -> buildQuantityClarificationMessage(customer.getLanguage());
            default -> buildGenericClarificationMessage(customer.getLanguage());
        };

        whatsAppService.sendText(customer.getPhone(), response);

        // Save conversation state
        OrderConversationState state = OrderConversationState.builder()
                .phone(customer.getPhone())
                .state(ConversationState.COLLECTING_ITEM)
                .item(intent.getItem())
                .quantityKg(intent.getQuantityKg())
                .build();
        dataStore.saveConversationState(state);
    }

    /**
     * Handle clear intent - proceed with order.
     */
    private void handleClearIntent(Customer customer, OrderIntent intent) {
        log.info("Clear intent: {} × {} kg", intent.getItem(), intent.getQuantityKg());

        // Check stock availability
        BigDecimal freeStock = dataStore.getFreeStock(intent.getItem());
        if (freeStock.compareTo(intent.getQuantityKg()) < 0) {
            handleInsufficientStock(customer, intent, freeStock);
            return;
        }

        // Stock available - proceed to address collection
        proceedToAddressCollection(customer, intent);
    }

    /**
     * Handle insufficient stock.
     */
    private void handleInsufficientStock(Customer customer, OrderIntent intent, BigDecimal available) {
        log.warn("Insufficient stock for {}: requested={}, available={}",
                intent.getItem(), intent.getQuantityKg(), available);

        String message = buildInsufficientStockMessage(
                customer.getLanguage(),
                intent.getItem(),
                intent.getQuantityKg(),
                available
        );
        whatsAppService.sendText(customer.getPhone(), message);
    }

    /**
     * Proceed to address collection.
     */
    private void proceedToAddressCollection(Customer customer, OrderIntent intent) {
        // Check if customer has default address
        Optional<CustomerAddress> defaultAddress = dataStore.findDefaultAddress(customer);

        if (defaultAddress.isPresent()) {
            // Show saved address for confirmation
            showSavedAddressConfirmation(customer, intent, defaultAddress.get());
        } else {
            // Ask for new address
            requestNewAddress(customer, intent);
        }
    }

    /**
     * Show saved address for confirmation.
     */
    private void showSavedAddressConfirmation(Customer customer, OrderIntent intent, CustomerAddress address) {
        String message = buildAddressConfirmationMessage(customer.getLanguage(), address);
        whatsAppService.sendText(customer.getPhone(), message);

        // Save conversation state
        OrderConversationState state = OrderConversationState.builder()
                .phone(customer.getPhone())
                .state(ConversationState.CONFIRMING_ADDRESS)
                .item(intent.getItem())
                .quantityKg(intent.getQuantityKg())
                .addressMode(AddressMode.SAVED)
                .build();
        dataStore.saveConversationState(state);
    }

    /**
     * Request new address from customer.
     */
    private void requestNewAddress(Customer customer, OrderIntent intent) {
        String message = buildAddressRequestMessage(customer.getLanguage());
        whatsAppService.sendText(customer.getPhone(), message);

        // Save conversation state
        OrderConversationState state = OrderConversationState.builder()
                .phone(customer.getPhone())
                .state(ConversationState.COLLECTING_ADDRESS)
                .item(intent.getItem())
                .quantityKg(intent.getQuantityKg())
                .addressMode(AddressMode.NEW)
                .build();
        dataStore.saveConversationState(state);
    }

    /**
     * Handle existing conversation based on state.
     */
    private void handleExistingConversation(Customer customer, OrderConversationState state, String messageText) {
        log.debug("Continuing conversation in state: {}", state.getState());

        switch (state.getState()) {
            case COLLECTING_ITEM -> handleCollectingItem(customer, state, messageText);
            case COLLECTING_ADDRESS -> handleCollectingAddress(customer, state, messageText);
            case CONFIRMING_ADDRESS -> handleConfirmingAddress(customer, state, messageText);
            default -> log.warn("Unexpected state: {}", state.getState());
        }
    }

    /**
     * Handle COLLECTING_ITEM state.
     */
    private void handleCollectingItem(Customer customer, OrderConversationState state, String messageText) {
        // Re-parse the message
        OrderIntent intent = claudeService.parseOrderIntent(messageText, customer.getLanguage().getCode());

        if (Boolean.TRUE.equals(intent.getNeedsClarification())) {
            handleClarificationNeeded(customer, intent);
        } else {
            handleClearIntent(customer, intent);
        }
    }

    /**
     * Handle COLLECTING_ADDRESS state.
     */
    private void handleCollectingAddress(Customer customer, OrderConversationState state, String messageText) {
        // Validate address with Claude
        AddressValidation validation = claudeService.validateAddress(messageText);

        if (Boolean.TRUE.equals(validation.getValid())) {
            // Address is valid - create order
            createOrderAndGeneratePaymentLink(customer, state, validation);
        } else {
            // Address invalid - ask for missing info
            String message = buildAddressValidationErrorMessage(customer.getLanguage(), validation.getMissing());
            whatsAppService.sendText(customer.getPhone(), message);
        }
    }

    /**
     * Handle CONFIRMING_ADDRESS state.
     */
    private void handleConfirmingAddress(Customer customer, OrderConversationState state, String messageText) {
        String cleaned = messageText.trim();

        if ("1".equals(cleaned)) {
            // Customer confirmed saved address
            Optional<CustomerAddress> defaultAddress = dataStore.findDefaultAddress(customer);
            if (defaultAddress.isPresent()) {
                createOrderWithSavedAddress(customer, state, defaultAddress.get());
            } else {
                whatsAppService.sendText(customer.getPhone(), "Error: No saved address found. Please enter your address.");
                requestNewAddress(customer, OrderIntent.builder()
                        .item(state.getItem())
                        .quantityKg(state.getQuantityKg())
                        .build());
            }
        } else if ("2".equals(cleaned)) {
            // Customer wants to enter new address
            state.setAddressMode(AddressMode.NEW);
            state.setState(ConversationState.COLLECTING_ADDRESS);
            dataStore.saveConversationState(state);
            requestNewAddress(customer, OrderIntent.builder()
                    .item(state.getItem())
                    .quantityKg(state.getQuantityKg())
                    .build());
        } else {
            // Invalid response
            whatsAppService.sendText(customer.getPhone(),
                    "कृपया '1' या '2' भेजें / Please send '1' or '2'");
        }
    }

    /**
     * Create order with saved address.
     */
    private void createOrderWithSavedAddress(Customer customer, OrderConversationState state, CustomerAddress address) {
        AddressValidation validation = AddressValidation.builder()
                .valid(true)
                .line1(address.getLine1())
                .line2(address.getLine2())
                .landmark(address.getLandmark())
                .city(address.getCity())
                .pincode(address.getPincode())
                .build();

        createOrderAndGeneratePaymentLink(customer, state, validation);
    }

    /**
     * Create order and generate payment link.
     */
    private void createOrderAndGeneratePaymentLink(Customer customer, OrderConversationState state,
                                                    AddressValidation addressValidation) {
        try {
            // Get current price
            PriceRecord priceRecord = dataStore.findCurrentPrice(state.getItem())
                    .orElseThrow(() -> new RuntimeException("Price not found for " + state.getItem()));

            BigDecimal totalAmount = state.getQuantityKg().multiply(priceRecord.getSellPrice());

            // Create and save address if new
            CustomerAddress address = null;
            if (state.getAddressMode() == AddressMode.NEW) {
                address = CustomerAddress.builder()
                        .customer(customer)
                        .line1(addressValidation.getLine1())
                        .line2(addressValidation.getLine2())
                        .landmark(addressValidation.getLandmark())
                        .city(addressValidation.getCity() != null ? addressValidation.getCity() : "Varanasi")
                        .pincode(addressValidation.getPincode())
                        .fullText(buildFullAddressText(addressValidation))
                        .isDefault(true)
                        .build();
                address = dataStore.saveAddress(address);
            } else {
                address = dataStore.findDefaultAddress(customer).orElse(null);
            }

            // Reserve stock (atomic operation in real DB)
            dataStore.reserveStock(state.getItem(), state.getQuantityKg());

            // Create order
            Order order = Order.builder()
                    .customer(customer)
                    .address(address)
                    .deliveryAddressText(address != null ? address.getFullText() : buildFullAddressText(addressValidation))
                    .item(state.getItem())
                    .quantityKg(state.getQuantityKg())
                    .pricePerKg(priceRecord.getSellPrice())
                    .totalAmount(totalAmount)
                    .status(OrderStatus.PENDING_PAYMENT)
                    .deliveryDate(calculateNextDeliveryDate())
                    .build();
            order = dataStore.createOrder(order);

            log.info("✅ Order created: #{} for {} × {} kg", order.getId(), order.getItem(), order.getQuantityKg());

            // Generate payment link
            String paymentUrl = razorpayService.createPaymentLink(order);

            // Send payment link message
            String message = buildPaymentLinkMessage(customer.getLanguage(), order, paymentUrl);
            whatsAppService.sendText(customer.getPhone(), message);

            // Clear conversation state
            dataStore.deleteConversationState(customer.getPhone());

        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            whatsAppService.sendText(customer.getPhone(),
                    "क्षमा करें, ऑर्डर बनाने में समस्या आई। कृपया दोबारा प्रयास करें।\n" +
                            "Sorry, there was an error creating your order. Please try again.");
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    private Customer createNewCustomer(String phone) {
        log.info("Creating new customer: {}", phone);
        return dataStore.createCustomer(phone, CustomerLanguage.HINDI);
    }

    private LocalDate calculateNextDeliveryDate() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        // Delivery on Tuesday (2) or Friday (5)
        if (dayOfWeek.getValue() < 2) {
            return today.plusDays(2 - dayOfWeek.getValue());
        } else if (dayOfWeek.getValue() < 5) {
            return today.plusDays(5 - dayOfWeek.getValue());
        } else {
            // Weekend - next Tuesday
            return today.plusDays(9 - dayOfWeek.getValue());
        }
    }

    private String buildFullAddressText(AddressValidation validation) {
        StringBuilder sb = new StringBuilder();
        if (validation.getLine1() != null) sb.append(validation.getLine1());
        if (validation.getLine2() != null) sb.append(", ").append(validation.getLine2());
        if (validation.getLandmark() != null) sb.append(", ").append(validation.getLandmark());
        if (validation.getCity() != null) sb.append(", ").append(validation.getCity());
        if (validation.getPincode() != null) sb.append(" - ").append(validation.getPincode());
        return sb.toString();
    }

    // ============================================
    // Message Templates
    // ============================================

    private String buildItemClarificationMessage(CustomerLanguage language) {
        return language == CustomerLanguage.ENGLISH
                ? "What would you like to order?\n1️⃣ Wheat (₹28/kg)\n2️⃣ Rice (₹25/kg)\n3️⃣ Wheat Flour (₹32/kg)"
                : "आप क्या ऑर्डर करना चाहेंगे?\n1️⃣ गेहूं (₹28/kg)\n2️⃣ चावल (₹25/kg)\n3️⃣ आटा (₹32/kg)";
    }

    private String buildQuantityClarificationMessage(CustomerLanguage language) {
        return language == CustomerLanguage.ENGLISH
                ? "How many kilograms would you like to order?"
                : "आप कितने किलो ऑर्डर करना चाहेंगे?";
    }

    private String buildGenericClarificationMessage(CustomerLanguage language) {
        return language == CustomerLanguage.ENGLISH
                ? "Please provide more details about your order (item and quantity).\nExample: 5kg wheat"
                : "कृपया अपने ऑर्डर के बारे में अधिक जानकारी दें (वस्तु और मात्रा)।\nउदाहरण: 5kg गेहूं";
    }

    private String buildInsufficientStockMessage(CustomerLanguage language, ProductItem item,
                                                  BigDecimal requested, BigDecimal available) {
        String itemName = item.getLocalizedName(language);
        return language == CustomerLanguage.ENGLISH
                ? String.format("Sorry, we only have %.0f kg of %s available. You requested %.0f kg.\n" +
                "Would you like to order %.0f kg instead?", available, itemName, requested, available)
                : String.format("क्षमा करें, हमारे पास केवल %.0f किलो %s उपलब्ध है। आपने %.0f किलो मांगा था।\n" +
                "क्या आप %.0f किलो ऑर्डर करना चाहेंगे?", available, itemName, requested, available);
    }

    private String buildAddressRequestMessage(CustomerLanguage language) {
        return language == CustomerLanguage.ENGLISH
                ? "Please provide your complete delivery address.\n" +
                "Example: B-14, Tulsi Nagar, near Lanka, Varanasi - 221005"
                : "कृपया अपना पूरा डिलीवरी पता दें।\n" +
                "उदाहरण: B-14, तुलसी नगर, लंका के पास, वाराणसी - 221005";
    }

    private String buildAddressConfirmationMessage(CustomerLanguage language, CustomerAddress address) {
        String addressText = address.getFullText();
        return language == CustomerLanguage.ENGLISH
                ? String.format("Deliver to this address?\n%s\n\n1️⃣ Yes, this address\n2️⃣ No, enter new address", addressText)
                : String.format("इस पते पर डिलीवर करें?\n%s\n\n1️⃣ हाँ, यह पता\n2️⃣ नहीं, नया पता दर्ज करें", addressText);
    }

    private String buildAddressValidationErrorMessage(CustomerLanguage language, String missing) {
        return language == CustomerLanguage.ENGLISH
                ? String.format("Address is incomplete. Missing: %s\nPlease provide complete address.", missing)
                : String.format("पता अधूरा है। गायब: %s\nकृपया पूरा पता दें।", missing);
    }

    private String buildPaymentLinkMessage(CustomerLanguage language, Order order, String paymentUrl) {
        String itemName = order.getItem().getLocalizedName(language);
        return language == CustomerLanguage.ENGLISH
                ? String.format("✅ Order Confirmed!\n\n" +
                "Item: %s\n" +
                "Quantity: %.0f kg\n" +
                "Rate: ₹%.2f/kg\n" +
                "Total: ₹%.2f\n\n" +
                "💳 Payment Link: %s\n\n" +
                "Delivery Date: %s\n" +
                "Address: %s",
                itemName, order.getQuantityKg(), order.getPricePerKg(), order.getTotalAmount(),
                paymentUrl, order.getDeliveryDate(), order.getDeliveryAddressText())
                : String.format("✅ ऑर्डर कन्फर्म हो गया!\n\n" +
                "वस्तु: %s\n" +
                "मात्रा: %.0f किलो\n" +
                "दर: ₹%.2f/kg\n" +
                "कुल: ₹%.2f\n\n" +
                "💳 पेमेंट लिंक: %s\n\n" +
                "डिलीवरी तिथि: %s\n" +
                "पता: %s",
                itemName, order.getQuantityKg(), order.getPricePerKg(), order.getTotalAmount(),
                paymentUrl, order.getDeliveryDate(), order.getDeliveryAddressText());
    }
}
