package io.harness.generator;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.OrchestrationVisualizationTestBase;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.beans.GraphVertex;
import io.harness.beans.converter.GraphVertexConverter;
import io.harness.beans.internal.EdgeListInternal;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.category.element.UnitTests;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.pms.execution.ExecutionMode;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.skip.SkipType;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.utils.DummyForkStepParameters;
import io.harness.utils.DummyOutcome;
import io.harness.utils.DummySectionStepParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Test class for {@link OrchestrationAdjacencyListGenerator}
 */
public class OrchestrationAdjacencyListGeneratorTest extends OrchestrationVisualizationTestBase {
  private static final String PLAN_EXECUTION_ID = "planId";
  private static final String STARTING_EXECUTION_NODE_ID = "startID";

  private static final StepType DUMMY_STEP_TYPE = StepType.builder().type("DUMMY").build();

  @Mock private OutcomeService outcomeService;
  @InjectMocks @Inject private OrchestrationAdjacencyListGenerator orchestrationAdjacencyListGenerator;

  @Before
  public void setUp() {
    when(outcomeService.findAllByRuntimeId(anyString(), anyString()))
        .thenReturn(Collections.singletonList(new DummyOutcome("outcome")));
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGenerateOrchestrationAdjacencyListInternalWithSection() {
    NodeExecution dummyStart =
        NodeExecution.builder()
            .uuid("node1")
            .ambiance(
                Ambiance.builder()
                    .planExecutionId(PLAN_EXECUTION_ID)
                    .levels(Collections.singletonList(Level.builder().setupId(STARTING_EXECUTION_NODE_ID).build()))
                    .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid(STARTING_EXECUTION_NODE_ID)
                      .name("name")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("identifier1")
                      .build())
            .nextId("node2")
            .build();
    StepParameters sectionStepParams = DummySectionStepParameters.builder().childNodeId("child_section_2").build();
    NodeExecution section =
        NodeExecution.builder()
            .uuid("node2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("section_2").build()))
                          .build())
            .mode(ExecutionMode.CHILD)
            .node(PlanNode.builder()
                      .uuid("section_2")
                      .name("name2")
                      .stepType(StepType.builder().type("SECTION").build())
                      .identifier("identifier2")
                      .stepParameters(sectionStepParams)
                      .build())
            .resolvedStepParameters(sectionStepParams)
            .previousId(dummyStart.getUuid())
            .build();
    NodeExecution sectionChild =
        NodeExecution.builder()
            .uuid("node_child_2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("child_section_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("child_section_2")
                      .name("name_child_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("identifier_child_2")
                      .build())
            .parentId(section.getUuid())
            .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(dummyStart, section, sectionChild);

    OrchestrationAdjacencyListInternal adjacencyList =
        orchestrationAdjacencyListGenerator.generateAdjacencyList(dummyStart.getUuid(), nodeExecutions, false);

    assertThat(adjacencyList).isNotNull();

    assertThat(adjacencyList.getGraphVertexMap().size()).isEqualTo(3);
    assertThat(adjacencyList.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(dummyStart.getUuid(), section.getUuid(), sectionChild.getUuid());

    assertThat(adjacencyList.getAdjacencyMap().size()).isEqualTo(3);
    assertThat(adjacencyList.getAdjacencyMap().get(dummyStart.getUuid()).getNextIds())
        .containsExactlyInAnyOrder(section.getUuid());
    assertThat(adjacencyList.getAdjacencyMap().get(section.getUuid()).getEdges())
        .containsExactlyInAnyOrder(sectionChild.getUuid());
    assertThat(adjacencyList.getAdjacencyMap().get(sectionChild.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.getAdjacencyMap().get(sectionChild.getUuid()).getNextIds()).isEmpty();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGenerateOrchestrationAdjacencyListInternalWithChildChain() {
    String dummyNode1Uuid = "dummyNode1";
    String dummyNode2Uuid = "dummyNode2";

    NodeExecution sectionChainParentNode =
        NodeExecution.builder()
            .uuid("section_chain_start")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("section_chain_plan_node").build()))
                          .build())
            .mode(ExecutionMode.CHILD_CHAIN)
            .node(PlanNode.builder()
                      .uuid("section_chain_plan_node")
                      .name("name_section_chain")
                      .identifier("name_section_chain")
                      .stepType(StepType.builder().type("_DUMMY_SECTION_CHAIN").build())
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();

    NodeExecution sectionChain1 =
        NodeExecution.builder()
            .uuid("section_chain_child1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(
                              Level.builder().setupId("section_chain_child1_plan_node").build()))
                          .build())
            .mode(ExecutionMode.TASK)
            .node(PlanNode.builder()
                      .uuid("section_chain_child1_plan_node")
                      .name("name_section_chain_child1_plan_node")
                      .identifier("name_section_chain_child1_plan_node")
                      .stepType(DUMMY_STEP_TYPE)
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .parentId(sectionChainParentNode.getUuid())
            .nextId(dummyNode1Uuid)
            .build();

    NodeExecution sectionChain2 =
        NodeExecution.builder()
            .uuid("section_chain_child2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(
                              Level.builder().setupId("section_chain_child2_plan_node").build()))
                          .build())
            .mode(ExecutionMode.TASK)
            .node(PlanNode.builder()
                      .uuid("section_chain_child2_plan_node")
                      .name("name_section_chain_child2_plan_node")
                      .identifier("name_section_chain_child2_plan_node")
                      .stepType(DUMMY_STEP_TYPE)
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .parentId(sectionChainParentNode.getUuid())
            .nextId(dummyNode2Uuid)
            .build();

    NodeExecution dummyNode1 =
        NodeExecution.builder()
            .uuid(dummyNode1Uuid)
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("dummy_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("dummy_plan_node_1")
                      .name("name_dummy_node_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_dummy_node_1")
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .parentId(sectionChainParentNode.getUuid())
            .previousId(sectionChain1.getUuid())
            .build();

    NodeExecution dummyNode2 =
        NodeExecution.builder()
            .uuid(dummyNode2Uuid)
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("dummy_plan_node_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("dummy_plan_node_2")
                      .name("name_dummy_node_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_dummy_node_2")
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .parentId(sectionChainParentNode.getUuid())
            .previousId(sectionChain2.getUuid())
            .build();

    List<NodeExecution> nodeExecutions =
        Lists.newArrayList(sectionChainParentNode, sectionChain1, sectionChain2, dummyNode1, dummyNode2);

    OrchestrationAdjacencyListInternal adjacencyList = orchestrationAdjacencyListGenerator.generateAdjacencyList(
        sectionChainParentNode.getUuid(), nodeExecutions, true);
    assertThat(adjacencyList).isNotNull();

    assertThat(adjacencyList.getGraphVertexMap().size()).isEqualTo(5);
    assertThat(adjacencyList.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(sectionChainParentNode.getUuid(), sectionChain1.getUuid(), sectionChain2.getUuid(),
            dummyNode1.getUuid(), dummyNode2.getUuid());

    assertThat(adjacencyList.getAdjacencyMap().size()).isEqualTo(5);
    assertThat(adjacencyList.getAdjacencyMap().get(sectionChainParentNode.getUuid()).getEdges())
        .containsExactlyInAnyOrder(sectionChain1.getUuid());
    assertThat(adjacencyList.getAdjacencyMap().get(sectionChain1.getUuid()).getNextIds())
        .containsExactlyInAnyOrder(dummyNode1.getUuid());
    assertThat(adjacencyList.getAdjacencyMap().get(dummyNode1.getUuid()).getNextIds())
        .containsExactlyInAnyOrder(sectionChain2.getUuid());
    assertThat(adjacencyList.getAdjacencyMap().get(sectionChain2.getUuid()).getNextIds())
        .containsExactlyInAnyOrder(dummyNode2.getUuid());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGenerateOrchestrationGraphWithFork() {
    StepParameters forkStepParams =
        DummyForkStepParameters.builder().parallelNodeId("parallel_node_1").parallelNodeId("parallel_node_2").build();
    NodeExecution fork =
        NodeExecution.builder()
            .uuid("node1")
            .ambiance(
                Ambiance.builder()
                    .planExecutionId(PLAN_EXECUTION_ID)
                    .levels(Collections.singletonList(Level.builder().setupId(STARTING_EXECUTION_NODE_ID).build()))
                    .build())
            .mode(ExecutionMode.CHILDREN)
            .node(PlanNode.builder()
                      .uuid(STARTING_EXECUTION_NODE_ID)
                      .name("name1")
                      .stepType(StepType.builder().type("DUMMY_FORK").build())
                      .identifier("identifier1")
                      .stepParameters(forkStepParams)
                      .build())
            .resolvedStepParameters(forkStepParams)
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode1 =
        NodeExecution.builder()
            .uuid("parallel_node_1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_1")
                      .name("name_children_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_1")
                      .build())
            .parentId(fork.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode2 =
        NodeExecution.builder()
            .uuid("parallel_node_2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_2")
                      .name("name_children_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_2")
                      .build())
            .parentId(fork.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(fork, parallelNode1, parallelNode2);

    OrchestrationAdjacencyListInternal adjacencyList =
        orchestrationAdjacencyListGenerator.generateAdjacencyList(fork.getUuid(), nodeExecutions, false);
    assertThat(adjacencyList).isNotNull();

    assertThat(adjacencyList.getGraphVertexMap().size()).isEqualTo(3);
    assertThat(adjacencyList.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(fork.getUuid(), parallelNode1.getUuid(), parallelNode2.getUuid());

    assertThat(adjacencyList.getAdjacencyMap().get(fork.getUuid())).isNotNull();
    assertThat(adjacencyList.getAdjacencyMap().get(fork.getUuid()).getNextIds()).isEmpty();
    assertThat(adjacencyList.getAdjacencyMap().get(fork.getUuid()).getEdges())
        .containsExactlyInAnyOrder(parallelNode1.getUuid(), parallelNode2.getUuid());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGeneratePartialOrchestrationGraphWithFork() {
    NodeExecution dummyNode1 =
        NodeExecution.builder()
            .uuid("dummy_node_1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("dummy_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("dummy_plan_node_1")
                      .name("dummy_node_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("dummy_node_1")
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    StepParameters forkStepParams =
        DummyForkStepParameters.builder().parallelNodeId("parallel_node_1").parallelNodeId("parallel_node_2").build();
    NodeExecution fork =
        NodeExecution.builder()
            .uuid("node1")
            .ambiance(
                Ambiance.builder()
                    .planExecutionId(PLAN_EXECUTION_ID)
                    .levels(Collections.singletonList(Level.builder().setupId(STARTING_EXECUTION_NODE_ID).build()))
                    .build())
            .mode(ExecutionMode.CHILDREN)
            .node(PlanNode.builder()
                      .uuid(STARTING_EXECUTION_NODE_ID)
                      .name("name1")
                      .stepType(StepType.builder().type("DUMMY_FORK").build())
                      .identifier("identifier1")
                      .stepParameters(forkStepParams)
                      .build())
            .resolvedStepParameters(forkStepParams)
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode1 =
        NodeExecution.builder()
            .uuid("parallel_node_1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_1")
                      .name("name_children_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_1")
                      .build())
            .parentId(fork.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode2 =
        NodeExecution.builder()
            .uuid("parallel_node_2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_2")
                      .name("name_children_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_2")
                      .build())
            .parentId(fork.getUuid())
            .nextId(dummyNode1.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(fork, parallelNode1, parallelNode2, dummyNode1);

    OrchestrationAdjacencyListInternal adjacencyList =
        orchestrationAdjacencyListGenerator.generateAdjacencyList(parallelNode2.getUuid(), nodeExecutions, true);
    assertThat(adjacencyList).isNotNull();

    assertThat(adjacencyList.getGraphVertexMap().size()).isEqualTo(2);
    assertThat(adjacencyList.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(parallelNode2.getUuid(), dummyNode1.getUuid());

    assertThat(adjacencyList.getAdjacencyMap().get(parallelNode2.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.getAdjacencyMap().get(parallelNode2.getUuid()).getNextIds())
        .containsExactlyInAnyOrder(dummyNode1.getUuid());

    assertThat(adjacencyList.getAdjacencyMap().get(dummyNode1.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.getAdjacencyMap().get(dummyNode1.getUuid()).getNextIds()).isEmpty();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestPopulateAdjacencyListWhenPreviousIdIsPresent() {
    NodeExecution dummyNode1 =
        NodeExecution.builder()
            .uuid("dummy_node_1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("dummy_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("dummy_plan_node_1")
                      .name("dummy_node_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("dummy_node_1")
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution dummyNode2 =
        NodeExecution.builder()
            .uuid("dummy_node_2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("dummy_plan_node_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("dummy_plan_node_2")
                      .name("dummy_node_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("dummy_node_2")
                      .build())
            .previousId(dummyNode1.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();

    Map<String, GraphVertex> graphVertexMap = new HashMap<>();
    graphVertexMap.put(dummyNode1.getUuid(), GraphVertexConverter.convertFrom(dummyNode1));

    Map<String, EdgeListInternal> adjacencyListMap = new HashMap<>();
    adjacencyListMap.put(dummyNode1.getUuid(),
        EdgeListInternal.builder()
            .edges(new ArrayList<>())
            .nextIds(new ArrayList<>())
            .prevIds(new ArrayList<>())
            .parentId(null)
            .build());

    OrchestrationAdjacencyListInternal orchestrationAdjacencyListInternal = OrchestrationAdjacencyListInternal.builder()
                                                                                .graphVertexMap(graphVertexMap)
                                                                                .adjacencyMap(adjacencyListMap)
                                                                                .build();

    orchestrationAdjacencyListGenerator.populateAdjacencyList(
        orchestrationAdjacencyListInternal, Lists.newArrayList(dummyNode2));

    assertThat(orchestrationAdjacencyListInternal).isNotNull();
    assertThat(orchestrationAdjacencyListInternal.getGraphVertexMap().size()).isEqualTo(2);

    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(dummyNode1.getUuid()).getNextIds())
        .containsExactlyInAnyOrder(dummyNode2.getUuid());
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(dummyNode1.getUuid()).getEdges()).isEmpty();
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(dummyNode1.getUuid()).getPrevIds()).isEmpty();
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(dummyNode1.getUuid()).getParentId()).isNull();

    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(dummyNode2.getUuid()).getParentId()).isNull();
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(dummyNode2.getUuid()).getPrevIds())
        .containsExactlyInAnyOrder(dummyNode1.getUuid());
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(dummyNode2.getUuid()).getNextIds()).isEmpty();
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(dummyNode2.getUuid()).getEdges()).isEmpty();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestPopulateAdjacencyListForChain() {
    String dummyNode1Uuid = "dummyNode1 ";
    NodeExecution sectionChainParentNode = NodeExecution.builder()
                                               .uuid("section_chain_start")
                                               .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                               .mode(ExecutionMode.CHILD_CHAIN)
                                               .node(PlanNode.builder()
                                                         .uuid("section_chain_plan_node")
                                                         .name("name_section_chain")
                                                         .identifier("name_section_chain")
                                                         .stepType(StepType.builder().type("SECTION_CHAIN").build())
                                                         .build())
                                               .createdAt(System.currentTimeMillis())
                                               .lastUpdatedAt(System.currentTimeMillis())
                                               .build();

    NodeExecution sectionChain1 = NodeExecution.builder()
                                      .uuid("section_chain_child1")
                                      .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                      .mode(ExecutionMode.TASK)
                                      .node(PlanNode.builder()
                                                .uuid("section_chain_child1_plan_node")
                                                .name("name_section_chain_child1_plan_node")
                                                .identifier("name_section_chain_child1_plan_node")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .build())
                                      .createdAt(System.currentTimeMillis())
                                      .lastUpdatedAt(System.currentTimeMillis())
                                      .parentId(sectionChainParentNode.getUuid())
                                      .nextId(dummyNode1Uuid)
                                      .build();

    NodeExecution sectionChain2 = NodeExecution.builder()
                                      .uuid("section_chain_child2")
                                      .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                      .mode(ExecutionMode.TASK)
                                      .node(PlanNode.builder()
                                                .uuid("section_chain_child2_plan_node")
                                                .name("name_section_chain_child2_plan_node")
                                                .identifier("name_section_chain_child2_plan_node")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .build())
                                      .createdAt(System.currentTimeMillis())
                                      .lastUpdatedAt(System.currentTimeMillis())
                                      .parentId(sectionChainParentNode.getUuid())
                                      .build();

    NodeExecution dummyNode1 = NodeExecution.builder()
                                   .uuid(dummyNode1Uuid)
                                   .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid("dummy_plan_node_1")
                                             .name("name_dummy_node_1")
                                             .stepType(StepType.builder().type("DUMMY").build())
                                             .identifier("name_dummy_node_1")
                                             .build())
                                   .createdAt(System.currentTimeMillis())
                                   .lastUpdatedAt(System.currentTimeMillis())
                                   .parentId(sectionChainParentNode.getUuid())
                                   .previousId(sectionChain1.getUuid())
                                   .build();

    Map<String, GraphVertex> graphVertexMap = new HashMap<>();
    graphVertexMap.put(sectionChainParentNode.getUuid(), GraphVertexConverter.convertFrom(sectionChainParentNode));
    graphVertexMap.put(sectionChain1.getUuid(), GraphVertexConverter.convertFrom(sectionChain1));
    graphVertexMap.put(dummyNode1.getUuid(), GraphVertexConverter.convertFrom(dummyNode1));

    Map<String, EdgeListInternal> adjacencyListMap = new HashMap<>();
    adjacencyListMap.put(sectionChainParentNode.getUuid(),
        EdgeListInternal.builder()
            .edges(Lists.newArrayList(sectionChain1.getUuid()))
            .nextIds(new ArrayList<>())
            .prevIds(new ArrayList<>())
            .parentId(null)
            .build());
    adjacencyListMap.put(sectionChain1.getUuid(),
        EdgeListInternal.builder()
            .edges(new ArrayList<>())
            .nextIds(Lists.newArrayList(dummyNode1.getUuid()))
            .parentId(sectionChainParentNode.getUuid())
            .prevIds(new ArrayList<>())
            .build());
    adjacencyListMap.put(dummyNode1.getUuid(),
        EdgeListInternal.builder()
            .edges(new ArrayList<>())
            .nextIds(new ArrayList<>())
            .prevIds(Lists.newArrayList(sectionChain1.getUuid()))
            .parentId(null)
            .build());

    OrchestrationAdjacencyListInternal orchestrationAdjacencyListInternal = OrchestrationAdjacencyListInternal.builder()
                                                                                .graphVertexMap(graphVertexMap)
                                                                                .adjacencyMap(adjacencyListMap)
                                                                                .build();

    orchestrationAdjacencyListGenerator.populateAdjacencyList(
        orchestrationAdjacencyListInternal, Lists.newArrayList(sectionChain2));

    assertThat(orchestrationAdjacencyListInternal).isNotNull();

    assertThat(orchestrationAdjacencyListInternal.getGraphVertexMap().size()).isEqualTo(4);
    assertThat(orchestrationAdjacencyListInternal.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(
            sectionChainParentNode.getUuid(), sectionChain1.getUuid(), sectionChain2.getUuid(), dummyNode1.getUuid());
    assertThat(
        orchestrationAdjacencyListInternal.getGraphVertexMap().get(sectionChainParentNode.getUuid()).getSkipType())
        .isEqualTo(SkipType.NOOP);

    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().size()).isEqualTo(4);
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(sectionChainParentNode.getUuid()).getEdges())
        .containsExactlyInAnyOrder(sectionChain1.getUuid());
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(sectionChain1.getUuid()).getNextIds())
        .containsExactlyInAnyOrder(dummyNode1.getUuid());
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(dummyNode1.getUuid()).getNextIds())
        .containsExactlyInAnyOrder(sectionChain2.getUuid());
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(sectionChain2.getUuid()).getNextIds())
        .isEmpty();
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(sectionChain2.getUuid()).getPrevIds())
        .containsExactlyInAnyOrder(dummyNode1Uuid);
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(sectionChain2.getUuid()).getParentId())
        .isEqualTo(sectionChainParentNode.getUuid());
    assertThat(orchestrationAdjacencyListInternal.getAdjacencyMap().get(sectionChain2.getUuid()).getEdges()).isEmpty();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestPopulateAdjacencyListForFork() {
    StepParameters forkStepParams =
        DummyForkStepParameters.builder().parallelNodeId("parallel_node_1").parallelNodeId("parallel_node_2").build();
    NodeExecution fork =
        NodeExecution.builder()
            .uuid("node1")
            .ambiance(
                Ambiance.builder()
                    .planExecutionId(PLAN_EXECUTION_ID)
                    .levels(Collections.singletonList(Level.builder().setupId(STARTING_EXECUTION_NODE_ID).build()))
                    .build())
            .mode(ExecutionMode.CHILDREN)
            .node(PlanNode.builder()
                      .uuid(STARTING_EXECUTION_NODE_ID)
                      .name("name1")
                      .stepType(StepType.builder().type("DUMMY_FORK").build())
                      .identifier("identifier1")
                      .stepParameters(forkStepParams)
                      .build())
            .resolvedStepParameters(forkStepParams)
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode1 =
        NodeExecution.builder()
            .uuid("parallel_node_1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_1")
                      .name("name_children_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_1")
                      .build())
            .parentId(fork.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode2 =
        NodeExecution.builder()
            .uuid("parallel_node_2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_2")
                      .name("name_children_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_2")
                      .build())
            .parentId(fork.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();

    Map<String, GraphVertex> graphVertexMap = new HashMap<>();
    graphVertexMap.put(fork.getUuid(), GraphVertexConverter.convertFrom(fork));
    graphVertexMap.put(parallelNode1.getUuid(), GraphVertexConverter.convertFrom(parallelNode1));

    Map<String, EdgeListInternal> adjacencyListMap = new HashMap<>();
    adjacencyListMap.put(fork.getUuid(),
        EdgeListInternal.builder()
            .edges(Lists.newArrayList(parallelNode1.getUuid()))
            .nextIds(new ArrayList<>())
            .parentId(null)
            .prevIds(new ArrayList<>())
            .build());
    adjacencyListMap.put(parallelNode1.getUuid(),
        EdgeListInternal.builder()
            .edges(new ArrayList<>())
            .nextIds(new ArrayList<>())
            .prevIds(new ArrayList<>())
            .parentId(fork.getUuid())
            .build());

    OrchestrationAdjacencyListInternal orchestrationAdjacencyListInternal = OrchestrationAdjacencyListInternal.builder()
                                                                                .graphVertexMap(graphVertexMap)
                                                                                .adjacencyMap(adjacencyListMap)
                                                                                .build();

    orchestrationAdjacencyListGenerator.populateAdjacencyList(
        orchestrationAdjacencyListInternal, Lists.newArrayList(parallelNode2));

    assertThat(orchestrationAdjacencyListInternal).isNotNull();

    assertThat(orchestrationAdjacencyListInternal.getGraphVertexMap().size()).isEqualTo(3);
    assertThat(orchestrationAdjacencyListInternal.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(fork.getUuid(), parallelNode1.getUuid(), parallelNode2.getUuid());

    Map<String, EdgeListInternal> adjacencyList = orchestrationAdjacencyListInternal.getAdjacencyMap();
    assertThat(adjacencyList).isNotNull();
    assertThat(adjacencyList.get(fork.getUuid()).getEdges())
        .containsExactlyInAnyOrder(parallelNode1.getUuid(), parallelNode2.getUuid());
    assertThat(adjacencyList.get(parallelNode1.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.get(parallelNode1.getUuid()).getNextIds()).isEmpty();
    assertThat(adjacencyList.get(parallelNode2.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.get(parallelNode2.getUuid()).getNextIds()).isEmpty();
    assertThat(adjacencyList.get(parallelNode2.getUuid()).getPrevIds()).isEmpty();
    assertThat(adjacencyList.get(parallelNode2.getUuid()).getParentId()).isEqualTo(fork.getUuid());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGeneratePartialAdjacencyListWithFork() {
    GraphVertex fork = GraphVertex.builder()
                           .uuid("fork1")
                           .planNodeId(STARTING_EXECUTION_NODE_ID)
                           .mode(ExecutionMode.CHILDREN)
                           .lastUpdatedAt(System.currentTimeMillis())
                           .build();
    GraphVertex parallelNode1 = GraphVertex.builder()
                                    .uuid("parallel_node_1")
                                    .planNodeId("parallel_plan_node_1")
                                    .mode(ExecutionMode.SYNC)
                                    .lastUpdatedAt(System.currentTimeMillis())
                                    .build();
    GraphVertex parallelNode2 = GraphVertex.builder()
                                    .uuid("parallel_node_2")
                                    .planNodeId("parallel_plan_node_2")
                                    .mode(ExecutionMode.SYNC)
                                    .lastUpdatedAt(System.currentTimeMillis())
                                    .build();
    GraphVertex dummyNode1 = GraphVertex.builder()
                                 .uuid("dummy_node_1")
                                 .planNodeId("dummy_plan_node_1")
                                 .mode(ExecutionMode.SYNC)
                                 .lastUpdatedAt(System.currentTimeMillis())
                                 .build();
    List<GraphVertex> vertices = Lists.newArrayList(fork, parallelNode1, parallelNode2, dummyNode1);

    Map<String, GraphVertex> graphVertexMap =
        vertices.stream().collect(Collectors.toMap(GraphVertex::getUuid, Function.identity()));
    Map<String, EdgeListInternal> adjacencyMap = new HashMap<>();
    adjacencyMap.put(fork.getUuid(),
        EdgeListInternal.builder()
            .edges(Arrays.asList(parallelNode1.getUuid(), parallelNode2.getUuid()))
            .nextIds(new ArrayList<>())
            .prevIds(new ArrayList<>())
            .parentId(null)
            .build());
    adjacencyMap.put(parallelNode1.getUuid(),
        EdgeListInternal.builder()
            .edges(new ArrayList<>())
            .nextIds(new ArrayList<>())
            .prevIds(new ArrayList<>())
            .parentId(fork.getUuid())
            .build());
    adjacencyMap.put(parallelNode2.getUuid(),
        EdgeListInternal.builder()
            .edges(new ArrayList<>())
            .nextIds(Collections.singletonList(dummyNode1.getUuid()))
            .prevIds(new ArrayList<>())
            .parentId(fork.getUuid())
            .build());
    adjacencyMap.put(dummyNode1.getUuid(),
        EdgeListInternal.builder()
            .edges(new ArrayList<>())
            .nextIds(new ArrayList<>())
            .prevIds(Collections.singletonList(parallelNode2.getUuid()))
            .parentId(null)
            .build());

    OrchestrationAdjacencyListInternal listInternal =
        OrchestrationAdjacencyListInternal.builder().graphVertexMap(graphVertexMap).adjacencyMap(adjacencyMap).build();

    OrchestrationAdjacencyListInternal adjacencyList =
        orchestrationAdjacencyListGenerator.generatePartialAdjacencyList(parallelNode2.getUuid(), listInternal);
    assertThat(adjacencyList).isNotNull();

    assertThat(adjacencyList.getGraphVertexMap().size()).isEqualTo(2);
    assertThat(adjacencyList.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(parallelNode2.getUuid(), dummyNode1.getUuid());

    assertThat(adjacencyList.getAdjacencyMap().get(parallelNode2.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.getAdjacencyMap().get(parallelNode2.getUuid()).getNextIds())
        .containsExactlyInAnyOrder(dummyNode1.getUuid());

    assertThat(adjacencyList.getAdjacencyMap().get(dummyNode1.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.getAdjacencyMap().get(dummyNode1.getUuid()).getNextIds()).isEmpty();
  }
}
