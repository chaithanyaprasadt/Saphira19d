/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.controller.user;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.model.data.MailAccountModel;
import io.jpom.model.data.UserModel;
import io.jpom.monitor.EmailUtil;
import io.jpom.service.system.SystemMailConfigService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @date 2019/8/10
 */
@Controller
@RequestMapping(value = "/user")
public class UserBasicInfoController extends BaseServerController {

	private static final TimedCache<String, Integer> CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(30));

	@Resource
	private SystemMailConfigService systemMailConfigService;
	@Resource
	private UserService userService;


	/**
	 * @return
	 * @author Hotstrip
	 * get user basic info
	 * 获取管理员基本信息接口
	 */
	@RequestMapping(value = "user-basic-info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String getUserBasicInfo() {
		UserModel userModel = getUser();
		userModel = userService.getItem(userModel.getId());
		// return basic info
		Map<String, Object> map = new HashMap<>();
		map.put("id", userModel.getId());
		map.put("name", userModel.getName());
		map.put("systemUser", userModel.isSystemUser());
		map.put("email", userModel.getEmail());
		map.put("dingDing", userModel.getDingDing());
		map.put("workWx", userModel.getWorkWx());
		map.put("md5Token", userModel.getUserMd5Key());
		return JsonMessage.getString(200, "success", map);
	}

	@RequestMapping(value = "save_basicInfo.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String saveBasicInfo(@ValidatorItem(value = ValidatorRule.EMAIL, msg = "邮箱格式不正确") String email,
								String dingDing, String workWx, String code) {
		UserModel userModel = getUser();
		userModel = userService.getItem(userModel.getId());
		// 判断是否一样
		if (!StrUtil.equals(email, userModel.getEmail())) {
			Integer cacheCode = CACHE.get(email);
			if (cacheCode == null || !Objects.equals(cacheCode.toString(), code)) {
				return JsonMessage.getString(405, "请输入正确验证码");
			}
		}
		userModel.setEmail(email);
		//
		if (StrUtil.isNotEmpty(dingDing) && !Validator.isUrl(dingDing)) {
			return JsonMessage.getString(405, "请输入正确钉钉地址");
		}
		userModel.setDingDing(dingDing);
		if (StrUtil.isNotEmpty(workWx) && !Validator.isUrl(workWx)) {
			return JsonMessage.getString(405, "请输入正确企业微信地址");
		}
		userModel.setWorkWx(workWx);
		userService.updateItem(userModel);
		setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
		return JsonMessage.getString(200, "修改成功");
	}

	/**
	 * 发送邮箱验证
	 *
	 * @param email 邮箱
	 * @return msg
	 */
	@RequestMapping(value = "sendCode.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String sendCode(@ValidatorItem(value = ValidatorRule.EMAIL, msg = "邮箱格式不正确") String email) {
		MailAccountModel config = systemMailConfigService.getConfig();
		if (config == null) {
			return JsonMessage.getString(405, "管理员还没有配置系统邮箱");
		}
		int randomInt = RandomUtil.randomInt(1000, 9999);
		try {
			EmailUtil.send(email, "Jpom 验证码", "验证码是：" + randomInt);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("发送失败", e);
			return JsonMessage.getString(500, "发送邮件失败：" + e.getMessage());
		}
		CACHE.put(email, randomInt);
		return JsonMessage.getString(200, "发送成功");
	}
}
