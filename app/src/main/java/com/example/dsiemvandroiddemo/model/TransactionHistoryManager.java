package com.example.dsiemvandroiddemo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages transaction history with automatic caching and size limits
 */
public class TransactionHistoryManager {
    private static final int MAX_HISTORY_SIZE = 10;
    private static TransactionHistoryManager instance;
    
    private final List<TransactionHistoryItem> history;
    private final List<HistoryChangeListener> listeners;
    
    private TransactionHistoryManager() {
        this.history = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }
    
    public static synchronized TransactionHistoryManager getInstance() {
        if (instance == null) {
            instance = new TransactionHistoryManager();
        }
        return instance;
    }
    
    /**
     * Adds a new transaction to the history
     * Automatically removes oldest entries if exceeding MAX_HISTORY_SIZE
     */
    public synchronized void addTransaction(String transactionType, String amount, String request,
                                          String response, boolean isSuccess, String deviceName) {
        TransactionHistoryItem item = new TransactionHistoryItem(
            transactionType, amount, request, response, isSuccess, deviceName);
        
        // Add to beginning of list (most recent first)
        history.add(0, item);
        
        // Remove oldest entries if exceeding max size
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
        
        // Notify listeners
        notifyHistoryChanged();
    }
    
    /**
     * Gets all transaction history items (most recent first)
     */
    public synchronized List<TransactionHistoryItem> getHistory() {
        return new ArrayList<>(history);
    }
    
    /**
     * Gets a specific transaction by index (0 = most recent)
     */
    public synchronized TransactionHistoryItem getTransaction(int index) {
        if (index >= 0 && index < history.size()) {
            return history.get(index);
        }
        return null;
    }
    
    /**
     * Gets the most recent transaction
     */
    public synchronized TransactionHistoryItem getLatestTransaction() {
        return history.isEmpty() ? null : history.get(0);
    }
    
    /**
     * Gets the total number of transactions in history
     */
    public synchronized int getHistorySize() {
        return history.size();
    }
    
    /**
     * Clears all transaction history
     */
    public synchronized void clearHistory() {
        history.clear();
        notifyHistoryChanged();
    }
    
    /**
     * Finds the index of a transaction by ID
     */
    public synchronized int findTransactionIndex(String transactionId) {
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getId().equals(transactionId)) {
                return i;
            }
        }
        return -1;
    }
    
    // Listener management
    public void addHistoryChangeListener(HistoryChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeHistoryChangeListener(HistoryChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyHistoryChanged() {
        for (HistoryChangeListener listener : listeners) {
            listener.onHistoryChanged(getHistorySize());
        }
    }
    
    /**
     * Interface for listening to history changes
     */
    public interface HistoryChangeListener {
        void onHistoryChanged(int newSize);
    }
}