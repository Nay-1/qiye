package com.qiye.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置：provider 可切换（mock / opencode / dashscope）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    /** mock | opencode | dashscope */
    private String provider = "mock";
    private int timeoutSeconds = 60;

    /** OpenCode Go：对话/出题/建议文案（OpenAI 兼容） */
    private Opencode opencode = new Opencode();
    /** DashScope / Qwen：向量化 embedding（OpenAI 兼容） */
    private DashScope dashscope = new DashScope();

    @Data
    public static class Opencode {
        private String baseUrl = "https://opencode.ai/v1";
        private String apiKey = "";
        private String chatModel = "deepseek-v4-flash";
    }

    @Data
    public static class DashScope {
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String apiKey = "";
        private String embeddingModel = "text-embedding-v3";
    }
}
