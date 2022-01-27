package cn.jiangzeyin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/")
public class WelcomeController extends BaseController {

    @RequestMapping(value = "welcome", method = RequestMethod.GET)
    public String welcome() {
        setAttribute("userInfo", getSocketPwd());
        return "welcome";
    }
}
