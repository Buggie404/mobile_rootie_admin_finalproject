package com.veganbeauty.admin.core.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class KeyboardUtils {

    public static void hideKeyboard(View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Traverses the view hierarchy and sets up the onEditorActionListener for all EditTexts.
     * Also sets up a touch listener on the root view to hide the keyboard when tapping outside.
     */
    public static void setupKeyboardAutoHiding(View rootView, Activity activity) {
        if (rootView == null) return;

        // Setup touch listener on the root view to hide keyboard when tapping outside
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View currentFocusedView = null;
                    if (activity != null) {
                        currentFocusedView = activity.getCurrentFocus();
                    }
                    if (currentFocusedView == null) {
                        currentFocusedView = rootView.findFocus();
                    }
                    if (currentFocusedView instanceof EditText) {
                        Rect outRect = new Rect();
                        currentFocusedView.getGlobalVisibleRect(outRect);
                        if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                            currentFocusedView.clearFocus();
                            hideKeyboard(currentFocusedView);
                        }
                    }
                }
                return false;
            }
        });

        // Setup Enter/Done listener for all EditTexts in the hierarchy
        setupEditTextListeners(rootView);
    }

    private static void setupEditTextListeners(View view) {
        if (view instanceof EditText) {
            ((EditText) view).setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_NEXT ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)
                ) {
                    view.clearFocus();
                    hideKeyboard(view);
                    return true;
                }
                return false;
            });
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setupEditTextListeners(viewGroup.getChildAt(i));
            }
        }
    }
}
