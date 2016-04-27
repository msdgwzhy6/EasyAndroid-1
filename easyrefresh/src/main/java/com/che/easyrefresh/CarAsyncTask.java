package com.che.easyrefresh;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Created by yutianran on 16/1/4.
 */
public abstract class CarAsyncTask<T> {

    private static final HandlerThread handlerThread = new HandlerThread(CarAsyncTask.class.getName(), 10);
    public CarAsyncTask() {
    }
    protected abstract void onPreExecute();
    protected abstract T doInBackground();
    protected abstract void onPostExecute(T param);
    public final CarAsyncTask<T> execute() {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        Handler bgHandler = new Handler(handlerThread.getLooper());
        this.onPreExecute();
        bgHandler.post(new Runnable() {
            public void run() {
                final T param= CarAsyncTask.this.doInBackground();
                mainHandler.post(new Runnable() {
                    public void run() {
                        CarAsyncTask.this.onPostExecute(param);
                    }
                });
            }
        });
        return this;
    }
    static {
        handlerThread.start();
    }
}