package in.agri.order.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.whatsapp")
@Getter
@Setter
public class WhatsAppConfig {
    private String phoneNumberId;
    private String accessToken;
    private String appSecret;
    private String verifyToken;
    private String apiUrl;
    private String workerPhone;
}
