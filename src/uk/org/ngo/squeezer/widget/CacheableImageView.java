/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package uk.org.ngo.squeezer.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class CacheableImageView extends ImageView {

    private CacheableBitmapWrapper mDisplayedBitmapWrapper;

    public CacheableImageView(Context context) {
        super(context);
    }

    public CacheableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * onDraw() is occasionally called after the bitmap has been recycled. This should not happen.
     * If it does, just return.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        final Drawable drawable = getDrawable();

        if (drawableContainsRecycledBitmap(drawable))
            return;

        if (drawable instanceof LayerDrawable) {
            // Iterate over the layers in the drawable (should be only two since this
            // should be a TransitionDrawable, but just in case...). Perform the same
            // isRecycled() check to make sure it's safe to use the bitmap. If it's not,
            // return.
            for (int i = 0, l = ((LayerDrawable) drawable).getNumberOfLayers(); i < l; i++) {
                if (drawableContainsRecycledBitmap(((LayerDrawable) drawable).getDrawable(i)))
                    return;
            }
        }

        super.onDraw(canvas);
    }

    /**
     * Check to see if a Drawable contains a recycled bitmap.
     * 
     * @param drawable The Drawable to check
     * @return true if contains a recycled bitmap, false otherwise (either the bitmap is not
     *         recycled, or the drawable did not contain a bitmap).
     */
    private boolean drawableContainsRecycledBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            final Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null && bitmap.isRecycled()) {
                Log.v("CacheableImageView", "Trying to draw with a recycled bitmap");
                return true;
            }
        }

        return false;
    }

    /**
     * Sets the current {@code CacheableBitmapWrapper}, and displays it Bitmap.
     *
     * @param wrapper - Wrapper to display.s
     */
    public void setImageCachedBitmap(final CacheableBitmapWrapper wrapper) {
        if (null != wrapper) {
            wrapper.setBeingUsed(true);
            setImageDrawable(new BitmapDrawable(getResources(), wrapper.getBitmap()));
        } else {
            setImageDrawable(null);
        }

        // Finally, set our new BitmapWrapper
        mDisplayedBitmapWrapper = wrapper;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        setImageCachedBitmap(new CacheableBitmapWrapper(bm));
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        resetCachedDrawable();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        resetCachedDrawable();
    }

    public CacheableBitmapWrapper getCachedBitmapWrapper() {
        return mDisplayedBitmapWrapper;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Will cause displayed bitmap wrapper to be 'free-able'
        setImageDrawable(null);
    }

    /**
     * Called when the current cached bitmap has been removed. This method will
     * remove the displayed flag and remove this objects reference to it.
     */
    private void resetCachedDrawable() {
        if (null != mDisplayedBitmapWrapper) {
            mDisplayedBitmapWrapper.setBeingUsed(false);
            mDisplayedBitmapWrapper = null;
        }
    }

}