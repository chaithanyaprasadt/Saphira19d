package cn.keepbx.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.system.AgentAutoUser;
import cn.keepbx.jpom.util.JsonFileUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * agent 端授权账号信息
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@Configuration
public class AgentAuthorize {

    private static AgentAuthorize agentAuthorize;
    /**
     * 账号
     */
    @Value("${jpom.authorize.agentName}")
    private String agentName;
    /**
     * 密码
     */
    @Value("${jpom.authorize.agentPwd:}")
    private String agentPwd;
    /**
     * 授权加密字符串
     */
    private String authorize;

    /**
     * 单例
     *
     * @return this
     */
    public static AgentAuthorize getInstance() {
        if (agentAuthorize == null) {
            agentAuthorize = SpringUtil.getBean(AgentAuthorize.class);
            // 登录名不能为空
            if (StrUtil.isEmpty(agentAuthorize.agentName)) {
                throw new JpomRuntimeException("agent 端登录名不能为空");
            }
            agentAuthorize.checkPwd();
            // 生成密码授权字符串
            agentAuthorize.authorize = SecureUtil.sha1(agentAuthorize.agentName + "@" + agentAuthorize.agentPwd);
        }
        return agentAuthorize;
    }


    public boolean checkAuthorize(String authorize) {
        return StrUtil.equals(authorize, this.authorize);
    }

    /**
     * 检查是否配置密码
     */
    private void checkPwd() {
        String path = ConfigBean.getInstance().getAgentAutoAuthorizeFile(ConfigBean.getInstance().getDataPath());
        if (StrUtil.isNotEmpty(agentPwd)) {
            // 有指定密码 清除旧密码信息
            FileUtil.del(path);
            return;
        }
        if (FileUtil.exist(path)) {
            // 读取旧密码
            try {
                String json = FileUtil.readString(path, CharsetUtil.CHARSET_UTF_8);
                AgentAutoUser autoUser = JSONObject.parseObject(json, AgentAutoUser.class);
                String oldAgentPwd = autoUser.getAgentPwd();
                if (!StrUtil.equals(autoUser.getAgentPwd(), this.agentName)) {
                    throw new JpomRuntimeException("已经存在的登录名和配置的登录名不一致");
                }
                if (StrUtil.isNotEmpty(oldAgentPwd)) {
                    this.agentPwd = oldAgentPwd;
                    DefaultSystemLog.LOG().info("已有授权账号:{}  密码:{},授权信息保存位置：{}", this.agentName, this.agentPwd, FileUtil.getAbsolutePath(path));
                    return;
                }

            } catch (Exception ignored) {
            }
        }
        this.agentPwd = RandomUtil.randomString(10);
        AgentAutoUser autoUser = new AgentAutoUser();
        autoUser.setAgentName(this.agentName);
        autoUser.setAgentPwd(this.agentPwd);
        // 写入文件中
        JsonFileUtil.saveJson(path, autoUser.toJson());
        DefaultSystemLog.LOG().info("已经生成授权账号:{}  密码:{},授权信息保存位置：{}", this.agentName, this.agentPwd, FileUtil.getAbsolutePath(path));
    }
}
