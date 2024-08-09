package com.example.dsiemvandroiddemo;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.datacap.android.SAFEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SAFListener extends DialogFragment implements SAFEventListener
{
    /*
    This is an example implementation of the SAFEventListener interface.
    Here we set it as a DialogFragment with a CardView and a ViewPager to allow
    the user to see the responses as they come in and see previous ones when the process is finished.
    */
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ProgressBar progressBar;
    private final ArrayList<String> responses = new ArrayList<>();
    FragmentManager fragmentManager;
    CardAdapter cardAdapter;
    ViewPager2 viewPager;

    public SAFListener(FragmentManager fragmentManager)
    {
        this.fragmentManager = fragmentManager;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.progress_popup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(root, savedInstanceState);
        progressBar = root.findViewById(R.id.progressBar);
        viewPager = root.findViewById(R.id.viewPager);
        cardAdapter = new CardAdapter(responses);
        viewPager.setAdapter(cardAdapter);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog)
    {
        super.onDismiss(dialog);
        responses.clear();
        progressBar.setProgress(0);
    }

     //SAFEventListener methods
    @Override
    public void onTransactionForwardResponse(int statusCode, int totalOperations, int currentOperation, String safResponse)
    {
        String responseToAdd = String.format(Locale.US,"StatusCode: %d, Transaction %d\nResponse:\n%s", statusCode, currentOperation, safResponse);
        int progress = (int) ((currentOperation / (float) totalOperations) * 100);
        // example: 4 / 13.00 = 0.325 * 100 = (int) 32.5 = 33
        handler.post(() ->
        {
            // Remember that all operations that interact with the UI MUST be done on the MainThread
            // You can force this by using a Handler to queue the operation.
            cardAdapter.addCard(responseToAdd);
            viewPager.setCurrentItem(cardAdapter.getItemCount(), true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                progressBar.setProgress(progress, true);
            }
            else
            {
                progressBar.setProgress(progress);
            }
        });
    }

    @Override
    public void onIsRunning(boolean running)
    {
        if (running)
        {
            handler.post(()->
                    this.show(fragmentManager, "SAFListenerDialog"));
        }
    }
    // End of SAFEventListener methods

    private static class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder>
    {
        private final List<String> cardTextList;

        public CardAdapter(List<String> cardTextList) {
            this.cardTextList = cardTextList;
        }

        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
            return new CardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CardViewHolder holder, int position)
        {
            holder.textView.setText(cardTextList.get(position));
        }

        @Override
        public int getItemCount()
        {
            return cardTextList.size();
        }

        public void addCard(String text)
        {
            cardTextList.add(text);
            notifyItemInserted(cardTextList.size() - 1);
        }

        static class CardViewHolder extends RecyclerView.ViewHolder
        {
            TextView textView;

            public CardViewHolder(@NonNull View itemView)
            {
                super(itemView);
                textView = itemView.findViewById(R.id.safResponse);
            }
        }
    }

}
