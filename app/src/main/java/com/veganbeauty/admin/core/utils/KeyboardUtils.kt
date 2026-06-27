package com.veganbeauty.admin.core.utils

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

object KeyboardUtils {

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Traverses the view hierarchy and sets up the onEditorActionListener for all EditTexts.
     * Also sets up a touch listener on the root view to hide the keyboard when tapping outside.
     */
    fun setupKeyboardAutoHiding(rootView: View, activity: Activity?) {
        // Setup touch listener on the root view to hide keyboard when tapping outside
        rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val currentFocusedView = activity?.currentFocus ?: rootView.findFocus()
                if (currentFocusedView is EditText) {
                    val outRect = android.graphics.Rect()
                    currentFocusedView.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        currentFocusedView.clearFocus()
                        hideKeyboard(currentFocusedView)
                    }
                }
            }
            false
        }

        // Setup Enter/Done listener for all EditTexts in the hierarchy
        setupEditTextListeners(rootView)
    }

    private fun setupEditTextListeners(view: View) {
        if (view is EditText) {
            view.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_NEXT ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    view.clearFocus()
                    hideKeyboard(view)
                    true
                } else {
                    false
                }
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupEditTextListeners(view.getChildAt(i))
            }
        }
    }
}
