package cn.keepbx.jpom.common.forward;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.*;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.request.XssFilter;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.system.AgentException;
import cn.keepbx.jpom.system.AuthorizeException;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.JpomRuntimeException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 节点请求转发
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class NodeForward {

    /**
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @return JSON
     */
    public static JsonMessage request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl) {
        return request(nodeModel, request, nodeUrl, true, null, null, null, null);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param nodeUrl   节点的url
     * @return JSON
     */
    public static JsonMessage request(NodeModel nodeModel, NodeUrl nodeUrl, UserModel userModel, JSONObject jsonObject) {
        return request(nodeModel, null, nodeUrl, true, userModel, jsonObject, null, null);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param nodeUrl   节点的url
     * @return JSON
     */
    public static JsonMessage requestBySys(NodeModel nodeModel, NodeUrl nodeUrl, String pName, Object pVal, Object... val) {
        return request(nodeModel, null, nodeUrl, false, null, null, pName, pVal, val);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @return JSON
     */
    private static JsonMessage request(NodeModel nodeModel,
                                       HttpServletRequest request,
                                       NodeUrl nodeUrl,
                                       boolean mustUser,
                                       UserModel userModel,
                                       JSONObject jsonData,
                                       String pName,
                                       Object pVal,
                                       Object... val) {
        String url = nodeModel.getRealUrl(nodeUrl);
        HttpRequest httpRequest = HttpUtil.createPost(url);
        //
        if (mustUser) {
            if (userModel == null) {
                userModel = BaseServerController.getUserModel();
            }
        }
        //
        addUser(httpRequest, nodeModel, nodeUrl, userModel);
        Map params = null;
        if (request != null) {
            params = request.getParameterMap();
            if (XssFilter.isXSS() && params != null) {
                for (Map.Entry<String, String[]> entry : (Iterable<Map.Entry<String, String[]>>) params.entrySet()) {
                    String[] values = entry.getValue();
                    if (values != null) {
                        for (int i = 0, len = values.length; i < len; i++) {
                            values[i] = HtmlUtil.unescape(values[i]);
                        }
                        entry.setValue(values);
                    }
                }
            }
        }
        httpRequest.form(pName, pVal, val);
        //
        if (jsonData != null) {
            httpRequest.form(jsonData);
        }
        HttpResponse response;
        try {
            response = httpRequest
                    .form(params)
                    .execute();
        } catch (Exception e) {
            throw new AgentException(nodeModel.getName() + "节点异常：" + e.getMessage(), e);
        }
        //
        return parseBody(response);
    }

    /**
     * 普通消息转发,并解析数据
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param tClass    要解析的类
     * @param <T>       泛型
     * @return T
     */
    public static <T> T requestData(NodeModel nodeModel, NodeUrl nodeUrl, HttpServletRequest request, Class<T> tClass) {
        JsonMessage jsonMessage = request(nodeModel, request, nodeUrl);
        return toObj(jsonMessage, tClass);
    }

    public static <T> T toObj(JsonMessage jsonMessage, Class<T> tClass) {
        Object data = jsonMessage.getData();
        if (null != data) {
            if (tClass == String.class) {
                return (T) data.toString();
            }
            return JSONObject.parseObject(data.toString(), tClass);
        }
        return null;
    }

    /**
     * 普通消息转发,并解析数据
     *
     * @param nodeModel 节点
     * @param nodeUrl   节点的url
     * @param tClass    要解析的类
     * @param <T>       泛型
     * @return T
     */
    public static <T> T requestData(NodeModel nodeModel, NodeUrl nodeUrl, Class<T> tClass, String name, Object value, Object... parameters) {
        String url = nodeModel.getRealUrl(nodeUrl);
        //
        HttpRequest httpRequest = HttpUtil.createPost(url);
        if (name != null && value != null) {
            httpRequest.form(name, value, parameters);
        }
        //
        addUser(httpRequest, nodeModel, nodeUrl);
        HttpResponse response;
        try {
            //
            response = httpRequest
                    .execute();
        } catch (Exception e) {
            throw new AgentException(nodeModel.getName() + "节点异常：" + e.getMessage(), e);
        }
        //
        JsonMessage jsonMessage = parseBody(response);
        return toObj(jsonMessage, tClass);
    }


    /**
     * 上传文件消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @return json
     */
    public static JsonMessage requestMultipart(NodeModel nodeModel, MultipartHttpServletRequest request, NodeUrl nodeUrl) {
        String url = nodeModel.getRealUrl(nodeUrl);
        HttpRequest httpRequest = HttpUtil.createPost(url);
        addUser(httpRequest, nodeModel, nodeUrl);
        //
        Map params = ServletUtil.getParams(request);
        httpRequest.form(params);
        //
        Map<String, MultipartFile> fileMap = request.getFileMap();
        fileMap.forEach((s, multipartFile) -> {
            try {
                httpRequest.form(s, multipartFile.getBytes(), multipartFile.getOriginalFilename());
            } catch (IOException e) {
                DefaultSystemLog.ERROR().error("转发文件异常", e);
            }
        });
        HttpResponse response;
        try {
            response = httpRequest.execute();
        } catch (Exception e) {
            throw new AgentException(nodeModel.getName() + "节点异常：" + e.getMessage(), e);
        }
        return parseBody(response);
    }

    /**
     * 下载文件消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param response  响应
     * @param nodeUrl   节点的url
     */
    public static void requestDownload(NodeModel nodeModel, HttpServletRequest request, HttpServletResponse response, NodeUrl nodeUrl) {
        String url = nodeModel.getRealUrl(nodeUrl);
        HttpRequest httpRequest = HttpUtil.createGet(url);
        addUser(httpRequest, nodeModel, nodeUrl);
        //
        Map params = ServletUtil.getParams(request);
        httpRequest.form(params);
        //
        HttpResponse response1;
        try {
            response1 = httpRequest.execute();
        } catch (Exception e) {
            throw new AgentException(nodeModel.getName() + "节点异常：" + e.getMessage(), e);
        }
        String contentDisposition = response1.header("Content-Disposition");
        response.setHeader("Content-Disposition", contentDisposition);
        String contentType = response1.header("Content-Type");
        response.setContentType(contentType);
        ServletUtil.write(response, response1.bodyStream());
    }

    private static void addUser(HttpRequest httpRequest, NodeModel nodeModel, NodeUrl nodeUrl) {
        UserModel userModel = BaseServerController.getUserModel();
        addUser(httpRequest, nodeModel, nodeUrl, userModel);
    }

    /**
     * 添加agent 授权信息header
     *
     * @param httpRequest request
     * @param nodeModel   节点
     * @param userModel   用户
     */
    private static void addUser(HttpRequest httpRequest, NodeModel nodeModel, NodeUrl nodeUrl, UserModel userModel) {
        // 判断开启状态
        if (!nodeModel.isOpenStatus()) {
            throw new JpomRuntimeException(nodeModel.getName() + "节点未启用");
        }
        if (userModel != null) {
            httpRequest.header(ConfigBean.JPOM_SERVER_USER_NAME, UserModel.getOptUserName(userModel));
            httpRequest.header(ConfigBean.JPOM_SERVER_SYSTEM_USER_ROLE, userModel.getUserRole(nodeModel).name());
        }
        httpRequest.header(ConfigBean.JPOM_AGENT_AUTHORIZE, nodeModel.getAuthorize(true));
        //
        if (nodeUrl.getTimeOut() != -1) {
            //
            httpRequest.timeout(nodeModel.getTimeOut());
        }
    }

    /**
     * 获取节点socket 信息
     *
     * @param nodeModel 节点信息
     * @param nodeUrl   url
     * @return url
     */
    public static String getSocketUrl(NodeModel nodeModel, NodeUrl nodeUrl) {
        String ws;
        if ("https".equalsIgnoreCase(nodeModel.getProtocol())) {
            ws = "wss";
        } else {
            ws = "ws";
        }
        return StrUtil.format("{}://{}{}", ws, nodeModel.getUrl(), nodeUrl.getUrl());
    }

    /**
     * 解析结果
     *
     * @param response 响应
     * @return json
     */
    private static JsonMessage parseBody(HttpResponse response) {
        int status = response.getStatus();
        if (status != HttpStatus.HTTP_OK) {
            throw new AgentException("agent 端响应异常：" + status);
        }
        String body = response.body();
        return toJsonMessage(body);
    }

    private static JsonMessage toJsonMessage(String body) {
        if (StrUtil.isEmpty(body)) {
            throw new AgentException("agent 端响应内容为空");
        }
        JsonMessage jsonMessage = JSON.parseObject(body, JsonMessage.class);
        if (jsonMessage.getCode() == ConfigBean.AUTHORIZE_ERROR) {
            throw new AuthorizeException(jsonMessage, jsonMessage.getMsg());
        }
        return jsonMessage;
    }
}
