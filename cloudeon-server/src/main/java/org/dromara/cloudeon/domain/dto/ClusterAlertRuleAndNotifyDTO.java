/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.dromara.cloudeon.domain.dto;

import lombok.Data;

import java.io.Serializable;


@Data
public class ClusterAlertRuleAndNotifyDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Integer id;

    private Integer clusterId;

    private String ruleName;

    private String alertLevel;

    private String promql;

    private String stackServiceName;

    private String stackRoleName;

    private Integer waitForFireDuration;

    private String alertInfo;

    private String alertAdvice;

    /**
     * 告警通知名称
     */
    private String alertNotifyName;

    /**
     * 告警通知类型 1 邮件
     */
    private Integer notifyType;

    /**
     * 启用状态 true启用 false 不启用
     */
    private Boolean enableStatus;

    /**
     * 接收者 多个用逗号分隔
     */
    private String recipients;

    /**
     * 描述
     */
    private String desp;
}
