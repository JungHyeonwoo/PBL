package com.pbl.quantumleap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {

  private static final String CONFIG_FILE_NAME = "quantumleap-config.yml";

  /**
   * 지정된 프로젝트 경로에서 설정 파일을 로드합니다.
   * 파일이 없으면 기본 설정을 반환합니다.
   * @param projectRootPath 분석할 프로젝트의 루트 경로
   * @return 로드된 또는 기본 Configuration 객체
   */
  public Configuration loadConfig(Path projectRootPath) {
    Path configPath = projectRootPath.resolve(CONFIG_FILE_NAME);

    if (Files.exists(configPath)) {
      System.err.println("✅ 설정 파일(" + CONFIG_FILE_NAME + ")을 발견했습니다. 설정을 로드합니다.");
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
        return mapper.readValue(configPath.toFile(), Configuration.class);
      } catch (IOException e) {
        System.err.println("⚠️ 설정 파일 읽기 오류! 기본 설정을 사용합니다. 오류: " + e.getMessage());
      }
    } else {
      System.err.println("ℹ️ 설정 파일이 없습니다. 기본 설정을 사용합니다.");
    }

    return new Configuration(); // 기본 설정 반환
  }
}

