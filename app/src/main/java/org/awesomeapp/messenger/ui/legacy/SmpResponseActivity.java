package org.awesomeapp.messenger.ui.legacy;

import org.awesomeapp.messenger.crypto.IOtrChatSession;
import org.awesomeapp.messenger.crypto.OtrDebugLogger;
import org.awesomeapp.messenger.service.IChatSession;
import im.zom.messenger.R;
import org.awesomeapp.messenger.model.Address;
import org.awesomeapp.messenger.service.ImServiceConstants;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.EditText;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.BaseActivity;

public class SmpResponseActivity extends BaseActivity {

    private EditText mInputSMP;
    private String mSessionId;
    private String mQuestion;
    private long mProviderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInputSMP = new EditText(this);

        mSessionId = getIntent().getStringExtra("sid");
        mProviderId = getIntent().getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, -1);
        mQuestion = getIntent().getStringExtra("q");
        showQuestionDialog();
    }

    private void showQuestionDialog() {

        String title = getString(R.string.smp_question_title);
        String strQuestion = mSessionId + ": " + mQuestion;

        new AlertDialog.Builder(this).setTitle(title).setMessage(strQuestion)
                .setView(mInputSMP)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String secret = mInputSMP.getText().toString();
                        respondSmp(mSessionId, secret);

                        SmpResponseActivity.this.finish();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();

    }

    private void respondSmp(String sid, String answer) {

        ImApp app = (ImApp)getApplication();



        IOtrChatSession iOtrSession;
        try {
            IChatSession chatSession = app.getChatSession(mProviderId, -1, Address.stripResource(sid));
            iOtrSession = chatSession.getDefaultOtrChatSession();
            if (iOtrSession == null) {
                OtrDebugLogger.log("no session in progress for provider " + mProviderId);
                return;
            }
            iOtrSession.respondSmpVerification(answer);

        } catch (RemoteException e) {
            OtrDebugLogger.log("could not respond to SMP", e);
        }
    }

}
