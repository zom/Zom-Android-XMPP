package org.awesomeapp.messenger.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import im.zom.messenger.R;

/**
 * Created by N-Pex on 2017-08-21.
 *
 * This is a custom class for flow layout. It assumes the following, for implementation simplicity:
 * 1. Child views can measure themselves, i.e. they are not set to "wrap_content" of "match_parent".
 * 2. All children are the same height.
 */
public class FlowLayout extends NestedScrollView {
    private int itemWidthPadding;
    private int itemHeightPadding;
    private FlowLayoutInner mContentView;

    public FlowLayout(Context context) {
        super(context);
        init(null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FlowLayout);
            if (a != null) {
                this.itemWidthPadding = a.getDimensionPixelOffset(R.styleable.FlowLayout_itemWidthPadding, 0);
                this.itemHeightPadding = a.getDimensionPixelOffset(R.styleable.FlowLayout_itemHeightPadding, 0);
                a.recycle();
            }
        }
        mContentView = new FlowLayoutInner(getContext());
        super.addView(mContentView);
    }

    @Override
    public void addView(View child) {
        mContentView.addView(child);
        post(new Runnable() {
            @Override
            public void run() {
                scrollTo(0, getBottom());
            }
        });
    }

    @Override
    public void removeAllViews() {
        mContentView.removeAllViews();
    }

    @Override
    public void removeView(View view) {
        mContentView.removeView(view);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mContentView.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), Math.min(mContentView.getMeasuredHeight(), getMeasuredHeight()));
    }

    class FlowLayoutInner extends ViewGroup {

        public FlowLayoutInner(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

            // Let the children measure themselves
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            }

            // Get the width we have available
            int width = MeasureSpec.getSize(widthMeasureSpec);
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
                // Use width of widest child
                width = 0;
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    width = Math.max(width, child.getMeasuredWidth());
                }
            }

            int currentX = 0;
            int currentY = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int childWidth = child.getMeasuredWidth();
                if (currentX + childWidth > width) {
                    if (currentX == 0) {
                        // Need to truncate the child, it is too wide for our layout!
                        child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                        i -= 1; // Redo this one
                    } else {
                        // Use previous child for height
                        currentY += getChildAt(i - 1).getMeasuredHeight() + itemHeightPadding;
                        currentX = childWidth;
                    }
                } else {
                    currentX += childWidth + itemWidthPadding;
                }
            }
            if (getChildCount() > 0) {
                currentY += getChildAt(getChildCount() - 1).getMeasuredHeight();
            }

            setMeasuredDimension(width, currentY);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int width = r - l;
            int currentX = 0;
            int currentY = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                if (currentX + childWidth > width) {
                    currentY += getChildAt(i - 1).getMeasuredHeight() + itemHeightPadding;
                    currentX = 0;
                }
                child.layout(currentX, currentY, currentX + childWidth, currentY + childHeight);
                currentX += childWidth + itemWidthPadding;
            }
        }
    }
}
