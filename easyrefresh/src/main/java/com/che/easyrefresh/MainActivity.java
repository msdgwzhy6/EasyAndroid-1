package com.che.easyrefresh;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.Random;

public class MainActivity extends Activity {

    private int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.print("");
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        index = 0;
        LogUtil.print("");
        final RefreshLayout refreshLayout = (RefreshLayout) findViewById(R.id.view_refresh);
//        View header = findViewById(R.id.view_header);
        View header = LayoutInflater.from(this).inflate(R.layout.view_header, null);
        refreshLayout.setHeader(header);
        View content = findViewById(R.id.view_content);

        final ImageView iv = (ImageView) header.findViewById(R.id.iv_header);
        final TextView tv = (TextView) header.findViewById(R.id.tv_header);
        final TextView tvIndex = (TextView) content.findViewById(R.id.tv);

        refreshLayout.setRefreshState(RefreshState.Refreshing);//进去就开始刷新

        refreshLayout.setListener(new RefreshListener() {
            @Override
            public void onPullProgress(float progress) {

            }

            @Override
            public void onStateChanged(RefreshState state) {
                switch (state) {
                    case Default:
                        tv.setText("默认状态");
                        Glide.with(MainActivity.this).load(R.drawable.l2_01).into(iv);
                        break;
                    case PullNo:
                        tv.setText("下拉刷新");
                        break;
                    case PullYes:
                        tv.setText("松开刷新");
                        break;
                    case Refreshing:
                        tv.setText("正在刷新");
                        Glide.with(MainActivity.this).load(R.drawable.l2).into(iv);
                        break;
                    case RefreshComplete:
                        tv.setText("刷新完成");
                        break;
                }
            }

            @Override
            public void doRefresh() {
                LogUtil.print("开始刷新");
                new CarAsyncTask<Object>() {
                    @Override
                    protected void onPreExecute() {

                    }

                    @Override
                    protected Object doInBackground() {
                        try {
                            LogUtil.print("正在获取网络数据");
                            index = new Random().nextInt(10);
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object param) {
                        LogUtil.print("刷新完成");
                        tv.setText("刷新完成");
                        tvIndex.setText("我是内容视图：" + index);
                        refreshLayout.setRefreshState(RefreshState.RefreshComplete);
                    }
                }.execute();

            }
        });
    }
}
