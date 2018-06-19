package org.ironrabbit.type;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.TextView;

public class CustomTypefaceTextView extends TextView {

    boolean mInit = false;
    int themeColorText = -1;

    public CustomTypefaceTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
   
       init();
    }

    
    
    public CustomTypefaceTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
	       init();
	}



	public CustomTypefaceTextView(Context context) {
		super(context);
	
	       init();
	}

	@TargetApi(21)
	public CustomTypefaceTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

        init();
	}



	private void init() {
    	
        if (!mInit) {
            Typeface t = CustomTypefaceManager.getCurrentTypeface(getContext());

            if (t != null)
                setTypeface(t);

            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
            themeColorText = settings.getInt("themeColorText", -1);

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