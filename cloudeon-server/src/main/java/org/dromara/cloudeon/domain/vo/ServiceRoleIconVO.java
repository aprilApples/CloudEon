package org.dromara.cloudeon.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ Author: Wang Cen
 * @ Date: 2025-03-28 14:43
 * @ Version: 1.0
 * @ Description:
 */
@Data
public class ServiceRoleIconVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String icon;

    private List<String> roleName;
}
