package org.awesomeapp.messenger.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.ui.widgets.ImageViewActivity;

import java.io.IOException;

import info.guardianproject.otr.app.im.R;

public class AccountFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GalleryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AccountFragment newInstance(String param1, String param2) {
        AccountFragment fragment = new AccountFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        final ImApp app = ((ImApp)getActivity().getApplication());

        View view = inflater.inflate(R.layout.awesome_fragment_account, container, false);

        String fullUserName = app.getDefaultUsername();
        XmppAddress xAddress = new XmppAddress(fullUserName);

        TextView tvNickname = (TextView)view.findViewById(R.id.tvNickname);
        TextView tvUsername = (TextView)view.findViewById(R.id.edtName);
        TextView tvFingerprint = (TextView)view.findViewById(R.id.tvFingerprint);

        ImageView ivScan = (ImageView)view.findViewById(R.id.buttonScan);

        ivScan.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {

                String inviteString;
                try {
                    inviteString = OnboardingManager.generateInviteLink(getActivity(), app.getDefaultUsername(), app.getDefaultOtrKey());
                    OnboardingManager.inviteScan(getActivity(), inviteString);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        });

        tvUsername.setText(fullUserName);
        tvNickname.setText(xAddress.getUser());

        tvFingerprint.setText(prettyPrintFingerprint(app.getDefaultOtrKey()));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private String prettyPrintFingerprint (String fingerprint)
    {
        StringBuffer spacedFingerprint = new StringBuffer();

        for (int i = 0; i + 8 <= fingerprint.length(); i+=8)
        {
            spacedFingerprint.append(fingerprint.subSequence(i,i+8));
            spacedFingerprint.append(' ');
        }

        return spacedFingerprint.toString();
    }
}
