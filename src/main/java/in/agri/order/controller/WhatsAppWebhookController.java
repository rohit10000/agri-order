package in.agri.order.controller;

import in.agri.order.dto.WhatsAppWebhookPayload;
import in.agri.order.service.OrderProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

/**
 * WhatsApp webhook controller.
 * Receives inbound messages from WhatsApp Business API.
 */
@RestController
@RequestMapping("/webhook")
@Slf4j
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final OrderProcessingService orderProcessingService;

    /**
     * GET /webhook - Meta verification handshake
     * This is called once by Meta to verify the webhook URL.
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        log.info("📞 Webhook verification request: mode={}, token={}", mode, token);

        // TODO: Verify token matches WA_VERIFY_TOKEN from config
        // For now, accept all verification requests
        if ("subscribe".equals(mode)) {
            log.info("✅ Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        log.warn("❌ Webhook verification failed");
        return ResponseEntity.status(403).body("Forbidden");
    }

    /**
     * POST /webhook - Receive inbound WhatsApp messages
     * IMPORTANT: Must return HTTP 200 within 200ms or Meta will retry.
     */
    @PostMapping
    public ResponseEntity<Void> handleIncomingMessage(
            @RequestBody WhatsAppWebhookPayload payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        log.info("📨 Received WhatsApp webhook");

        // TODO: Verify HMAC signature in production
        // For now, skip signature verification

        try {
            // Extract phone and message
            String phone = payload.getFirstPhone();
            String messageText = payload.getFirstTextMessage();

            if (phone != null && messageText != null) {
                log.info("📱 Message from {}: {}", phone, messageText);

                // Process asynchronously to return 200 quickly
                processMessageAsync(phone, messageText);
            } else {
                log.warn("⚠️ Non-text message or invalid payload");
            }

        } catch (Exception e) {
            log.error("❌ Error processing webhook: {}", e.getMessage(), e);
            // Still return 200 to prevent Meta retries
        }

        // ALWAYS return 200, even on errors
        return ResponseEntity.ok().build();
    }

    /**
     * Process message asynchronously.
     * This allows webhook to return HTTP 200 within 200ms.
     */
    @Async
    public void processMessageAsync(String phone, String messageText) {
        try {
            log.debug("🔄 Processing message async for {}", phone);
            orderProcessingService.processIncomingMessage(phone, messageText);
        } catch (Exception e) {
            log.error("❌ Error in async message processing: {}", e.getMessage(), e);
        }
    }
}
