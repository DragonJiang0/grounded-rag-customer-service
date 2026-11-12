package com.zidong.common.enums;

public enum RpcServerEnum {



    PORTAL("portal");

    private final String name;

    RpcServerEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
