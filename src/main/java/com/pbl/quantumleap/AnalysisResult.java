package com.pbl.quantumleap;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class AnalysisResult {
  private final List<List<String>> circularDependencies;
  private final String aiArchitectureSuggestions;
  private final Map<String, List<String>> testsWithPaths;
}
