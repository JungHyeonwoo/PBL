package com.pbl.quantumleap;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Configuration {
  private String projectBasePackage = "com.example";
  private String sourceDirectory = "src/main/java";
  private String testDirectory = "src/test/java";
}
