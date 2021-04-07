package cn.my.mvc.annotation;

import java.lang.annotation.*;

/**
 * @user 郑超
 * @date 2021/4/7
 */
@Target({ElementType.METHOD, ElementType.TYPE})// 注解的作用目标：类、接口(包括注释类型)或枚举声明及方法
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {
    String value();
}
