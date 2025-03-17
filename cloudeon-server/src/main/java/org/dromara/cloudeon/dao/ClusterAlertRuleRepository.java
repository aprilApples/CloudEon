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
package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.ClusterAlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface ClusterAlertRuleRepository extends JpaRepository<ClusterAlertRuleEntity, Integer> {

    List<ClusterAlertRuleEntity> findByClusterId(Integer clusterId);

    List<ClusterAlertRuleEntity> findByClusterIdAndStackServiceName(Integer clusterId, String stackServiceName);


    @Query(value = "select rule.id,rule.cluster_id as clusterId,rule.rule_name as ruleName,rule.alert_level as alertLevel,rule.promql ,rule.stack_service_name as stackServiceName,rule.stack_role_name as stackRoleName,rule.wait_for_fire_duration as waitForFireDuration ,rule.alert_info as alertInfo,rule.alert_advice as alertAdvice, notify.alert_notify_name as alertNotifyName, notify.notify_type as notifyType,notify.recipients,notify.enable_status as enableStatus,notify.desp from ce_alert_rule_define rule left join gh_alert_notify_rule_relation relation on rule.id = relation.alert_rule_id left join gh_alert_notify_info notify on notify.id = relation.alert_notify_id" +
            " where rule.cluster_id = :clusterId and rule.stack_service_name = :stackServiceName", nativeQuery = true)
    List<Map<String, String>> findClusterAlertRuleAndNotifyInfo(@Param("clusterId") Integer clusterId, @Param("stackServiceName") String stackServiceName);
}