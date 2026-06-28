package in.agri.order.controller;

import in.agri.order.service.InMemoryDataStore;
import in.agri.order.service.OrderProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test controller for manual testing without WhatsApp.
 * Use this to test the order flow directly.
 */
@RestController
@RequestMapping("/test")
@Slf4j
@RequiredArgsConstructor
public class TestController {

    private final OrderProcessingService orderProcessingService;
    private final InMemoryDataStore dataStore;

    /**
     * Send a simulated message as if from WhatsApp.
     *
     * Example:
     * POST /test/message
     * {
     *   "phone": "919876543210",
     *   "message": "5kg gehun chahiye"
     * }
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> sendMessage(
            @RequestBody Map<String, String> request) {

        String phone = request.get("phone");
        String message = request.get("message");

        if (phone == null || message == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Phone and message are required"));
        }

        log.info("🧪 Test message from {}: {}", phone, message);

        try {
            orderProcessingService.processIncomingMessage(phone, message);
            return ResponseEntity.ok(Map.of(
                    "status", "processed",
                    "phone", phone,
                    "message", "Message processed successfully. Check logs for WhatsApp responses."
            ));
        } catch (Exception e) {
            log.error("Error processing test message: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current data store status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        dataStore.printStatus();

        return ResponseEntity.ok(Map.of(
                "customers", dataStore.getCustomersByPhone().size(),
                "orders", dataStore.getOrdersById().size(),
                "conversations", dataStore.getConversationStateByPhone().size(),
                "stock", dataStore.getStockByProduct()
        ));
    }

    /**
     * Reset all data.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        dataStore.reset();
        log.info("🔄 Data store reset");
        return ResponseEntity.ok(Map.of("status", "reset", "message", "All data cleared"));
    }

    /**
     * Health check.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "Khet Freshness Order Service",
                "mode", "test"
        ));
    }
}
