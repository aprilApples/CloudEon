package org.dromara.cloudeon.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "gh_alert_notify_rule_relation")
public class AlertNotifyRuleRelationEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;

    /**
     * 告警通知ID
     */
    private Integer alertNotifyId;

    /**
     * 规则ID
     */
    private Integer alertRuleId;
}
