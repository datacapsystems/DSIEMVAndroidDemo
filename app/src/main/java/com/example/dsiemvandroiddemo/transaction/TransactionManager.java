package com.example.dsiemvandroiddemo.transaction;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Manages transaction execution ensuring only one transaction is active at a time
 */
public class TransactionManager {
    private static final Logger LOGGER = Logger.getLogger(TransactionManager.class.getName());
    
    public enum TransactionState {
        IDLE,
        PROCESSING,
        COMPLETED,
        ERROR
    }
    
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final AtomicReference<TransactionState> currentState;
    private final WeakReference<Context> contextRef;
    
    public TransactionManager(Context context) {
        this.contextRef = new WeakReference<>(context);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "TransactionThread");
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        });
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.currentState = new AtomicReference<>(TransactionState.IDLE);
    }
    
    /**
     * Attempts to execute a transaction. Returns false if a transaction is already in progress.
     */
    public boolean executeTransaction(TransactionRequest request, TransactionCallback callback) {
        // Only allow new transaction if current state is IDLE
        if (!currentState.compareAndSet(TransactionState.IDLE, TransactionState.PROCESSING)) {
            LOGGER.warning("Transaction rejected - another transaction is in progress");
            callback.onTransactionBusy();
            return false;
        }
        
        executor.execute(() -> {
            Context context = contextRef.get();
            if (context == null) {
                LOGGER.severe("Context is null - activity may have been destroyed");
                currentState.set(TransactionState.ERROR);
                runOnUiThread(() -> callback.onError(new IllegalStateException("Activity context no longer available")));
                return;
            }
            
            try {
                LOGGER.info("Starting transaction: " + request.getTransactionType());
                
                // Execute the actual transaction
                String result = request.execute(context);
                
                // Update state and notify success
                currentState.set(TransactionState.COMPLETED);
                runOnUiThread(() -> callback.onSuccess(result));
                
            } catch (Exception e) {
                LOGGER.severe("Transaction failed: " + e.getMessage());
                currentState.set(TransactionState.ERROR);
                runOnUiThread(() -> callback.onError(e));
            } finally {
                // Reset to IDLE after a short delay to allow UI updates
                executor.execute(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    currentState.set(TransactionState.IDLE);
                });
            }
        });
        
        return true;
    }
    
    /**
     * Cancels the current transaction if possible
     */
    public void cancelCurrentTransaction() {
        if (currentState.get() == TransactionState.PROCESSING) {
            // Implement cancellation logic here
            LOGGER.info("Attempting to cancel current transaction");
        }
    }
    
    /**
     * Gets the current transaction state
     */
    public TransactionState getCurrentState() {
        return currentState.get();
    }
    
    /**
     * Checks if a transaction is currently in progress
     */
    public boolean isTransactionInProgress() {
        return currentState.get() == TransactionState.PROCESSING;
    }
    
    /**
     * Shuts down the transaction manager
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warning("Forcing shutdown of transaction executor");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
    
    /**
     * Interface for transaction requests
     */
    public interface TransactionRequest {
        String getTransactionType();
        String execute(Context context) throws Exception;
    }
    
    /**
     * Callback interface for transaction results
     */
    public interface TransactionCallback {
        void onSuccess(String response);
        void onError(Exception e);
        void onTransactionBusy();
    }
}