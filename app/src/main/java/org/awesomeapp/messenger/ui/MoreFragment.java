package org.awesomeapp.messenger.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.thebluealliance.spectrum.SpectrumDialog;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;

import cn.pedant.SweetAlert.SweetAlertDialog;
import im.zom.messenger.R;

public class MoreFragment extends Fragment {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GalleryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MoreFragment newInstance(String param1, String param2) {
        MoreFragment fragment = new MoreFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public MoreFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        View btn = view.findViewById(R.id.btnOpenGalllery);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getActivity(),GalleryActivity.class);
                getActivity().startActivity(intent);
            }
        });

        btn = view.findViewById(R.id.btnOpenGroups);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity)getActivity()).showGroupChatDialog();

            }
        });

        btn = view.findViewById(R.id.btnOpenStickers);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(),StickerActivity.class);
                getActivity().startActivity(intent);

            }
        });

        btn = view.findViewById(R.id.btnOpenThemes);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showColors();

            }
        });
        return view;
    }

    private void showColors ()
    {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int selColor = settings.getInt("themeColor",-1);

        new SpectrumDialog.Builder(getContext())
                .setColors(R.array.zom_colors)
                .setDismissOnColorSelected(false)
                .setSelectedColor(selColor)
                .setOnColorSelectedListener(new SpectrumDialog.OnColorSelectedListener() {
                    @Override public void onColorSelected(boolean positiveResult, @ColorInt int color) {
                        if(positiveResult) {
                            MainActivity activity = (MainActivity)getActivity();
                            settings.edit().putInt("themeColor",color).commit();
                            activity.applyStyleForToolbar();
                        }
                    }
                }).build().show(getFragmentManager(), "dialog_theme_1");

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


}
