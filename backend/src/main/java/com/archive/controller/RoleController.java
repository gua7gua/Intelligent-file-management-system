package com.archive.controller;

import com.archive.common.R;
import com.archive.dto.response.RoleResponse;
import com.archive.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Tag(name = "角色", description = "预设角色查询")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "查询角色列表")
    public R<List<RoleResponse>> list() {
        return R.ok(roleService.listRoles());
    }
}
