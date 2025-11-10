package com.pbl.quantumleap.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class OpenAIService {

  private final String openaiApiUrl;
  private final String openaiApiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;


  public OpenAIService() {
    // 환경 변수에서 직접 읽어옵니다.
    this.openaiApiUrl = System.getenv("OPENAI_API_URL");
    this.openaiApiKey = System.getenv("OPENAI_API_KEY");

    if (this.openaiApiUrl == null || this.openaiApiKey == null) {
      System.err.println("경고: 환경변수 OPENAI_API_URL 또는 OPENAI_API_KEY가 설정되지 않았습니다. AI 분석을 건너뜁니다.");
    }

    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  // 테스트 또는 다른 설정 방식을 위해 추가 생성자 제공 가능
  public OpenAIService(String apiUrl, String apiKey) {
    this.openaiApiUrl = apiUrl;
    this.openaiApiKey = apiKey;
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }
  public String callOpenAI(String prompt) {
    if (openaiApiUrl == null || openaiApiKey == null) {
      return "OpenAI API 설정이 없어 AI 분석을 수행할 수 없습니다.";
    }

    try {
      Map<String, Object> requestBodyMap = Map.of(
          "model", "gpt-4.1", // 또는 사용 가능한 최신 모델
          "messages", List.of(Map.of("role", "user", "content", prompt))
      );
      String requestBody = objectMapper.writeValueAsString(requestBodyMap);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(openaiApiUrl))
          .header("Authorization", "Bearer " + openaiApiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        Map<String, Object> responseMap = objectMapper.readValue(response.body(),
            new TypeReference<Map<String, Object>>() {});

        Object choicesObj = responseMap.get("choices");
        if (choicesObj instanceof List) {
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObj;

          if (!choices.isEmpty()) {
            Map<String, Object> firstChoice = choices.get(0);
            Object messageObj = firstChoice.get("message");
            if (messageObj instanceof Map) {
              @SuppressWarnings("unchecked")
              Map<String, Object> message = (Map<String, Object>) messageObj;
              Object contentObj = message.get("content");
              if (contentObj instanceof String) {
                return (String) contentObj;
              }
            }
          }
        }
        return "AI 응답에서 유효한 내용을 찾을 수 없습니다.";

      } else {
        System.err.println("OpenAI API 호출 실패: " + response.statusCode() + " " + response.body());
        return "AI 분석 중 오류가 발생했습니다. 상태 코드: " + response.statusCode();
      }

    } catch (Exception e) {
      System.err.println("OpenAI API 호출 중 예외 발생: " + e.getMessage());
      return "AI 분석 중 예외가 발생했습니다.";
    }
  }
}