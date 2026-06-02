package center.oneapi.mobile.ui.composer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class FlowTagLayout extends ViewGroup {
    public FlowTagLayout(Context context) {
        super(context);
        setClipToPadding(false);
        setClipChildren(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthLimit = Math.max(0, MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight());
        int usedWidth = 0;
        int lineHeight = 0;
        int totalHeight = getPaddingTop() + getPaddingBottom();
        int maxWidth = getPaddingLeft() + getPaddingRight();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams lp = marginLayoutParams(child);
            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            if (usedWidth > 0 && usedWidth + childWidth > widthLimit) {
                totalHeight += lineHeight;
                maxWidth = Math.max(maxWidth, getPaddingLeft() + getPaddingRight() + usedWidth);
                usedWidth = 0;
                lineHeight = 0;
            }
            usedWidth += childWidth;
            lineHeight = Math.max(lineHeight, childHeight);
        }
        totalHeight += lineHeight;
        maxWidth = Math.max(maxWidth, getPaddingLeft() + getPaddingRight() + usedWidth);
        setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec), resolveSize(totalHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int widthLimit = Math.max(0, right - left - getPaddingLeft() - getPaddingRight());
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int lineHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            MarginLayoutParams lp = marginLayoutParams(child);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            int required = lp.leftMargin + childWidth + lp.rightMargin;
            if (x > getPaddingLeft() && x - getPaddingLeft() + required > widthLimit) {
                x = getPaddingLeft();
                y += lineHeight;
                lineHeight = 0;
            }
            int childLeft = x + lp.leftMargin;
            int childTop = y + lp.topMargin;
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            x += required;
            lineHeight = Math.max(lineHeight, lp.topMargin + childHeight + lp.bottomMargin);
        }
    }

    private MarginLayoutParams marginLayoutParams(View child) {
        ViewGroup.LayoutParams raw = child.getLayoutParams();
        if (raw instanceof MarginLayoutParams) {
            return (MarginLayoutParams) raw;
        }
        return new MarginLayoutParams(raw == null ? generateDefaultLayoutParams() : raw);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(android.util.AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }
}
