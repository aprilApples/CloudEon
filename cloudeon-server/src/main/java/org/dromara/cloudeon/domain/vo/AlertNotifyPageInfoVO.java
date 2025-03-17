package org.dromara.cloudeon.domain.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class AlertNotifyPageInfoVO implements Serializable {

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
    private List<String> alertRules;

    /**
     * 描述
     */
    private String desp;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
