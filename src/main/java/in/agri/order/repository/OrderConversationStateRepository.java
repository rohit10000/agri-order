package in.agri.order.repository;

import in.agri.order.model.OrderConversationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;

@Repository
public interface OrderConversationStateRepository extends JpaRepository<OrderConversationState, Long> {

    Optional<OrderConversationState> findByPhone(String phone);

    void deleteByPhone(String phone);

    @Modifying
    @Query("DELETE FROM OrderConversationState ocs WHERE ocs.createdAt < :cutoff")
    int deleteOldConversations(@Param("cutoff") ZonedDateTime cutoff);
}
