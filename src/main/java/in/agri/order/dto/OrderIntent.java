package in.agri.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import in.agri.order.model.ProductItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderIntent {

    private ProductItem item;

    @JsonProperty("quantity_kg")
    private BigDecimal quantityKg;

    @JsonProperty("delivery_area")
    private String deliveryArea;

    private String language;

    @JsonProperty("needs_clarification")
    private Boolean needsClarification;

    @JsonProperty("clarification_reason")
    private String clarificationReason;
}
