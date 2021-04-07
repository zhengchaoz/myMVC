package cn.my.mvc.annotation;

import java.lang.annotation.*;

/**
 * @user 郑超
 * @date 2021/4/7
 */
@Target(ElementType.PARAMETER)// 注解的作用目标：参数
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {
    String value();
}
