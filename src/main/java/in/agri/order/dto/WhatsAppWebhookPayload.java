package in.agri.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppWebhookPayload {

    private String object;
    private List<Entry> entry;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        private String id;
        private List<Change> changes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        private Value value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        private String messaging_product;
        private List<Message> messages;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String id;
        private String from;
        private String type;
        private TextMessage text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextMessage {
        private String body;
    }

    public String getFirstPhone() {
        if (entry != null && !entry.isEmpty()) {
            Entry first = entry.get(0);
            if (first.changes != null && !first.changes.isEmpty()) {
                Change change = first.changes.get(0);
                if (change.value != null && change.value.messages != null && !change.value.messages.isEmpty()) {
                    return change.value.messages.get(0).from;
                }
            }
        }
        return null;
    }

    public String getFirstTextMessage() {
        if (entry != null && !entry.isEmpty()) {
            Entry first = entry.get(0);
            if (first.changes != null && !first.changes.isEmpty()) {
                Change change = first.changes.get(0);
                if (change.value != null && change.value.messages != null && !change.value.messages.isEmpty()) {
                    Message msg = change.value.messages.get(0);
                    if ("text".equals(msg.type) && msg.text != null) {
                        return msg.text.body;
                    }
                }
            }
        }
        return null;
    }
}
