package com.archive.enums;

import lombok.Getter;

@Getter
public enum RoleCode {
    front_archivist("档案管理员（前台）"),
    back_archivist("档案管理员（后台）"),
    transfer_user("移交单位经办人"),
    internal_reader("内部查阅者"),
    public_user("社会公众"),
    director("馆领导"),
    sys_admin("系统管理员");

    private final String displayName;

    RoleCode(String displayName) {
        this.displayName = displayName;
    }
}
