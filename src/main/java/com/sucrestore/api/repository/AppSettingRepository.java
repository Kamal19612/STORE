package com.sucrestore.api.repository;

import com.sucrestore.api.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

    /**
     * Legacy method kept for backward compatibility (historically key was global-unique).
     * In multi-store mode, prefer findByKeyAndStoreId.
     */
    Optional<AppSetting> findByKey(String key);

    Optional<AppSetting> findByKeyAndStoreId(String key, Long storeId);

    /**
     * Backward compatibility: legacy global setting (store_id is NULL).
     */
    Optional<AppSetting> findByKeyAndStoreIsNull(String key);
}
