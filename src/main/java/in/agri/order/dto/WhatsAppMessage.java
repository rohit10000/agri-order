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
public class WhatsAppMessage {

    @JsonProperty("messaging_product")
    @Builder.Default
    private String messagingProduct = "whatsapp";

    @JsonProperty("recipient_type")
    @Builder.Default
    private String recipientType = "individual";

    private String to;

    private String type;

    private TextContent text;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TextContent {
        @Builder.Default
        @JsonProperty("preview_url")
        private Boolean previewUrl = false;

        private String body;
    }
}
