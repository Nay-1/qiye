package com.qiye.ai;

import com.qiye.common.BizException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量化客户端（DashScope / Qwen text-embedding，OpenAI 兼容）
 */
@Component
public class EmbeddingClient {

    private final AiProperties props;
    private final RestClient restClient;

    public EmbeddingClient(AiProperties props) {
        this.props = props;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutSeconds() * 1000);
        factory.setReadTimeout(props.getTimeoutSeconds() * 1000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public boolean enabled() {
        return StringUtils.hasText(props.getDashscope().getApiKey());
    }

    public float[] embed(String text) {
        String key = props.getDashscope().getApiKey();
        if (!StringUtils.hasText(key)) {
            throw new BizException("未配置 DASHSCOPE_API_KEY，无法向量化");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getDashscope().getEmbeddingModel());
        body.put("input", text);

        Map<?, ?> resp = restClient.post()
                .uri(props.getDashscope().getBaseUrl() + "/embeddings")
                .header("Authorization", "Bearer " + key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        if (resp == null) {
            throw new BizException("向量化服务返回为空");
        }
        List<?> data = (List<?>) resp.get("data");
        if (data == null || data.isEmpty()) {
            throw new BizException("向量化服务无返回");
        }
        Map<?, ?> first = (Map<?, ?>) data.get(0);
        List<?> embedding = (List<?>) first.get("embedding");
        if (embedding == null) {
            throw new BizException("向量化结果为空");
        }
        float[] vec = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vec[i] = ((Number) embedding.get(i)).floatValue();
        }
        return vec;
    }
}
