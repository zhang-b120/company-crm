package com.qykj.finmaster.third.deepseek.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 大语言模型调用服务
 */
@Slf4j
@Service
public class DeepSeekService {

    @Value("${third.deepseek.api-key}")
    private String apiKey;

    @Value("${third.deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    private static final String MODEL = "deepseek-chat";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "你是一位专业的电话销售培训师和质量分析专家。请对销售人员的通话内容进行专业评估，" +
            "以JSON格式输出分析结果，不要包含任何其他内容。";

    private static final String USER_PROMPT_TEMPLATE =
            "请分析以下电话销售通话内容，从两个维度进行评估：\n" +
            "一、销售员表现（质量分析）；二、客户购买意向度分析。\n\n" +
            "通话内容：\n%s\n\n" +
            "【质量分析】请从以下5个维度对销售员表现评分（每项0-100分），并给出综合得分：\n" +
            "1. 开场白与建立信任\n" +
            "2. 需求挖掘与倾听\n" +
            "3. 产品介绍与话术\n" +
            "4. 异议处理\n" +
            "5. 促成与收尾\n\n" +
            "【意向度分析】请从客户（被叫方）角度判断购买意向：\n" +
            "- 意向等级：HIGH（高意向≥75分）/ MEDIUM（中意向50-74分）/ LOW（低意向25-49分）/ NONE（无意向<25分）\n" +
            "- 购买信号：通话中客户表现出意向的具体言语或行为\n" +
            "- 主要异议：客户提出的顾虑或拒绝理由\n" +
            "- 跟进建议：针对当前意向度的下一步跟进策略\n\n" +
            "请严格按照以下JSON格式输出，不要包含任何其他内容：\n" +
            "{\n" +
            "  \"overall_score\": 80,\n" +
            "  \"dimensions\": [\n" +
            "    {\"name\": \"开场白与建立信任\", \"score\": 80, \"comment\": \"...\"}, \n" +
            "    {\"name\": \"需求挖掘与倾听\", \"score\": 75, \"comment\": \"...\"}, \n" +
            "    {\"name\": \"产品介绍与话术\", \"score\": 85, \"comment\": \"...\"}, \n" +
            "    {\"name\": \"异议处理\", \"score\": 70, \"comment\": \"...\"}, \n" +
            "    {\"name\": \"促成与收尾\", \"score\": 80, \"comment\": \"...\"}\n" +
            "  ],\n" +
            "  \"strengths\": [\"优点1\", \"优点2\"],\n" +
            "  \"improvements\": [\"改进建议1\", \"改进建议2\"],\n" +
            "  \"summary\": \"综合评价...\",\n" +
            "  \"customer_intention\": {\n" +
            "    \"level\": \"HIGH\",\n" +
            "    \"score\": 78,\n" +
            "    \"buy_signals\": [\"主动询问价格\", \"表达了近期有采购计划\"],\n" +
            "    \"objections\": [\"需要请示领导\"],\n" +
            "    \"follow_up_strategy\": \"建议3天内跟进，重点确认决策时间...\",\n" +
            "    \"summary\": \"客户有明确采购需求，意向较高...\"\n" +
            "  }\n" +
            "}";

    /**
     * 分析通话质量，返回 JSON 字符串
     *
     * @param transcriptionText 转写文字
     * @return 质量分析 JSON 字符串
     */
    public String analyzeCallQuality(String transcriptionText) throws Exception {
        String userPrompt = String.format(USER_PROMPT_TEMPLATE, transcriptionText);

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
        );

        String bodyJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMinutes(3))
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        log.info("调用 DeepSeek 分析通话质量, textLength={}", transcriptionText.length());

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("DeepSeek API 返回异常: status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("DeepSeek API 调用失败，HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            throw new RuntimeException("DeepSeek 返回结果为空");
        }

        String content = choices.get(0).path("message").path("content").asText();
        log.info("DeepSeek 分析完成, contentLength={}", content.length());

        // 验证是合法 JSON
        objectMapper.readTree(content);
        return content;
    }
}
