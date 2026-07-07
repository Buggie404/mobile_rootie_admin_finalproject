package com.veganbeauty.admin.features.home;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.features.product.list.ProductListFragment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BottomNavHelper {

    private static final String ACTIVE_COLOR = "#4F6544";
    private static final String INACTIVE_COLOR = "#95A192";

    public interface OnTabSelectedListener {
        void onTabSelected(int tabId);
    }

    public static void setup(MainActivity activity, View root, int activeTabId, OnTabSelectedListener onTabSelected) {
        List<Integer> tabs = Arrays.asList(
            R.id.nav_home,
            R.id.nav_product,
            R.id.nav_customer,
            R.id.nav_order,
            R.id.nav_menu
        );

        View navRoot = activity.findViewById(R.id.bottom_nav);
        if (navRoot == null) {
            navRoot = root;
        }

        for (int viewId : tabs) {
            ViewGroup tabView = navRoot.findViewById(viewId);
            if (tabView != null) {
                tabView.setOnClickListener(v -> {
                    if (onTabSelected != null) {
                        onTabSelected.onTabSelected(viewId);
                    }
                });
            }
        }

        highlightTab(navRoot, activeTabId);
    }

    public static void setup(MainActivity activity, View root, OnTabSelectedListener onTabSelected) {
        setup(activity, root, R.id.nav_home, onTabSelected);
    }

    private static class TabConfig {
        final int tabId;
        final int pillId;
        final int bgRes;

        TabConfig(int tabId, int pillId, int bgRes) {
            this.tabId = tabId;
            this.pillId = pillId;
            this.bgRes = bgRes;
        }
    }

    private static class IconPair {
        final int outlineRes;
        final int filledRes;

        IconPair(int outlineRes, int filledRes) {
            this.outlineRes = outlineRes;
            this.filledRes = filledRes;
        }
    }

    public static void highlightTab(View root, int activeTabId) {
        List<TabConfig> tabConfigs = Arrays.asList(
            new TabConfig(R.id.nav_home, R.id.nav_home_pill, R.drawable.bg_nav_pill),
            new TabConfig(R.id.nav_product, R.id.nav_product_pill, R.drawable.bg_nav_pill),
            new TabConfig(R.id.nav_customer, R.id.nav_customer_pill, R.drawable.bg_nav_pill),
            new TabConfig(R.id.nav_order, R.id.nav_order_pill, R.drawable.bg_nav_pill),
            new TabConfig(R.id.nav_menu, R.id.nav_menu_pill, R.drawable.bg_nav_pill)
        );

        Map<Integer, IconPair> iconDrawables = new HashMap<>();
        iconDrawables.put(R.id.nav_home, new IconPair(R.drawable.ic_home, R.drawable.ic_home_filled));
        iconDrawables.put(R.id.nav_product, new IconPair(R.drawable.ic_bag, R.drawable.ic_bag_filled));
        iconDrawables.put(R.id.nav_customer, new IconPair(R.drawable.ic_friends, R.drawable.ic_friends_filled));
        iconDrawables.put(R.id.nav_order, new IconPair(R.drawable.ic_box, R.drawable.ic_box_filled));
        iconDrawables.put(R.id.nav_menu, new IconPair(R.drawable.ic_grid, R.drawable.ic_grid));

        for (TabConfig config : tabConfigs) {
            ViewGroup tab = root.findViewById(config.tabId);
            if (tab == null) continue;
            ViewGroup pill = tab.findViewById(config.pillId);
            if (pill == null) continue;
            ImageView icon = null;
            if (pill.getChildCount() > 0 && pill.getChildAt(0) instanceof ImageView) {
                icon = (ImageView) pill.getChildAt(0);
            }

            boolean isActive = config.tabId == activeTabId;
            IconPair icons = iconDrawables.get(config.tabId);
            int outlineRes = icons != null ? icons.outlineRes : 0;
            int filledRes = icons != null ? icons.filledRes : 0;

            if (isActive) {
                pill.setBackgroundResource(config.bgRes);
                if (icon != null) {
                    if (filledRes != 0) {
                        icon.setImageResource(filledRes);
                    }
                    icon.setColorFilter(Color.parseColor(ACTIVE_COLOR));
                }
            } else {
                pill.setBackgroundResource(android.R.color.transparent);
                if (icon != null) {
                    if (outlineRes != 0) {
                        icon.setImageResource(outlineRes);
                    }
                    icon.setColorFilter(Color.parseColor(INACTIVE_COLOR));
                }
            }
        }
    }

    public static void navigate(MainActivity activity, int tabId) {
        activity.ensureBottomNavVisible();
        Fragment target = null;
        if (tabId == R.id.nav_home) {
            target = new HomeFragment();
        } else if (tabId == R.id.nav_product) {
            target = new ProductListFragment();
        } else if (tabId == R.id.nav_customer) {
            target = new com.veganbeauty.admin.features.customer.CustomerAdminFragment();
        } else if (tabId == R.id.nav_order) {
            target = new com.veganbeauty.admin.features.order.OrderListFragment();
        } else if (tabId == R.id.nav_menu) {
            target = new com.veganbeauty.admin.features.profile.ProfileFragment();
        }

        if (target != null) {
            activity.setCurrentTabId(tabId);
            activity.loadFragment(target);
            View bottomNav = activity.findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                highlightTab(bottomNav, tabId);
            }
        }
    }
}
