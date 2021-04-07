package cn.my.mvc.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解以实现自动注入
 *
 * @user 郑超
 * @date 2021/4/7
 */
@Target(ElementType.FIELD)// 注解的作用目标：字段
@Retention(RetentionPolicy.RUNTIME)// 注解的生命周期：运行时
@Documented// 用来标注生成javadoc的时候是否会被记录
public @interface MyAutowired {
    String value() default "";
}
