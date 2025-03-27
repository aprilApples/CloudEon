package org.dromara.cloudeon.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
public class HistoryAlertPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "集群ID不能为空")
    private Integer clusterId;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /**
     * 服务ID
     */
    private Integer historyServiceId;

    /**
     * 角色ID
     */
    private Integer historyRoleId;

    @NotNull(message = "pageNo不能为空")
    private Integer pageNum;

    @NotNull(message = "pageSize不能为空")
    private Integer pageSize;
}
