package com.che.easyrefresh;

/**
 * 作者：余天然 on 16/4/27 下午11:10
 */
public enum RefreshState {
    Default,//默认状态
    PullNo,//下拉中，没有到刷新位置
    PullYes,//下拉中，超过了刷新位置
    Refreshing,//刷新中
    RefreshComplete//刷新完成
}
