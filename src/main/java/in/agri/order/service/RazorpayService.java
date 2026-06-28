package in.agri.order.service;

import in.agri.order.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Razorpay payment service.
 * Currently simulated - generates fake payment links.
 * Will be integrated with Razorpay API later.
 */
@Service
@Slf4j
public class RazorpayService {

    /**
     * Create payment link for order.
     * Currently simulated - returns a fake URL.
     */
    public String createPaymentLink(Order order) {
        String fakePaymentId = "pay_sim_" + UUID.randomUUID().toString().substring(0, 8);
        String fakeUrl = "https://razorpay-simulated.com/pay/" + fakePaymentId;

        log.info("💳 [Razorpay Simulated] Created payment link for order #{}: {}",
                order.getId(), fakeUrl);
        log.info("   Amount: ₹{} ({} kg × ₹{}/kg)",
                order.getTotalAmount(), order.getQuantityKg(), order.getPricePerKg());

        return fakeUrl;

        // TODO: Real Razorpay integration
        // 1. Create payment link via Razorpay API
        // 2. Set expire_by to 24 hours from now
        // 3. Include order ID as reference_id
        // 4. Return short_url from response
    }

    /**
     * Simulate payment confirmation.
     * In real scenario, this would be called by webhook.
     */
    public void simulatePaymentSuccess(Long orderId) {
        String fakePaymentId = "pay_" + UUID.randomUUID().toString().substring(0, 16);
        log.info("💳 [Razorpay Simulated] Payment successful for order #{}, payment_id: {}",
                orderId, fakePaymentId);
    }
}
