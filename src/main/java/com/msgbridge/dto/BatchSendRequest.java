package com.msgbridge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchSendRequest(
        @Valid @NotEmpty List<SendMessageRequest> messages
) {
}
