package com.lifetrenz.lths.core.license_management.controller;

import com.lifetrenz.lths.core.common.app.ApplicationResponse;
import com.lifetrenz.lths.core.common.app.constant.CommonConstants;
import com.lifetrenz.lths.core.license_management.dto.LicenseManagementRequest;
import com.lifetrenz.lths.core.license_management.dto.PublishAdminRequest;
import com.lifetrenz.lths.core.license_management.dto.PublishAdminResponse;
import com.lifetrenz.lths.core.license_management.dto.PublishAllRequest;
import com.lifetrenz.lths.core.license_management.dto.PublishAllResponse;
import com.lifetrenz.lths.core.license_management.dto.PublishLicenseRequest;
import com.lifetrenz.lths.core.license_management.dto.PublishLicenseResponse;
import com.lifetrenz.lths.core.license_management.service.LicenseManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for License Management operations.
 *
 * <p>Acts as the entry point for onboarding data sent by PMS after customer creation.
 * All heavy lifting is delegated to {@link LicenseManagementService}.
 */
@RestController
@RequestMapping("/api/license-management")
@RequiredArgsConstructor
@Slf4j
public class LicenseManagementController {

    private final LicenseManagementService licenseManagementService;

    /**
     * Accepts an onboarding payload from PMS and initiates the license management flow.
     *
     * @param request validated onboarding request body
     * @return {@link ApplicationResponse} wrapping a confirmation message
     */
    @PostMapping("/onboard")
    public ApplicationResponse<String> onboard(@Valid @RequestBody LicenseManagementRequest request) {
        String message = licenseManagementService.processOnboarding(request);
        return new ApplicationResponse<>(
                CommonConstants.SUCCESS,
                String.valueOf(HttpStatus.OK.value()),
                message,
                message
        );
    }

    /**
     * Publishes a complete customer hierarchy (customer + businesses + sites) to core-service
     * in a single atomic operation. Returns the generated core-service IDs for all entities
     * so that PMS can persist them locally.
     *
     * @param request validated publish-all request body
     * @return {@link ApplicationResponse} wrapping core IDs for all saved entities
     */
    @PostMapping("/publish-all")
    public ApplicationResponse<PublishAllResponse> publishAll(@Valid @RequestBody PublishAllRequest request) {
        PublishAllResponse response = licenseManagementService.processPublishAll(request);
        return new ApplicationResponse<>(
                CommonConstants.SUCCESS,
                String.valueOf(HttpStatus.OK.value()),
                "Customer hierarchy published successfully",
                response
        );
    }

    /**
     * Persists a single license record in core-service.
     * Called by PMS when a license is published.
     *
     * @param request validated publish-license request body
     * @return {@link ApplicationResponse} wrapping the generated coreLicenseId
     */
    @PostMapping("/publish-license")
    public ApplicationResponse<PublishLicenseResponse> publishLicense(
            @Valid @RequestBody PublishLicenseRequest request) {
        PublishLicenseResponse response = licenseManagementService.processPublishLicense(request);
        return new ApplicationResponse<>(
                CommonConstants.SUCCESS,
                String.valueOf(HttpStatus.OK.value()),
                "License published successfully",
                response
        );
    }

    /**
     * Publishes a PMS AdminUser to Core by creating a Person, Employee and User record
     * and firing a Kafka event to User Management.
     *
     * @param request validated publish-admin request body
     * @return {@link ApplicationResponse} wrapping the generated coreEmployeeId
     */
    @PostMapping("/publish-admin")
    public ApplicationResponse<PublishAdminResponse> publishAdmin(
            @Valid @RequestBody PublishAdminRequest request) {
        try {
            PublishAdminResponse response = licenseManagementService.publishAdmin(request);
            return new ApplicationResponse<>(
                    CommonConstants.SUCCESS,
                    String.valueOf(HttpStatus.OK.value()),
                    "Admin user published successfully",
                    response
            );
        } catch (Exception ex) {
            log.error("[LicenseManagement] Failed to publish admin user: {}", ex.getMessage(), ex);
            return new ApplicationResponse<>(
                    "ERROR",
                    String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                    "Failed to publish admin user: " + ex.getMessage(),
                    null
            );
        }
    }
}
