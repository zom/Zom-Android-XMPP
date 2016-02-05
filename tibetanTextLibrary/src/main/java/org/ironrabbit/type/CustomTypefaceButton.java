package org.ironrabbit.type;


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TextView;

public class CustomTypefaceButton extends Button {

    boolean mInit = false;

	public CustomTypefaceButton(Context context) {
		super(context);
		init();
	}

	public CustomTypefaceButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CustomTypefaceButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	@TargetApi(21)
	public CustomTypefaceButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	private void init() {
    	
		if (!mInit)
        {
			Typeface t = CustomTypefaceManager.getCurrentTypeface(getContext());
			
			if (t != null)
				setTypeface(t);
	    	 
			mInit = true;
        }

        
    }



	@Override
	public void setText(CharSequence text, BufferType type) {
		init();
		super.setText(text, type);
	}
    



}