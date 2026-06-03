package com.msgbridge.repository;

import com.msgbridge.domain.MbApp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRepository extends JpaRepository<MbApp, Long> {
    Optional<MbApp> findByAppId(String appId);

    boolean existsByAppId(String appId);
}
