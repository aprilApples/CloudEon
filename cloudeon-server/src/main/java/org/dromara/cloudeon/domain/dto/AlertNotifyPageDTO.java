package org.dromara.cloudeon.domain.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class AlertNotifyPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
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
    private String recipient;

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
     * 接收者 多个用逗号分隔
     */
    private List<String> recipients;
}
