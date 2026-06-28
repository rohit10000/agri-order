package in.agri.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayPaymentLinkRequest {

    private Long amount; // in paise

    @Builder.Default
    private String currency = "INR";

    private String description;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("expire_by")
    private Long expireBy; // Unix timestamp

    private Customer customer;

    private Notify notify;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Customer {
        private String contact;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Notify {
        @Builder.Default
        private Boolean sms = false;

        @Builder.Default
        private Boolean email = false;
    }
}
