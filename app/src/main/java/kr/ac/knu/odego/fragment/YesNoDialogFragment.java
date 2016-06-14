package kr.ac.knu.odego.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import lombok.Setter;

/**
 * Created by BHI on 2016-06-14.
 */
public class YesNoDialogFragment extends AppCompatDialogFragment {

    public interface YesNoDialogListener {
        public void onDialogPositiveClick();
        public void onDialogNegativeClick();
    }

    @Setter private YesNoDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

       /* builder.setTitle(R.string.set_dest_dialog_title)
                .setMessage(R.string.set_dest_dialog_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogPositiveClick();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogNegativeClick();
                    }
                });*/

        return builder.create();
    }
}