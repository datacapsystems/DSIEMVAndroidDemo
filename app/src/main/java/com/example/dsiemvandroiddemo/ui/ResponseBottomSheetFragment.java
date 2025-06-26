package com.example.dsiemvandroiddemo.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dsiemvandroiddemo.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Bottom sheet fragment for displaying transaction responses
 */
public class ResponseBottomSheetFragment extends BottomSheetDialogFragment {
    
    private static final String ARG_RESPONSE = "response";
    private static final String ARG_TRANSACTION_TYPE = "transaction_type";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_STATUS = "status";
    
    private String response;
    private String transactionType;
    private String amount;
    private boolean isSuccess;
    
    private TextView rawResponseText;
    private ScrollView rawResponseContent;
    private ImageView rawResponseExpandIcon;
    private LinearLayout rawResponseHeader;
    private boolean isRawResponseExpanded = false;
    
    public static ResponseBottomSheetFragment newInstance(String response, String transactionType, 
                                                         String amount, boolean isSuccess) {
        ResponseBottomSheetFragment fragment = new ResponseBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RESPONSE, response);
        args.putString(ARG_TRANSACTION_TYPE, transactionType);
        args.putString(ARG_AMOUNT, amount);
        args.putBoolean(ARG_STATUS, isSuccess);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            response = getArguments().getString(ARG_RESPONSE, "");
            transactionType = getArguments().getString(ARG_TRANSACTION_TYPE, "Transaction");
            amount = getArguments().getString(ARG_AMOUNT, "");
            isSuccess = getArguments().getBoolean(ARG_STATUS, false);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.response_bottom_sheet, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews(view);
        populateData();
        setupClickListeners();
    }
    
    private void setupViews(View view) {
        rawResponseText = view.findViewById(R.id.rawResponseText);
        rawResponseContent = view.findViewById(R.id.rawResponseContent);
        rawResponseExpandIcon = view.findViewById(R.id.rawResponseExpandIcon);
        rawResponseHeader = view.findViewById(R.id.rawResponseHeader);
    }
    
    private void populateData() {
        View view = getView();
        if (view == null) return;
        
        // Set status chip
        Chip statusChip = view.findViewById(R.id.statusChip);
        if (isSuccess) {
            statusChip.setText("Success");
            statusChip.setChipIconResource(R.drawable.ic_check_circle);
            statusChip.setChipBackgroundColorResource(R.color.success_background);
        } else {
            statusChip.setText("Error");
            statusChip.setChipIconResource(android.R.drawable.ic_dialog_alert);
            statusChip.setChipBackgroundColorResource(R.color.error_background);
        }
        
        // Set timestamp
        TextView timestampText = view.findViewById(R.id.timestampText);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        timestampText.setText(sdf.format(new Date()));
        
        // Set summary data
        TextView summaryAmount = view.findViewById(R.id.summaryAmount);
        if (!amount.isEmpty()) {
            summaryAmount.setText("$" + amount);
        }
        
        // Extract card type and auth code from response if possible
        TextView summaryCardType = view.findViewById(R.id.summaryCardType);
        TextView summaryAuthCode = view.findViewById(R.id.summaryAuthCode);
        
        // Simple parsing - in a real app you'd use proper XML parsing
        if (response.contains("Visa")) {
            summaryCardType.setText("Visa");
        } else if (response.contains("MasterCard")) {
            summaryCardType.setText("MasterCard");
        } else {
            summaryCardType.setText("Unknown");
        }
        
        // Set raw response
        rawResponseText.setText(response.isEmpty() ? "No response data available" : response);
    }
    
    private void setupClickListeners() {
        View view = getView();
        if (view == null) return;
        
        // Copy button
        MaterialButton copyButton = view.findViewById(R.id.copyButton);
        copyButton.setOnClickListener(v -> copyResponseToClipboard());
        
        // Share button
        MaterialButton shareButton = view.findViewById(R.id.shareButton);
        shareButton.setOnClickListener(v -> shareResponse());
        
        // Raw response expand/collapse
        rawResponseHeader.setOnClickListener(v -> toggleRawResponse());
    }
    
    private void copyResponseToClipboard() {
        if (getContext() == null) return;
        
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Transaction Response", response);
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(getContext(), "Response copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    private void shareResponse() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, transactionType + " Response");
        shareIntent.putExtra(Intent.EXTRA_TEXT, response);
        
        if (shareIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(Intent.createChooser(shareIntent, "Share Response"));
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
        
        // Show/hide content
        rawResponseContent.setVisibility(isRawResponseExpanded ? View.VISIBLE : View.GONE);
    }
}