package cn.my.mvc.controller;

import cn.my.mvc.annotation.MyController;
import cn.my.mvc.annotation.MyRequestMapping;
import cn.my.mvc.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @user 郑超
 * @date 2021/4/7
 */
@MyController
@MyRequestMapping("test2")
public class Test2Controller {

    @MyRequestMapping("test")
    public void myTest(HttpServletRequest request, HttpServletResponse response,
                       @MyRequestParam("param") String param) {
        try {
            response.getWriter().write("Test2Controller:the param you send is :" + param);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
