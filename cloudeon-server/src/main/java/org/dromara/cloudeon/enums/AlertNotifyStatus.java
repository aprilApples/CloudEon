package org.dromara.cloudeon.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AlertNotifyStatus {
    DISABLE(0, "未启用"),
    ENABLE(1, "启用");

    private Integer value;

    private String desc;

    AlertNotifyStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public static AlertNotifyStatus fromDesc(String desc) {
        for (AlertNotifyStatus alertLevel : values()) {
            if (alertLevel.desc.equals(desc)) {
                return alertLevel;
            }
        }
        return null;
    }

    @JsonValue
    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }


    @Override
    public String toString() {
        return this.desc;
    }
}
