package org.dromara.cloudeon.domain.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClusterRoleInstanceDropDownBoxVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    /**
     * 服务角色名称
     */
    private String serviceRoleName;
}
