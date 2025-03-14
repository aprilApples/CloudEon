package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.AlertNotifyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Map;

/**
 * @ Author: Wang Cen
 * @ Date: 2025-03-13 18:21
 * @ Version: 1.0
 * @ Description:
 */
public interface AlertNotifyRepository extends JpaRepository<AlertNotifyEntity, Integer>, JpaSpecificationExecutor<AlertNotifyEntity> {

    @Query(value = "select notify.*, rule.stack_service_name from gh_alert_notify_info notify left join gh_alert_notify_rule_relation relation on notify.id = relation.alert_notify_id left join ce_alert_rule_define rule on rule.id = relation.alert_rule_id" +
            "where notify.cluster_id = ?1 and if(?2 is not null,notify.enable_status=?2 ,1=1) and if(?3 is not null,rule.id=?3 ,1=1)  and if(?4 !='',notify.recipients like ?4 ,1=1)",
            countQuery = "select count(*) from gh_alert_notify_info notify left join gh_alert_notify_rule_relation relation on notify.id = relation.alert_notify_id left join ce_alert_rule_define rule on rule.id = relation.alert_rule_id" +
                    "where notify.cluster_id = ?1 and if(?2 is not null,notify.enable_status=?2 ,1=1) and if(?3 is not null,rule.id=?3 ,1=1)  and if(?4 !='',notify.recipients like ?4 ,1=1)",
            nativeQuery = true)
    Page<Map<String, Object>> page(Integer clusterId, Integer enableStatus, Integer ruleId, String recipients, Pageable pageable);
}
