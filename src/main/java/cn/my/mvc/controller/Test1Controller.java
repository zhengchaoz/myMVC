package cn.my.mvc.controller;

import cn.my.mvc.annotation.MyAutowired;
import cn.my.mvc.annotation.MyController;
import cn.my.mvc.annotation.MyRequestMapping;
import cn.my.mvc.annotation.MyRequestParam;
import cn.my.mvc.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @user 郑超
 * @date 2021/4/7
 */
@MyController
@MyRequestMapping("test1")
public class Test1Controller {

    @MyAutowired
    private TestService testService;

    @MyRequestMapping("test")
    public void myTest(HttpServletRequest request, HttpServletResponse response,
                       @MyRequestParam("param") String param) {
        try {
            response.getWriter().write("Test1Controller:the param you send is :" + param);
            testService.printParam(param);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
