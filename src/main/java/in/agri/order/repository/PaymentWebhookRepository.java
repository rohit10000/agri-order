package in.agri.order.repository;

import in.agri.order.model.PaymentWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, Long> {

    List<PaymentWebhook> findByEventTypeOrderByProcessedAtDesc(String eventType);
}
