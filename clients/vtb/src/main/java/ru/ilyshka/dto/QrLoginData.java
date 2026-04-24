package ru.ilyshka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class holding QR login session information.
 * Contains session identifier, challenge token, and QR code data for display.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrLoginData {
    /**
     * Unique session identifier for the QR login session.
     */
    private String sessionId;
    
    /**
     * Challenge token used for session verification.
     */
    private String challenge;
    
    /**
     * QR code data - can be a data URL, image URL, or raw QR payload string.
     */
    private String qrCodeData;
    
    /**
     * Returns the QR code data formatted for display.
     * If qrCodeData is a URL, returns the URL.
     * If qrCodeData is a data URL, returns it directly.
     * Otherwise wraps it for QR code generation.
     */
    public String getDisplayData() {
        if (qrCodeData == null || qrCodeData.isEmpty()) {
            // Generate QR data from session info
            return String.format("{\"sessionId\":\"%s\",\"challenge\":\"%s\"}", sessionId, challenge);
        }
        return qrCodeData;
    }
}