package in.agri.order.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "payment_webhooks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "JSONB")
    private String payload;

    @Column(name = "razorpay_signature", length = 500)
    private String razorpaySignature;

    @Column(name = "processed_at", nullable = false)
    @Builder.Default
    private ZonedDateTime processedAt = ZonedDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = ZonedDateTime.now();
        }
    }
}
