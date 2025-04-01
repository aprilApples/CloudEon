package org.dromara.cloudeon.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.fabric8.kubernetes.api.model.EventList;
import io.fabric8.kubernetes.api.model.Pod;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.dromara.cloudeon.controller.response.RolePodEventVO;
import org.dromara.cloudeon.controller.response.ServiceInstanceRoleVO;
import org.dromara.cloudeon.dao.*;
import org.dromara.cloudeon.domain.req.AddServiceRoleReq;
import org.dromara.cloudeon.domain.vo.ClusterNodeServiceRoleVO;
import org.dromara.cloudeon.domain.vo.ServiceRoleIconVO;
import org.dromara.cloudeon.domain.vo.ServiceRoleVO;
import org.dromara.cloudeon.dto.ResultDTO;
import org.dromara.cloudeon.entity.*;
import org.dromara.cloudeon.enums.CommandType;
import org.dromara.cloudeon.enums.RoleType;
import org.dromara.cloudeon.enums.ServiceRoleState;
import org.dromara.cloudeon.service.CommandHandler;
import org.dromara.cloudeon.service.KubeService;
import org.dromara.cloudeon.utils.K8sUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.dromara.cloudeon.utils.Constant.VERTX_COMMAND_ADDRESS;

/**
 * @Author: Wang Cen
 * @Date: 2025-03-27 14:32
 * @Version: 1.0
 * @Description: 服务角色相关Controller
 */
@RestController
@RequestMapping("/service")
@Slf4j
public class ClusterRoleController {

    private static final String HDFS_STACK_SERVICE_NAME = "HDFS";

    private static final String YARN_STACK_SERVICE_NAME = "YARN";

    private static final String HDFS_ROLE_NAME_NODE = "Name Node";

    private static final String YARN_ROLE_RESOURCEMANAGER = "Resource Manager";


    @Resource(name = "cloudeonVertx")
    private Vertx cloudeonVertx;

    @Resource
    private KubeService kubeService;

    @Resource
    private CommandHandler commandHandler;

    @Resource
    private CommandRepository commandRepository;

    @Resource
    private ClusterInfoRepository clusterInfoRepository;

    @Resource
    private ClusterNodeRepository clusterNodeRepository;

    @Resource
    private StackServiceRepository stackServiceRepository;

    @Resource
    private AlertMessageRepository alertMessageRepository;

    @Resource
    private ServiceInstanceRepository serviceInstanceRepository;

    @Resource
    private ServiceRoleInstanceRepository roleInstanceRepository;

    @Resource
    private StackServiceRoleRepository stackServiceRoleRepository;

    @Resource
    private ServiceInstanceConfigRepository serviceInstanceConfigRepository;

    @Resource
    private ServiceRoleInstanceWebuisRepository roleInstanceWebUisRepository;

    @Resource
    private ClusterAlertRuleRepository clusterAlertRuleRepository;

    /**
     * 获取当前服务左侧可添加的角色列表
     */
    @GetMapping("/listCurrentServiceRoleInfos")
    public ResultDTO<ServiceRoleIconVO> listCurrentServiceRoleInfos(Integer serviceInstanceId) {
        ServiceInstanceEntity serviceInstanceEntity = validateAndLoadServiceInstance(serviceInstanceId);
        Integer stackServiceId = serviceInstanceEntity.getStackServiceId();
        List<StackServiceRoleEntity> stackServiceRoleInfos = stackServiceRoleRepository.findByServiceIdOrderBySortNum(stackServiceId);
        List<String> stackServiceRoleLabels = stackServiceRoleInfos.stream()
                .filter(item -> RoleType.DEPLOYMENT.equals(RoleType.getRoleType(item.getType())))
                .map(StackServiceRoleEntity::getLabel).collect(Collectors.toList());
        ServiceRoleIconVO serviceIconInfo = new ServiceRoleIconVO();
        serviceIconInfo.setRoleName(stackServiceRoleLabels);
        // 查询icon
        StackServiceEntity stackServiceEntity = stackServiceRepository.findById(stackServiceId).get();
        serviceIconInfo.setIcon(stackServiceEntity.getIconApp());
        return ResultDTO.success(serviceIconInfo);
    }


    /**
     * 获取集群节点 服务 角色信息
     */
    @GetMapping("/listClusterNodeServiceRoleInfos")
    public ResultDTO<List<ClusterNodeServiceRoleVO>> listClusterNodeServiceRoleInfos(@RequestParam Integer serviceInstanceId) {
        ServiceInstanceEntity serviceInstance = validateAndLoadServiceInstance(serviceInstanceId);
        Integer clusterId = serviceInstance.getClusterId();

        List<ClusterNodeEntity> clusterNodes = clusterNodeRepository.findByClusterId(clusterId);
        if (clusterNodes.isEmpty()) {
            return ResultDTO.success(Collections.emptyList());
        }

        // 预加载元数据（服务图标、角色标签）
        Map<Integer, String> serviceIconMap = loadServiceIcons();
        Map<Integer, String> roleLabelMap = loadDeploymentRoleLabels();

        // 批量查询服务与角色实例
        List<ServiceInstanceEntity> clusterServices = serviceInstanceRepository.findByClusterId(clusterId);
        Map<Integer, ServiceInstanceEntity> serviceMap = clusterServices.stream().collect(Collectors.toMap(ServiceInstanceEntity::getId, Function.identity()));

        List<ServiceRoleInstanceEntity> clusterRoles = roleInstanceRepository.findByClusterId(clusterId);
        Map<Integer, List<ServiceRoleInstanceEntity>> nodeRoleMap = clusterRoles.stream().collect(Collectors.groupingBy(ServiceRoleInstanceEntity::getNodeId));

        // 构建响应对象
        List<ClusterNodeServiceRoleVO> result = clusterNodes.stream()
                .map(node -> buildClusterNodeInfo(node, nodeRoleMap.get(node.getId()), serviceMap, serviceIconMap, roleLabelMap, serviceInstanceId))
                .collect(Collectors.toList());

        return ResultDTO.success(result);
    }

    // 校验服务实例存在性
    private ServiceInstanceEntity validateAndLoadServiceInstance(Integer serviceInstanceId) {
        return serviceInstanceRepository.findById(serviceInstanceId)
                .orElseThrow(() -> new RuntimeException("ServiceInstance not found: " + serviceInstanceId));
    }

    // 预加载服务图标
    private Map<Integer, String> loadServiceIcons() {
        return stackServiceRepository.findAll().stream()
                .collect(Collectors.toMap(StackServiceEntity::getId, StackServiceEntity::getIconApp));
    }

    // 预加载部署类角色标签
    private Map<Integer, String> loadDeploymentRoleLabels() {
        return stackServiceRoleRepository.findAll().stream()
                .filter(role -> RoleType.DEPLOYMENT.equals(RoleType.getRoleType(role.getType())))
                .collect(Collectors.toMap(StackServiceRoleEntity::getId, StackServiceRoleEntity::getLabel));
    }

    // 构建单个节点VO对象
    private ClusterNodeServiceRoleVO buildClusterNodeInfo(ClusterNodeEntity node,
                                                          List<ServiceRoleInstanceEntity> nodeRoles,
                                                          Map<Integer, ServiceInstanceEntity> serviceMap,
                                                          Map<Integer, String> serviceIconMap,
                                                          Map<Integer, String> roleLabelMap,
                                                          Integer targetServiceId) {

        ClusterNodeServiceRoleVO nodeServiceRoleInfo = new ClusterNodeServiceRoleVO();
        BeanUtils.copyProperties(node, nodeServiceRoleInfo);
        if (CollUtil.isEmpty(nodeRoles)) {
            nodeServiceRoleInfo.setExistingRoles(Collections.emptyList());
            nodeServiceRoleInfo.setCurrentRoles(Collections.emptyList());
            return nodeServiceRoleInfo;
        }

        // 按服务分组角色
        Map<Integer, List<ServiceRoleInstanceEntity>> rolesByService = nodeRoles.stream()
                .collect(Collectors.groupingBy(ServiceRoleInstanceEntity::getServiceInstanceId));

        List<ServiceRoleVO> existingRoles = new ArrayList<>();
        List<ServiceRoleVO> currentRoles = new ArrayList<>();

        rolesByService.forEach((serviceId, roles) -> {
            ServiceInstanceEntity service = serviceMap.get(serviceId);
            if (service == null) {
                return;
            }

            ServiceRoleVO serviceRole = new ServiceRoleVO();
            serviceRole.setServiceName(service.getLabel());
            serviceRole.setIcon(serviceIconMap.get(service.getStackServiceId()));

            List<String> labels = roles.stream()
                    .map(role -> roleLabelMap.get(role.getStackServiceRoleId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            serviceRole.setRoleLabel(labels);

            existingRoles.add(serviceRole);
            if (serviceId.equals(targetServiceId)) {
                currentRoles.add(serviceRole);
            }
        });

        nodeServiceRoleInfo.setExistingRoles(existingRoles);
        nodeServiceRoleInfo.setCurrentRoles(currentRoles);
        nodeServiceRoleInfo.setAddRoles(Collections.emptyList());
        return nodeServiceRoleInfo;
    }

    /**
     * 添加服务角色信息
     */
    @PostMapping("/addRole")
    public ResultDTO<Void> addServiceRoleInstance(@RequestBody AddServiceRoleReq req) {
        Integer serviceInstanceId = req.getServiceInstanceId();

        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(serviceInstanceId)
                .orElseThrow(() -> new RuntimeException("找不到对应的服务信息！"));
        Integer stackServiceId = serviceInstanceEntity.getStackServiceId();

        // 更新数据
        for (AddServiceRoleReq.UpdateServiceRole role : req.getRoles()) {
            String stackRoleName = role.getStackRoleName();
            StackServiceRoleEntity stackServiceRole = stackServiceRoleRepository.findByServiceIdAndLabel(stackServiceId, stackRoleName);
            if (stackServiceRole == null) {
                throw new RuntimeException("找不到服务角色: " + stackRoleName);
            }
            // 检查是否包含hdfs的nameNode
            if (HDFS_STACK_SERVICE_NAME.equals(serviceInstanceEntity.getLabel()) && HDFS_ROLE_NAME_NODE.equals(stackRoleName)) {
                List<StackServiceRoleEntity> stackServiceRoleInfos = stackServiceRoleRepository.findByServiceIdAndStackId(serviceInstanceId, stackServiceId);
                if (stackServiceRoleInfos.size() > 1) {
                    throw new RuntimeException(HDFS_STACK_SERVICE_NAME + "服务只能包含2个" + HDFS_ROLE_NAME_NODE + "角色实例！");
                } else {
                    serviceInstanceEntity.setNeedRestart(Boolean.TRUE);
                }
            }
            // 检查是否包含yarn的resourceManager
            if (YARN_STACK_SERVICE_NAME.equals(serviceInstanceEntity.getLabel()) && YARN_ROLE_RESOURCEMANAGER.equals(stackRoleName)) {
                serviceInstanceEntity.setNeedRestart(Boolean.TRUE);
            }
            updateServiceRoleInstance(role, serviceInstanceEntity, stackServiceRole);
        }
        // 更新状态
        serviceInstanceEntity.setNeedReloadMonitorConfig(Boolean.TRUE);
        serviceInstanceRepository.save(serviceInstanceEntity);
        return ResultDTO.success(null);
    }

    private void updateServiceRoleInstance(AddServiceRoleReq.UpdateServiceRole role, ServiceInstanceEntity serviceInstanceEntity, StackServiceRoleEntity stackServiceRole) {
        List<ServiceRoleInstanceEntity> newInstances = role.getNodeIds().stream()
                .map(nodeId -> createRoleInstance(serviceInstanceEntity, stackServiceRole, nodeId))
                .collect(Collectors.toList());
        List<ServiceRoleInstanceEntity> savedInstances = roleInstanceRepository.saveAllAndFlush(newInstances);

        List<ServiceRoleInstanceWebuisEntity> webUis = savedInstances.stream()
                .filter(instance -> StrUtil.isNotBlank(stackServiceRole.getLinkExpression()))
                .map(instance -> createWebUiEntity(instance, stackServiceRole))
                .collect(Collectors.toList());
        roleInstanceWebUisRepository.saveAll(webUis);
    }

    private ServiceRoleInstanceEntity createRoleInstance(ServiceInstanceEntity serviceInstance, StackServiceRoleEntity role, Integer nodeId) {
        ServiceRoleInstanceEntity entity = new ServiceRoleInstanceEntity();
        entity.setClusterId(serviceInstance.getClusterId());
        entity.setServiceInstanceId(serviceInstance.getId());
        entity.setStackServiceRoleId(role.getId());
        entity.setServiceRoleName(role.getName());
        entity.setServiceRoleState(ServiceRoleState.ROLE_STOPPED);
        entity.setNodeId(nodeId);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        return entity;
    }

    private ServiceRoleInstanceWebuisEntity createWebUiEntity(ServiceRoleInstanceEntity instance, StackServiceRoleEntity role) {
        ServiceRoleInstanceWebuisEntity webUi = new ServiceRoleInstanceWebuisEntity();
        webUi.setName(instance.getServiceRoleName() + "UI地址");
        webUi.setServiceInstanceId(instance.getServiceInstanceId());
        webUi.setServiceRoleInstanceId(instance.getId());
        webUi.setWebHostUrl(genWebUI(role.getLinkExpression(), instance.getServiceInstanceId(), instance, false));
        webUi.setWebIpUrl(genWebUI(role.getLinkExpression(), instance.getServiceInstanceId(), instance, true));
        return webUi;
    }

    private String genWebUI(String roleLinkExpression, Integer serviceInstanceEntityId, ServiceRoleInstanceEntity serviceRoleInstanceEntity, boolean isUseIp) {
        String localHostname;
        // 查询角色的部署节点
        ClusterNodeEntity clusterNodeEntity = clusterNodeRepository.findById(serviceRoleInstanceEntity.getNodeId()).get();
        if (isUseIp) {
            localHostname = clusterNodeEntity.getIp();
        } else {
            localHostname = clusterNodeEntity.getHostname();
        }

        // 查询服务实例所有配置项
        List<ServiceInstanceConfigEntity> allConfigEntityList = serviceInstanceConfigRepository.findByServiceInstanceId(serviceInstanceEntityId);
        Map<String, String> confMap = allConfigEntityList.stream().collect(Collectors.toMap(ServiceInstanceConfigEntity::getName, ServiceInstanceConfigEntity::getValue));
        // 渲染
        Configuration cfg = new Configuration();
        StringTemplateLoader stringLoader = new StringTemplateLoader();
        String template = "webUITemplate";
        stringLoader.putTemplate(template, roleLinkExpression);
        cfg.setTemplateLoader(stringLoader);
        try (Writer out = new StringWriter(2048);) {
            Template temp = cfg.getTemplate(template, "utf-8");
            temp.process(Dict.create().set("localHostname", localHostname).set("conf", confMap), out);
            return out.toString();
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 等待删除角色信息
     */
    @Transactional
    @PostMapping("/deleteRole")
    public ResultDTO<Void> deleteServiceRoleInstance(Integer roleInstanceId) {
        ServiceRoleInstanceEntity roleInstanceInfo = roleInstanceRepository.findById(roleInstanceId).get();
        if (roleInstanceInfo.getServiceRoleState() != ServiceRoleState.ROLE_STOPPED) {
            throw new RuntimeException("角色未停止,无法执行删除!");
        }
        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(roleInstanceInfo.getServiceInstanceId()).get();
        List<ServiceRoleInstanceEntity> allRoleInstanceInfos = roleInstanceRepository.findByServiceInstanceId(serviceInstanceEntity.getId());
        // 检查是否包含hdfs的nameNode
        if (HDFS_STACK_SERVICE_NAME.equals(serviceInstanceEntity.getLabel()) && HDFS_ROLE_NAME_NODE.equals(roleInstanceInfo.getServiceRoleName())) {
            int liveNameNodeSize = (int) allRoleInstanceInfos.stream()
                    .filter(item -> roleInstanceInfo.getServiceRoleName().equals(item.getServiceRoleName()))
                    .filter(item -> !roleInstanceId.equals(item.getId())).count();
            if (0 == liveNameNodeSize) {
                throw new RuntimeException(HDFS_STACK_SERVICE_NAME + "服务至少应有1个" + HDFS_ROLE_NAME_NODE + "角色实例！");
            }
        }
        // 检查是否包含yarn的resourceManager
        if (YARN_STACK_SERVICE_NAME.equals(serviceInstanceEntity.getLabel()) && YARN_ROLE_RESOURCEMANAGER.equals(roleInstanceInfo.getServiceRoleName())) {
            int liveResourceManagerSize = (int) allRoleInstanceInfos.stream()
                    .filter(item -> roleInstanceInfo.getServiceRoleName().equals(item.getServiceRoleName()))
                    .filter(item -> !roleInstanceId.equals(item.getId())).count();
            if (0 == liveResourceManagerSize) {
                throw new RuntimeException(YARN_STACK_SERVICE_NAME + "服务至少应有1个" + YARN_ROLE_RESOURCEMANAGER + "角色实例！");
            }
            serviceInstanceEntity.setNeedRestart(Boolean.TRUE);
        }
        // 删除角色相关
        roleInstanceRepository.deleteById(roleInstanceId);
        roleInstanceWebUisRepository.deleteByServiceRoleInstanceId(roleInstanceId);

        // 更新monitor
        if (serviceInstanceEntity.getNeedReloadMonitorConfig() != null && !serviceInstanceEntity.getNeedReloadMonitorConfig()) {
            serviceInstanceEntity.setNeedReloadMonitorConfig(Boolean.FALSE);
            Integer monitorCommandId = commandHandler.buildServiceCommand(Collections.singletonList(serviceInstanceEntity), serviceInstanceEntity.getClusterId(), CommandType.UPGRADE_MONITOR_CONFIG);
            // 调用workflow
            cloudeonVertx.eventBus().request(VERTX_COMMAND_ADDRESS, monitorCommandId);
        }
        serviceInstanceRepository.save(serviceInstanceEntity);
        return ResultDTO.success(null);
    }


    @PostMapping("/startRole")
    public ResultDTO<Void> startRole(Integer roleInstanceId) {
        ServiceRoleInstanceEntity roleInstanceEntity = roleInstanceRepository.findById(roleInstanceId).get();
        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(roleInstanceEntity.getServiceInstanceId()).get();
        if (roleInstanceEntity.getServiceRoleState() != ServiceRoleState.ROLE_STOPPED) {
            throw new RuntimeException("角色未停止,无法执行启动!");
        }
        // 更新角色实例状态
        roleInstanceEntity.setServiceRoleState(ServiceRoleState.STARTING_ROLE);
        roleInstanceRepository.save(roleInstanceEntity);
        //  生成启动角色command
        List<ServiceInstanceEntity> serviceInstanceEntities = Lists.newArrayList(serviceInstanceEntity);
        Integer commandId = commandHandler.buildRoleCommand(serviceInstanceEntities, Lists.newArrayList(roleInstanceEntity),
                serviceInstanceEntity.getClusterId(), CommandType.START_ROLE);
        //  调用workflow
        cloudeonVertx.eventBus().request(VERTX_COMMAND_ADDRESS, commandId);

        // 更新monitor
        if (serviceInstanceEntity.getNeedReloadMonitorConfig() != null && serviceInstanceEntity.getNeedReloadMonitorConfig()) {
            serviceInstanceEntity.setNeedReloadMonitorConfig(Boolean.FALSE);
            Integer monitorCommandId = commandHandler.buildServiceCommand(Collections.singletonList(serviceInstanceEntity), serviceInstanceEntity.getClusterId(), CommandType.UPGRADE_MONITOR_CONFIG);
            //  调用workflow
            cloudeonVertx.eventBus().request(VERTX_COMMAND_ADDRESS, monitorCommandId);
        }
        return ResultDTO.success(null);
    }

    @PostMapping("/stopRole")
    public ResultDTO<Void> stopRoles(Integer roleInstanceId) {

        ServiceRoleInstanceEntity roleInstanceEntity = roleInstanceRepository.findById(roleInstanceId).get();
        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(roleInstanceEntity.getServiceInstanceId()).get();
        if (roleInstanceEntity.getServiceRoleState() != ServiceRoleState.ROLE_STARTED) {
            throw new RuntimeException("角色未启动,无法执行停止!");
        }
        // 更新角色实例状态
        roleInstanceEntity.setServiceRoleState(ServiceRoleState.STOPPING_ROLE);
        roleInstanceRepository.save(roleInstanceEntity);

        //  生成停止角色command
        List<ServiceInstanceEntity> serviceInstanceEntities = Lists.newArrayList(serviceInstanceEntity);
        Integer commandId = commandHandler.buildRoleCommand(serviceInstanceEntities, Lists.newArrayList(roleInstanceEntity),
                serviceInstanceEntity.getClusterId(), CommandType.STOP_ROLE);
        //  调用workflow
        cloudeonVertx.eventBus().request(VERTX_COMMAND_ADDRESS, commandId);

        return ResultDTO.success(null);
    }

    /**
     * 服务实例角色列表
     */
    @GetMapping("/serviceInstanceRoles")
    public ResultDTO<List<ServiceInstanceRoleVO>> serviceInstanceRoles(Integer serviceInstanceId) {

        List<ServiceInstanceRoleVO> result = roleInstanceRepository.findByServiceInstanceId(serviceInstanceId).stream().map(new Function<ServiceRoleInstanceEntity, ServiceInstanceRoleVO>() {
            @Override
            public ServiceInstanceRoleVO apply(ServiceRoleInstanceEntity roleInstanceEntity) {
                ClusterNodeEntity nodeEntity = clusterNodeRepository.findById(roleInstanceEntity.getNodeId()).get();
                // 查找该角色实例绑定的web地址
                ServiceRoleInstanceWebuisEntity webuisEntity = roleInstanceWebUisRepository.findByServiceRoleInstanceId(roleInstanceEntity.getId());
                StackServiceRoleEntity stackServiceRoleEntity = stackServiceRoleRepository.findById(roleInstanceEntity.getStackServiceRoleId()).get();
                ServiceRoleState serviceRoleState = roleInstanceEntity.getServiceRoleState();
                // 查询角色实例相关告警
                List<String> alertNames = alertMessageRepository.findByServiceRoleInstanceIdAndResolved(roleInstanceEntity.getId(), false)
                        .stream().map(AlertMessageEntity::getAlertName).collect(Collectors.toList());
                ServiceInstanceRoleVO serviceInstanceRoleVO = ServiceInstanceRoleVO.builder()
                        .roleStatus(serviceRoleState.getDesc())
                        .roleStatusValue(serviceRoleState.getValue())
                        .id(roleInstanceEntity.getId())
                        .nodeHostIp(nodeEntity.getIp())
                        .nodeHostname(nodeEntity.getHostname())
                        .alertMsgCnt(alertNames.size())
                        .alertMsgName(alertNames)
                        .nodeId(nodeEntity.getId())
                        // 用stackServiceRoleEntity label更清晰 （如：Doris Be）
                        .name(stackServiceRoleEntity.getLabel())
                        .build();
                if (webuisEntity != null) {
                    serviceInstanceRoleVO.setUiUrls(Lists.newArrayList(webuisEntity.getWebHostUrl(), webuisEntity.getWebIpUrl()));
                }
                return serviceInstanceRoleVO;
            }
        }).collect(Collectors.toList());

        return ResultDTO.success(result);
    }

    @GetMapping("/rolePodEvents")
    public ResultDTO<List<RolePodEventVO>> rolePodEvents(Integer roleId, Integer clusterId) {
        ServiceRoleInstanceEntity roleInstanceEntity = roleInstanceRepository.findById(roleId).get();
        StackServiceRoleEntity stackServiceRoleEntity = stackServiceRoleRepository.findById(roleInstanceEntity.getStackServiceRoleId()).get();
        Integer nodeId = roleInstanceEntity.getNodeId();
        String hostIp = clusterNodeRepository.findById(nodeId).get().getIp();
        String namespace = clusterInfoRepository.findById(clusterId).get().getNamespace();
        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(roleInstanceEntity.getServiceInstanceId()).get();
        return kubeService.executeWithKubeClient(clusterId, client -> {
            String roleServiceFullName = stackServiceRoleEntity.getRoleFullName() + "-" + serviceInstanceEntity.getServiceName().toLowerCase();

            // 带有标签的pod
            List<Pod> podList = client.pods().inNamespace(namespace).withLabel("app", roleServiceFullName).list().getItems();
            // 指定节点的pod
            Pod pod = podList.stream().filter(pod1 -> pod1.getStatus().getHostIP().equals(hostIp)).findFirst().get();

            EventList eventList = client.v1().events()
                    .inNamespace(namespace)
                    .withField("involvedObject.name", pod.getMetadata().getName())
                    .list();

            List<RolePodEventVO> rolePodEventVOS = eventList.getItems().stream().map(event -> RolePodEventVO.builder()
                    .type(event.getType()).message(event.getMessage()).reason(event.getReason())
                    .count(event.getCount()).lastTimestamp(K8sUtil.formatK8sDateStr(event.getLastTimestamp()))
                    .build()).collect(Collectors.toList());
            return ResultDTO.success(rolePodEventVOS);
        });
    }
}
