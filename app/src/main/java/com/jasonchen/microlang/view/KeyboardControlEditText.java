package com.jasonchen.microlang.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

/**
 * jasonchen
 * 2015/04/17
 */
public class KeyboardControlEditText extends AutoCompleteTextView {
    private boolean mShowKeyboard = true;

    public void setShowKeyboard(boolean value) {
        mShowKeyboard = value;
    }

    public KeyboardControlEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return mShowKeyboard;
    }
}