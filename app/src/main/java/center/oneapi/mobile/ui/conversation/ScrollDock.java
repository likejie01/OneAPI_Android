package center.oneapi.mobile.ui.conversation;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import center.oneapi.mobile.R;
import center.oneapi.mobile.ui.UiKit;

public class ScrollDock extends LinearLayout {
    private RecyclerView recyclerView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable = () -> setAlpha(0f);
    private static final float VISIBLE_ALPHA = 0.6f;
    private ValueAnimator activeScroll;

    public ScrollDock(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setClipToPadding(false);
        setClipChildren(false);
        setAlpha(VISIBLE_ALPHA);
        addButton(R.drawable.ic_scroll_top, "顶部", this::scrollTop);
        addButton(R.drawable.ic_scroll_current_top, "当前气泡顶部", this::scrollCurrentTop);
        addButton(R.drawable.ic_scroll_current_bottom, "当前气泡底部", this::scrollCurrentBottom);
        addButton(R.drawable.ic_scroll_bottom, "底部", this::scrollBottom);
    }

    public void bind(RecyclerView recyclerView, View root) {
        this.recyclerView = recyclerView;
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recenter(root);
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                revealTemporarily();
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                revealTemporarily();
            }
        });
        revealTemporarily();
    }

    private void addButton(int icon, String desc, Runnable action) {
        ImageButton button = UiKit.imageButton(getContext(), icon, desc);
        button.setColorFilter(UiKit.muted(getContext()));
        button.setPadding(UiKit.dp(getContext(), 4), UiKit.dp(getContext(), 4), UiKit.dp(getContext(), 4), UiKit.dp(getContext(), 4));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(getContext(), 22), UiKit.dp(getContext(), 22));
        lp.setMargins(0, UiKit.dp(getContext(), 9), 0, UiKit.dp(getContext(), 9));
        addView(button, lp);
    }

    private void recenter(View root) {
        if (!(getLayoutParams() instanceof FrameLayout.LayoutParams)) {
            return;
        }
        Rect visible = new Rect();
        root.getWindowVisibleDisplayFrame(visible);
        int[] rootPos = new int[2];
        root.getLocationOnScreen(rootPos);
        int visibleBottomInRoot = visible.bottom - rootPos[1];
        int rootHeight = Math.max(1, root.getHeight());
        int available = Math.max(UiKit.dp(getContext(), 160), Math.min(rootHeight, visibleBottomInRoot));
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.gravity = Gravity.RIGHT | Gravity.TOP;
        lp.topMargin = Math.max(UiKit.dp(getContext(), 12), (available - getMeasuredHeight()) / 2);
        lp.rightMargin = UiKit.dp(getContext(), 3);
        setLayoutParams(lp);
    }

    private void revealTemporarily() {
        refreshButtonColors();
        setAlpha(VISIBLE_ALPHA);
        handler.removeCallbacks(hideRunnable);
        handler.postDelayed(hideRunnable, 5000);
    }

    private void refreshButtonColors() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof ImageButton) {
                ((ImageButton) child).setColorFilter(UiKit.muted(getContext()));
            }
        }
    }

    public void scrollBottom() {
        if (recyclerView == null || recyclerView.getAdapter() == null) return;
        int delta = recyclerView.computeVerticalScrollRange()
                - recyclerView.computeVerticalScrollExtent()
                - recyclerView.computeVerticalScrollOffset();
        if (delta > 0) linearScrollBy(delta);
    }

    private void scrollTop() {
        if (recyclerView == null) return;
        int delta = -recyclerView.computeVerticalScrollOffset();
        if (delta < 0) linearScrollBy(delta);
    }

    private void scrollCurrentTop() {
        LinearLayoutManager manager = manager();
        if (manager == null) return;
        int first = manager.findFirstVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION) return;
        View child = manager.findViewByPosition(first);
        if (child == null) return;
        int delta = child.getTop() - recyclerView.getPaddingTop();
        if (delta != 0) linearScrollBy(delta);
    }

    private void scrollCurrentBottom() {
        LinearLayoutManager manager = manager();
        if (manager == null || recyclerView.getAdapter() == null) return;
        int last = manager.findLastVisibleItemPosition();
        if (last == RecyclerView.NO_POSITION) return;
        View child = manager.findViewByPosition(last);
        if (child == null) return;
        int viewportBottom = recyclerView.getHeight() - recyclerView.getPaddingBottom();
        int delta = child.getBottom() - viewportBottom;
        if (delta != 0) linearScrollBy(delta);
    }

    private LinearLayoutManager manager() {
        if (recyclerView == null || !(recyclerView.getLayoutManager() instanceof LinearLayoutManager)) {
            return null;
        }
        return (LinearLayoutManager) recyclerView.getLayoutManager();
    }

    private void linearScrollBy(int delta) {
        if (recyclerView == null || delta == 0) return;
        if (activeScroll != null) activeScroll.cancel();
        final int[] last = new int[]{0};
        int distance = Math.abs(delta);
        int duration = Math.max(180, Math.min(900, distance / Math.max(1, UiKit.dp(getContext(), 2))));
        activeScroll = ValueAnimator.ofInt(0, delta);
        activeScroll.setInterpolator(new LinearInterpolator());
        activeScroll.setDuration(duration);
        activeScroll.addUpdateListener(animation -> {
            int value = (Integer) animation.getAnimatedValue();
            int step = value - last[0];
            if (step != 0) recyclerView.scrollBy(0, step);
            last[0] = value;
        });
        activeScroll.start();
    }
}
