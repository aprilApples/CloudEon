package org.dromara.cloudeon.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import io.vertx.core.Vertx;
import org.dromara.cloudeon.dao.AlertNotifyRepository;
import org.dromara.cloudeon.dao.AlertNotifyRuleRelationRepository;
import org.dromara.cloudeon.dao.ClusterAlertRuleRepository;
import org.dromara.cloudeon.dao.ServiceInstanceRepository;
import org.dromara.cloudeon.domain.req.AlertNotifyAddReq;
import org.dromara.cloudeon.domain.req.AlertNotifyPageReq;
import org.dromara.cloudeon.domain.req.AlertNotifyUpdateReq;
import org.dromara.cloudeon.domain.vo.AlertNotifyPageInfoVO;
import org.dromara.cloudeon.domain.vo.JsonPage;
import org.dromara.cloudeon.dto.ResultDTO;
import org.dromara.cloudeon.entity.AlertNotifyEntity;
import org.dromara.cloudeon.entity.AlertNotifyRuleRelationEntity;
import org.dromara.cloudeon.entity.ClusterAlertRuleEntity;
import org.dromara.cloudeon.entity.ServiceInstanceEntity;
import org.dromara.cloudeon.enums.AlertNotifyStatus;
import org.dromara.cloudeon.enums.CommandType;
import org.dromara.cloudeon.service.AlertNotifyService;
import org.dromara.cloudeon.service.CommandHandler;
import org.dromara.cloudeon.utils.BeanCopyUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.dromara.cloudeon.utils.Constant.VERTX_COMMAND_ADDRESS;

@Service
public class AlertNotifyServiceImpl implements AlertNotifyService {

    @Resource(name = "cloudeonVertx")
    private Vertx cloudeonVertx;

    @Resource
    private CommandHandler commandHandler;

    private final AlertNotifyRepository alertNotifyRepository;

    private ClusterAlertRuleRepository clusterAlertRuleRepository;

    private final ServiceInstanceRepository serviceInstanceRepository;

    private final AlertNotifyRuleRelationRepository alertNotifyRuleRelationRepository;

    public AlertNotifyServiceImpl(AlertNotifyRepository alertNotifyRepository, ClusterAlertRuleRepository clusterAlertRuleRepository, ServiceInstanceRepository serviceInstanceRepository, AlertNotifyRuleRelationRepository alertNotifyRuleRelationRepository) {
        this.alertNotifyRepository = alertNotifyRepository;
        this.clusterAlertRuleRepository = clusterAlertRuleRepository;
        this.serviceInstanceRepository = serviceInstanceRepository;
        this.alertNotifyRuleRelationRepository = alertNotifyRuleRelationRepository;
    }

    @Override
    public JsonPage<AlertNotifyPageInfoVO> page(AlertNotifyPageReq req) {
        Pageable pageable = PageRequest.of(req.getPageNo() - 1, req.getPageSize());
        String recipients = req.getRecipients();
        if (StrUtil.isNotBlank(recipients)) {
            recipients = "%" + req.getRecipients() + "%";
        }
        Page<Map<String, Object>> pageResult = alertNotifyRepository.page(req.getClusterId(), req.getEnableStatus(), req.getRuleId(), recipients, pageable);

        return null;
    }

    @Override
    public ResultDTO<Void> add(AlertNotifyAddReq req) {
        List<Integer> alertNotifyIds = alertNotifyRuleRelationRepository.checkAlertRuleIdExist(null, req.getAlertRules());
        if (CollUtil.isNotEmpty(alertNotifyIds)) {
            String alertNotifyName = alertNotifyRepository.findAllById(alertNotifyIds).stream().map(AlertNotifyEntity::getAlertNotifyName).collect(Collectors.joining(","));
            throw new RuntimeException("所选告警规则与以下告警通知" + alertNotifyName + "配置冲突，请检查后再添加");
        }

        AlertNotifyEntity alertNotifyAddEntity = BeanCopyUtils.deepCopy(req, AlertNotifyEntity.class);
        alertNotifyAddEntity.setRecipients(String.join(",", req.getRecipients())).setCreateTime(new Date());
        AlertNotifyEntity alertNotifyEntity = alertNotifyRepository.save(alertNotifyAddEntity);

        saveAlertNotifyRuleRelationAndPublishK8s(req.getClusterId(), req.getEnableStatus(), req.getAlertRules(), alertNotifyEntity);
        return ResultDTO.success();
    }

    private void saveAlertNotifyRuleRelationAndPublishK8s(Integer clusterId, Integer enableStatus, List<Integer> alertRules, AlertNotifyEntity alertNotifyEntity) {
        List<AlertNotifyRuleRelationEntity> alertNotifyRuleRelationEntityList = buildAlertNotifyRuleRelationEntities(alertNotifyEntity.getId(), alertRules);
        alertNotifyRuleRelationRepository.saveAll(alertNotifyRuleRelationEntityList);
        if (AlertNotifyStatus.DISABLE.getValue().equals(enableStatus)) {
            return;
        }
        // 发布告警规则至k8s
        publishK8sUpdateAlertRules(clusterId, alertRules);
    }

    private void publishK8sUpdateAlertRules(Integer clusterId, List<Integer> alertRules) {
        List<ClusterAlertRuleEntity> clusterAlertRuleEntities = clusterAlertRuleRepository.findAllById(alertRules);
        for (ClusterAlertRuleEntity clusterAlertRuleEntity : clusterAlertRuleEntities) {
            String stackServiceName = clusterAlertRuleEntity.getStackServiceName();
            ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findEntityByClusterIdAndStackServiceName(clusterId, stackServiceName);
            //  生成刷新服务配置command
            List<ServiceInstanceEntity> serviceInstanceEntities = Lists.newArrayList(serviceInstanceEntity);
            Integer commandId = commandHandler.buildServiceCommand(serviceInstanceEntities, serviceInstanceEntity.getClusterId(), CommandType.UPGRADE_SERVICE_CONFIG);
            //  调用workflow
            cloudeonVertx.eventBus().request(VERTX_COMMAND_ADDRESS, commandId);
        }
    }

    private List<AlertNotifyRuleRelationEntity> buildAlertNotifyRuleRelationEntities(Integer alertNotifyId, List<Integer> alertRules) {
        return alertRules.stream().map(relation -> {
            AlertNotifyRuleRelationEntity alertNotifyRuleRelationEntity = new AlertNotifyRuleRelationEntity();
            alertNotifyRuleRelationEntity.setAlertNotifyId(alertNotifyId).setAlertRuleId(relation);
            return alertNotifyRuleRelationEntity;
        }).collect(Collectors.toList());
    }

    @Override
    public ResultDTO<Void> update(AlertNotifyUpdateReq req) {
        List<Integer> alertNotifyIds = alertNotifyRuleRelationRepository.checkAlertRuleIdExist(req.getId(), req.getAlertRules());
        if (CollUtil.isNotEmpty(alertNotifyIds)) {
            String alertNotifyName = alertNotifyRepository.findAllById(alertNotifyIds).stream().map(AlertNotifyEntity::getAlertNotifyName).collect(Collectors.joining(","));
            throw new RuntimeException("所选告警规则与以下告警通知" + alertNotifyName + "配置冲突，请检查后再添加");
        }
        AlertNotifyEntity oriAlertNotifyEntity = alertNotifyRepository.getReferenceById(req.getId());
        if (ObjectUtil.isEmpty(oriAlertNotifyEntity)) {
            throw new RuntimeException("当前告警通知不存在，请检查后再操作！");
        }

        AlertNotifyEntity alertNotifyEntity = BeanCopyUtils.deepCopy(req, AlertNotifyEntity.class);
        alertNotifyEntity.setRecipients(String.join(",", req.getRecipients())).setUpdateTime(new Date());
        alertNotifyRepository.save(alertNotifyEntity);

        alertNotifyRuleRelationRepository.deleteByAlertNotifyIdIn(Collections.singletonList(req.getId()));

        saveAlertNotifyRuleRelationAndPublishK8s(req.getClusterId(), req.getEnableStatus(), req.getAlertRules(), alertNotifyEntity);
        return ResultDTO.success();
    }

    @Override
    public ResultDTO<Void> delete(Integer id) {
        AlertNotifyEntity oriAlertNotifyEntity = alertNotifyRepository.getReferenceById(id);
        if (ObjectUtil.isEmpty(oriAlertNotifyEntity)) {
            throw new RuntimeException("当前告警通知不存在，请检查后再操作！");
        }
        alertNotifyRepository.deleteById(id);
        List<Integer> alertRules = alertNotifyRuleRelationRepository.findByAlertNotifyIdIn(Collections.singletonList(id)).stream().map(AlertNotifyRuleRelationEntity::getAlertRuleId).collect(Collectors.toList());
        alertNotifyRuleRelationRepository.deleteByAlertNotifyIdIn(Collections.singletonList(id));
        // 发布告警规则至k8s
        publishK8sUpdateAlertRules(oriAlertNotifyEntity.getClusterId(), alertRules);
        return ResultDTO.success();
    }

    @Override
    public ResultDTO<Void> release(Integer id, Integer enableStatus) {
        AlertNotifyEntity oriAlertNotifyEntity = alertNotifyRepository.getReferenceById(id);
        if (ObjectUtil.isEmpty(oriAlertNotifyEntity)) {
            throw new RuntimeException("当前告警通知不存在，请检查后再操作！");
        }
        oriAlertNotifyEntity.setEnableStatus(enableStatus).setUpdateTime(new Date());
        alertNotifyRepository.save(oriAlertNotifyEntity);
        List<Integer> alertRules = alertNotifyRuleRelationRepository.findByAlertNotifyIdIn(Collections.singletonList(id)).stream().map(AlertNotifyRuleRelationEntity::getAlertRuleId).collect(Collectors.toList());
        // 发布告警规则至k8s
        publishK8sUpdateAlertRules(oriAlertNotifyEntity.getClusterId(), alertRules);
        return ResultDTO.success();
    }
}
