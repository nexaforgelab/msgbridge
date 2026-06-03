package com.msgbridge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.MessageType;
import com.msgbridge.domain.MbTemplate;
import com.msgbridge.dto.TemplatePreviewRequest;
import com.msgbridge.dto.UnifiedMessageDto;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void rendersTemplateVariablesFromMessageDataAndBuiltIns() {
        MbTemplate template = new MbTemplate();
        template.setSceneCode("ORDER_PAID");
        template.setChannelType(ChannelType.WE_COM_ROBOT);
        template.setMsgType(MessageType.MARKDOWN);
        template.setContentTemplate("客户：${customerName}\n金额：${amount}\n详情：${url}");

        UnifiedMessageDto message = new UnifiedMessageDto(
                "ALERT",
                "新订单提醒",
                null,
                null,
                "INFO",
                "https://example.test/orders/1",
                null,
                Map.of("customerName", "张三", "amount", "12800"));

        TemplateRenderer.PreparedMessage prepared = renderer.render(template, message);

        assertThat(prepared.type()).isEqualTo(MessageType.MARKDOWN);
        assertThat(prepared.content()).contains("客户：张三", "金额：12800", "https://example.test/orders/1");
    }

    @Test
    void previewRequestShapeCanCarryAdHocTemplateData() {
        TemplatePreviewRequest request = new TemplatePreviewRequest(
                null,
                "ORDER_PAID",
                ChannelType.WE_COM_ROBOT,
                MessageType.MARKDOWN,
                "客户：${customerName}",
                null,
                new LinkedHashMap<>(Map.of("customerName", "李四")));

        assertThat(request.contentTemplate()).contains("${customerName}");
        assertThat(request.data()).containsEntry("customerName", "李四");
    }
}
