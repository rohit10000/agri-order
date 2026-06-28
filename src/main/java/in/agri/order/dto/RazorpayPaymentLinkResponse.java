package in.agri.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayPaymentLinkResponse {

    private String id;

    @JsonProperty("short_url")
    private String shortUrl;

    private String status;

    @JsonProperty("reference_id")
    private String referenceId;
}
