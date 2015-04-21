package com.jasonchen.microlang.utils;

import android.app.Activity;
import android.view.View;

/**
 * jasonchen
 * 2015/04/10
 */
public class ViewUtility {

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    public static <T extends View> T findViewById(View view, int id) {
        return (T) view.findViewById(id);
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    public static <T extends View> T findViewById(Activity activity, int id) {
        return (T) activity.findViewById(id);
    }
}
