package org.dromara.cloudeon.controller;

import lombok.extern.slf4j.Slf4j;
import org.dromara.cloudeon.domain.req.AlertNotifyAddReq;
import org.dromara.cloudeon.domain.req.AlertNotifyPageReq;
import org.dromara.cloudeon.domain.req.AlertNotifyUpdateReq;
import org.dromara.cloudeon.domain.vo.AlertNotifyPageInfoVO;
import org.dromara.cloudeon.domain.vo.AlertRuleDropDownBoxVO;
import org.dromara.cloudeon.domain.vo.JsonPage;
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
        JsonPage<AlertNotifyPageInfoVO> results = alertNotifyService.page(req);
        return ResultDTO.success(results);
    }

    @PostMapping("/add")
    public ResultDTO<Void> add(@RequestBody AlertNotifyAddReq req) {
        return alertNotifyService.add(req);
    }

    @PutMapping("/{id}")
    public ResultDTO<Void> update(@RequestBody AlertNotifyUpdateReq req) {
        return alertNotifyService.update(req);
    }

    @DeleteMapping("/delete")
    public ResultDTO<Void> delete(@RequestParam("id") Integer id) {
        return alertNotifyService.delete(id);
    }

    @GetMapping("/release")
    public ResultDTO<Void> release(@RequestParam("id") Integer id, @RequestParam("enableStatus") Boolean enableStatus) {
        return alertNotifyService.release(id, enableStatus);
    }

    @GetMapping("/listAlertRules")
    public ResultDTO<List<AlertRuleDropDownBoxVO>> listAlertRules(@RequestParam("clusterId") Integer clusterId) {
        List<AlertRuleDropDownBoxVO> results = alertNotifyService.listAlertRules(clusterId);
        return ResultDTO.success(results);
    }
}
