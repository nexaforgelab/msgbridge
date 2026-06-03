package com.msgbridge.service;

import com.msgbridge.core.BusinessException;
import com.msgbridge.core.ChannelType;
import com.msgbridge.core.Constants;
import com.msgbridge.domain.MbTemplate;
import com.msgbridge.dto.TemplateRequest;
import com.msgbridge.dto.TemplatePreviewRequest;
import com.msgbridge.dto.TemplatePreviewResponse;
import com.msgbridge.dto.TemplateResponse;
import com.msgbridge.dto.UnifiedMessageDto;
import com.msgbridge.repository.TemplateRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {
    private final TemplateRepository templateRepository;
    private final JsonService jsonService;
    private final AuditLogService auditLogService;
    private final TemplateRenderer templateRenderer;

    public TemplateService(
            TemplateRepository templateRepository,
            JsonService jsonService,
            AuditLogService auditLogService,
            TemplateRenderer templateRenderer) {
        this.templateRepository = templateRepository;
        this.jsonService = jsonService;
        this.auditLogService = auditLogService;
        this.templateRenderer = templateRenderer;
    }

    @Transactional
    public TemplateResponse create(TemplateRequest request) {
        int version = request.version() == null ? 1 : request.version();
        if (templateRepository.existsBySceneCodeAndChannelTypeAndVersion(request.sceneCode(), request.channelType(), version)) {
            throw BusinessException.conflict("template version already exists for scene and channel");
        }
        MbTemplate template = new MbTemplate();
        template.setTemplateCode(request.templateCode());
        template.setTemplateName(request.templateName());
        template.setSceneCode(request.sceneCode());
        template.setChannelType(request.channelType());
        template.setMsgType(request.msgType());
        template.setContentTemplate(request.contentTemplate());
        template.setVariablesJson(jsonService.toJson(request.variables() == null ? Map.of() : request.variables()));
        template.setVersion(version);
        template.setStatus(request.status() == null ? Constants.STATUS_ENABLED : request.status());
        templateRepository.save(template);
        auditLogService.record("CREATE_TEMPLATE", "TEMPLATE", request.templateCode());
        return toResponse(template);
    }

    @Transactional
    public TemplateResponse update(String templateCode, TemplateRequest request) {
        MbTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> BusinessException.notFound("template not found"));
        template.setTemplateCode(request.templateCode());
        template.setTemplateName(request.templateName());
        template.setSceneCode(request.sceneCode());
        template.setChannelType(request.channelType());
        template.setMsgType(request.msgType());
        template.setContentTemplate(request.contentTemplate());
        template.setVariablesJson(jsonService.toJson(request.variables() == null ? Map.of() : request.variables()));
        template.setVersion(request.version() == null ? template.getVersion() : request.version());
        template.setStatus(request.status() == null ? template.getStatus() : request.status());
        auditLogService.record("UPDATE_TEMPLATE", "TEMPLATE", templateCode);
        return toResponse(template);
    }

    @Transactional
    public TemplateResponse setStatus(String templateCode, int status) {
        MbTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> BusinessException.notFound("template not found"));
        template.setStatus(status == Constants.STATUS_ENABLED ? Constants.STATUS_ENABLED : Constants.STATUS_DISABLED);
        auditLogService.record("SET_TEMPLATE_STATUS", "TEMPLATE", templateCode);
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public Optional<MbTemplate> latest(String sceneCode, ChannelType channelType) {
        return templateRepository.findFirstBySceneCodeAndChannelTypeAndStatusOrderByVersionDesc(
                sceneCode, channelType, Constants.STATUS_ENABLED);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> list() {
        return templateRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TemplatePreviewResponse preview(TemplatePreviewRequest request) {
        MbTemplate template = resolvePreviewTemplate(request);
        UnifiedMessageDto message = previewMessage(request);
        TemplateRenderer.PreparedMessage rendered = templateRenderer.render(template, message);
        return new TemplatePreviewResponse(
                template.getTemplateCode(),
                template.getSceneCode(),
                template.getChannelType(),
                rendered.type(),
                rendered.title(),
                rendered.content(),
                rendered.url(),
                rendered.variables());
    }

    private MbTemplate resolvePreviewTemplate(TemplatePreviewRequest request) {
        if (request.templateCode() != null && !request.templateCode().isBlank()) {
            return templateRepository.findByTemplateCode(request.templateCode())
                    .orElseThrow(() -> BusinessException.notFound("template not found"));
        }
        if (request.sceneCode() != null && request.channelType() != null) {
            return latest(request.sceneCode(), request.channelType())
                    .orElseGet(() -> adHocTemplate(request));
        }
        return adHocTemplate(request);
    }

    private MbTemplate adHocTemplate(TemplatePreviewRequest request) {
        if (request.contentTemplate() == null || request.contentTemplate().isBlank()
                || request.sceneCode() == null
                || request.channelType() == null
                || request.msgType() == null) {
            throw BusinessException.badRequest("templateCode or sceneCode/channelType/msgType/contentTemplate is required");
        }
        MbTemplate template = new MbTemplate();
        template.setTemplateCode("PREVIEW");
        template.setTemplateName("Preview");
        template.setSceneCode(request.sceneCode());
        template.setChannelType(request.channelType());
        template.setMsgType(request.msgType());
        template.setContentTemplate(request.contentTemplate());
        return template;
    }

    private UnifiedMessageDto previewMessage(TemplatePreviewRequest request) {
        if (request.message() != null) {
            return request.message();
        }
        Map<String, Object> data = request.data() == null ? Map.of() : new LinkedHashMap<>(request.data());
        return new UnifiedMessageDto(
                "ALERT",
                String.valueOf(data.getOrDefault("title", "模板预览")),
                null,
                String.valueOf(data.getOrDefault("content", "")),
                String.valueOf(data.getOrDefault("level", "INFO")),
                data.get("url") == null ? null : String.valueOf(data.get("url")),
                null,
                data);
    }

    public TemplateResponse toResponse(MbTemplate template) {
        return new TemplateResponse(
                template.getId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getSceneCode(),
                template.getChannelType(),
                template.getMsgType(),
                template.getContentTemplate(),
                jsonService.readMap(template.getVariablesJson()),
                template.getVersion(),
                template.getStatus(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
