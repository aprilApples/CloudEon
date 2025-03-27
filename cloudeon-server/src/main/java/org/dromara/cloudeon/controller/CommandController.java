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
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import org.dromara.cloudeon.controller.response.CommandDetailVO;
import org.dromara.cloudeon.controller.response.CommandVO;
import org.dromara.cloudeon.dao.CommandRepository;
import org.dromara.cloudeon.dao.CommandTaskRepository;
import org.dromara.cloudeon.domain.req.CommandPageReq;
import org.dromara.cloudeon.domain.vo.JsonPage;
import org.dromara.cloudeon.dto.ResultDTO;
import org.dromara.cloudeon.dto.ServiceProgress;
import org.dromara.cloudeon.entity.CommandEntity;
import org.dromara.cloudeon.entity.CommandTaskEntity;
import org.dromara.cloudeon.enums.CommandState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.persistence.criteria.Order;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/command")
public class CommandController {

    @Resource
    private CommandRepository commandRepository;

    @Resource
    private CommandTaskRepository commandTaskRepository;


    @Deprecated
    @GetMapping("/list")
    public ResultDTO<List<CommandVO>> listCommand(Integer clusterId) {
        List<CommandEntity> clusterSubmitCommandInfos = commandRepository.findByClusterIdOrderBySubmitTimeDesc(clusterId);
        if (CollUtil.isEmpty(clusterSubmitCommandInfos)) {
            return ResultDTO.success(Collections.emptyList());
        }
        Map<Integer, Set<String>> commandIdServiceMap = getCommandServiceMap(clusterSubmitCommandInfos.stream());

        List<CommandVO> result = clusterSubmitCommandInfos.stream().map(new Function<CommandEntity, CommandVO>() {
            @Override
            public CommandVO apply(CommandEntity commandEntity) {
                CommandVO commandVO = new CommandVO();
                BeanUtil.copyProperties(commandEntity, commandVO);
                // 查出关联的commandTask找到对应的services
                Set<String> serviceNames = commandIdServiceMap.getOrDefault(
                        commandEntity.getId(), Collections.emptySet());
                commandVO.setServiceNames(new ArrayList<>(serviceNames));
                return commandVO;
            }
        }).collect(Collectors.toList());

        return ResultDTO.success(result);
    }

    /**
     * 指令分页查询
     */
    @PostMapping("/pageCommandInfos")
    public ResultDTO<JsonPage<CommandVO>> pageCommandInfos(@RequestBody CommandPageReq req) {
        Page<CommandEntity> pageResult = getCommandPageEntity(req);
        if (CollUtil.isEmpty(pageResult.getContent())) {
            return ResultDTO.success(new JsonPage(Long.valueOf(req.getPageNum()), Long.valueOf(req.getPageSize()), pageResult.getTotalElements(), Collections.emptyList()));
        }
        // 获取关联服务类型
        Map<Integer, Set<String>> commandIdServiceMap = getCommandServiceMap(pageResult.get());
        // 构建分页对象
        JsonPage jsonPage = new JsonPage(Long.valueOf(req.getPageNum()), Long.valueOf(req.getPageSize()), pageResult.getTotalElements(), buildCommandPageInfos(pageResult, commandIdServiceMap));
        return ResultDTO.success(jsonPage);
    }

    private List<CommandVO> buildCommandPageInfos(Page<CommandEntity> pageResult, Map<Integer, Set<String>> commandIdServiceMap) {
        List<CommandVO> results = pageResult.get().map(commandEntity -> {
            CommandVO commandVO = new CommandVO();
            BeanUtil.copyProperties(commandEntity, commandVO);
            // 查出关联的commandTask找到对应的services
            Set<String> serviceNames = commandIdServiceMap.getOrDefault(
                    commandEntity.getId(), Collections.emptySet());
            commandVO.setServiceNames(new ArrayList<>(serviceNames));
            return commandVO;
        }).collect(Collectors.toList());
        return results;
    }

    private Map<Integer, Set<String>> getCommandServiceMap(Stream<CommandEntity> pageResult) {
        List<Integer> commandIds = pageResult.map(CommandEntity::getId).collect(Collectors.toList());
        Map<Integer, Set<String>> commandIdServiceMap = commandTaskRepository.findByCommandIdIn(commandIds)
                .stream()
                .collect(Collectors.groupingBy(
                        CommandTaskEntity::getCommandId,
                        Collectors.mapping(
                                CommandTaskEntity::getServiceInstanceName,
                                Collectors.toCollection(LinkedHashSet::new)
                        )
                ));
        return commandIdServiceMap;
    }

    private Page<CommandEntity> getCommandPageEntity(CommandPageReq req) {
        Pageable pageable = PageRequest.of(req.getPageNum() - 1, req.getPageSize());

        return commandRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("clusterId"), req.getClusterId()));
            Order order = criteriaBuilder.desc(root.get("submitTime"));
            if (ObjectUtil.isNotEmpty(req.getCommandState())) {
                predicates.add(criteriaBuilder.equal(root.get("commandState"), req.getCommandState()));
            }
            if (ObjectUtil.isNotEmpty(req.getStartTime())) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("submitTime"), req.getStartTime()));
            }
            if (ObjectUtil.isNotEmpty(req.getEndTime())) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("submitTime"), req.getEndTime()));
            }
            javax.persistence.criteria.Predicate predicate = criteriaBuilder.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
            return criteriaQuery.where(predicate).orderBy(order).getRestriction();
        }, pageable);
    }

    @GetMapping("/countActive")
    public ResultDTO<Long> countActive(Integer clusterId) {
        long result = commandRepository.countByCommandStateAndClusterId(CommandState.RUNNING, clusterId);
        return ResultDTO.success(result);
    }

    @GetMapping("/detail")
    public ResultDTO<CommandDetailVO> commandDetail(Integer commandId) {
        CommandDetailVO result = new CommandDetailVO();

        // 查出command
        CommandEntity commandEntity = commandRepository.findById(commandId).get();
        BeanUtil.copyProperties(commandEntity, result);
        // 查出关联的commandTask
        List<CommandTaskEntity> taskEntities = commandTaskRepository.findByCommandId(commandId);
        Map<String, List<CommandTaskEntity>> tasksMap = taskEntities.stream().sorted(new Comparator<CommandTaskEntity>() {
            @Override
            public int compare(CommandTaskEntity o1, CommandTaskEntity o2) {
                return o1.getTaskShowSortNum() - o2.getTaskShowSortNum();
            }
        }).collect(Collectors.groupingBy(CommandTaskEntity::getServiceInstanceName));
        // 计算各个服务的当前状态
        List<ServiceProgress> serviceProgresses = tasksMap.entrySet().stream().map(new Function<Map.Entry<String, List<CommandTaskEntity>>, ServiceProgress>() {
            @Override
            public ServiceProgress apply(Map.Entry<String, List<CommandTaskEntity>> serviceTaskMap) {
                List<CommandTaskEntity> commandTaskEntities = serviceTaskMap.getValue();
                long successCnt = commandTaskEntities.stream().filter(new Predicate<CommandTaskEntity>() {
                    @Override
                    public boolean test(CommandTaskEntity commandTaskEntity) {
                        return commandTaskEntity.getCommandState() == CommandState.SUCCESS;
                    }
                }).count();
                long totalCnt = commandTaskEntities.stream().count();
                String currentState = "";
                Map<CommandState, List<CommandTaskEntity>> commandStateListMap = commandTaskEntities.stream().collect(Collectors.groupingBy(CommandTaskEntity::getCommandState));
                if (commandStateListMap.get(CommandState.ERROR) != null && commandStateListMap.get(CommandState.ERROR).size() > 0) {
                    currentState = CommandState.ERROR.name();
                }
                if (commandStateListMap.get(CommandState.RUNNING) != null && commandStateListMap.get(CommandState.RUNNING).size() > 0) {
                    currentState = CommandState.RUNNING.name();
                }
                if (commandStateListMap.get(CommandState.WAITING) != null && commandStateListMap.get(CommandState.WAITING).size() == commandTaskEntities.size()) {
                    currentState = CommandState.WAITING.name();
                }
                if (commandStateListMap.get(CommandState.SUCCESS) != null && commandStateListMap.get(CommandState.SUCCESS).size() == commandTaskEntities.size()) {
                    currentState = CommandState.SUCCESS.name();
                }
                return new ServiceProgress(currentState, serviceTaskMap.getKey(), serviceTaskMap.getValue(), totalCnt, successCnt);
            }
        }).collect(Collectors.toList());

        result.setServiceProgresses(serviceProgresses);

        return ResultDTO.success(result);
    }
}
