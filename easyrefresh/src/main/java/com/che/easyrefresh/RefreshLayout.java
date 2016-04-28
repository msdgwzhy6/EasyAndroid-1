package com.che.easyrefresh;

import android.animation.Animator;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        LogUtil.print("");
        setOrientation(VERTICAL);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        evaluator = new FloatEvaluator();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LogUtil.print("");
        content = getChildAt(0);
//        header = LayoutInflater.from(getContext()).inflate(R.layout.view_header, null);
//        addView(header, 0);
    }

    public void setHeader(View header) {
        LogUtil.print("");
        this.header = header;
        addView(header,0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LogUtil.print("");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (content != null) {
            measureContentView(content, widthMeasureSpec, heightMeasureSpec);
        }
    }

    //重新测量内容视图
    private void measureContentView(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    //重新测量内容视图
    @Override
    protected void onLayout(boolean flag, int i, int j, int k, int l) {
        LogUtil.print("");
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        LogUtil.print("");
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //刷新过程中,不再处理手势
        if (refreshState == RefreshState.Refreshing) {
            return super.onTouchEvent(ev);
        }
        //刷新完成，恢复默认中，不再处理手势
        if (refreshState == RefreshState.RefreshComplete) {
            return super.onTouchEvent(ev);
        }
        float curX = ev.getX();
        float curY = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDistance = yDistance = 0f;
                xLast = curX;
                yLast = curY;
                return true;
            case MotionEvent.ACTION_MOVE:
                xDistance += (curX - xLast);
                yDistance += (curY - yLast);
                LogUtil.print("xDistance=" + xDistance + "\tyDistance=" + yDistance);
                xLast = curX;
                yLast = curY;
                //上拉时，不处理手势
                if (yDistance <= 0) {
                    return super.onTouchEvent(ev);
                }
                if (Math.abs(yDistance) > Math.abs(xDistance) && yDistance > touchSlop && yDistance < maxScrollDistance) {
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
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                //上拉时，不处理手势
                if (yDistance <= 0) {
                    return super.onTouchEvent(ev);
                }
                //PullYes到Refreshing的动画开始
                if (yDistance >= headerHeight) {
                    movePos(headerHeight, gotoHeaderTime);
                }
                //PullNo到Default的动画开始
                else {
                    movePos(0, gotoDefaultTime);
                }
                break;
        }
        return super.onTouchEvent(ev);
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
                        listener.doRefresh();
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

    @Override
    public void scrollTo(int x, int y) {
        LogUtil.print("x=" + x + "\ty=" + y);
        super.scrollTo(x, y);
    }

    public RefreshState getRefreshState() {
        return refreshState;
    }

    public void setRefreshState(RefreshState refreshState) {
        switch (refreshState) {
            case Refreshing:
                //直接打开刷新状态（不必再手动下拉）
                movePos(headerHeight);
                if (listener != null) {
                    listener.doRefresh();
                }
                break;
            case RefreshComplete:
                //恢复到完成状态
                movePos(0, gotoCompleteTime);
                break;
        }
    }

    public void setListener(RefreshListener listener) {
        this.listener = listener;
    }

}
