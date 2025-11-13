package com.pbl.quantumleap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pbl.quantumleap.model.DependencyGraph;
import com.pbl.quantumleap.model.DependencyGraph.ClassNode;

import java.io.IOException;
import java.util.Set;

public class ArchitectureJsonExporter {

  /**
   * DependencyGraph를 상위/하위 의존성 정보를 포함한 JSON 형식으로 콘솔에 출력합니다.
   * @param graph 분석된 의존성 그래프
   * @param classesToExclude JSON 출력에서 제외할 클래스 이름 Set
   */
  public String getJsonString(DependencyGraph graph, Set<String> classesToExclude) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();
    ArrayNode classesArray = mapper.createArrayNode();

    // 각 클래스 노드를 순회하며 JSON 객체 생성
    for (ClassNode node : graph.getNodes().values()) {
      // --- 신규 추가: 제외 목록에 포함된 클래스는 JSON에 추가하지 않음 ---
      if (classesToExclude.contains(node.getName())) {
        continue;
      }
      // -----------------------------------------------------------

      ObjectNode classEntry = mapper.createObjectNode();
      ObjectNode classInfo = mapper.createObjectNode();

      ArrayNode dependents = mapper.createArrayNode();
      node.getDependents().stream()
          .map(ClassNode::getName)
          .filter(name -> !classesToExclude.contains(name)) // 연결된 클래스도 필터링
          .sorted().forEach(dependents::add);

      ArrayNode dependencies = mapper.createArrayNode();
      node.getDependencies().stream()
          .map(ClassNode::getName)
          .filter(name -> !classesToExclude.contains(name)) // 연결된 클래스도 필터링
          .sorted().forEach(dependencies::add);

      classInfo.set("depends", dependents);
      classInfo.set("dependencies", dependencies);

      classEntry.set(node.getName(), classInfo);
      classesArray.add(classEntry);
    }

    rootNode.set("classes", classesArray);

    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    } catch (IOException e) {
      System.err.println("JSON 생성 중 오류가 발생했습니다: " + e.getMessage());
    }
    return "";
  }
}

