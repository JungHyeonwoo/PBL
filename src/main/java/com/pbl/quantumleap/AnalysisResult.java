package com.pbl.quantumleap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class AnalysisResult {
  private final Set<String> testsToRun;
  private final List<List<String>> circularDependencies;
  private final String aiArchitectureSuggestions;
}
