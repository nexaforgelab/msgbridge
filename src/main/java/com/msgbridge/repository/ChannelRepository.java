package com.msgbridge.repository;

import com.msgbridge.domain.MbChannel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<MbChannel, Long> {
    Optional<MbChannel> findByChannelCode(String channelCode);

    boolean existsByChannelCode(String channelCode);

    List<MbChannel> findByChannelCodeIn(Collection<String> channelCodes);
}
