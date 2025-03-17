package org.dromara.cloudeon.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Accessors(chain = true)
@Table(name = "gh_alert_notify_info")
public class AlertNotifyEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;

    /**
     * 集群ID
     */
    private Integer clusterId;

    /**
     * 告警通知名称
     */
    private String alertNotifyName;

    /**
     * 告警通知类型 1 邮件
     */
    private Integer notifyType;

    /**
     * 启用状态 true启用 false 不启用
     */
    private Boolean enableStatus;

    /**
     * 接收者 多个用逗号分隔
     */
    private String recipients;

    /**
     * 描述
     */
    private String desp;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;
}
