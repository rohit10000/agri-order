package in.agri.order.repository;

import in.agri.order.model.Product;
import in.agri.order.model.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductItem(ProductItem productItem);
}
