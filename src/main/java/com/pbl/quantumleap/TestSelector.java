package com.pbl.quantumleap;

import com.pbl.quantumleap.model.DependencyGraph;
import com.pbl.quantumleap.model.DependencyGraph.ClassNode;
import lombok.Getter;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class TestSelector {

  private final DependencyGraph dependencyGraph;
  private final Map<String, String> sourceToTestMap;

  @Getter // 외부에서 마지막으로 계산된 impactSet에 접근할 수 있도록 Getter 추가
  private Set<String> impactSet;

  public TestSelector(DependencyGraph dependencyGraph, Map<String, String> sourceToTestMap) {
    this.dependencyGraph = dependencyGraph;
    this.sourceToTestMap = sourceToTestMap;
  }

  /**
   * 변경된 클래스 목록을 기반으로 실행해야 할 테스트 클래스 목록을 선별합니다.
   * @param changedClasses 변경된 소스 클래스 이름 Set
   * @return 실행해야 할 테스트 클래스 이름 Set
   */
  public Set<String> selectTests(Set<String> changedClasses) {
    System.out.println("--- 테스트 선별 시작 ---");
    System.out.println("변경된 클래스: " + changedClasses);

    // 1. 변경된 파일의 영향 범위를 모두 찾고, 결과를 멤버 변수에 저장합니다.
    this.impactSet = findImpactSet(changedClasses);
    System.out.println("영향을 받는 클래스: " + this.impactSet);

    // 2. 영향받는 클래스들에 해당하는 테스트 클래스를 찾습니다.
    Set<String> testsToRun = this.impactSet.stream()
        .map(sourceToTestMap::get)
        .filter(java.util.Objects::nonNull) // 매핑되는 테스트가 없는 경우는 제외
        .collect(Collectors.toSet());

    System.out.println("실행될 테스트: " + testsToRun);
    return testsToRun;
  }

  /**
   * 변경된 클래스들로부터 시작하여, 그래프를 따라가며 영향을 받는 모든 클래스를 찾습니다. (BFS)
   */
  private Set<String> findImpactSet(Set<String> changedClasses) {
    Set<String> visited = new HashSet<>();
    Queue<String> queue = new ArrayDeque<>(changedClasses);
    visited.addAll(changedClasses);

    while (!queue.isEmpty()) {
      String currentClassName = queue.poll();
      ClassNode currentNode = dependencyGraph.getNode(currentClassName);

      if (currentNode == null) continue;

      // ClassNode에 미리 계산된 역의존성(dependents) 정보를 바로 사용합니다.
      for (ClassNode dependentNode : currentNode.getDependents()) {
        if (!visited.contains(dependentNode.getName())) {
          visited.add(dependentNode.getName());
          queue.add(dependentNode.getName());
        }
      }
    }
    return visited;
  }
}
