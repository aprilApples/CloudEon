package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.AlertNotifyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;

/**
 * @ Author: Wang Cen
 * @ Date: 2025-03-13 18:21
 * @ Version: 1.0
 * @ Description:
 */
public interface AlertNotifyRepository extends JpaRepository<AlertNotifyEntity, Integer>, JpaSpecificationExecutor<AlertNotifyEntity> {
    @Query(value = "SELECT notify.id, notify.cluster_id AS clusterId, notify.alert_notify_name AS alertNotifyName, notify.notify_type AS notifyType, notify.enable_status AS enableStatus, notify.recipients as recipient,notify.desp, notify.create_time AS createTime, notify.update_time AS updateTime FROM gh_alert_notify_info notify WHERE notify.cluster_id = :clusterId AND (:enableStatus IS NULL OR notify.enable_status = :enableStatus) AND (:recipients IS NULL OR :recipients = '' OR notify.recipients LIKE CONCAT('%', :recipients, '%')) AND (:alertNotifyName IS NULL OR :alertNotifyName = '' OR notify.alert_notify_name LIKE CONCAT('%', :alertNotifyName, '%')) AND (:ruleId IS NULL OR EXISTS (SELECT 1 FROM gh_alert_notify_rule_relation relation WHERE relation.alert_notify_id = notify.id AND relation.alert_rule_id = :ruleId ))",
            countQuery = "SELECT count(*) FROM gh_alert_notify_info notify WHERE notify.cluster_id = :clusterId AND (:enableStatus IS NULL OR notify.enable_status = :enableStatus) AND (:recipients IS NULL OR :recipients = '' OR notify.recipients LIKE CONCAT('%', :recipients, '%')) AND (:alertNotifyName IS NULL OR :alertNotifyName = '' OR notify.alert_notify_name LIKE CONCAT('%', :alertNotifyName, '%')) AND (:ruleId IS NULL OR EXISTS (SELECT 1 FROM gh_alert_notify_rule_relation relation WHERE relation.alert_notify_id = notify.id AND relation.alert_rule_id = :ruleId ))",
            nativeQuery = true)
    Page<Map<String, Object>> page(@Param("clusterId") Integer clusterId, @Param("enableStatus") Boolean enableStatus, @Param("ruleId") Integer ruleId, @Param("recipients") String recipients, @Param("alertNotifyName") String alertNotifyName, Pageable pageable);
}
