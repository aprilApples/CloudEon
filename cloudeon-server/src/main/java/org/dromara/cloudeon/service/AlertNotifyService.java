package org.dromara.cloudeon.service;

import org.dromara.cloudeon.domain.req.AlertNotifyAddReq;
import org.dromara.cloudeon.domain.req.AlertNotifyPageReq;
import org.dromara.cloudeon.domain.req.AlertNotifyUpdateReq;
import org.dromara.cloudeon.domain.vo.AlertNotifyPageInfoVO;
import org.dromara.cloudeon.domain.vo.JsonPage;
import org.dromara.cloudeon.dto.ResultDTO;

/**
 * @ Author: Wang Cen
 * @ Date: 2025-03-13 18:19
 * @ Version: 1.0
 * @ Description: 告警通知管理Service
 */
public interface AlertNotifyService {

    JsonPage<AlertNotifyPageInfoVO> page(AlertNotifyPageReq req);

    ResultDTO<Void> add(AlertNotifyAddReq req);

    ResultDTO<Void> update(AlertNotifyUpdateReq req);

    ResultDTO<Void> delete(Integer id);

    ResultDTO<Void> release(Integer id, Integer enableStatus);
}
