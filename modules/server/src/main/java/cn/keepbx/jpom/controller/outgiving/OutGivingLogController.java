package cn.keepbx.jpom.controller.outgiving;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.OutGivingModel;
import cn.keepbx.jpom.model.data.OutGivingNodeProject;
import cn.keepbx.jpom.model.log.OutGivingLog;
import cn.keepbx.jpom.service.dblog.DbOutGivingLogService;
import cn.keepbx.jpom.service.node.OutGivingServer;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * 分发日志
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Controller
@RequestMapping(value = "/outgiving")
public class OutGivingLogController extends BaseServerController {
    @Resource
    private OutGivingServer outGivingServer;
    @Resource
    private DbOutGivingLogService dbOutGivingLogService;

    @RequestMapping(value = "log.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() throws IOException {
        // 所有节点
        List<NodeModel> nodeModels = nodeService.list();
        setAttribute("nodeArray", nodeModels);
        //
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        setAttribute("outGivingModels", outGivingModels);
        //
        JSONArray status = BaseEnum.toJSONArray(OutGivingNodeProject.Status.class);
        setAttribute("status", status);
        return "outgiving/loglist";
    }

    @RequestMapping(value = "log_list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String listData(String time,
                           String nodeId,
                           String outGivingId,
                           String status,
                           @ValidatorConfig(value = {
                                   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")
                           }, defaultVal = "10") int limit,
                           @ValidatorConfig(value = {
                                   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")
                           }, defaultVal = "1") int page) {

        Page pageObj = new Page(page, limit);
        Entity entity = Entity.create();
        pageObj.addOrder(new Order("startTime", Direction.DESC));
        // 时间
        if (StrUtil.isNotEmpty(time)) {
            String[] val = StrUtil.split(time, "~");
            if (val.length == 2) {
                DateTime startDateTime = DateUtil.parse(val[0], DatePattern.NORM_DATETIME_FORMAT);
                entity.set("startTime", ">= " + startDateTime.getTime());

                DateTime endDateTime = DateUtil.parse(val[1], DatePattern.NORM_DATETIME_FORMAT);
                if (startDateTime.equals(endDateTime)) {
                    endDateTime = DateUtil.endOfDay(endDateTime);
                }
                entity.set("startTime ", "<= " + endDateTime.getTime());
            }
        }
        if (StrUtil.isNotEmpty(nodeId)) {
            entity.set("nodeId", nodeId);
        }

        if (StrUtil.isNotEmpty(outGivingId)) {
            entity.set("outGivingId", outGivingId);
        }

        if (StrUtil.isNotEmpty(status)) {
            entity.set("outGivingId", status);
        }

        PageResult<OutGivingLog> pageResult = dbOutGivingLogService.listPage(entity, pageObj);
        JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", pageResult);
        jsonObject.put("total", pageResult.getTotal());
        return jsonObject.toString();
    }
}
