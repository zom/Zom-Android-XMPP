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

            if (CustomTypefaceManager.precomposeRequired()) {
                //	addTextChangedListener(mTibetanTextWatcher);
            }

            addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                    if (themeColorText != -1)
                        setTextColor(themeColorText);
                }
            });
        }


		if (themeColorText != -1)
			setTextColor(themeColorText);
    	

    }


	@Override
	public void setText(CharSequence text, BufferType type) {

		super.setText(text, type);

		if (themeColorText != -1)
			setTextColor(themeColorText);

	}



    
	/*
    TextWatcher mTibetanTextWatcher = new TextWatcher()
	{
    	
    	
		@Override
		public void afterTextChanged(Editable s) {
			
			String newText = s.toString();
			
			if (CustomTypefaceManager.precomposeRequired() && newText.endsWith("\u0f0b"))
			{
				newText = CustomTypefaceManager.handlePrecompose(newText).trim();
			
				//now remove our watcher, set the value, then re-add our watcher
				removeTextChangedListener(mTibetanTextWatcher);
				setText(newText);
				addTextChangedListener(mTibetanTextWatcher);
			
				//move the cursor to the end
				setSelection(newText.length());
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start,
				int count, int after) {
			
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start,
				int before, int count) {
			
			
		}
		
	};
    */

}