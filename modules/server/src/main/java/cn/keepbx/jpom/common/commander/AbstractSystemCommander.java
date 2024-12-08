package cn.keepbx.jpom.common.commander;

import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.common.commander.impl.LinuxSystemCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsSystemCommander;
import cn.keepbx.jpom.model.system.ProcessModel;
import cn.keepbx.jpom.system.JpomRuntimeException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.util.List;

/**
 * 系统监控基类
 *
 * @author jiangzeyin
 * @date 2019/4/15
 */
public abstract class AbstractSystemCommander {
    private static AbstractSystemCommander abstractSystemCommander = null;

    public static AbstractSystemCommander getInstance() {
        if (abstractSystemCommander != null) {
            return abstractSystemCommander;
        }
        if (BaseJpomApplication.OS_INFO.isLinux()) {
            // Linux系统
            abstractSystemCommander = new LinuxSystemCommander();
        } else if (BaseJpomApplication.OS_INFO.isWindows()) {
            // Windows系统
            abstractSystemCommander = new WindowsSystemCommander();
        } else {
            throw new JpomRuntimeException("不支持的：" + BaseJpomApplication.OS_INFO.getName());
        }
        return abstractSystemCommander;
    }

    /**
     * 获取当前服务器的所有进程列表
     *
     * @return array
     */
    public abstract List<ProcessModel> getProcessList();

    /**
     * 获取指定进程的 内存信息
     *
     * @param pid 进程id
     * @return json
     */
    public abstract ProcessModel getPidInfo(int pid);



    protected static JSONObject putObject(String name, Object value, String type) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("value", value);
        jsonObject.put("type", type);
        return jsonObject;
    }
}
