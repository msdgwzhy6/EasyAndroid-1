package com.che.easyrefresh;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by yutianran on 16/2/25.
 */
public class LogUtil {

    private static final String TAG = "print";
    private static final DateFormat formatter = new SimpleDateFormat("MM-dd-HH:mm:ss.SSS");

    /*打印*/
    public static void print(String msg) {
        String tmp = getLineFile() + getLineMethod() + msg;
        Log.i(TAG, tmp);
    }


    /*获取行所在的行号和方法名*/
    public static String getLineMethod() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[2];
        StringBuffer toStringBuffer = new StringBuffer("[")
                .append(traceElement.getLineNumber()).append(" | ")
                .append(traceElement.getMethodName()).append("]");
        return toStringBuffer.toString();
    }

    /*获取行所在的文件名*/
    public static String getLineFile() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[2];
        return traceElement.getFileName();
    }
}
