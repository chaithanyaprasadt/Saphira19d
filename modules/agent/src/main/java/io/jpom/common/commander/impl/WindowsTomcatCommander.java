package io.jpom.common.commander.impl;

import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractTomcatCommander;
import io.jpom.model.data.TomcatInfoModel;

/**
 * tomcat的Windows管理命令
 *
 * @author LF
 */
public class WindowsTomcatCommander extends AbstractTomcatCommander {

    /**
     * windows下执行tomcat命令
     *
     * @param tomcatInfoModel tomcat信息
     * @param cmd             执行的命令，包括start stop
     * @return 返回tomcat启动结果
     */
    @Override
    public String execCmd(TomcatInfoModel tomcatInfoModel, String cmd) {
        String tomcatPath = tomcatInfoModel.pathAndCheck();
        //截取盘符
        String dcPath = null;
        if (tomcatPath != null && tomcatPath.indexOf("/") > 1) {
            dcPath = tomcatPath.substring(0,tomcatPath.indexOf("/"));
        }
        String command = null;
        if (StrUtil.isBlank(tomcatPath)) {
            return "tomcat path blank";
        }

        if (cmd.equals("stop")) {
            // 拼接命令
            command = String.format("cmd /c java -Djava.util.logging.config.file=\"%sconf/logging.properties\" " +
                            "-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager  " +
                            "-Djdk.tls.ephemeralDHKeySize=2048" +
                            "-Djava.protocol.handler.pkgs=org.apache.catalina.webresources   " +
                            "-Dignore.endorsed.dirs=\"%s\" " +
                            "-classpath \"%sbin/bootstrap.jar;%sbin/tomcat-juli.jar\" " +
                            "-Dcatalina.base=\"%s\" " +
                            "-Dcatalina.home=\"%s\" " +
                            "-Djava.io.tmpdir=\"%stemp/\" " +
                            "org.apache.catalina.startup.Bootstrap %s", tomcatPath, tomcatPath,
                    tomcatPath, tomcatPath, tomcatPath,
                    tomcatPath, tomcatPath, cmd);
            //
            exec(command, true);
        } else {
            command = String.format("cmd /k %s && cd %s/bin && start startup.bat", dcPath, tomcatPath);
            exec(command, true);
        }

        // 查询操作结果并返回
        return getStatus(tomcatInfoModel, cmd);
    }
}
