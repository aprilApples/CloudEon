package org.dromara.cloudeon.domain.req;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @ Author: Wang Cen
 * @ Date: 2025-03-26 18:54
 * @ Version: 1.0
 * @ Description:
 */
@Data
public class AlertRulePageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "集群ID不能为空")
    private Integer clusterId;

    private String ruleName;

    private String ruleStackRoleName;

    private String ruleStackServiceName;

    @NotNull(message = "pageNo不能为空")
    private Integer pageNum;

    @NotNull(message = "pageSize不能为空")
    private Integer pageSize;
}
