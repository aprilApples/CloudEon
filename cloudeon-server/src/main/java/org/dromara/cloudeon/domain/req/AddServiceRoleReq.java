package org.dromara.cloudeon.domain.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ Author: Wang Cen
 * @ Date: 2025-03-24 9:55
 * @ Version: 1.0
 * @ Description:
 */
@Data
public class AddServiceRoleReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer serviceInstanceId;

    private List<UpdateServiceRole> roles;

    @Data
    public static class UpdateServiceRole {
        private String stackRoleName;
        private List<Integer> nodeIds;
    }
}
