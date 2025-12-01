package com.fishseller.whatsappservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WhatsAppMessageDto {
    @JsonProperty("messaging_product")
    @Builder.Default
    private String messagingProduct = "whatsapp";

    @JsonProperty("recipient_type")
    @Builder.Default
    private String recipientType = "individual";

    private String to;

    private String type;

    private TextDto text;

    private InteractiveDto interactive;

    @Data
    @Builder
    public static class TextDto {
        @JsonProperty("preview_url")
        private boolean previewUrl;
        private String body;
    }

    @Data
    @Builder
    public static class InteractiveDto {
        private String type;
        private ActionDto action;
        private BodyDto body;
    }

    @Data
    @Builder
    public static class BodyDto {
        private String text;
    }

    @Data
    @Builder
    public static class ActionDto {
        private String button;
        private List<ButtonDto> buttons;
        private List<SectionDto> sections;
    }

    @Data
    @Builder
    public static class ButtonDto {
        private String type;
        private ReplyDto reply;
    }

    @Data
    @Builder
    public static class ReplyDto {
        private String id;
        private String title;
    }

    @Data
    @Builder
    public static class SectionDto {
        private String title;
        private List<RowDto> rows;
    }

    @Data
    @Builder
    public static class RowDto {
        private String id;
        private String title;
        private String description;
    }
}
