package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.AlertNotifyRuleRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @ Author: Wang Cen
 * @ Date: 2025-03-13 18:22
 * @ Version: 1.0
 * @ Description:
 */
public interface AlertNotifyRuleRelationRepository extends JpaRepository<AlertNotifyRuleRelationEntity, Integer> {

    @Query(value = "SELECT DISTINCT(alert_notify_id) AS notifyId FROM gh_alert_notify_rule_relation WHERE IF(:alertNotifyId IS NOT NULL, alert_notify_id!=:alertNotifyId, 1=1) AND alert_rule_id IN :alertRuleIds", nativeQuery = true)
    List<Integer> checkAlertRuleIdExist(@Param("alertNotifyId") Integer alertNotifyId, @Param("alertRuleIds") List<Integer> alertRuleIds);

    Integer deleteByAlertNotifyIdIn(List<Integer> alertNotifyIds);

    List<AlertNotifyRuleRelationEntity> findByAlertNotifyIdIn(List<Integer> alertNotifyIds);

    @Query(value = "SELECT alert_rule_id AS notifyId FROM gh_alert_notify_rule_relation relation left join gh_alert_notify_info notify on notify.id = relation.alert_notify_id WHERE notify.cluster_id=?1", nativeQuery = true)
    List<Integer> findAlertRuleIdsByClusterId(Integer clusterId);

}
