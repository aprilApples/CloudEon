package org.dromara.cloudeon.domain.req;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AlertNotifyPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "集群ID不能为空")
    private Integer clusterId;

    /**
     * 启用状态 true启用 false 不启用
     */
    private Boolean enableStatus;

    /**
     * 告警通知名称
     */
    private String alertNotifyName;

    /**
     * 告警规则ID
     */
    private Integer ruleId;

    /**
     * 通知人
     */
    private String recipients;

    @NotNull(message = "pageNo不能为空")
    private Integer pageNum;

    @NotNull(message = "pageSize不能为空")
    private Integer pageSize;
}
