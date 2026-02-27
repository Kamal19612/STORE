package com.sucrestore.api.controller;

import com.sucrestore.api.entity.AppSetting;
import com.sucrestore.api.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppSettingController {

    private final AppSettingService appSettingService;

    @GetMapping("/public/settings")
    public Map<String, String> getPublicSettings() {
        return appSettingService.getPublicSettings();
    }

    @GetMapping("/admin/settings")
    public List<AppSetting> getAllSettings() {
        return appSettingService.getAllSettings();
    }

    @PutMapping("/admin/settings")
    public void updateSettings(@RequestBody Map<String, String> settings) {
        appSettingService.updateSettings(settings);
    }
}
