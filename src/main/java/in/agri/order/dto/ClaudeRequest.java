package in.agri.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaudeRequest {

    private String model;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private String system;

    private List<ClaudeMessage> messages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaudeMessage {
        private String role;
        private String content;
    }
}
