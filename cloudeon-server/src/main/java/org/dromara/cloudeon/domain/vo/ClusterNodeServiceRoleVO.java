package org.dromara.cloudeon.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ Author: Wang Cen
 * @ Date: 2025-03-27 14:42
 * @ Version: 1.0
 * @ Description:
 */
@Data
public class ClusterNodeServiceRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Integer id;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * IP
     */
    private String ip;

    /**
     * 核数
     */
    private Integer coreNum;

    /**
     * 总内存
     */
    private Integer totalMem;


    /**
     * 总磁盘
     */
    private String totalDisk;

    /**
     * 当前node节点已有所有组件的全部角色
     */
    private List<ServiceRoleVO> existingRoles;

    /**
     * 当前服务在node节点上已经添加的角色
     */
    private List<ServiceRoleVO> currentRoles;

    /**
     * 待添加的角色
     */
    private List<String> addRoles;
}
