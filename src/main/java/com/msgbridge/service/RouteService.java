package com.msgbridge.service;

import com.msgbridge.core.BusinessException;
import com.msgbridge.core.Constants;
import com.msgbridge.domain.MbRouteRule;
import com.msgbridge.dto.ReceiverDto;
import com.msgbridge.dto.RouteRuleRequest;
import com.msgbridge.dto.RouteRuleResponse;
import com.msgbridge.dto.RouteSimulateRequest;
import com.msgbridge.dto.RouteSimulateResponse;
import com.msgbridge.repository.RouteRuleRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RouteService {
    private final RouteRuleRepository routeRuleRepository;
    private final ConditionEvaluator conditionEvaluator;
    private final JsonService jsonService;
    private final AuditLogService auditLogService;

    public RouteService(
            RouteRuleRepository routeRuleRepository,
            ConditionEvaluator conditionEvaluator,
            JsonService jsonService,
            AuditLogService auditLogService) {
        this.routeRuleRepository = routeRuleRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.jsonService = jsonService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RouteRuleResponse create(RouteRuleRequest request) {
        if (routeRuleRepository.existsByRuleCode(request.ruleCode())) {
            throw BusinessException.conflict("ruleCode already exists");
        }
        MbRouteRule rule = new MbRouteRule();
        apply(rule, request);
        routeRuleRepository.save(rule);
        auditLogService.record("CREATE_ROUTE", "ROUTE", request.ruleCode());
        return toResponse(rule);
    }

    @Transactional
    public RouteRuleResponse update(String ruleCode, RouteRuleRequest request) {
        MbRouteRule rule = routeRuleRepository.findByRuleCode(ruleCode)
                .orElseThrow(() -> BusinessException.notFound("route rule not found"));
        apply(rule, request);
        auditLogService.record("UPDATE_ROUTE", "ROUTE", ruleCode);
        return toResponse(rule);
    }

    @Transactional
    public RouteRuleResponse setStatus(String ruleCode, int status) {
        MbRouteRule rule = routeRuleRepository.findByRuleCode(ruleCode)
                .orElseThrow(() -> BusinessException.notFound("route rule not found"));
        rule.setStatus(status == Constants.STATUS_ENABLED ? Constants.STATUS_ENABLED : Constants.STATUS_DISABLED);
        auditLogService.record("SET_ROUTE_STATUS", "ROUTE", ruleCode);
        return toResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<RouteRuleResponse> list() {
        return routeRuleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RouteResult resolve(String sceneCode, ReceiverDto receiver, Map<String, Object> data) {
        if (receiver != null && "CHANNEL".equalsIgnoreCase(receiver.type()) && receiver.channelCodes() != null) {
            return new RouteResult(receiver.channelCodes(), List.of("DIRECT_CHANNEL"));
        }
        String routeKey = receiver == null ? null : receiver.routeKey();
        Set<String> channels = new LinkedHashSet<>();
        List<String> matchedRules = new ArrayList<>();
        List<MbRouteRule> rules = routeRuleRepository.findBySceneCodeAndStatusOrderByPriorityAsc(
                sceneCode, Constants.STATUS_ENABLED);
        for (MbRouteRule rule : rules) {
            if (StringUtils.hasText(rule.getRouteKey())
                    && StringUtils.hasText(routeKey)
                    && !rule.getRouteKey().equals(routeKey)) {
                continue;
            }
            if (StringUtils.hasText(rule.getRouteKey()) && !StringUtils.hasText(routeKey)) {
                continue;
            }
            if (!conditionEvaluator.matches(rule.getConditionExpr(), data)) {
                continue;
            }
            channels.addAll(jsonService.readStringList(rule.getTargetChannelsJson()));
            matchedRules.add(rule.getRuleCode());
        }
        return new RouteResult(List.copyOf(channels), matchedRules);
    }

    @Transactional(readOnly = true)
    public RouteSimulateResponse simulate(RouteSimulateRequest request) {
        RouteResult result = resolve(request.sceneCode(), new ReceiverDto("ROUTE", request.routeKey(), null), request.data());
        return new RouteSimulateResponse(result.channelCodes(), result.matchedRules());
    }

    private void apply(MbRouteRule rule, RouteRuleRequest request) {
        rule.setRuleCode(request.ruleCode());
        rule.setRuleName(request.ruleName());
        rule.setSceneCode(request.sceneCode());
        rule.setRouteKey(request.routeKey());
        rule.setConditionExpr(request.conditionExpr());
        rule.setTargetChannelsJson(jsonService.toJson(request.targetChannels()));
        rule.setPriority(request.priority() == null ? 100 : request.priority());
        rule.setStatus(request.status() == null ? Constants.STATUS_ENABLED : request.status());
    }

    public RouteRuleResponse toResponse(MbRouteRule rule) {
        return new RouteRuleResponse(
                rule.getId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getSceneCode(),
                rule.getRouteKey(),
                rule.getConditionExpr(),
                jsonService.readStringList(rule.getTargetChannelsJson()),
                rule.getPriority(),
                rule.getStatus(),
                rule.getCreatedAt(),
                rule.getUpdatedAt());
    }

    public record RouteResult(List<String> channelCodes, List<String> matchedRules) {
    }
}
