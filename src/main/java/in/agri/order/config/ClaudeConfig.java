package in.agri.order.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.claude")
@Getter
@Setter
public class ClaudeConfig {
    private String apiKey;
    private String model;
    private String apiUrl;
    private int timeoutSeconds;
    private int maxTokens;
}
