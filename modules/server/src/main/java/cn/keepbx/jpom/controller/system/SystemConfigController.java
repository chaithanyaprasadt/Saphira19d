package cn.keepbx.jpom.controller.system;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.system.ExtConfigBean;
import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;

/**
 * 系统配置
 *
 * @author bwcx_jzy
 * @date 2019/08/08
 */
@Controller
@RequestMapping(value = "system")
public class SystemConfigController extends BaseServerController {

    @RequestMapping(value = "config.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String config(String nodeId) throws IOException {
        String content;
        if (StrUtil.isNotEmpty(nodeId)) {
            JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.SystemGetConfig, getRequest(), JSONObject.class);
            content = jsonObject.getString("content");
        } else {
            content = IoUtil.read(ExtConfigBean.getResource().getInputStream(), CharsetUtil.CHARSET_UTF_8);
        }
        setAttribute("content", content);
        return "system/config";
    }

    @RequestMapping(value = "save_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.EditSysConfig)
    public String saveConfig(String nodeId, String content, String restart) {
        if (StrUtil.isNotEmpty(nodeId)) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.SystemSaveConfig).toString();
        }
        if (StrUtil.isEmpty(content)) {
            return JsonMessage.getString(405, "内容不能为空");
        }
        try {
            YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
            ByteArrayResource resource = new ByteArrayResource(content.getBytes());
            yamlPropertySourceLoader.load("test", resource);
        } catch (Exception e) {
            return JsonMessage.getString(500, "内容格式错误，请检查修正:" + e.getMessage());
        }
        if (JpomManifest.getInstance().isDebug()) {
            return JsonMessage.getString(405, "调试模式不支持在线修改,请到resource目录下");
        }
        File resourceFile = ExtConfigBean.getResourceFile();
        FileUtil.writeString(content, resourceFile, CharsetUtil.CHARSET_UTF_8);

        if (Convert.toBool(restart, false)) {
            // 重启
            ThreadUtil.execute(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                JpomApplication.restart();
            });
        }
        return JsonMessage.getString(200, "修改成功");
    }
}