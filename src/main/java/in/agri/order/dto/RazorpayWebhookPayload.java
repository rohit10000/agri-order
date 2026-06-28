package in.agri.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayWebhookPayload {

    private String event;

    private JsonNode payload;

    @JsonProperty("contains")
    private String[] containsArray;

    public String getPaymentId() {
        if (payload != null) {
            JsonNode payment = payload.get("payment");
            if (payment != null) {
                JsonNode entity = payment.get("entity");
                if (entity != null && entity.has("id")) {
                    return entity.get("id").asText();
                }
            }

            // Fallback for payment.captured event
            if (payload.has("payment")) {
                JsonNode paymentNode = payload.get("payment");
                if (paymentNode.has("entity")) {
                    JsonNode entityNode = paymentNode.get("entity");
                    if (entityNode.has("id")) {
                        return entityNode.get("id").asText();
                    }
                }
            }
        }
        return null;
    }

    public String getReferenceId() {
        if (payload != null) {
            JsonNode paymentLink = payload.get("payment_link");
            if (paymentLink != null) {
                JsonNode entity = paymentLink.get("entity");
                if (entity != null && entity.has("reference_id")) {
                    return entity.get("reference_id").asText();
                }
            }
        }
        return null;
    }
}
