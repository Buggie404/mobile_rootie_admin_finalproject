package com.veganbeauty.admin.features.home;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.veganbeauty.admin.core.base.RootieAdminFragment;

public class PlaceholderFragment extends RootieAdminFragment {

    public static PlaceholderFragment newInstance(String title) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String title = "Placeholder Screen";
        if (getArguments() != null) {
            title = getArguments().getString("title", "Placeholder Screen");
        }
        TextView textView = new TextView(getContext());
        textView.setText(title);
        textView.setTextSize(20f);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return textView;
    }

    @Override
    protected void setupUI(View view) {
        // Simple placeholder
    }
}
