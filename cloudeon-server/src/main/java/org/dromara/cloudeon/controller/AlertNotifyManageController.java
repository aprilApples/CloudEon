package org.dromara.cloudeon.controller;

import com.gh.dc.common.core.core.JsonPage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.cloudeon.domain.req.AlertNotifyAddReq;
import org.dromara.cloudeon.domain.req.AlertNotifyPageReq;
import org.dromara.cloudeon.domain.req.AlertNotifyUpdateReq;
import org.dromara.cloudeon.domain.vo.AlertNotifyPageInfoVO;
import org.dromara.cloudeon.domain.vo.AlertRuleDropDownBoxVO;
import org.dromara.cloudeon.dto.ResultDTO;
import org.dromara.cloudeon.service.AlertNotifyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/alert/notify")
public class AlertNotifyManageController {

    private final AlertNotifyService alertNotifyService;

    public AlertNotifyManageController(AlertNotifyService alertNotifyService) {
        this.alertNotifyService = alertNotifyService;
    }

    @PostMapping("/page")
    public ResultDTO<JsonPage<AlertNotifyPageInfoVO>> page(@RequestBody AlertNotifyPageReq req) {
        return null;
    }

    @PostMapping("/add")
    public ResultDTO<Boolean> add(@RequestBody AlertNotifyAddReq req) {
        return null;
    }

    @PutMapping("/{id}")
    public ResultDTO<Boolean> update(@RequestBody AlertNotifyUpdateReq req) {
        return null;
    }

    @DeleteMapping
    public ResultDTO<Boolean> delete(@RequestParam("idList") List<Long> idList) {
        return null;
    }

    @GetMapping("/release")
    public ResultDTO<Boolean> release(@RequestParam("id") Integer id) {
        return null;
    }

    @GetMapping("/listAlertRules")
    public ResultDTO<AlertRuleDropDownBoxVO> listAlertRules(@RequestParam("clusterId") Integer clusterId) {
        return null;
    }
}
