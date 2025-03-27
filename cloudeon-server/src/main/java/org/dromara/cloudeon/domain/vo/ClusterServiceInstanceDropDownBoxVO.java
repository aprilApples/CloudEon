package org.dromara.cloudeon.domain.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClusterServiceInstanceDropDownBoxVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    /**
     * 服务名称
     */
    private String serviceName;

    private String label;
}
