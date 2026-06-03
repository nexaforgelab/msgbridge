package com.msgbridge.repository;

import com.msgbridge.domain.MbTokenCache;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenCacheRepository extends JpaRepository<MbTokenCache, Long> {
    Optional<MbTokenCache> findByTokenKey(String tokenKey);
}
