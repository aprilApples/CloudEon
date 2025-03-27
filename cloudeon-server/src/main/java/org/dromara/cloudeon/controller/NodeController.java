/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.dromara.cloudeon.controller;

import cn.hutool.core.bean.BeanUtil;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import org.dromara.cloudeon.config.CloudeonConfigProp;
import org.dromara.cloudeon.controller.request.SaveNodeRequest;
import org.dromara.cloudeon.controller.response.NodeInfoVO;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.dto.ResultDTO;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.service.KubeService;
import org.dromara.cloudeon.utils.ByteConverter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/node")
public class NodeController {

    @Resource
    private CloudeonConfigProp cloudeonConfigProp;


    @Resource
    private ClusterNodeRepository clusterNodeRepository;

    @Resource
    private KubeService kubeService;

    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public ResultDTO<Void> addNode(@RequestBody SaveNodeRequest req) throws IOException {
        String ip = req.getIp().trim();
        Integer clusterId = req.getClusterId();
        // 检查ip不能重复
        if (clusterNodeRepository.countByIp(ip) > 0) {
            return ResultDTO.failed("已添加ip为：" + ip + " 的节点(服务器)");
        }
        NodeInfoVO k8sNodeInfoVO = kubeService.executeWithKubeClient(clusterId, kubeClient -> {
            NodeList nodeList = kubeClient.nodes().list();
            List<Node> items = nodeList.getItems();
            // 获取该节点在k8s上的信息
            return items.stream().filter(node -> {
                String nodeIp = getNodeIp(node);
                return ip.equals(nodeIp);
            }).map(node -> getNodeInfoVO(node)).findFirst().get();
        });

        String containerRuntimeVersion = k8sNodeInfoVO.getContainerRuntimeVersion();
        // 保存到数据库
        ClusterNodeEntity newClusterNodeEntity = new ClusterNodeEntity();
        BeanUtil.copyProperties(req, newClusterNodeEntity);
        newClusterNodeEntity.setCreateTime(new Date());
        newClusterNodeEntity.setRuntimeContainer(containerRuntimeVersion);
        clusterNodeRepository.save(newClusterNodeEntity);


        return ResultDTO.success(null);
    }

    /**
     * 根据集群id查询绑定的k8s节点信息
     */
    @GetMapping("/list")
    @Transactional
    public ResultDTO<List<NodeInfoVO>> listNode(Integer clusterId) {
        // 获取k8s集群节点信息
        Map<String, Node> nodeMap = kubeService.executeWithKubeClient(clusterId, kubeClient -> {
            List<Node> items = kubeClient.nodes().list().getItems();
            return items.stream().collect(Collectors.toMap(this::getNodeIp, node -> node));
        });
        // 从数据库查出当前集群绑定的节点
        List<ClusterNodeEntity> nodeEntities = clusterNodeRepository.findByClusterId(clusterId);

        // 持久化node的最新信息
        List<ClusterNodeEntity> updateClusterNodeInfos = new ArrayList<>();

        List<NodeInfoVO> k8sNodeInfos = nodeEntities.stream().map(nodeEntity -> {
            // 从map中获得k8s上最新的节点信息
            Node node = nodeMap.get(nodeEntity.getIp());
            NodeInfoVO k8sNodeInfo = getNodeInfoVO(node);
            k8sNodeInfo.setId(nodeEntity.getId());
            k8sNodeInfo.setClusterId(nodeEntity.getClusterId());
            k8sNodeInfo.setCreateTime(nodeEntity.getCreateTime());

            nodeEntity.setCoreNum(k8sNodeInfo.getCoreNum());
            nodeEntity.setTotalMem(k8sNodeInfo.getTotalMem());
            nodeEntity.setTotalDisk(k8sNodeInfo.getTotalDisk());
            nodeEntity.setCpuArchitecture(k8sNodeInfo.getCpuArchitecture());
            updateClusterNodeInfos.add(nodeEntity);
            return k8sNodeInfo;
        }).collect(Collectors.toList());

        clusterNodeRepository.saveAll(updateClusterNodeInfos);

        return ResultDTO.success(k8sNodeInfos);
    }


    /**
     * 查询k8s节点信息详情
     */
    @GetMapping("/listK8sNode")
    public ResultDTO<List<NodeInfoVO>> listK8sNode(Integer clusterId) {
        // 从数据库查出已经和集群绑定的k8s节点
        List<ClusterNodeEntity> clusterNodeInfos = clusterNodeRepository.findByClusterId(clusterId);
        Set<String> clusterIpSets = clusterNodeInfos.stream().map(ClusterNodeEntity::getIp).collect(Collectors.toSet());
        // 通过集群id连接k8s集群
        List<NodeInfoVO> k8sNodeInfos = kubeService.executeWithKubeClient(clusterId, kubeClient -> {
            // 获取k8s集群节点信息
            return kubeClient.nodes().list().getItems().stream().filter(node -> {
                String ip = getNodeIp(node);
                //  过滤出未绑定的节点
                return !clusterIpSets.contains(ip);
            }).map(this::getNodeInfoVO).collect(Collectors.toList());
        });
        return ResultDTO.success(k8sNodeInfos);
    }

    private NodeInfoVO getNodeInfoVO(Node e) {
        if (e == null) {
            return new NodeInfoVO();
        }
        int cpu = e.getStatus().getCapacity().get("cpu").getNumericalAmount().intValue();
        long memory = e.getStatus().getCapacity().get("memory").getNumericalAmount().longValue();
        long storage = e.getStatus().getCapacity().get("ephemeral-storage").getNumericalAmount().longValue();
        String ip = getNodeIp(e);
        String hostname = getNodeHostname(e);
        String architecture = e.getStatus().getNodeInfo().getArchitecture();
        String containerRuntimeVersion = e.getStatus().getNodeInfo().getContainerRuntimeVersion();
        String kubeletVersion = e.getStatus().getNodeInfo().getKubeletVersion();
        String kernelVersion = e.getStatus().getNodeInfo().getKernelVersion();
        String osImage = e.getStatus().getNodeInfo().getOsImage();

        return NodeInfoVO.builder()
                .ip(ip)
                .hostname(hostname)
                .cpuArchitecture(architecture)
                .coreNum(cpu)
                .totalMem(ByteConverter.convertKBToGB(memory) + " GB")
                .totalDisk(ByteConverter.convertKBToGB(storage) + " GB")
                .kernelVersion(kernelVersion)
                .kubeletVersion(kubeletVersion)
                .containerRuntimeVersion(containerRuntimeVersion)
                .osImage(osImage)
                .build();
    }

    private String getNodeIp(Node e) {
        return e.getStatus().getAddresses().stream().filter(n -> {
            return n.getType().equals("InternalIP");
        }).findFirst().get().getAddress();
    }

    private String getNodeHostname(Node e) {
        return e.getStatus().getAddresses().stream().filter(n -> {
            return n.getType().equals("Hostname");
        }).findFirst().get().getAddress();
    }
}
