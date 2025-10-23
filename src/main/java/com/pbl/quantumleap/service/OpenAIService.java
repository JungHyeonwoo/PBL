package com.pbl.quantumleap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

public class OpenAIService {

  private final String openaiApiUrl;
  private final String openaiApiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public OpenAIService() {
    // TODO: 실제 환경에서는 환경 변수나 설정 파일에서 API 키와 URL을 안전하게 로드해야 합니다.
    this.openaiApiUrl = System.getenv(
        "OPENAI_API_URL"); // 예: "https://api.openai.com/v1/chat/completions"
    this.openaiApiKey = System.getenv("OPENAI_API_KEY");

    if (this.openaiApiUrl == null || this.openaiApiKey == null) {
      System.err.println("경고: OpenAI API URL 또는 API 키가 설정되지 않았습니다. AI 분석을 건너<0xEB><0x9A><0x81>니다.");
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

      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
          Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
          return (String) message.get("content");
        } else {
          return "AI 응답에서 유효한 내용을 찾을 수 없습니다.";
        }
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