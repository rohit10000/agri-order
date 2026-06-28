package in.agri.order.service;

import in.agri.order.model.*;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory data store simulating database tables.
 * Used for testing before PostgreSQL integration.
 */
@Component
@Getter
public class InMemoryDataStore {

    private final AtomicLong customerIdGenerator = new AtomicLong(1);
    private final AtomicLong orderIdGenerator = new AtomicLong(1);
    private final AtomicLong addressIdGenerator = new AtomicLong(1);

    // Simulated tables
    private final Map<String, Customer> customersByPhone = new ConcurrentHashMap<>();
    private final Map<Long, Customer> customersById = new ConcurrentHashMap<>();
    private final Map<Long, Order> ordersById = new ConcurrentHashMap<>();
    private final Map<String, OrderConversationState> conversationStateByPhone = new ConcurrentHashMap<>();
    private final Map<Long, CustomerAddress> addressesById = new ConcurrentHashMap<>();
    private final Map<ProductItem, Stock> stockByProduct = new ConcurrentHashMap<>();
    private final Map<ProductItem, PriceRecord> pricesByProduct = new ConcurrentHashMap<>();

    public InMemoryDataStore() {
        initializeStock();
        initializePrices();
    }

    // Stock initialization
    private void initializeStock() {
        for (ProductItem item : ProductItem.values()) {
            Stock stock = Stock.builder()
                    .id((long) item.ordinal() + 1)
                    .productItem(item)
                    .availableKg(new BigDecimal("500")) // 500 kg available for testing
                    .reservedKg(BigDecimal.ZERO)
                    .updatedAt(ZonedDateTime.now())
                    .build();
            stockByProduct.put(item, stock);
        }
    }

    // Price initialization
    private void initializePrices() {
        pricesByProduct.put(ProductItem.WHEAT, PriceRecord.builder()
                .id(1L)
                .productItem(ProductItem.WHEAT)
                .sellPrice(new BigDecimal("28.00"))
                .effectiveDate(LocalDate.now())
                .createdAt(ZonedDateTime.now())
                .build());

        pricesByProduct.put(ProductItem.RICE, PriceRecord.builder()
                .id(2L)
                .productItem(ProductItem.RICE)
                .sellPrice(new BigDecimal("25.00"))
                .effectiveDate(LocalDate.now())
                .createdAt(ZonedDateTime.now())
                .build());

        pricesByProduct.put(ProductItem.ATTA, PriceRecord.builder()
                .id(3L)
                .productItem(ProductItem.ATTA)
                .sellPrice(new BigDecimal("32.00"))
                .effectiveDate(LocalDate.now())
                .createdAt(ZonedDateTime.now())
                .build());
    }

    // Customer operations
    public Customer createCustomer(String phone, CustomerLanguage language) {
        Customer customer = Customer.builder()
                .id(customerIdGenerator.getAndIncrement())
                .phone(phone)
                .language(language)
                .addresses(new ArrayList<>())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
        customersByPhone.put(phone, customer);
        customersById.put(customer.getId(), customer);
        return customer;
    }

    public Optional<Customer> findCustomerByPhone(String phone) {
        return Optional.ofNullable(customersByPhone.get(phone));
    }

    // Order operations
    public Order createOrder(Order order) {
        order.setId(orderIdGenerator.getAndIncrement());
        order.setCreatedAt(ZonedDateTime.now());
        order.setUpdatedAt(ZonedDateTime.now());
        ordersById.put(order.getId(), order);
        return order;
    }

    public Optional<Order> findOrderById(Long id) {
        return Optional.ofNullable(ordersById.get(id));
    }

    public List<Order> findOrdersByCustomer(Customer customer) {
        return ordersById.values().stream()
                .filter(o -> o.getCustomer().getId().equals(customer.getId()))
                .toList();
    }

    // Address operations
    public CustomerAddress saveAddress(CustomerAddress address) {
        if (address.getId() == null) {
            address.setId(addressIdGenerator.getAndIncrement());
            address.setCreatedAt(ZonedDateTime.now());
        }
        address.setUpdatedAt(ZonedDateTime.now());
        addressesById.put(address.getId(), address);
        return address;
    }

    public Optional<CustomerAddress> findDefaultAddress(Customer customer) {
        return addressesById.values().stream()
                .filter(a -> a.getCustomer().getId().equals(customer.getId()) && a.getIsDefault())
                .findFirst();
    }

    // Conversation state operations
    public void saveConversationState(OrderConversationState state) {
        if (state.getCreatedAt() == null) {
            state.setCreatedAt(ZonedDateTime.now());
        }
        state.setUpdatedAt(ZonedDateTime.now());
        conversationStateByPhone.put(state.getPhone(), state);
    }

    public Optional<OrderConversationState> findConversationState(String phone) {
        return Optional.ofNullable(conversationStateByPhone.get(phone));
    }

    public void deleteConversationState(String phone) {
        conversationStateByPhone.remove(phone);
    }

    // Stock operations
    public Optional<Stock> findStockByProduct(ProductItem item) {
        return Optional.ofNullable(stockByProduct.get(item));
    }

    public synchronized void reserveStock(ProductItem item, BigDecimal quantity) {
        Stock stock = stockByProduct.get(item);
        if (stock != null) {
            stock.reserveStock(quantity);
            stock.setUpdatedAt(ZonedDateTime.now());
        }
    }

    public BigDecimal getFreeStock(ProductItem item) {
        return stockByProduct.get(item).getFreeStock();
    }

    // Price operations
    public Optional<PriceRecord> findCurrentPrice(ProductItem item) {
        return Optional.ofNullable(pricesByProduct.get(item));
    }

    // Debug/utility methods
    public void reset() {
        customersByPhone.clear();
        customersById.clear();
        ordersById.clear();
        conversationStateByPhone.clear();
        addressesById.clear();
        initializeStock();
        initializePrices();
    }

    public void printStatus() {
        System.out.println("=== In-Memory Data Store Status ===");
        System.out.println("Customers: " + customersByPhone.size());
        System.out.println("Orders: " + ordersById.size());
        System.out.println("Conversation States: " + conversationStateByPhone.size());
        System.out.println("Stock:");
        stockByProduct.forEach((item, stock) ->
                System.out.printf("  %s: %.2f kg available, %.2f kg reserved, %.2f kg free%n",
                        item, stock.getAvailableKg(), stock.getReservedKg(), stock.getFreeStock()));
    }
}
