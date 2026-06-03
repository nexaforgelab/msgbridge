package com.msgbridge.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkMessageActionRequest(
        @NotEmpty List<String> messageIds
) {
}
