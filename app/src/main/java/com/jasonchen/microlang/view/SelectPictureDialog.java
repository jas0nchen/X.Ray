package com.jasonchen.microlang.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.jasonchen.microlang.R;

/**
 * jasonchen
 * 2015/04/10
 */
public class SelectPictureDialog extends DialogFragment {

	public static SelectPictureDialog newInstance() {
		return new SelectPictureDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String[] items = { getString(R.string.take_camera),
				getString(R.string.select_pic) };

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setItems(items,
						(DialogInterface.OnClickListener) getActivity());
		return builder.create();
	}
}
