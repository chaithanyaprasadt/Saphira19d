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
package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSONArray;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.BaseModel;
import io.jpom.model.Cycle;

import java.util.Objects;

/**
 * 节点实体
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class NodeModel extends BaseModel {

	private String url;
	private String loginName;
	private String loginPwd;
	/**
	 * 节点协议
	 */
	private String protocol = "http";

	private String authorize;
	/**
	 * 项目信息  临时信息
	 */
	private JSONArray projects;
	/**
	 * 开启状态，如果关闭状态就暂停使用节点
	 */
	private boolean openStatus;
	/**
	 * 节点超时时间
	 */
	private int timeOut;
	/**
	 * 绑定的sshId
	 */
	private String sshId;

	/**
	 * 节点分组
	 */
	private String group;

	/**
	 * 监控周期
	 */
	private int cycle = Cycle.none.getCode();

	public int getCycle() {
		return cycle;
	}

	public void setCycle(int cycle) {
		this.cycle = cycle;
	}

	public String getGroup() {
		return StrUtil.emptyToDefault(group, "默认");
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getSshId() {
		return sshId;
	}

	public void setSshId(String sshId) {
		this.sshId = sshId;
	}

	public int getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(int timeOut) {
		this.timeOut = timeOut;
	}

	public boolean isOpenStatus() {
		return openStatus;
	}

	public void setOpenStatus(boolean openStatus) {
		this.openStatus = openStatus;
	}

	public JSONArray getProjects() {
		return projects;
	}
//
//    /**
//     * 返回按照项目分组 排列的数组
//     *
//     * @return array
//     */
//    public JSONArray getGroupProjects() {
//        JSONArray array = getProjects();
//        if (array == null) {
//            return null;
//        }
//        JSONArray newArray = new JSONArray();
//        Map<String, JSONObject> map = new HashMap<>(array.size());
//        array.forEach(o -> {
//            JSONObject pItem = (JSONObject) o;
//            String group = pItem.getString("group");
//            JSONObject jsonObject = map.computeIfAbsent(group, s -> {
//                JSONObject jsonObject1 = new JSONObject();
//                jsonObject1.put("group", s);
//                jsonObject1.put("projects", new JSONArray());
//                return jsonObject1;
//            });
//            JSONArray jsonArray = jsonObject.getJSONArray("projects");
//            jsonArray.add(pItem);
//        });
//        newArray.addAll(map.values());
//        return newArray;
//    }

	public void setProjects(JSONArray projects) {
		this.projects = projects;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol.toLowerCase();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getLoginPwd() {
		return loginPwd;
	}

	public void setLoginPwd(String loginPwd) {
		this.loginPwd = loginPwd;
	}

	/**
	 * 获取 授权的信息
	 *
	 * @return sha1
	 */
	public String toAuthorize() {
		if (authorize == null) {
			authorize = SecureUtil.sha1(loginName + "@" + loginPwd);
		}
		return authorize;
	}

	public String getRealUrl(NodeUrl nodeUrl) {
		return StrUtil.format("{}://{}{}", getProtocol(), getUrl(), nodeUrl.getUrl());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NodeModel nodeModel = (NodeModel) o;
		return openStatus == nodeModel.openStatus &&
				timeOut == nodeModel.timeOut &&
				Objects.equals(url, nodeModel.url) &&
				Objects.equals(loginName, nodeModel.loginName) &&
				Objects.equals(loginPwd, nodeModel.loginPwd) &&
				Objects.equals(protocol, nodeModel.protocol) &&
				Objects.equals(authorize, nodeModel.authorize) &&
				Objects.equals(projects, nodeModel.projects);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, loginName, loginPwd, protocol, authorize, projects, openStatus, timeOut);
	}
}
