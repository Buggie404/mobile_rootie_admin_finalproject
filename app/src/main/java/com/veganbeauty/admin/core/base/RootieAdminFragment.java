package com.veganbeauty.admin.core.base;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.features.home.HomeMessageFragment;
import com.veganbeauty.admin.core.utils.KeyboardUtils;

public abstract class RootieAdminFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        KeyboardUtils.setupKeyboardAutoHiding(view, getActivity());
        setupUI(view);
        observeViewModel();
    }

    protected abstract void setupUI(View view);

    protected void observeViewModel() {
        // Option to observe common ViewModel data
    }

    protected void setupHeaderMessageButton(View messageBtn) {
        if (messageBtn == null) return;
        Context context = requireContext();
        boolean isAdmin = "admin".equals(UserSession.getRole(context));
        if (isAdmin) {
            messageBtn.setVisibility(View.VISIBLE);
            messageBtn.setOnClickListener(v -> {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.loadFragment(new HomeMessageFragment());
                }
            });

            TextView badgeView = messageBtn.findViewById(com.veganbeauty.admin.R.id.message_badge);
            if (badgeView == null) {
                badgeView = messageBtn.findViewById(com.veganbeauty.admin.R.id.home_header_message_badge);
            }

            MainActivity mainAct = (MainActivity) getActivity();
            if (mainAct != null && badgeView != null) {
                if (mainAct.getLastUnreadCount() > 0) {
                    badgeView.setText(String.valueOf(mainAct.getLastUnreadCount()));
                    badgeView.setVisibility(View.VISIBLE);
                } else {
                    badgeView.setVisibility(View.GONE);
                }
            }
        } else {
            messageBtn.setVisibility(View.GONE);
        }
    }
}
