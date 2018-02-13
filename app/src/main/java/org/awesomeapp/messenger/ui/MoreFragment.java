package org.awesomeapp.messenger.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.awesomeapp.messenger.MainActivity;

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

        btn = view.findViewById(R.id.btnOpenServices);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openZomServices ();

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

        ColorPickerDialogBuilder
                .with(getContext())
                .setTitle("Choose color")
                .initialColor(selColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .lightnessSliderOnly()
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {

                    }
                })
                .setPositiveButton(getString(R.string.ok), new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {

                        settings.edit().putInt("themeColor",selectedColor).commit();

                        /**
                        int textColor = getContrastColor(selectedColor);
                        int bgColor = getContrastColor(textColor);

                        settings.edit().putInt("themeColorBg",bgColor).commit();
                        settings.edit().putInt("themeColorText",textColor).commit();
                         */

                        MainActivity activity = (MainActivity)getActivity();
                        activity.applyStyle();

                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void openZomServices ()
    {
        Intent intent = new Intent(getActivity(),ServicesActivity.class);
        getActivity().startActivity(intent);
    }
}
