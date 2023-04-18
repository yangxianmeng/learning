package com.yxm.demo.mvc.handler;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Yxm
 **/
@Controller
public class HelloController {


/*    @RequestMapping("/")
    public String index() {
        //返回视图名称
        return "index";
    }*/
    @RequestMapping("/target")
    public String target() {
        //返回视图名称
        return "success";
    }

    @RequestMapping("/testModelAndView")
    public ModelAndView testModelAndView() {
        ModelAndView modelAndView = new ModelAndView();
        //处理模型数据 级想请求与request共享数据
        modelAndView.addObject("test", "hello");
        // 设置视图名称
        modelAndView.setViewName("success");
        return modelAndView;
    }
    @RequestMapping("/test_view")
    public String testView() {
        //返回视图名称
        return "test_view";
    }
    @RequestMapping("/testForward")
    public String testForward() {
        //返回视图名称
        return "forward:/test_view";
    }

    @RequestMapping("/testRedirect")
    public String testRedirect() {
        //返回视图名称
        return "redirect:/testForward";
    }
}
