/**
 * 节点管理 api
 */
import axios from './config';

/**
 * jdk 列表
 * @param {String} nodeId 节点 ID
 */
export function getJdkList(nodeId) {
  return axios({
    url: '/node/manage/jdk/list',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * jdk 编辑
 * @param {nodeId, id, name, path} params
 * params.nodeId 节点 ID 
 * params.id 编辑修改时判断 ID
 * params.name 名称
 * params.path jdk 路径
 */
export function editJdk(params) {
  return axios({
    url: '/node/manage/jdk/update',
    method: 'post',
    data: params
  })
}

/**
 * 删除 JDK
 * @param {nodeId, id} params 
 * params.nodeId 节点 ID 
 * params.id 编辑修改时判断 ID
 */
export function deleteJdk(params) {
  return axios({
    url: '/node/manage/jdk/delete',
    method: 'post',
    data: params
  })
}

/**
 * 项目列表
 * @param {
 *  nodeId: 节点 ID
 *  group: 分组名称
 * } params 
 */
export function getProjectList(params) {
  return axios({
    url: '/node/manage/getProjectInfo',
    method: 'post',
    data: params
  })
}

/**
 * 项目运行信息，返回项目占用端口和 pid
 * @param {
 *  nodeId: 节点 ID
 *  ids: 项目 ID 数组字符串格式 ["id1", "id2"]
 * } params 
 */
export function getRuningProjectInfo(params) {
  return axios({
    url: '/node/manage/getProjectPort',
    method: 'post',
    data: params
  })
}

/**
 * 获取单个项目信息
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID 
 * } params 
 */
export function getProjectById(params) {
  return axios({
    url: '/node/manage/getProjectById',
    method: 'post',
    data: params
  })
}

/**
 * 加载项目分组列表
 * @param {String} nodeId 节点 ID
 */
export function getPorjectGroupList(nodeId) {
  return axios({
    url: '/node/manage/project-group-list',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * 项目白名单列表
 * @param {String} nodeId 节点 ID
 */
export function getProjectAccessList(nodeId) {
  return axios({
    url: '/node/manage/project-access-list',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * 编辑项目
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID 
 *  name: 项目名称
 *  runMode: 运行方式
 *  whitelistDirectory: 项目白名单路径
 *  lib: 项目文件夹
 *  group: 分组名称
 *  jdkId: JDK
 *  ...
 * } params 
 */
export function editProject(params) {
  return axios({
    url: '/node/manage/saveProject',
    method: 'post',
    data: params
  })
}

/**
 * 删除项目
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID 
 * } params 
 */
export function deleteProject(params) {
  return axios({
    url: '/node/manage/deleteProject',
    method: 'post',
    data: params
  })
}

/**
 * 项目文件列表
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID 
 * } params 
 */
export function getFileList(params) {
  return axios({
    url: '/node/manage/file/getFileList',
    method: 'post',
    data: params
  })
}

/**
 * 下载项目文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 * } params 
 */
export function downloadProjectFile(params) {
  return axios({
    url: '/node/manage/file/download',
    method: 'get',
    responseType: 'blob',
    timeout: 0,
    params
  })
}

/**
 * 删除文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 * } params
 */
export function deleteProjectFile(params) {
  return axios({
    url: '/node/manage/file/deleteFile',
    method: 'post',
    data: params
  })
}
