package com.msgbridge.repository;

import com.msgbridge.domain.MbRouteRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRuleRepository extends JpaRepository<MbRouteRule, Long> {
    Optional<MbRouteRule> findByRuleCode(String ruleCode);

    boolean existsByRuleCode(String ruleCode);

    List<MbRouteRule> findBySceneCodeAndStatusOrderByPriorityAsc(String sceneCode, Integer status);
}
