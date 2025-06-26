package com.example.dsiemvandroiddemo.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class for parsing EMV transaction responses
 */
public class TransactionResponseParser {
    private static final Logger LOG = Logger.getLogger(TransactionResponseParser.class.getName());
    
    /**
     * Parsed transaction data container
     */
    public static class ParsedTransactionData {
        private String amount;
        private String cardType;
        private String authCode;
        private String responseCode;
        private String responseText;
        private String transactionId;
        private String maskedPan;
        private boolean hasValidData;
        
        public ParsedTransactionData() {
            this.hasValidData = false;
        }
        
        // Getters and setters
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        
        public String getCardType() { return cardType; }
        public void setCardType(String cardType) { this.cardType = cardType; }
        
        public String getAuthCode() { return authCode; }
        public void setAuthCode(String authCode) { this.authCode = authCode; }
        
        public String getResponseCode() { return responseCode; }
        public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
        
        public String getResponseText() { return responseText; }
        public void setResponseText(String responseText) { this.responseText = responseText; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public String getMaskedPan() { return maskedPan; }
        public void setMaskedPan(String maskedPan) { this.maskedPan = maskedPan; }
        
        public boolean hasValidData() { return hasValidData; }
        public void setHasValidData(boolean hasValidData) { this.hasValidData = hasValidData; }
        
        public boolean isSuccess() {
            return responseCode != null && 
                   (responseCode.equals("000") || responseCode.equals("00"));
        }
    }
    
    /**
     * Parse EMV transaction response XML
     */
    public static ParsedTransactionData parseTransactionResponse(String xmlResponse) {
        ParsedTransactionData data = new ParsedTransactionData();
        
        if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
            return data;
        }
        
        try {
            // Parse basic transaction fields - use Authorize for the authorized amount
            data.setAmount(extractXmlValue(xmlResponse, "Authorize"));
            data.setAuthCode(extractXmlValue(xmlResponse, "AuthCode"));
            data.setResponseCode(extractXmlValue(xmlResponse, "ResponseCode"));
            data.setResponseText(extractXmlValue(xmlResponse, "ResponseText"));
            data.setTransactionId(extractXmlValue(xmlResponse, "TransactionID"));
            data.setMaskedPan(extractXmlValue(xmlResponse, "MaskedPAN"));
            
            // Determine card type from various possible fields
            String cardType = determineCardType(xmlResponse);
            data.setCardType(cardType);
            
            // Check if we have any meaningful transaction data
            boolean hasValidData = data.getAmount() != null || 
                                 data.getAuthCode() != null || 
                                 data.getResponseCode() != null ||
                                 data.getMaskedPan() != null;
            
            data.setHasValidData(hasValidData);
            
        } catch (Exception e) {
            LOG.warning("Failed to parse transaction response: " + e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Extract value from XML tag
     */
    private static String extractXmlValue(String xml, String tagName) {
        try {
            String openTag = "<" + tagName + ">";
            String closeTag = "</" + tagName + ">";
            
            int start = xml.indexOf(openTag);
            if (start == -1) return null;
            
            start += openTag.length();
            int end = xml.indexOf(closeTag, start);
            if (end == -1) return null;
            
            String value = xml.substring(start, end).trim();
            return value.isEmpty() ? null : value;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Determine card type from response
     */
    private static String determineCardType(String xmlResponse) {
        // Check various possible fields for card type information
        String[] cardTypeFields = {
            "CardType", "CardName", "IssuerName", "PaymentMethod"
        };
        
        for (String field : cardTypeFields) {
            String value = extractXmlValue(xmlResponse, field);
            if (value != null) {
                return normalizeCardType(value);
            }
        }
        
        // Check for card indicators in the response text
        String responseText = extractXmlValue(xmlResponse, "ResponseText");
        if (responseText != null) {
            String cardType = extractCardTypeFromText(responseText);
            if (cardType != null) {
                return cardType;
            }
        }
        
        // Check masked PAN for card type
        String maskedPan = extractXmlValue(xmlResponse, "MaskedPAN");
        if (maskedPan != null) {
            return determineCardTypeFromPan(maskedPan);
        }
        
        return null;
    }
    
    /**
     * Normalize card type names
     */
    private static String normalizeCardType(String cardType) {
        if (cardType == null) return null;
        
        String normalized = cardType.toLowerCase();
        
        if (normalized.contains("visa")) {
            return "Visa";
        } else if (normalized.contains("mastercard") || normalized.contains("master card")) {
            return "MasterCard";
        } else if (normalized.contains("amex") || normalized.contains("american express")) {
            return "American Express";
        } else if (normalized.contains("discover")) {
            return "Discover";
        } else if (normalized.contains("diners")) {
            return "Diners Club";
        } else if (normalized.contains("jcb")) {
            return "JCB";
        }
        
        return cardType; // Return original if no match
    }
    
    /**
     * Extract card type from response text
     */
    private static String extractCardTypeFromText(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("visa")) return "Visa";
        if (lowerText.contains("mastercard") || lowerText.contains("master card")) return "MasterCard";
        if (lowerText.contains("amex") || lowerText.contains("american express")) return "American Express";
        if (lowerText.contains("discover")) return "Discover";
        if (lowerText.contains("diners")) return "Diners Club";
        if (lowerText.contains("jcb")) return "JCB";
        
        return null;
    }
    
    /**
     * Determine card type from PAN (first 4 digits)
     */
    private static String determineCardTypeFromPan(String maskedPan) {
        if (maskedPan == null || maskedPan.length() < 4) return null;
        
        // Extract first 4 digits
        String firstFour = maskedPan.replaceAll("[^0-9]", "");
        if (firstFour.length() < 4) return null;
        
        int bin = Integer.parseInt(firstFour.substring(0, 4));
        
        // Basic BIN ranges (simplified)
        if (bin >= 4000 && bin <= 4999) {
            return "Visa";
        } else if ((bin >= 5100 && bin <= 5599) || (bin >= 2221 && bin <= 2720)) {
            return "MasterCard";
        } else if (bin >= 3400 && bin <= 3499) {
            return "American Express";
        } else if (bin >= 6000 && bin <= 6599) {
            return "Discover";
        } else if (bin >= 3000 && bin <= 3099) {
            return "Diners Club";
        } else if (bin >= 3528 && bin <= 3589) {
            return "JCB";
        }
        
        return null;
    }
}