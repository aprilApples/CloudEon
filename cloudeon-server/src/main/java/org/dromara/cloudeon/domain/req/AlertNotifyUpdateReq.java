package org.dromara.cloudeon.domain.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AlertNotifyUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 框架服务类型 多个用逗号分隔
     */
    private List<String> services;

    /**
     * 接收者 多个用逗号分隔
     */
    private List<String> recipients;

    /**
     * 告警规则 多个用逗号分隔
     */
    private List<Integer> alertRules;

    /**
     * 描述
     */
    private String desp;
}
