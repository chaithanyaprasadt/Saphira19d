package cn.keepbx.jpom.socket;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.system.WebAopLog;
import cn.keepbx.util.SocketSessionUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
public class ServerWebSocketTomcatHandler extends BaseServerWebSocketHandler {

    ServerWebSocketTomcatHandler() {
        super(NodeUrl.Tomcat_Socket, "tomcatId");
    }

    @Override
    protected void handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        String tomcatId = (String) attributes.get("tomcatId");
        String fileName = json.getString("fileName");
        if (!JpomApplication.SYSTEM_ID.equals(tomcatId) && consoleCommandOp == ConsoleCommandOp.heart) {
            // 服务端心跳
            return;
        }
        if (consoleCommandOp == ConsoleCommandOp.showlog && JpomApplication.SYSTEM_ID.equals(tomcatId)) {
            WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
            // 进入管理页面后需要实时加载日志
            File file = FileUtil.file(webAopLog.getPropertyValue(), fileName);
            //
            File nowFile = (File) attributes.get("nowFile");
            if (nowFile != null && !nowFile.equals(file)) {
                // 离线上一个日志
                ServiceFileTailWatcher.offlineFile(file, session);
            }
            try {
                ServiceFileTailWatcher.addWatcher(file, session);
                attributes.put("nowFile", file);
            } catch (Exception io) {
                DefaultSystemLog.ERROR().error("监听日志变化", io);
                SocketSessionUtil.send(session, io.getMessage());
            }
        }
    }

    @Override
    protected void handleTextMessage(Map<String, Object> attributes, ProxySession proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) {
        proxySession.send(json.toString());
    }

    @Override
    protected void destroy(WebSocketSession session) {
        super.destroy(session);
        ServiceFileTailWatcher.offline(session);
    }
}
