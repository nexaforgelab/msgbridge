package com.msgbridge.repository;

import com.msgbridge.domain.MbAdminUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<MbAdminUser, Long> {
    Optional<MbAdminUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
