package com.example.dsiemvandroiddemo.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dsiemvandroiddemo.R;
import com.example.dsiemvandroiddemo.model.TransactionHistoryItem;
import com.example.dsiemvandroiddemo.util.TransactionResponseParser;
import com.google.android.material.chip.Chip;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for ViewPager2 to display transaction history with swipe navigation
 */
public class TransactionHistoryAdapter extends RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder> {
    
    private static List<TransactionHistoryItem> transactions;
    
    public TransactionHistoryAdapter() {
        this.transactions = new ArrayList<>();
    }
    
    public void updateTransactions(List<TransactionHistoryItem> newTransactions) {
        this.transactions = new ArrayList<>(newTransactions);
        notifyDataSetChanged();
    }
    
    public TransactionHistoryItem getTransaction(int position) {
        if (position >= 0 && position < transactions.size()) {
            return transactions.get(position);
        }
        return null;
    }
    
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.transaction_history_page, parent, false);
        return new TransactionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionHistoryItem transaction = transactions.get(position);
        holder.bind(transaction);
    }
    
    @Override
    public int getItemCount() {
        return transactions.size();
    }
    
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private TextView transactionTitle;
        private TextView deviceName;
        private Chip statusChip;
        private TextView timestampText;
        private MaterialCardView summaryCard;
        private TextView summaryAmount;
        private TextView summaryCardType;
        private TextView summaryAuthCode;
        private LinearLayout rawRequestHeader;
        private ImageView rawRequestExpandIcon;
        private LinearLayout rawRequestContent;
        private TextView rawRequestText;
        private boolean isRawRequestExpanded = false;
        private LinearLayout rawResponseHeader;
        private ImageView rawResponseExpandIcon;
        private LinearLayout rawResponseContent;
        private TextView rawResponseText;
        private boolean isRawResponseExpanded = false;
        
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            initViews(itemView);
            setupClickListeners();
        }
        
        private void initViews(View itemView) {
            transactionTitle = itemView.findViewById(R.id.transactionTitle);
            deviceName = itemView.findViewById(R.id.deviceName);
            statusChip = itemView.findViewById(R.id.statusChip);
            timestampText = itemView.findViewById(R.id.timestampText);
            summaryCard = itemView.findViewById(R.id.summaryCard);
            summaryAmount = itemView.findViewById(R.id.summaryAmount);
            summaryCardType = itemView.findViewById(R.id.summaryCardType);
            summaryAuthCode = itemView.findViewById(R.id.summaryAuthCode);
            rawRequestHeader = itemView.findViewById(R.id.rawRequestHeader);
            rawRequestExpandIcon = itemView.findViewById(R.id.rawRequestExpandIcon);
            rawRequestContent = itemView.findViewById(R.id.rawRequestContent);
            rawRequestText = itemView.findViewById(R.id.rawRequestText);
            rawResponseHeader = itemView.findViewById(R.id.rawResponseHeader);
            rawResponseExpandIcon = itemView.findViewById(R.id.rawResponseExpandIcon);
            rawResponseContent = itemView.findViewById(R.id.rawResponseContent);
            rawResponseText = itemView.findViewById(R.id.rawResponseText);
        }
        
        private void setupClickListeners() {
            rawRequestHeader.setOnClickListener(v -> toggleRawRequest());
            rawResponseHeader.setOnClickListener(v -> toggleRawResponse());
        }
        
        public void bind(TransactionHistoryItem transaction) {
            // Parse transaction response for detailed information first
            String response = transaction.getResponse();
            TransactionResponseParser.ParsedTransactionData parsedData = 
                TransactionResponseParser.parseTransactionResponse(response);
            
            // Set transaction title with parsed authorized amount if available
            String authorizedAmount = parsedData.getAmount();
            if (authorizedAmount != null && !authorizedAmount.isEmpty()) {
                transactionTitle.setText(transaction.getDisplayTitleWithAmount(authorizedAmount));
            } else {
                transactionTitle.setText(transaction.getDisplayTitle());
            }
            deviceName.setText(transaction.getDeviceName() != null ? transaction.getDeviceName() : "Unknown Device");
            timestampText.setText(transaction.getFormattedTime());
            
            // Set status chip
            statusChip.setText(transaction.getStatusText());
            if (transaction.isSuccess()) {
                statusChip.setChipIconResource(R.drawable.ic_check_circle);
                statusChip.setChipBackgroundColorResource(R.color.success_background);
            } else {
                statusChip.setChipIconResource(android.R.drawable.ic_dialog_alert);
                statusChip.setChipBackgroundColorResource(R.color.error_background);
            }
            
            // Show/hide summary card based on whether we have meaningful data
            if (parsedData.hasValidData()) {
                summaryCard.setVisibility(View.VISIBLE);
                populateSummaryData(parsedData, transaction);
            } else {
                summaryCard.setVisibility(View.GONE);
            }
            
            // Reset expand state - don't load raw data until expanded
            isRawRequestExpanded = false;
            rawRequestContent.setVisibility(View.GONE);
            rawRequestExpandIcon.setRotation(0);
            rawRequestText.setText(""); // Clear previous content
            
            isRawResponseExpanded = false;
            rawResponseContent.setVisibility(View.GONE);
            rawResponseExpandIcon.setRotation(0);
            rawResponseText.setText(""); // Clear previous content
        }
        
        private void populateSummaryData(TransactionResponseParser.ParsedTransactionData parsedData, 
                                        TransactionHistoryItem transaction) {
            // Set amount (prefer parsed amount, fallback to transaction amount)
            String amount = parsedData.getAmount();
            if (amount == null || amount.isEmpty()) {
                amount = transaction.getAmount();
            }
            if (amount != null && !amount.isEmpty()) {
                summaryAmount.setText("$" + amount);
            } else {
                summaryAmount.setText("N/A");
            }
            
            // Set card type
            String cardType = parsedData.getCardType();
            if (cardType != null && !cardType.isEmpty()) {
                summaryCardType.setText(cardType);
            } else {
                summaryCardType.setText("Unknown");
            }
            
            // Set auth code
            String authCode = parsedData.getAuthCode();
            if (authCode != null && !authCode.isEmpty()) {
                summaryAuthCode.setText(authCode);
            } else {
                summaryAuthCode.setText("N/A");
            }
        }
        
        private void toggleRawRequest() {
            isRawRequestExpanded = !isRawRequestExpanded;
            
            // Animate the expand icon
            RotateAnimation rotateAnimation = new RotateAnimation(
                isRawRequestExpanded ? 0 : 180,
                isRawRequestExpanded ? 180 : 0,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotateAnimation.setDuration(200);
            rotateAnimation.setFillAfter(true);
            rawRequestExpandIcon.startAnimation(rotateAnimation);
            
            if (isRawRequestExpanded) {
                // Show content and load request text in background
                rawRequestContent.setVisibility(View.VISIBLE);
                loadRawRequestAsync();
            } else {
                // Hide content
                rawRequestContent.setVisibility(View.GONE);
            }
        }
        
        private void toggleRawResponse() {
            isRawResponseExpanded = !isRawResponseExpanded;
            
            // Animate the expand icon
            RotateAnimation rotateAnimation = new RotateAnimation(
                isRawResponseExpanded ? 0 : 180,
                isRawResponseExpanded ? 180 : 0,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotateAnimation.setDuration(200);
            rotateAnimation.setFillAfter(true);
            rawResponseExpandIcon.startAnimation(rotateAnimation);
            
            if (isRawResponseExpanded) {
                // Show content and load response text in background
                rawResponseContent.setVisibility(View.VISIBLE);
                loadRawResponseAsync();
            } else {
                // Hide content
                rawResponseContent.setVisibility(View.GONE);
            }
        }
        
        private void loadRawResponseAsync() {
            // Show loading message immediately
            rawResponseText.setText("Loading response...");
            
            // Load the actual response in background thread to prevent UI freezing
            new Thread(() -> {
                try {
                    // Get the transaction from the current position
                    int currentPosition = getAbsoluteAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        TransactionHistoryItem transaction = transactions.get(currentPosition);
                        String response = transaction.getResponse();
                        String displayText;
                        
                        if (response != null && !response.isEmpty()) {
                            // Truncate very large responses to prevent memory issues
                            final int MAX_DISPLAY_LENGTH = 10000; // 10KB limit
                            if (response.length() > MAX_DISPLAY_LENGTH) {
                                displayText = response.substring(0, MAX_DISPLAY_LENGTH) + 
                                    "\n\n... (Response truncated - " + (response.length() - MAX_DISPLAY_LENGTH) + 
                                    " more characters)";
                            } else {
                                displayText = response;
                            }
                        } else {
                            displayText = "No response data available";
                        }
                        
                        // Update UI on main thread
                        rawResponseText.post(() -> {
                            rawResponseText.setText(displayText);
                        });
                    }
                } catch (Exception e) {
                    // Handle any errors gracefully
                    rawResponseText.post(() -> {
                        rawResponseText.setText("Error loading response data");
                    });
                }
            }).start();
        }
        
        private void loadRawRequestAsync() {
            // Show loading message immediately
            rawRequestText.setText("Loading request...");
            
            // Load the actual request in background thread to prevent UI freezing
            new Thread(() -> {
                try {
                    // Get the transaction from the current position
                    int currentPosition = getAbsoluteAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        TransactionHistoryItem transaction = transactions.get(currentPosition);
                        String request = transaction.getRequest();
                        String displayText;
                        
                        if (request != null && !request.isEmpty()) {
                            // Truncate very large requests to prevent memory issues
                            final int MAX_DISPLAY_LENGTH = 10000; // 10KB limit
                            if (request.length() > MAX_DISPLAY_LENGTH) {
                                displayText = request.substring(0, MAX_DISPLAY_LENGTH) + 
                                    "\n\n... (Request truncated - " + (request.length() - MAX_DISPLAY_LENGTH) + 
                                    " more characters)";
                            } else {
                                displayText = request;
                            }
                        } else {
                            displayText = "No request data available";
                        }
                        
                        // Update UI on main thread
                        rawRequestText.post(() -> {
                            rawRequestText.setText(displayText);
                        });
                    }
                } catch (Exception e) {
                    // Handle any errors gracefully
                    rawRequestText.post(() -> {
                        rawRequestText.setText("Error loading request data");
                    });
                }
            }).start();
        }
    }
}