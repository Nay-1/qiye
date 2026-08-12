package com.qiye.ai;

import com.qiye.common.BizException;
import com.qiye.ai.AiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型对话客户端（OpenAI 兼容）
 * - opencode  → OpenCode Go
 * - dashscope → 通义 qwen
 * - mock      → 抛异常，由上层降级为预设文案
 */
@Component
public class LlmClient {

    private final AiProperties props;
    private final RestClient restClient;

    public LlmClient(AiProperties props) {
        this.props = props;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutSeconds() * 1000);
        factory.setReadTimeout(props.getTimeoutSeconds() * 1000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public boolean enabled() {
        return switch (props.getProvider()) {
            case "opencode" -> StringUtils.hasText(props.getOpencode().getApiKey());
            case "dashscope" -> StringUtils.hasText(props.getDashscope().getApiKey());
            default -> false;
        };
    }

    /** 对话；system 可空 */
    public String chat(String system, String user, double temperature) {
        String base;
        String key;
        String model;
        switch (props.getProvider()) {
            case "opencode" -> {
                base = props.getOpencode().getBaseUrl();
                key = props.getOpencode().getApiKey();
                model = props.getOpencode().getChatModel();
            }
            case "dashscope" -> {
                base = props.getDashscope().getBaseUrl();
                key = props.getDashscope().getApiKey();
                model = "qwen-plus";
            }
            default -> throw new BizException("AI 服务未配置（当前为 mock 模式）");
        }
        if (!StringUtils.hasText(key)) {
            throw new BizException("未配置 AI API Key");
        }
        return chatOpenAI(base, key, model, system, user, temperature);
    }

    private String chatOpenAI(String base, String key, String model,
                              String system, String user, double temperature) {
        List<Map<String, String>> messages = new java.util.ArrayList<>();
        if (StringUtils.hasText(system)) {
            messages.add(Map.of("role", "system", "content", system));
        }
        messages.add(Map.of("role", "user", "content", user));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);

        Map<?, ?> resp = restClient.post()
                .uri(base + "/chat/completions")
                .header("Authorization", "Bearer " + key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (resp == null) {
            throw new BizException("AI 服务返回为空");
        }
        List<?> choices = (List<?>) resp.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BizException("AI 服务无返回内容");
        }
        Map<?, ?> first = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) first.get("message");
        return message == null ? "" : String.valueOf(message.get("content"));
    }
}
