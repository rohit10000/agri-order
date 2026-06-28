package in.agri.order.service;

import in.agri.order.model.CustomerLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WhatsApp service for sending messages.
 * Currently simulated - just logs messages instead of actually sending.
 * Will be integrated with WhatsApp Business API later.
 */
@Service
@Slf4j
public class WhatsAppService {

    /**
     * Send text message to phone number.
     * Currently simulated - just logs the message.
     */
    public void sendText(String phone, String message) {
        log.info("📱 [WhatsApp Simulated] To: {} | Message: {}", phone, message);
        // TODO: Real WhatsApp API integration
        // WebClient call to WhatsApp Graph API
    }

    /**
     * Send message in appropriate language.
     */
    public void sendLocalizedText(String phone, CustomerLanguage language, String hindiMessage, String englishMessage) {
        String message = language == CustomerLanguage.ENGLISH ? englishMessage : hindiMessage;
        sendText(phone, message);
    }
}
