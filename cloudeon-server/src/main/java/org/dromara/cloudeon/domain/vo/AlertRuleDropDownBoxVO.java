package org.dromara.cloudeon.domain.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class AlertRuleDropDownBoxVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private String ruleName;
}
