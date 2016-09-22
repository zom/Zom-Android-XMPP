package org.ironrabbit.type;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

public class CustomTypefaceEditText extends EditText {

    boolean mInit = false;
    int themeColorText = -1;

    public CustomTypefaceEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
      

       init();
    }

    public CustomTypefaceEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		

	       init();
	}



	public CustomTypefaceEditText(Context context) {
		super(context);
		

	       init();
	}
	
	


	private void init() {

        if (!mInit) {
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
            themeColorText = settings.getInt("themeColorText",-1);

            Typeface t = CustomTypefaceManager.getCurrentTypeface(getContext());

            if (t != null)
                setTypeface(t);

            mInit = true;
        }


		if (themeColorText > 0 || themeColorText < -1)
			setTextColor(themeColorText);
    	

    }


	@Override
	public void setText(CharSequence text, BufferType type) {

		super.setText(text, type);

		if (themeColorText > 0 || themeColorText < -1)
			setTextColor(themeColorText);

	}



    

}