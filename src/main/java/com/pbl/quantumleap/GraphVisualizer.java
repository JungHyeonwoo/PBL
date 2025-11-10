package com.pbl.quantumleap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pbl.quantumleap.model.DependencyGraph;
import com.pbl.quantumleap.model.DependencyGraph.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphVisualizer {

  /**
   * 변경점 분석과 전체 클래스 탐색 기능을 모두 포함하는 통합 인터랙티브 HTML 리포트를 생성합니다.
   * @param graph 분석된 의존성 그래프
   * @param changedClasses 변경된 클래스 이름 Set
   * @param impactSet 영향을 받는 모든 클래스 이름 Set
   * @param outputDir HTML 파일이 저장될 디렉토리
   */
  public void generateInteractiveReport(DependencyGraph graph, Set<String> changedClasses, Set<String> impactSet, String aiAnalysisResult, String outputDir) {
    String graphJson = graphToJson(graph);
    StringBuilder htmlContent = new StringBuilder();

    htmlContent.append(generateHtmlHeader());

    // 탭 UI 구조 생성
    htmlContent.append("""
            <div class="tabs">
                <button id="impact-tab-btn" class="tab-button active">영향 분석 뷰</button>
                <button id="explorer-tab-btn" class="tab-button">전체 탐색 뷰</button>
                <button id="ai-tab-btn" class="tab-button">AI 분석 뷰</button>
            </div>
        """);

    // 탭 1: 영향 분석 뷰
    htmlContent.append("<div id=\"impact-view\" class=\"tab-content\" style=\"display:block;\">");
    if (changedClasses.isEmpty()) {
      htmlContent.append("<p class=\"initial-message\">분석할 변경점이 없습니다.</p>");
    } else {
      for (String className : changedClasses) {
        ClassNode startNode = graph.getNode(className);
        if (startNode != null) {
          htmlContent.append("<h2><i class=\"fas fa-file-code\"></i> 변경된 클래스: ").append(className).append("</h2>");
          htmlContent.append("<div class=\"tree\"><ul>");
          htmlContent.append(buildTreeHtmlForImpact(startNode, impactSet, new HashSet<>()));
          htmlContent.append("</ul></div>");
        }
      }
    }
    htmlContent.append("</div>");

    // 탭 2: 전체 탐색 뷰
    htmlContent.append("<div id=\"explorer-view\" class=\"tab-content\">");
    htmlContent.append("""
            <div class="controls">
               <label for="classSelector"><b>클래스 탐색기:</b> </label>
               <select id="classSelector">
                   <option value="">-- 클래스를 선택하여 전체 연관 관계를 확인하세요 --</option>
        """);
    List<String> classNames = new ArrayList<>(graph.getNodes().keySet());
    classNames.sort(String.CASE_INSENSITIVE_ORDER);
    for (String className : classNames) {
      htmlContent.append("<option value=\"").append(className).append("\">").append(className).append("</option>");
    }
    htmlContent.append("""
               </select>
            </div>
            <div id="explorer-tree-container">
                <p class="initial-message">클래스를 선택하여 연관 관계를 확인하세요.</p>
            </div>
        """);
    htmlContent.append("</div>");
    // 탭 3: AI 분석 뷰
    htmlContent.append("<div id=\"ai-view\" class=\"tab-content\">");
    htmlContent.append("<h2><i class=\"fas fa-brain\"></i> AI 아키텍처 건전성 분석 (GPT-4)</h2>");

    String safeAiResult = aiAnalysisResult
        .replace("<", "&lt;")
        .replace(">", "&gt;");
    htmlContent.append("<pre class=\"ai-report\">").append(safeAiResult).append("</pre>");
    htmlContent.append("</div>");

    htmlContent.append(generateHtmlFooter(graphJson));

    try {
      Path outputPath = Paths.get(outputDir, "interactive-report.html");
      Files.createDirectories(outputPath.getParent());
      Files.writeString(outputPath, htmlContent.toString());
      System.out.println("✅ 통합 인터랙티브 리포트 HTML 파일이 생성되었습니다: " + outputPath);
    } catch (IOException e) {
      System.err.println("❌ HTML 파일 생성 중 오류가 발생했습니다: " + e.getMessage());
    }
  }

  private String buildTreeHtmlForImpact(ClassNode node, Set<String> impactSet, Set<ClassNode> visited) {
    if (node == null || visited.contains(node)) return "";
    visited.add(node);
    StringBuilder sb = new StringBuilder();
    sb.append("<li><span class=\"impacted\">").append(node.getName()).append("</span>");
    Set<ClassNode> dependentsToRender = node.getDependents().stream()
        .filter(d -> impactSet.contains(d.getName()))
        .collect(Collectors.toSet());
    if (!dependentsToRender.isEmpty()) {
      sb.append("<ul>");
      dependentsToRender.forEach(d -> sb.append(buildTreeHtmlForImpact(d, impactSet, visited)));
      sb.append("</ul>");
    }
    sb.append("</li>");
    return sb.toString();
  }

  /**
   * DependencyGraph를 상위/하위 의존성 관계를 모두 포함하는 JSON으로 변환합니다.
   */
  private String graphToJson(DependencyGraph graph) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();
    for (ClassNode node : graph.getNodes().values()) {
      ObjectNode classInfo = mapper.createObjectNode();

      ArrayNode dependencies = mapper.createArrayNode();
      node.getDependencies().stream().map(ClassNode::getName).sorted().forEach(dependencies::add);

      ArrayNode dependents = mapper.createArrayNode();
      node.getDependents().stream().map(ClassNode::getName).sorted().forEach(dependents::add);

      classInfo.set("dependencies", dependencies); // 하위 구조
      classInfo.set("dependents", dependents);     // 상위 구조

      rootNode.set(node.getName(), classInfo);
    }
    try {
      return mapper.writeValueAsString(rootNode);
    } catch (IOException e) {
      return "{}";
    }
  }

  private String generateHtmlHeader() {
    return """
               <!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
               <title>QuantumLeap - 통합 분석 리포트</title>
               <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
               <style>
                   body { font-family: sans-serif; padding: 20px; background-color: #f9fafb; color: #1f2937; }
                   h1, h2, h3 { color: #111827; }
                   .tabs { border-bottom: 2px solid #e5e7eb; margin-bottom: 20px; }
                   .tab-button { background-color: transparent; border: none; padding: 10px 15px; cursor: pointer; font-size: 16px; font-weight: 600; color: #6b7280; }
                   .tab-button.active { color: #4f46e5; border-bottom: 2px solid #4f46e5; }
                   .tab-content { display: none; }
                   .controls { margin-bottom: 20px; }
                   #classSelector { padding: 8px; border-radius: 4px; border: 1px solid #d1d5db; min-width: 300px; }
                   .tree ul { padding-left: 20px; position: relative; } .tree li { list-style-type: none; position: relative; padding: 5px 0 5px 25px; }
                   .tree li::before, .tree li::after { content: ''; position: absolute; left: 0; }
                   .tree li::before { border-left: 1px solid #d1d5db; height: 100%; width: 1px; top: -12px; }
                   .tree li::after { border-top: 1px solid #d1d5db; height: 1px; width: 20px; top: 15px; }
                   .tree > ul > li::before { border-left: none; } .tree li:last-child::before { height: 28px; }
                   .tree span { padding: 4px 8px; border-radius: 4px; display: inline-block; }
                   .tree span.base { background-color: #e5e7eb; }
                   .tree span.impacted { background-color: #fef3c7; border: 1px solid #fcd34d; font-weight: bold; }
                   .initial-message { color: #6b7280; padding: 20px; }
                   .ai-report {
                                   background-color: #ffffff; /* 흰색 배경 */
                                   border: 1px solid #d1d5db; /* gray-300 */
                                   border-radius: 8px;
                                   padding: 16px;
                                   font-family: monospace, sans-serif;
                                   white-space: pre-wrap; /* 줄바꿈 및 공백 유지 */
                                   word-wrap: break-word; /* 긴 줄바꿈 처리 */
                                   line-height: 1.6;
                                   color: #374151; /* gray-700 */
                               }
               </style></head><body>
               <h1><i class="fas fa-sitemap"></i> QuantumLeap 통합 분석 리포트</h1>
               """;
  }

  private String generateHtmlFooter(String graphJson) {
    return String.format("""
            <!-- Data Island for JSON data -->
            <script id="graph-data" type="application/json">%s</script>
        
            <script>
                // --- Functions ---
                function openTab(evt, tabName) {
                    let i, tabcontent, tablinks;
                    tabcontent = document.getElementsByClassName("tab-content");
                    for (i = 0; i < tabcontent.length; i++) { tabcontent[i].style.display = "none"; }
                    tablinks = document.getElementsByClassName("tab-button");
                    for (i = 0; i < tablinks.length; i++) { tablinks[i].className = tablinks[i].className.replace(" active", ""); }
                    document.getElementById(tabName).style.display = "block";
                    evt.currentTarget.className += " active";
                }
        
                function buildJsTree(nodes, graphData) {
                    if (!nodes || nodes.length === 0) return '<ul><li><span class="base">없음</span></li></ul>';
                    let html = '<ul>';
                    for (const nodeName of nodes) {
                        html += `<li><span class="base">${nodeName}</span>`;
                        html += '</li>';
                    }
                    html += '</ul>';
                    return html;
                }
        
                // --- Event Listeners ---
                document.addEventListener('DOMContentLoaded', function() {
                    const graphData = JSON.parse(document.getElementById('graph-data').textContent);
        
                    document.getElementById('impact-tab-btn').addEventListener('click', (e) => openTab(e, 'impact-view'));
                    document.getElementById('explorer-tab-btn').addEventListener('click', (e) => openTab(e, 'explorer-view'));
                    document.getElementById('ai-tab-btn').addEventListener('click', (e) => openTab(e, 'ai-view'));
        
                    const selector = document.getElementById('classSelector');
                    selector.addEventListener('change', (event) => {
                        const selectedClass = event.target.value;
                        const container = document.getElementById('explorer-tree-container');
        
                        if (selectedClass && graphData[selectedClass]) {
                            const classInfo = graphData[selectedClass];
                            const dependentsTree = buildJsTree(classInfo.dependents, graphData);
                            const dependenciesTree = buildJsTree(classInfo.dependencies, graphData);
        
                            const treeHtml = `
                                <h2><i class="fas fa-search"></i> 탐색된 클래스: <span class="impacted">${selectedClass}</span></h2>
                                <h3 style="margin-top: 1rem;"><i class="fas fa-arrow-up" style="color: #6366f1;"></i> 상위 구조 (이 클래스를 사용하는 클래스)</h3>
                                <div class="tree">${dependentsTree}</div>
                                <h3 style="margin-top: 1rem;"><i class="fas fa-arrow-down" style="color: #f43f5e;"></i> 하위 구조 (이 클래스가 사용하는 클래스)</h3>
                                <div class="tree">${dependenciesTree}</div>
                            `;
                            container.innerHTML = treeHtml;
                        } else {
                            container.innerHTML = '<p class="initial-message">클래스를 선택하여 연관 관계를 확인하세요.</p>';
                        }
                    });
                });
            </script>
        </body></html>
        """, graphJson);
  }
}

