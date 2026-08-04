package com.vidsprout.modules.config.controller;

import com.vidsprout.common.ApiResponse;
import com.vidsprout.modules.config.service.SystemConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/configs")
public class ConfigController {

    private final SystemConfigService configService;

    public ConfigController(SystemConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/global/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGlobalConfig() {
        return ResponseEntity.ok(ApiResponse.success(configService.getGlobalConfig()));
    }

    @GetMapping("/admin/list/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminConfig() {
        return ResponseEntity.ok(ApiResponse.success(configService.getAdminConfig()));
    }

    @PostMapping("/admin/update/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAdminConfig(@RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(ApiResponse.success(configService.updateAdminConfig(updates)));
    }
}
