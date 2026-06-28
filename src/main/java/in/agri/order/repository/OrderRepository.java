package in.agri.order.repository;

import in.agri.order.model.Customer;
import in.agri.order.model.Order;
import in.agri.order.model.OrderStatus;
import in.agri.order.model.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerOrderByCreatedAtDesc(Customer customer);

    Optional<Order> findFirstByCustomerOrderByCreatedAtDesc(Customer customer);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusAndDeliveryDate(OrderStatus status, LocalDate deliveryDate);

    @Query("SELECT o FROM Order o WHERE o.customer = :customer AND o.item = :item " +
           "AND o.status = :status AND o.createdAt >= :since")
    Optional<Order> findRecentOrder(
        @Param("customer") Customer customer,
        @Param("item") ProductItem item,
        @Param("status") OrderStatus status,
        @Param("since") ZonedDateTime since
    );

    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING_PAYMENT' AND o.createdAt < :cutoff")
    List<Order> findStuckOrders(@Param("cutoff") ZonedDateTime cutoff);

    Optional<Order> findByPaymentId(String paymentId);
}
