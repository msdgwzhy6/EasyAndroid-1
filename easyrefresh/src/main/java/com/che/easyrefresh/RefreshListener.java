package com.che.easyrefresh;

/**
 * 作者：余天然 on 16/4/27 下午11:10
 */
public interface RefreshListener {

    void onPullProgress(float progress);//下拉进度值

    void onStateChanged(RefreshState state);//视图状态改变

    void doRefresh();//具体执行刷新的方法
}
