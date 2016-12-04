package org.awesomeapp.messenger.ui.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;

import org.ironrabbit.type.CustomTypefaceManager;

public class LetterAvatar extends ColorDrawable {
        Paint               paint   = new Paint();
        Rect                bounds  = new Rect();
        
        String              pLetters;
        private float       ONE_DP  = 0.0f;
        private Resources   pResources;
        private int         pPadding;
        int                 pSize   = 0;
        float               pMesuredTextWidth;
        
        int                 pBoundsTextwidth;
        int                 pBoundsTextHeight;
        
        int                 backgroundColor;

        Context             context;

        RectF rectBounds = new RectF();
        
        public LetterAvatar (Context context, String nickname, int paddingInDp) {
            super(Color.TRANSPARENT);
            this.context = context;

            if (!TextUtils.isEmpty(nickname))
                this.pLetters = nickname.substring(0,1);
            else
                this.pLetters = "?";

            this.pResources = context.getResources();
            ONE_DP = 1 * pResources.getDisplayMetrics().density;
            this.pPadding = Math.round(paddingInDp * ONE_DP);
            this.rectBounds = new RectF();

            this.backgroundColor = ColorGenerator.MATERIAL.getColor(nickname);

        }
        
        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            paint.setAntiAlias(true);
        
            paint.setColor(backgroundColor);

            canvas.drawOval(rectBounds, paint);

            if (CustomTypefaceManager.hasCustomTypeface())
                paint.setTypeface(CustomTypefaceManager.getCurrentTypeface(context));
            else {
                paint.setTypeface(Typeface.SANS_SERIF);
            }

            pLetters = pLetters.toUpperCase();

            do {
                paint.setTextSize(++pSize);
                paint.getTextBounds(pLetters, 0, pLetters.length(), bounds);

            } while ((bounds.height() < (canvas.getHeight() - pPadding)) && (paint.measureText(pLetters) < (canvas.getWidth() - pPadding)));
        
            paint.setTextSize(pSize); 
            pMesuredTextWidth = paint.measureText(pLetters);
            pBoundsTextHeight = bounds.height();
        
            float xOffset = ((canvas.getWidth() - pMesuredTextWidth) / 2f);
            float yOffset = (pBoundsTextHeight + (canvas.getHeight() - pBoundsTextHeight) / 2f);

            paint.setColor(0xffffffff);
            
            canvas.drawText(pLetters, xOffset, yOffset, paint);
        }
        
        @Override
        protected void onBoundsChange(Rect bounds) {
          super.onBoundsChange(bounds);

          rectBounds.set(bounds);
          
        }



    }