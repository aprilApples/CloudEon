package org.dromara.cloudeon.domain.req;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ActiveAlertPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "集群ID不能为空")
    private Integer clusterId;

    /**
     * 服务ID
     */
    private Integer activeServiceId;

    /**
     * 角色ID
     */
    private Integer activeRoleId;

    @NotNull(message = "pageNo不能为空")
    private Integer pageNum;

    @NotNull(message = "pageSize不能为空")
    private Integer pageSize;
}
