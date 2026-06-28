package in.agri.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaudeResponse {

    private String id;
    private String type;
    private String role;
    private List<Content> content;
    private String model;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String type;
        private String text;
    }

    public String getTextContent() {
        if (content != null && !content.isEmpty()) {
            return content.get(0).getText();
        }
        return null;
    }
}
