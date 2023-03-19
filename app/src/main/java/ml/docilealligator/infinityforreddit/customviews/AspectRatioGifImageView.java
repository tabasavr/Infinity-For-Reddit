package ml.docilealligator.infinityforreddit.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import ml.docilealligator.infinityforreddit.R;
import pl.droidsonroids.gif.GifImageView;

public class AspectRatioGifImageView extends GifImageView {
    private float ratio = 1f;

    public AspectRatioGifImageView(Context context) {
        super(context);
        init(context, null);
    }

    public AspectRatioGifImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public final float getRatio() {
        return this.ratio;
    }

    public final void setRatio(float var1) {
        if (Math.abs(this.ratio - var1) > 0.0001) {
            this.ratio = var1;

            requestLayout();
            invalidate();
        }
    }

    private void init(Context context, AttributeSet attrs) {
        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AspectRatioGifImageView)) {
            this.ratio = a.getFloat(R.styleable.AspectRatioGifImageView_ratio, 1f);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.ratio > 0) {
            int width = this.getMeasuredWidth();
            int height = this.getMeasuredHeight();
            if (width != 0 || height != 0) {
                if (width > 0) {
                    height = (int) ((float) width * this.ratio);
                } else {
                    width = (int) ((float) height / this.ratio);
                }

                this.setMeasuredDimension(width, height);
            }
        }
    }

    /*@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int desiredHeight = (int) (widthSize * ratio);
        int selectedHeight;

        if(heightMode == MeasureSpec.EXACTLY) {
            selectedHeight = heightSize;
        } else if(heightMode == MeasureSpec.AT_MOST) {
            selectedHeight = Math.min(heightSize, desiredHeight);
        } else {
            selectedHeight = desiredHeight;
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(selectedHeight, MeasureSpec.EXACTLY));
    }*/
}
