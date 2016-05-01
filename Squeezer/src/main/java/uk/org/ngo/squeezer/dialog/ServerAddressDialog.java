package uk.org.ngo.squeezer.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import uk.org.ngo.squeezer.DisconnectedActivity;
import uk.org.ngo.squeezer.NowPlayingFragment;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.ConnectionHelper;

/**
 * Created by Stefan on 30-4-2016.
 */
public class ServerAddressDialog extends DialogFragment {

    private BaseActivity mActivity;
    private Resources resources;
    private FragmentManager fragmentManager;
    private ServerAddressView form;
    private ConnectionHelper ConnectionHelper= null;

    private EditText mUserNameEditText;
    private EditText mPasswordEditText;

    public void setData(BaseActivity mActivity, Resources resources, FragmentManager fragmentManager) {
        this.mActivity = mActivity;
        this.resources = resources;
        this.fragmentManager = fragmentManager;
        ConnectionHelper = (ConnectionHelper) new ConnectionHelper(mActivity, resources);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.server_address_view, null);

        mUserNameEditText = (EditText) view.findViewById(R.id.username);
        mPasswordEditText = (EditText) view.findViewById(R.id.password);
        mPasswordEditText.setHint(R.string.settings_username_hint_required);
        mUserNameEditText.setHint(R.string.settings_password_hint_required);

        return new AlertDialog.Builder(getActivity())
                .setTitle("Login Required")
                .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ConnectionHelper.savePreferences("192.168.2.22", mUserNameEditText.getText().toString(), mPasswordEditText.getText().toString());

                        NowPlayingFragment fragment = (NowPlayingFragment) fragmentManager.findFragmentById(R.id.now_playing_fragment);
                        fragment.startVisibleConnection();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DisconnectedActivity.showLoginFailed(mActivity);
                    }
                })
                .setView(view)
                .create();
    }


    @Override
    public void onCancel(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }
}
