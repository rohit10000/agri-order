package in.agri.order.repository;

import in.agri.order.model.PriceRecord;
import in.agri.order.model.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    @Query("SELECT pr FROM PriceRecord pr WHERE pr.productItem = :productItem " +
           "AND pr.effectiveDate <= :date ORDER BY pr.effectiveDate DESC LIMIT 1")
    Optional<PriceRecord> findCurrentPrice(
        @Param("productItem") ProductItem productItem,
        @Param("date") LocalDate date
    );

    default Optional<PriceRecord> findTodaysPrice(ProductItem productItem) {
        return findCurrentPrice(productItem, LocalDate.now());
    }
}
