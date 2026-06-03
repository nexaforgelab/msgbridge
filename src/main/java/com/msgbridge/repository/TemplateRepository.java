package com.msgbridge.repository;

import com.msgbridge.core.ChannelType;
import com.msgbridge.domain.MbTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<MbTemplate, Long> {
    Optional<MbTemplate> findByTemplateCode(String templateCode);

    Optional<MbTemplate> findFirstBySceneCodeAndChannelTypeAndStatusOrderByVersionDesc(
            String sceneCode, ChannelType channelType, Integer status);

    List<MbTemplate> findBySceneCodeOrderByChannelTypeAscVersionDesc(String sceneCode);

    boolean existsBySceneCodeAndChannelTypeAndVersion(String sceneCode, ChannelType channelType, Integer version);
}
