package com.example.dsiemvandroiddemo.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.example.dsiemvandroiddemo.R;
import com.example.dsiemvandroiddemo.model.TransactionHistoryItem;
import com.example.dsiemvandroiddemo.model.TransactionHistoryManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

/**
 * Bottom sheet fragment for displaying transaction history with swipe navigation
 */
public class TransactionHistoryBottomSheetFragment extends BottomSheetDialogFragment 
    implements TransactionHistoryManager.HistoryChangeListener {
    
    private static final String ARG_INITIAL_TRANSACTION_ID = "initial_transaction_id";
    
    private ViewPager2 transactionViewPager;
    private TabLayout pageIndicator;
    private TextView historyCountText;
    private MaterialButton copyButton;
    private MaterialButton shareButton;
    private MaterialButton clearHistoryButton;
    
    private TransactionHistoryAdapter adapter;
    private TransactionHistoryManager historyManager;
    private String initialTransactionId;
    
    public static TransactionHistoryBottomSheetFragment newInstance() {
        return newInstance(null);
    }
    
    public static TransactionHistoryBottomSheetFragment newInstance(String initialTransactionId) {
        TransactionHistoryBottomSheetFragment fragment = new TransactionHistoryBottomSheetFragment();
        Bundle args = new Bundle();
        if (initialTransactionId != null) {
            args.putString(ARG_INITIAL_TRANSACTION_ID, initialTransactionId);
        }
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyManager = TransactionHistoryManager.getInstance();
        if (getArguments() != null) {
            initialTransactionId = getArguments().getString(ARG_INITIAL_TRANSACTION_ID);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.response_history_bottom_sheet, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews(view);
        setupViewPager();
        setupClickListeners();
        loadTransactionHistory();
        
        // Register for history updates
        historyManager.addHistoryChangeListener(this);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        historyManager.removeHistoryChangeListener(this);
    }
    
    private void setupViews(View view) {
        transactionViewPager = view.findViewById(R.id.transactionViewPager);
        pageIndicator = view.findViewById(R.id.pageIndicator);
        historyCountText = view.findViewById(R.id.historyCountText);
        copyButton = view.findViewById(R.id.copyButton);
        shareButton = view.findViewById(R.id.shareButton);
        clearHistoryButton = view.findViewById(R.id.clearHistoryButton);
    }
    
    private void setupViewPager() {
        adapter = new TransactionHistoryAdapter();
        transactionViewPager.setAdapter(adapter);
        
        // Setup page change listener to update counter and button states
        transactionViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateHistoryCounter();
                updateActionButtons();
            }
        });
        
        // Connect TabLayout with ViewPager2 for page indicators
        new TabLayoutMediator(pageIndicator, transactionViewPager,
            (tab, position) -> {
                // Create dot indicators
                tab.setText("•");
            }
        ).attach();
    }
    
    private void setupClickListeners() {
        copyButton.setOnClickListener(v -> copyCurrentTransactionResponse());
        shareButton.setOnClickListener(v -> shareCurrentTransactionResponse());
        clearHistoryButton.setOnClickListener(v -> showClearHistoryDialog());
    }
    
    private void loadTransactionHistory() {
        List<TransactionHistoryItem> history = historyManager.getHistory();
        
        if (history.isEmpty()) {
            // No transactions to show
            dismiss();
            return;
        }
        
        adapter.updateTransactions(history);
        
        // Navigate to initial transaction if specified
        if (initialTransactionId != null) {
            int index = historyManager.findTransactionIndex(initialTransactionId);
            if (index >= 0) {
                transactionViewPager.setCurrentItem(index, false);
            }
        }
        
        updateHistoryCounter();
        updateActionButtons();
    }
    
    private void updateHistoryCounter() {
        int currentPosition = transactionViewPager.getCurrentItem() + 1;
        int totalCount = adapter.getItemCount();
        historyCountText.setText(currentPosition + " of " + totalCount);
    }
    
    private void updateActionButtons() {
        boolean hasTransactions = adapter.getItemCount() > 0;
        copyButton.setEnabled(hasTransactions);
        shareButton.setEnabled(hasTransactions);
        clearHistoryButton.setEnabled(hasTransactions);
    }
    
    private void copyCurrentTransactionResponse() {
        TransactionHistoryItem currentTransaction = getCurrentTransaction();
        if (currentTransaction == null || getContext() == null) return;
        
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Transaction Response", currentTransaction.getResponse());
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(getContext(), "Response copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    private void shareCurrentTransactionResponse() {
        TransactionHistoryItem currentTransaction = getCurrentTransaction();
        if (currentTransaction == null) return;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentTransaction.getDisplayTitle() + " Response");
        shareIntent.putExtra(Intent.EXTRA_TEXT, currentTransaction.getResponse());
        
        if (shareIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(Intent.createChooser(shareIntent, "Share Transaction Response"));
        }
    }
    
    private void showClearHistoryDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Transaction History")
            .setMessage("Are you sure you want to clear all transaction history? This action cannot be undone.")
            .setPositiveButton("Clear", (dialog, which) -> {
                historyManager.clearHistory();
                dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private TransactionHistoryItem getCurrentTransaction() {
        int currentPosition = transactionViewPager.getCurrentItem();
        return adapter.getTransaction(currentPosition);
    }
    
    @Override
    public void onHistoryChanged(int newSize) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (newSize == 0) {
                    dismiss();
                } else {
                    loadTransactionHistory();
                    // Only navigate to the latest transaction if it's not a PadReset
                    if (adapter.getItemCount() > 0) {
                        TransactionHistoryItem latestTransaction = adapter.getTransaction(0);
                        if (latestTransaction != null && 
                            !"Pad Reset".equals(latestTransaction.getTransactionType())) {
                            transactionViewPager.setCurrentItem(0, true);
                        }
                        // Otherwise stay on current page
                    }
                }
            });
        }
    }
    
    /**
     * Navigate to the latest transaction in the history
     */
    public void navigateToLatestTransaction() {
        if (adapter.getItemCount() > 0) {
            transactionViewPager.setCurrentItem(0, true);
        }
    }
}