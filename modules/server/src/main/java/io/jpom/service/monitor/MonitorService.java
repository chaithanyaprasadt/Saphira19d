/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
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
package io.jpom.service.monitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.cron.CronUtils;
import io.jpom.cron.ICron;
import io.jpom.model.data.MonitorModel;
import io.jpom.monitor.MonitorItem;
import io.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 监控管理Service
 *
 * @author Arno
 */
@Service
public class MonitorService extends BaseWorkspaceService<MonitorModel> implements ICron {

	@Override
	public void insert(MonitorModel monitorModel) {
		super.insert(monitorModel);
		this.checkCron(monitorModel);
	}

	@Override
	public int delByKey(String keyValue) {
		int byKey = super.delByKey(keyValue);
		if (byKey > 0) {
			String taskId = "monitor:" + keyValue;
			CronUtils.remove(taskId);
		}
		return byKey;
	}

	@Override
	public int startCron() {
		// 关闭监听
		MonitorModel monitorModel = new MonitorModel();
		monitorModel.setStatus(true);
		List<MonitorModel> monitorModels = super.listByBean(monitorModel);
		int size = CollUtil.size(monitorModels);
		if (size > 0) {
			monitorModels.forEach(this::checkCron);
		}
		return size;
	}

	@Override
	public int updateById(MonitorModel info) {
		int updateById = super.updateById(info);
		if (updateById > 0) {
			this.checkCron(info);
		}
		return updateById;
	}

	/**
	 * 检查定时任务 状态
	 *
	 * @param monitorModel 监控信息
	 */
	private void checkCron(MonitorModel monitorModel) {
		String id = monitorModel.getId();
		String taskId = "monitor:" + id;
		String autoExecCron = monitorModel.getExecCron();
		if (StrUtil.isEmpty(autoExecCron)) {
			// 忽略没有关键字段更新
			return;
		}
		if (!monitorModel.status()) {
			CronUtils.remove(taskId);
			return;
		}
		DefaultSystemLog.getLog().debug("start monitor cron {} {} {}", id, monitorModel.getName(), autoExecCron);
		CronUtils.upsert(taskId, autoExecCron, new MonitorService.CronTask(id));
	}

	private static class CronTask implements Task {

		private final String id;

		public CronTask(String id) {
			this.id = id;
		}

		@Override
		public void execute() {
			new MonitorItem(id).run();
		}
	}

//	/**
//	 * 根据周期获取list
//	 *
//	 * @param cycle 周期
//	 * @return list
//	 */
//	public List<MonitorModel> listRunByCycle(Cycle cycle) {
//		MonitorModel monitorModel = new MonitorModel();
//		monitorModel.setCycle(cycle.getCode());
//		monitorModel.setStatus(true);
//		List<MonitorModel> monitorModels = this.listByBean(monitorModel);
//		return ObjectUtil.defaultIfNull(monitorModels, Collections.EMPTY_LIST);
//	}

	/**
	 * 设置报警状态
	 *
	 * @param id    监控id
	 * @param alarm 状态
	 */
	public void setAlarm(String id, boolean alarm) {
		MonitorModel monitorModel = new MonitorModel();
		monitorModel.setId(id);
		monitorModel.setAlarm(alarm);
		super.update(monitorModel);
	}

	/**
	 * 判断是否存在对应节点数据
	 *
	 * @param nodeId 节点id
	 * @return true 存在
	 */
	public boolean checkNode(String nodeId) {
		List<MonitorModel> list = list();
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (MonitorModel monitorModel : list) {
			List<MonitorModel.NodeProject> projects = monitorModel.projects();
			if (projects != null) {
				for (MonitorModel.NodeProject project : projects) {
					if (nodeId.equals(project.getNode())) {
						return true;
					}
				}
			}
		}
		return false;
	}


	public boolean checkProject(String nodeId, String projectId) {
		List<MonitorModel> list = list();
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (MonitorModel monitorModel : list) {
			List<MonitorModel.NodeProject> projects = monitorModel.projects();
			if (projects != null) {
				for (MonitorModel.NodeProject project : projects) {
					if (project.getNode().equals(nodeId)) {
						List<String> projects1 = project.getProjects();
						if (projects1 != null && projects1.contains(projectId)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
