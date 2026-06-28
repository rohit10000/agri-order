package in.agri.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.agri.order.config.ClaudeConfig;
import in.agri.order.dto.AddressValidation;
import in.agri.order.dto.ClaudeRequest;
import in.agri.order.dto.ClaudeResponse;
import in.agri.order.dto.OrderIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Service for interacting with Claude API (Anthropic).
 * Handles order intent parsing and address validation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClaudeService {

    private final ClaudeConfig config;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse natural language message into structured order intent.
     * Supports Hindi, English, and Hinglish.
     */
    public OrderIntent parseOrderIntent(String message, String currentLanguage) {
        log.debug("Parsing order intent from message: {}", message);

        String systemPrompt = buildOrderParsingSystemPrompt();
        String userMessage = String.format("Current user language preference: %s\nMessage: %s",
                currentLanguage != null ? currentLanguage : "hindi", message);

        try {
            String response = callClaudeAPI(systemPrompt, userMessage);
            OrderIntent intent = parseJsonResponse(response, OrderIntent.class);
            log.info("Parsed intent: item={}, quantity={}, needs_clarification={}",
                    intent.getItem(), intent.getQuantityKg(), intent.getNeedsClarification());
            return intent;
        } catch (Exception e) {
            log.error("Error parsing order intent: {}", e.getMessage(), e);
            // Return a clarification request on error
            return OrderIntent.builder()
                    .needsClarification(true)
                    .clarificationReason("parse_error")
                    .language(currentLanguage != null ? currentLanguage : "hindi")
                    .build();
        }
    }

    /**
     * Validate and parse address text.
     */
    public AddressValidation validateAddress(String addressText) {
        log.debug("Validating address: {}", addressText);

        String systemPrompt = buildAddressValidationSystemPrompt();
        String userMessage = String.format("Validate this address: %s", addressText);

        try {
            String response = callClaudeAPI(systemPrompt, userMessage);
            AddressValidation validation = parseJsonResponse(response, AddressValidation.class);
            log.info("Address validation: valid={}, missing={}",
                    validation.getValid(), validation.getMissing());
            return validation;
        } catch (Exception e) {
            log.error("Error validating address: {}", e.getMessage(), e);
            // Return invalid address on error
            return AddressValidation.builder()
                    .valid(false)
                    .missing("Unable to parse address. Please provide: house number, area, landmark")
                    .build();
        }
    }

    /**
     * Call Claude API with system prompt and user message.
     */
    private String callClaudeAPI(String systemPrompt, String userMessage) {
        WebClient webClient = webClientBuilder
                .baseUrl(config.getApiUrl())
                .defaultHeader("x-api-key", config.getApiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", "application/json")
                .build();

        ClaudeRequest request = ClaudeRequest.builder()
                .model(config.getModel())
                .maxTokens(config.getMaxTokens())
                .system(systemPrompt)
                .messages(List.of(
                        ClaudeRequest.ClaudeMessage.builder()
                                .role("user")
                                .content(userMessage)
                                .build()
                ))
                .build();

        log.debug("Calling Claude API with model: {}", config.getModel());

        ClaudeResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .onErrorResume(e -> {
                    log.error("Claude API call failed: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Claude API call failed", e));
                })
                .block();

        if (response == null || response.getTextContent() == null) {
            throw new RuntimeException("Empty response from Claude API");
        }

        return response.getTextContent();
    }

    /**
     * Parse JSON response from Claude into object.
     */
    private <T> T parseJsonResponse(String jsonText, Class<T> clazz) throws JsonProcessingException {
        // Extract JSON from markdown code blocks if present
        String cleaned = jsonText.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        return objectMapper.readValue(cleaned, clazz);
    }

    /**
     * System prompt for order intent parsing.
     */
    private String buildOrderParsingSystemPrompt() {
        return """
                You are an order parser for a farm near Varanasi, India selling wheat (gehun), rice (chawal), and wheat flour (atta).

                Parse customer messages in Hindi, English, or Hinglish into structured JSON.

                Products:
                - WHEAT (gehun/gehoon/gehu)
                - RICE (chawal/rice)
                - ATTA (atta/flour/aata)

                Quantity conversions:
                - bori/bag = 50 kg
                - quintal = 100 kg
                - theli/packet = 5 kg
                - kg/kilo/kilogram = as specified

                Language detection:
                - If message contains Hindi words (gehun, chahiye, kg, etc.) → "hindi"
                - If message is primarily English → "english"
                - Default → "hindi"

                Output JSON format:
                {
                  "item": "WHEAT|RICE|ATTA|null",
                  "quantity_kg": <number or null>,
                  "delivery_area": "<area mentioned or null>",
                  "language": "hindi|english",
                  "needs_clarification": true|false,
                  "clarification_reason": "item_unclear|quantity_unclear|parse_error|null"
                }

                Rules:
                - If item is unclear or not mentioned → needs_clarification: true, reason: "item_unclear"
                - If quantity is unclear or not mentioned → needs_clarification: true, reason: "quantity_unclear"
                - If both are clear → needs_clarification: false
                - Extract delivery area if mentioned (Sigra, Lanka, BHU, etc.)

                Examples:
                - "5kg gehun chahiye" → {"item":"WHEAT","quantity_kg":5,"language":"hindi","needs_clarification":false}
                - "kuch chahiye" → {"item":null,"quantity_kg":null,"language":"hindi","needs_clarification":true,"clarification_reason":"item_unclear"}
                - "gehun chahiye" → {"item":"WHEAT","quantity_kg":null,"language":"hindi","needs_clarification":true,"clarification_reason":"quantity_unclear"}
                - "2 bori rice" → {"item":"RICE","quantity_kg":100,"language":"hindi","needs_clarification":false}

                Return ONLY the JSON object, no other text.
                """;
    }

    /**
     * System prompt for address validation.
     */
    private String buildAddressValidationSystemPrompt() {
        return """
                You are an address validator for deliveries in Varanasi, India.

                Validate the address and extract components.

                Required components:
                - line1: House/flat number + street/colony name (required)
                - landmark: Nearby landmark or area (required)
                - city: City name (default: Varanasi)
                - pincode: PIN code (optional)

                Common Varanasi areas: Lanka, BHU, Sigra, Assi, Orderly Bazar, Maldahiya, Bhelupur

                Output JSON format:
                {
                  "valid": true|false,
                  "line1": "<house number, street/colony>",
                  "line2": "<additional details>",
                  "landmark": "<landmark or area>",
                  "city": "Varanasi",
                  "pincode": "<pincode or null>",
                  "missing": "house/flat number|landmark|null"
                }

                Rules:
                - If house number is missing → valid: false, missing: "house/flat number"
                - If landmark/area is missing → valid: false, missing: "landmark"
                - If both present → valid: true
                - Always extract city as "Varanasi"
                - Extract pincode if present

                Examples:
                - "B-14, Tulsi Nagar, Lanka ke paas" → valid: true, line1: "B-14, Tulsi Nagar", landmark: "Lanka ke paas"
                - "Lanka ke paas" → valid: false, missing: "house/flat number"
                - "123, Sigra, 221005" → valid: true, line1: "123, Sigra", city: "Varanasi", pincode: "221005"

                Return ONLY the JSON object, no other text.
                """;
    }
}
