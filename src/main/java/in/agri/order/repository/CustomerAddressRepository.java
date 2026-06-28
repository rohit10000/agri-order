package in.agri.order.repository;

import in.agri.order.model.Customer;
import in.agri.order.model.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress> findByCustomer(Customer customer);

    Optional<CustomerAddress> findByCustomerAndIsDefaultTrue(Customer customer);

    @Modifying
    @Query("UPDATE CustomerAddress ca SET ca.isDefault = false WHERE ca.customer = :customer")
    void clearDefaultAddress(@Param("customer") Customer customer);
}
