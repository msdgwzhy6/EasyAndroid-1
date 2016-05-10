package com.che.easyrefresh;

import android.animation.Animator;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.LinearLayout;

/**
 * 作者：余天然 on 16/4/27 下午6:20
 */
public class RefreshLayout extends LinearLayout {

    private View header;//头部视图
    private View content;//内容视图

    private int touchSlop;//系统最小滑动距离
    private int headerWidth;//头部视图的宽度
    private int headerHeight;//头部视图的高度
    private int contentWidth;//内容视图的宽度
    private int contentHeight;//内容视图的高度
    private int maxScrollDistance = 600;//最大滑动距离
    private int gotoDefaultTime = 200;//PullNo到Default的时间
    private int gotoHeaderTime = 201;//PullYes到Refreshing的时间
    private int gotoCompleteTime = 500;//RefreshComplete到Default的时间

    private RefreshState refreshState = RefreshState.Default;
    private final FloatEvaluator evaluator;
    private RefreshListener listener;
    private float xDistance, yDistance, xLast, yLast;//滑动距离及坐标

    public enum RefreshState {
        Default,//默认状态
        PullNo,//下拉中，没有到刷新位置
        PullYes,//下拉中，超过了刷新位置
        Refreshing,//刷新中
        RefreshComplete//刷新完成
    }

    public interface RefreshListener {

        void onPullProgress(float progress);//下拉进度值

        void onStateChanged(RefreshState state);//视图状态改变

        void doRefresh();//具体执行刷新的方法
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        evaluator = new FloatEvaluator();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        content = getChildAt(0);
    }

    public void setHeader(View header) {
        this.header = header;
        addView(header, 0);
    }

    //重新测量
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (content != null) {
            final MarginLayoutParams lp = (MarginLayoutParams) content.getLayoutParams();
            final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
            final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    getPaddingTop() + getPaddingBottom() + lp.topMargin, lp.height);
            content.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }

    //重新布局
    @Override
    protected void onLayout(boolean flag, int i, int j, int k, int l) {
        super.onLayout(flag, i, j, k, l);
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        if (header != null) {
            headerWidth = header.getMeasuredWidth();
            headerHeight = header.getMeasuredHeight();
            MarginLayoutParams lp = (MarginLayoutParams) header.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin - headerHeight;
            final int right = left + headerWidth;
            final int bottom = top + headerHeight;
            header.layout(left, top, right, bottom);
        }
        if (content != null) {
            contentWidth = content.getMeasuredWidth();
            contentHeight = content.getMeasuredHeight();
            MarginLayoutParams lp = (MarginLayoutParams) content.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin;
            final int right = left + contentWidth;
            final int bottom = top + contentHeight;
            content.layout(left, top, right, bottom);
        }
    }

    //检查内容视图是否能向下滚动
    public static boolean checkViewCanScrollDown(View view) {
        if (Build.VERSION.SDK_INT < 14) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return view.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(view, -1);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //刷新过程中,不再处理手势
        if (refreshState == RefreshState.Refreshing) {
            return dispatchTouchEventSupper(ev);
        }
        //刷新完成，恢复默认中，不再处理手势
        if (refreshState == RefreshState.RefreshComplete) {
            return dispatchTouchEventSupper(ev);
        }
        //内容视图能向下滚动时，不再处理手势
        if (checkViewCanScrollDown(content)) {
            return dispatchTouchEventSupper(ev);
        }
        float curX = ev.getX();
        float curY = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDistance = yDistance = 0f;
                xLast = curX;
                yLast = curY;
                //子类和自己同时处理手势
                dispatchTouchEventSupper(ev);
                return true;
            case MotionEvent.ACTION_MOVE:
                xDistance += (curX - xLast);
                yDistance += (curY - yLast);
                xLast = curX;
                yLast = curY;
                if (Math.abs(yDistance) > Math.abs(xDistance) && yDistance > touchSlop && yDistance <= maxScrollDistance) {
                    movePos(yDistance);
                    //下拉中，没有到刷新位置
                    if (yDistance < headerHeight) {
                        refreshState = RefreshState.PullNo;
                    }
                    //下拉中，超过了刷新位置
                    if (yDistance >= headerHeight) {
                        refreshState = RefreshState.PullYes;
                    }
                    float progress = yDistance / (headerHeight - touchSlop);
                    //回调
                    if (listener != null) {
                        listener.onPullProgress(progress);
                        listener.onStateChanged(refreshState);
                    }
                }
                if(refreshState == RefreshState.Default){
                    return dispatchTouchEventSupper(ev);
                }else {
                    return true;
                }
            case MotionEvent.ACTION_UP:
                //PullYes到Refreshing的动画开始
                if (yDistance >= headerHeight) {
                    movePos(headerHeight, gotoHeaderTime);
                }
                //PullNo到Default的动画开始
                else {
                    movePos(0, gotoDefaultTime);
                }
                if(refreshState == RefreshState.Default){
                    return dispatchTouchEventSupper(ev);
                }else {
                    return true;
                }
        }
        return dispatchTouchEventSupper(ev);
    }

    public boolean dispatchTouchEventSupper(MotionEvent e) {
        return super.dispatchTouchEvent(e);
    }

    private void movePos(float yDistance) {
        scrollTo(0, (int) (-yDistance));
    }

    //通过属性动画实现弹性滑动
    public void movePos(final float destY, final int time) {
        final int srcY = -getScrollY();
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1).setDuration(time);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                float value = animator.getAnimatedFraction();
                float currentDistance = evaluator.evaluate(value, srcY, destY);
                movePos(currentDistance);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //PullNo到Default的动画完成
                if (time == gotoDefaultTime) {
                    refreshState = RefreshState.Default;
                    if (listener != null) {
                        listener.onStateChanged(refreshState);
                    }
                }
                //PullYes到Refreshing的动画完成
                if (time == gotoHeaderTime) {
                    refreshState = RefreshState.Refreshing;
                    if (listener != null) {
                        listener.onStateChanged(refreshState);
                        listener.doRefresh();
                    }
                }
                //RefreshComplete到Default的动画完成
                if (time == gotoCompleteTime) {
                    refreshState = RefreshState.Default;
                    if (listener != null) {
                        listener.onStateChanged(refreshState);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    public RefreshState getRefreshState() {
        return refreshState;
    }

    public void setRefreshState(RefreshState refreshState) {
        switch (refreshState) {
            case Refreshing:
                //直接打开刷新状态（不必再手动下拉）
                movePos(headerHeight);
                refreshState = RefreshState.Refreshing;
                if (listener != null) {
                    listener.onStateChanged(refreshState);
                    listener.doRefresh();
                }
                break;
            case RefreshComplete:
                //恢复到完成状态
                movePos(0, gotoCompleteTime);
                refreshState = RefreshState.RefreshComplete;
                if (listener != null) {
                    listener.onStateChanged(refreshState);
                }
                break;
        }
    }

    public void setListener(RefreshListener listener) {
        this.listener = listener;
    }

}
