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
package io.jpom.controller.build;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.enums.GitProtocolEnum;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.plugin.*;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.RepositoryService;
import io.jpom.system.JpomRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repository controller
 *
 * @author Hotstrip
 */
@RestController
@Feature(cls = ClassFeature.BUILD_REPOSITORY)
public class RepositoryController extends BaseServerController {

    private final RepositoryService repositoryService;
    private final BuildInfoService buildInfoService;

    public RepositoryController(RepositoryService repositoryService,
                                BuildInfoService buildInfoService) {
        this.repositoryService = repositoryService;
        this.buildInfoService = buildInfoService;
    }

    /**
     * load repository list
     *
     * <pre>
     *     ???????????????????????????????????????????????????????????????????????????{@link #loadRepositoryListAll()}
     * </pre>
     *
     * @return json
     */
    @PostMapping(value = "/build/repository/list")
    @Feature(method = MethodFeature.LIST)
    public Object loadRepositoryList() {
        PageResultDto<RepositoryModel> pageResult = repositoryService.listPage(getRequest());
        return JsonMessage.getString(200, "????????????", pageResult);
    }

    /**
     * load repository list
     *
     * <pre>
     *     ??????????????????????????????????????????????????????????????????{@link #loadRepositoryList()}
     * </pre>
     *
     * @return json
     */
    @GetMapping(value = "/build/repository/list_all")
    @Feature(method = MethodFeature.LIST)
    public Object loadRepositoryListAll() {
        List<RepositoryModel> repositoryModels = repositoryService.listByWorkspace(getRequest());
        return JsonMessage.getString(200, "", repositoryModels);
    }

    /**
     * edit
     *
     * @param repositoryModelReq ????????????
     * @return json
     */
    @PostMapping(value = "/build/repository/edit")
    @Feature(method = MethodFeature.EDIT)
    public Object editRepository(RepositoryModel repositoryModelReq) {
        this.checkInfo(repositoryModelReq);
        // ?????? rsa ??????
        boolean andUpdateSshKey = this.checkAndUpdateSshKey(repositoryModelReq);
        Assert.state(andUpdateSshKey, "rsa ?????????????????????????????????");

        if (repositoryModelReq.getRepoType() == RepositoryModel.RepoType.Git.getCode()) {
            RepositoryModel repositoryModel = repositoryService.getByKey(repositoryModelReq.getId(), false);
            if (repositoryModel != null) {
                repositoryModelReq.setRsaPrv(StrUtil.emptyToDefault(repositoryModelReq.getRsaPrv(), repositoryModel.getRsaPrv()));
                repositoryModelReq.setPassword(StrUtil.emptyToDefault(repositoryModelReq.getPassword(), repositoryModel.getPassword()));
            }
            // ?????? git ????????????
            try {
                IPlugin plugin = PluginFactory.getPlugin("git-clone");
                Map<String, Object> map = repositoryModelReq.toMap();
                Tuple branchAndTagList = (Tuple) plugin.execute("branchAndTagList", map);
                //Tuple tuple = GitUtil.getBranchAndTagList(repositoryModelReq);
            } catch (JpomRuntimeException jpomRuntimeException) {
                throw jpomRuntimeException;
            } catch (Exception e) {
                DefaultSystemLog.getLog().warn("????????????????????????", e);
                return JsonMessage.toJson(500, "????????????????????????" + e.getMessage());
            }
        }
        if (StrUtil.isEmpty(repositoryModelReq.getId())) {
            // insert data
            repositoryService.insert(repositoryModelReq);
        } else {
            // update data
            //repositoryModelReq.setWorkspaceId(repositoryService.getCheckUserWorkspace(getRequest()));
            repositoryService.updateById(repositoryModelReq, getRequest());
        }

        return JsonMessage.toJson(200, "????????????");
    }

    /**
     * edit
     *
     * @param id ????????????
     * @return json
     */
    @PostMapping(value = "/build/repository/rest_hide_field")
    @Feature(method = MethodFeature.EDIT)
    public Object restHideField(@ValidatorItem String id) {
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setId(id);
        repositoryModel.setPassword(StrUtil.EMPTY);
        repositoryModel.setRsaPrv(StrUtil.EMPTY);
        repositoryModel.setRsaPub(StrUtil.EMPTY);
        repositoryModel.setWorkspaceId(repositoryService.getCheckUserWorkspace(getRequest()));
        repositoryService.updateById(repositoryModel);
        return JsonMessage.toJson(200, "????????????");
    }

    @GetMapping(value = "/build/repository/authorize_repos")
    @Feature(method = MethodFeature.LIST)
    public Object authorizeRepos() {
        // ??????????????????
        HttpServletRequest request = getRequest();
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        Page page = repositoryService.parsePage(paramMap);
        String token = paramMap.get("token");
        Assert.hasText(token, "?????????????????????");
        //
        String type = paramMap.get("type");
        PageResultDto<JSONObject> pageResultDto;
        switch (type) {
            case "gitee":
                pageResultDto = this.giteeRepos(token, page);
                break;
            case "github":
                pageResultDto = this.githubRepos(token, page);
                break;
            case "gitlab":
                pageResultDto = this.gitlabRepos(token, page);
                break;
            default:
                throw new IllegalArgumentException("??????????????????");
        }
        return JsonMessage.toJson(HttpStatus.OK.value(), HttpStatus.OK.name(), pageResultDto);
    }

    /**
     * gitlab ??????
     * <p>
     * https://docs.gitlab.com/ee/api/projects.html#list-all-projects
     *
     * @param token ????????????
     * @param page  ??????
     * @return page
     */
    private PageResultDto<JSONObject> gitlabRepos(String token, Page page) {
        //
        HttpResponse userResponse = HttpUtil.createGet("https://gitlab.com/api/v4/user")
                .form("access_token", token)
                .execute();
        Assert.state(userResponse.isOk(), "??????????????????" + userResponse.body());
        JSONObject userBody = JSONObject.parseObject(userResponse.body());
        String username = userBody.getString("username");
        // ??????????????????
        HttpResponse reposResponse = HttpUtil.createGet("https://gitlab.com/api/v4/projects")
                .form("private_token", token)
                .form("membership", true)
                .form("simple", true)
                .form("order_by", "updated_at")
                .form("page", page.getPageNumber())
                .form("per_page", Math.max(page.getPageSize(), 15))
                .execute();
        String body = reposResponse.body();
        Assert.state(userResponse.isOk(), "???????????????????????????" + body);

        String totalCountStr = reposResponse.header("X-Total");
        int totalCount = Convert.toInt(totalCountStr, 0);
        //String totalPage = reposResponse.header("total_page");
        JSONArray jsonArray = JSONArray.parseArray(body);
        List<JSONObject> objects = jsonArray.stream().map(o -> {
            JSONObject repo = (JSONObject) o;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", repo.getString("name"));
            String htmlUrl = repo.getString("http_url_to_repo");
            jsonObject.put("url", htmlUrl);
            jsonObject.put("full_name", repo.getString("path_with_namespace"));
            jsonObject.put("private", StrUtil.equalsIgnoreCase("private", repo.getString("visibility")));
            jsonObject.put("description", repo.getString("description"));
            //
            jsonObject.put("username", username);
            jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(null, htmlUrl));
            return jsonObject;
        }).collect(Collectors.toList());
        //
        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), totalCount);
        pageResultDto.setResult(objects);
        return pageResultDto;
    }

    /**
     * github ??????
     *
     * @param token ????????????
     * @param page  ??????
     * @return page
     */
    private PageResultDto<JSONObject> githubRepos(String token, Page page) {
        String accept = "application/vnd.github.v3+json";
        HttpRequest request = HttpUtil.createGet("https://api.github.com/user");
        request.header("Authorization", StrUtil.format("token {}", token));
        request.header("Accept", accept);
        HttpResponse httpResponse = request.execute();
        String body = httpResponse.body();
        Assert.state(httpResponse.isOk(), "?????????????????????" + body);
        JSONObject userBody = JSONObject.parseObject(body);
        String username = userBody.getString("login");
        // ??????????????????
        HttpRequest httpRequestRepos = HttpUtil.createGet("https://api.github.com/user/repos");
        httpRequestRepos.header("Authorization", StrUtil.format("token {}", token));
        httpRequestRepos.header("Accept", accept);
        HttpResponse reposResponse = httpRequestRepos
                .form("access_token", token)
                .form("sort", "pushed")
                .form("page", page.getPageNumber())
                .form("per_page", page.getPageSize())
                .execute();
        body = reposResponse.body();
        Assert.state(reposResponse.isOk(), "???????????????????????????" + body);
        JSONArray jsonArray = JSONArray.parseArray(body);
        List<JSONObject> objects = jsonArray.stream().map(o -> {
            JSONObject repo = (JSONObject) o;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", repo.getString("name"));
            String cloneUrl = repo.getString("clone_url");
            jsonObject.put("url", cloneUrl);
            jsonObject.put("full_name", repo.getString("full_name"));
            jsonObject.put("description", repo.getString("description"));
            jsonObject.put("private", repo.getBooleanValue("private"));
            //
            jsonObject.put("username", username);
            jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(null, cloneUrl));
            return jsonObject;
        }).collect(Collectors.toList());
        //
        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), 1000);
        pageResultDto.setResult(objects);
        return pageResultDto;
    }


    /**
     * gitee ??????
     *
     * @param token ????????????
     * @param page  ??????
     * @return page
     */
    private PageResultDto<JSONObject> giteeRepos(String token, Page page) {
        //
        HttpResponse userResponse = HttpUtil.createGet("https://gitee.com/api/v5/user")
                .form("access_token", token)
                .execute();
        Assert.state(userResponse.isOk(), "??????????????????" + userResponse.body());
        JSONObject userBody = JSONObject.parseObject(userResponse.body());
        String username = userBody.getString("login");
        // ??????????????????
        HttpResponse reposResponse = HttpUtil.createGet("https://gitee.com/api/v5/user/repos")
                .form("access_token", token)
                .form("sort", "pushed")
                .form("page", page.getPageNumber())
                .form("per_page", page.getPageSize())
                .execute();
        String body = reposResponse.body();
        Assert.state(userResponse.isOk(), "???????????????????????????" + body);

        String totalCountStr = reposResponse.header("total_count");
        int totalCount = Convert.toInt(totalCountStr, 0);
        //String totalPage = reposResponse.header("total_page");
        JSONArray jsonArray = JSONArray.parseArray(body);
        List<JSONObject> objects = jsonArray.stream().map(o -> {
            JSONObject repo = (JSONObject) o;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", repo.getString("name"));
            String htmlUrl = repo.getString("html_url");
            jsonObject.put("url", htmlUrl);
            jsonObject.put("full_name", repo.getString("full_name"));
            jsonObject.put("private", repo.getBooleanValue("private"));
            jsonObject.put("description", repo.getString("description"));
            //
            jsonObject.put("username", username);
            jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(null, htmlUrl));
            return jsonObject;
        }).collect(Collectors.toList());
        //
        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), totalCount);
        pageResultDto.setResult(objects);
        return pageResultDto;
    }

    /**
     * ????????????
     *
     * @param repositoryModelReq ????????????
     */
    private void checkInfo(RepositoryModel repositoryModelReq) {
        Assert.notNull(repositoryModelReq, "????????????????????????");
        Assert.hasText(repositoryModelReq.getName(), "?????????????????????");
        Integer repoType = repositoryModelReq.getRepoType();
        Assert.state(repoType != null && (repoType == RepositoryModel.RepoType.Git.getCode() || repoType == RepositoryModel.RepoType.Svn.getCode()), "?????????????????????");
        Assert.hasText(repositoryModelReq.getGitUrl(), "?????????????????????");
        //
        Integer protocol = repositoryModelReq.getProtocol();
        Assert.state(protocol != null && (protocol == GitProtocolEnum.HTTP.getCode() || protocol == GitProtocolEnum.SSH.getCode()), "??????????????????????????????");
        // ????????????
        if (protocol == GitProtocolEnum.HTTP.getCode()) {
            //  http
            repositoryModelReq.setRsaPub(StrUtil.EMPTY);
            repositoryModelReq.setRsaPrv(StrUtil.EMPTY);
        } else if (protocol == GitProtocolEnum.SSH.getCode()) {
            // ssh
            repositoryModelReq.setPassword(StrUtil.emptyToDefault(repositoryModelReq.getPassword(), StrUtil.EMPTY));
        }
        //
        boolean repositoryUrl = this.checkRepositoryUrl(repositoryModelReq.getId(), repositoryModelReq.getGitUrl());
        Assert.state(!repositoryUrl, "????????????????????????????????????");
    }

    /**
     * ??????????????????????????????
     *
     * @param id  ??????ID
     * @param url ?????? url
     * @return true ????????????????????????????????????
     */
    private boolean checkRepositoryUrl(String id, String url) {
        // ????????????????????????
        Entity entity = Entity.create();
        if (StrUtil.isNotEmpty(id)) {
            Validator.validateGeneral(id, "?????????ID");
            entity.set("id", "<> " + id);
        }
        String workspaceId = repositoryService.getCheckUserWorkspace(getRequest());
        entity.set("workspaceId", workspaceId);
        entity.set("gitUrl", url);
        return repositoryService.exists(entity);
    }

    /**
     * check and update ssh key
     *
     * @param repositoryModelReq ??????
     */
    private boolean checkAndUpdateSshKey(RepositoryModel repositoryModelReq) {
        if (repositoryModelReq.getProtocol() == GitProtocolEnum.SSH.getCode()) {
            // if rsa key is not empty
            if (StrUtil.isNotEmpty(repositoryModelReq.getRsaPrv())) {
                /**
                 * if rsa key is start with "file:"
                 * copy this file
                 */
                if (StrUtil.startWith(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX)) {
                    String rsaPath = StrUtil.removePrefix(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX);
                    if (!FileUtil.exist(rsaPath)) {
                        DefaultSystemLog.getLog().warn("there is no rsa file... {}", rsaPath);
                        return false;
                    }
                } else {
                    //File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModelReq.getId() + Const.ID_RSA);
                    //  or else put into file
                    //FileUtil.writeUtf8String(repositoryModelReq.getRsaPrv(), rsaFile);
                }
            }
        }
        return true;
    }

    /**
     * delete
     *
     * @param id ??????ID
     * @return json
     */
    @PostMapping(value = "/build/repository/delete")
    @Feature(method = MethodFeature.DEL)
    public Object delRepository(String id) {
        // ???????????????????????????
        Entity entity = Entity.create();
        entity.set("repositoryId", id);
        boolean exists = buildInfoService.exists(entity);
        Assert.state(!exists, "????????????????????????????????????????????????");

        repositoryService.delByKey(id, getRequest());
        File rsaFile = BuildUtil.getRepositoryRsaFile(id + Const.ID_RSA);
        FileUtil.del(rsaFile);
        return JsonMessage.getString(200, "????????????");
    }
}
