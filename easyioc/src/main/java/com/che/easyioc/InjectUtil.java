package com.che.easyioc;

import android.app.Activity;

import java.lang.reflect.Field;

/**
 * Created by Zane on 15/12/1.
 */
public class InjectUtil {

    public static void bind(Object activity){
        //获得被注解的变量
        Field[] fields = activity.getClass().getDeclaredFields();

        if(fields != null && fields.length != 0) {
            for (Field field : fields) {
                //防止我不能查询private变量
                field.setAccessible(true);
                //获取注解类型
                Bind bind = field.getAnnotation(Bind.class);
                if (bind != null) {
                    //获得变量值，赋值给被注解的变量
                    int id = bind.id();
                    try {
                        field.set(activity, ((Activity) activity).findViewById(id));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void unbind(Object activity){
        //获得被注解的变量
        Field[] fields = activity.getClass().getDeclaredFields();

        if(fields != null && fields.length != 0) {
            for (Field field : fields) {
                //防止我不能查询private变量
                field.setAccessible(true);
                //设为空
                try {
                    field.set(activity, null);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
