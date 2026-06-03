package com.msgbridge.repository;

import com.msgbridge.core.SendStatus;
import com.msgbridge.core.ChannelType;
import com.msgbridge.domain.MbSendLog;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SendLogRepository extends JpaRepository<MbSendLog, Long> {
    List<MbSendLog> findByMessageIdOrderByCreatedAtAsc(String messageId);

    Optional<MbSendLog> findFirstByChannelIdOrderByCreatedAtDesc(Long channelId);

    long countByStatusAndCreatedAtAfter(SendStatus status, Instant after);

    long countByCreatedAtAfter(Instant after);

    long countByCreatedAtBefore(Instant before);

    long deleteByCreatedAtBefore(Instant before);

    @Query("""
            select l.channelType as channelType, count(l) as countValue
            from MbSendLog l
            where l.createdAt >= :after
            group by l.channelType
            """)
    List<ChannelCount> countByChannelTypeSince(@Param("after") Instant after);

    interface ChannelCount {
        ChannelType getChannelType();

        long getCountValue();
    }
}
