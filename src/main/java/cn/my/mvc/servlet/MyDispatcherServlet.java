package cn.my.mvc.servlet;

import cn.my.mvc.annotation.MyAutowired;
import cn.my.mvc.annotation.MyController;
import cn.my.mvc.annotation.MyRequestMapping;
import cn.my.mvc.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * Dispatcher：MVC框架的请求分发中专
 * 继承HttpServlet，重写init()、doGet()、doPost()
 *
 * @user 郑超
 * @date 2021/4/7
 */
public class MyDispatcherServlet extends HttpServlet {

    private final Logger log = Logger.getLogger("init");
    private final Properties props = new Properties();
    // 该集合装载着指定配置文件中设定的包下面所有的类的全类名（cn.my.mvc.annotation.MyAutowired）
    private final List<String> classNames = new ArrayList<String>();
    // 该集合装载着 classNames 中的所有类的对象(k-v  beanName-bean) beanName默认和类名相同但是首字母小写
    private final Map<String, Object> ioc = new HashMap<>();
    // (k-v  url-method)
    private final Map<String, Method> handlerMapping = new HashMap<>();
    // (k-v  url-bean)
    private final Map<String, Object> controllerMap = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 注释掉父类实现，不然会报错：405 HTTP method GET is not supported by this URL
        //super.doPost(req, resp);
        log.info("执行MyDispatcherServlet的doPost()");
        try {
            //处理请求
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 注释掉父类实现，不然会报错：405 HTTP method GET is not supported by this URL
        //super.doGet(req, resp);
        log.info("执行MyDispatcherServlet的doGet()");
        try {
            //处理请求
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        log.info("初始化DispatcherServlet");
        // 1.加载配置文件，填充properties
        // 该属性在web.xml中配置，其指向myapplication.properties
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        // 2.根据properties，扫描配置文件设定的包下面所有的类，得到相关类的全类名（cn.my.mvc.annotation.MyAutowired）
        doScanner(props.getProperty("scanPackage"));
        // 3.拿到扫描到的类，通过反射机制实例化，并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写
        doInstance();
        // 4.自动化注入依赖--字段，主要是Controller中的业务接口
        doAutowired();
        // 5.初始化HandlerMapping(将url和method对应上)
        initHandlerMapping();
    }

    /**
     * 通过请求url找到对应的method，再通过反射来调用
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (handlerMapping.isEmpty()) {
            return;
        }
        // 获取请求url
        String url = req.getRequestURI();
        // 上下文路径 :项目名
        String contextPath = req.getContextPath();
        // 将请求去掉项目名和/
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        // 去掉url前面的斜杠"/"，所有的@MyRequestMapping可以不用写斜杠"/"
        if (url.lastIndexOf("/") != 0)
            url = url.substring(1);
        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 没找到！");
            log.info("404 没找到！");
            return;
        }
        // 根据请求找到对应的方法，即Controller中的带有@MyRequestMapping注解的方法
        Method method = this.handlerMapping.get(url);
        // 获得方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 获得请求的参数，不包括req和resp
        Map<String, String[]> parameterMap = req.getParameterMap();
        // 保存参数值
        Object[] paramValues = new Object[parameterTypes.length];
        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            //根据参数名称，做某些处理
            String requestParam = parameterTypes[i].getSimpleName();
            if (requestParam.equals("HttpServletRequest")) {
                //参数类型已明确，这边强转类型
                paramValues[i] = req;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")) {
                paramValues[i] = resp;
                continue;
            }
            if (requestParam.equals("String")) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                    paramValues[i] = value;
                }
            }
        }
        //利用反射机制来调用
        try {
            //第一个参数是method所对应的实例 在controllerMap容器中 利用url找到对应的bean
            //method.invoke(this.controllerMap.get(url), paramValues);
            method.invoke(this.controllerMap.get(url), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据配置文件位置，读取配置文件中的配置信息，将其填充到properties字段
     *
     * @param location
     */
    private void doLoadConfig(String location) {
        //把web.xml中的contextConfigLocation对应value值的文件加载到流里面
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            //用Properties文件加载文件里的内容
            log.info("读取" + location + "里面的文件");
            props.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关流
            if (null != resourceAsStream) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将指定包下扫描得到的类，添加到classNames字段中
     * 只是简单的得到全类名，例如：cn.my.mvc.annotation.MyAutowired
     *
     * @param packageName
     */
    private void doScanner(String packageName) {
        // 先将将配置文件中配置的扫描路径cn.my.mvc 准成文件目录cn/my/mvc
        // 然后在得到带盘符的根目录E:/... /cn/my/mvc
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        assert url != null;
        File dir = new File(url.getFile());// 将路径转成文件对象
        // 相当于循环获得cn/my/mvc文件下的所有.java文件
        // 即将cn.my.mvc包下的所有类加入list中
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory())// 是否是文件夹
                doScanner(packageName + "." + file.getName());//递归读取包
            else {
                // 获得全类名：cn.my.mvc.annotation.MyAutowired
                // packageName = cn.my.mvc.annotation
                // file.getName() = MyAutowired.class
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    /**
     * 将字符串中的首字母小写，将类名转成按驼峰命名的对象名
     *
     * @param name
     * @return
     */
    private String toLowerFirstWord(String name) {
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    /**
     * 将classNames中的类实例化，经key-value：类名（小写）-类对象放入ioc字段中
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                //把类搞出来，反射来实例化(只有加@MyController需要实例化)
                Class<?> clazz = Class.forName(className);
                // 获得该类的注解，通过判断是那种注解来对该类做相应的操作
                if (clazz.isAnnotationPresent(MyController.class))// 控制器层的注解
                    ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
                else if (clazz.isAnnotationPresent(MyService.class)) {// 业务层的注解
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String beanName = myService.value();
                    // 判断注解是否有值，没有就将类名按照驼峰命名
                    if ("".equals(beanName.trim()))
                        beanName = toLowerFirstWord(clazz.getSimpleName());// 得到类名

                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    Class[] interfaces = clazz.getInterfaces();// 该类所实现的所有接口
                    for (Class<?> i : interfaces)
                        // 该类所实现的所有接口，对象都默认是该类本身，等价于：TestService t = new TestServiceImpl();
                        ioc.put(i.getName(), instance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 只有加上了@MyAutowired注解的字段才会自动化的依赖注入
     */
    private void doAutowired() {
        if (ioc.isEmpty())
            return;

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //包括私有的方法，在spring中没有隐私，@MyAutowired可以注入public、private字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                // 判断该字段有没@MyAutowired注解，有的话就进行自动装配
                if (!field.isAnnotationPresent(MyAutowired.class))
                    continue;
                // 判断注解中有没有值，有的话就以此作为变量名，否则按照驼峰命名
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName))
                    beanName = field.getType().getName();

                field.setAccessible(true);
                try {
                    // @MyAutowired
                    // private TestService testService;
                    // 给以上变量赋值，在Test1Controller中
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化HandlerMapping(将url和method对应上)，就是@MyRequestMapping注解里写的url
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty())
            return;

        try {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<? extends Object> clazz = entry.getValue().getClass();
                // 只有Controller层中的类才需要以下操作
                if (!clazz.isAnnotationPresent(MyController.class))
                    continue;

                //拼url时,是controller头的url拼上方法上的url
                String baseUrl = "";
                // 获得类上面的@MyRequestMapping注解中的url值
                if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                    MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl = annotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    // 只有加了@MyRequestMapping注解的方法才需要以下操作
                    if (!method.isAnnotationPresent(MyRequestMapping.class))
                        continue;
                    // 获得方法上面的@MyRequestMapping注解中的url值
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    String url = annotation.value();
                    // 将类url和类中方法url拼接
                    url = (baseUrl + "/" + url).replaceAll("/+", "/");
                    handlerMapping.put(url, method);// 将拼接后的url和方法相对应
                    controllerMap.put(url, entry.getValue());// 将拼接后的url和对象相对应
                    log.info(url + "," + method);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
