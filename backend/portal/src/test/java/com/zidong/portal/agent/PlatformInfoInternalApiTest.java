package com.zidong.portal.agent;

import com.embabel.agent.core.Action;
import com.embabel.agent.core.ActionMetadata;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentMetadata;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.Condition;
import com.embabel.agent.core.Goal;
import com.embabel.agent.core.ToolGroupMetadata;
import com.embabel.common.ai.model.ModelMetadata;
import com.embabel.common.ai.model.ModelProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 对应 embabel 内置 HTTP 接口的内部调用验证。
 *
 * <p>HTTP 层（{@code PlatformInfoController}）只是把下面这些 bean 的返回值包了一层，
 * 因此项目内部要查同样的信息，直接注入 {@link AgentPlatform} 与 {@link ModelProvider} 即可，
 * 不必绕 HTTP，也不受 /api/** 鉴权与白名单影响。</p>
 *
 * <table>
 *   <tr><th>HTTP 接口</th><th>内部 API</th></tr>
 *   <tr><td>GET /api/v1/platform-info/agents</td><td>{@code agentPlatform.agents()} + {@code new AgentMetadata(agent)}</td></tr>
 *   <tr><td>GET /api/v1/platform-info</td><td>{@code agentPlatform.agents()/actions/goals/conditions/domainTypes} + {@code modelProvider.listModels()} + {@code toolGroupResolver.availableToolGroups()}</td></tr>
 *   <tr><td>GET /api/v1/platform-info/actions</td><td>{@code agentPlatform.getActions()}</td></tr>
 *   <tr><td>GET /api/v1/platform-info/goals</td><td>{@code agentPlatform.getGoals()}</td></tr>
 *   <tr><td>GET /api/v1/platform-info/conditions</td><td>{@code agentPlatform.getConditions()}</td></tr>
 *   <tr><td>GET /api/v1/platform-info/models</td><td>{@code modelProvider.listModels()}</td></tr>
 *   <tr><td>GET /api/v1/platform-info/tool-groups</td><td>{@code agentPlatform.getToolGroupResolver().availableToolGroups()}</td></tr>
 * </table>
 *
 * <p>测试用随机 gRPC 端口（{@code portal.rpc.port=0}），可以与本机已启动的 portal 共存。</p>
 */
@SpringBootTest(properties = {
        "portal.rpc.port=0",
        "logging.level.com.embabel=warn"
})
class PlatformInfoInternalApiTest {

    @Autowired
    private AgentPlatform agentPlatform;

    @Autowired
    private ModelProvider modelProvider;

    /**
     * 对应 GET /api/v1/platform-info/agents。
     * HTTP 层返回 {@code List<AgentMetadata>}，这里直接解析同一个对象。
     */
    @Test
    @DisplayName("agents: agentPlatform.agents() -> AgentMetadata")
    void agents() {
        List<Agent> agents = agentPlatform.agents();
        System.out.println("========== /agents ==========");
        System.out.println("agentCount = " + agents.size());

        for (Agent agent : agents) {
            AgentMetadata meta = new AgentMetadata(agent);
            // version 是 Kotlin value class（Semver），字节码里的 getter 被改名为 getVersion-Id9oKnY()，
            // Java 源码无法直接调用，这里用反射取；不需要版本时也可以省掉
            System.out.printf("agent: name=%s | provider=%s | version=%s | description=%s%n",
                    meta.getName(), meta.getProvider(), versionOf(meta), meta.getDescription());

            for (ActionMetadata action : meta.getActions()) {
                System.out.printf("   action: %s | %s | inputs=%d | outputs=%d%n",
                        action.getName(), action.getDescription(),
                        action.getInputs().size(), action.getOutputs().size());
            }
            for (Goal goal : meta.getGoals()) {
                System.out.printf("   goal: %s | %s | tags=%s | examples=%s%n",
                        goal.getName(), goal.getDescription(), goal.getTags(), goal.getExamples());
            }
            for (String condition : meta.getConditions()) {
                System.out.printf("   condition: %s%n", condition);
            }
        }

        assertNotNull(agents, "agents() 不应返回 null");
    }

    /**
     * 对应 GET /api/v1/platform-info（平台汇总）。
     * HTTP 层的 PlatformInfoSummary 就是下面这些值的拼装。
     */
    @Test
    @DisplayName("platform-info: agents/actions/goals/conditions/domainTypes + models + toolGroups")
    void platformInfo() {
        List<Agent> agents = agentPlatform.agents();
        List<Action> actions = agentPlatform.getActions();
        List<Goal> goals = agentPlatform.getGoals().stream().toList();
        List<Condition> conditions = agentPlatform.getConditions().stream().toList();
        List<ModelMetadata> models = modelProvider.listModels();
        List<ToolGroupMetadata> toolGroups = agentPlatform.getToolGroupResolver().availableToolGroups();

        System.out.println("========== /platform-info ==========");
        System.out.printf("name=%s | description=%s%n", agentPlatform.getName(), agentPlatform.getDescription());
        System.out.printf("agentCount=%d | agentNames=%s%n", agents.size(),
                agents.stream().map(Agent::getName).toList());
        System.out.printf("actionCount=%d | goalCount=%d | conditionCount=%d | domainTypeCount=%d%n",
                actions.size(), goals.size(), conditions.size(), agentPlatform.getDomainTypes().size());
        System.out.printf("domainTypes=%s%n",
                agentPlatform.getDomainTypes().stream().map(d -> d.getName()).toList());
        System.out.printf("modelCount=%d | toolGroupCount=%d%n", models.size(), toolGroups.size());

        assertNotNull(agentPlatform.getName(), "平台名称不应为 null");
    }

    /**
     * 对应 GET /api/v1/platform-info/actions。
     */
    @Test
    @DisplayName("actions: agentPlatform.getActions()")
    void actions() {
        List<Action> actions = agentPlatform.getActions();

        System.out.println("========== /actions ==========");
        System.out.println("actionCount = " + actions.size());
        for (Action action : actions) {
            ActionMetadata meta = new ActionMetadata(action);
            System.out.printf("action: %s | %s | inputs=%s | outputs=%s%n",
                    meta.getName(), meta.getDescription(), meta.getInputs(), meta.getOutputs());
        }

        assertNotNull(actions, "actions 不应为 null");
    }

    /**
     * 对应 GET /api/v1/platform-info/goals。
     */
    @Test
    @DisplayName("goals: agentPlatform.getGoals()")
    void goals() {
        var goals = agentPlatform.getGoals();

        System.out.println("========== /goals ==========");
        System.out.println("goalCount = " + goals.size());
        for (Goal goal : goals) {
            System.out.printf("goal: %s | %s | tags=%s | examples=%s%n",
                    goal.getName(), goal.getDescription(), goal.getTags(), goal.getExamples());
        }

        assertNotNull(goals, "goals 不应为 null");
    }

    /**
     * 对应 GET /api/v1/platform-info/conditions。
     * HTTP 层会把 Condition 收敛成 ConditionMetadata(name, cost)。
     */
    @Test
    @DisplayName("conditions: agentPlatform.getConditions()")
    void conditions() {
        var conditions = agentPlatform.getConditions();

        System.out.println("========== /conditions ==========");
        System.out.println("conditionCount = " + conditions.size());
        for (Condition condition : conditions) {
            System.out.printf("condition: name=%s | cost=%s%n",
                    condition.getName(), String.valueOf(condition.getCost()));
        }

        assertNotNull(conditions, "conditions 不应为 null");
    }

    /**
     * 对应 GET /api/v1/platform-info/models。
     * 这里只包含 embabel 自己管理的模型（规划 / 排序 / 关键词映射），
     * 不含 portal 直连的 langchain4j 模型（portal.ai.chat）。
     */
    @Test
    @DisplayName("models: modelProvider.listModels()")
    void models() {
        List<ModelMetadata> models = modelProvider.listModels();

        System.out.println("========== /models ==========");
        System.out.println("modelCount = " + models.size());
        for (ModelMetadata model : models) {
            System.out.printf("model: name=%s | provider=%s | detail=%s%n",
                    model.getName(), model.getProvider(), model);
        }

        assertNotNull(models, "models 不应为 null");
    }

    /**
     * 对应 GET /api/v1/platform-info/tool-groups。
     */
    @Test
    @DisplayName("tool-groups: agentPlatform.getToolGroupResolver().availableToolGroups()")
    void toolGroups() {
        List<ToolGroupMetadata> toolGroups = agentPlatform.getToolGroupResolver().availableToolGroups();

        System.out.println("========== /tool-groups ==========");
        System.out.println("toolGroupCount = " + toolGroups.size());
        for (ToolGroupMetadata group : toolGroups) {
            System.out.printf("toolGroup: name=%s | provider=%s | description=%s | detail=%s%n",
                    group.getName(), group.getProvider(), group.getDescription(), group);
        }

        assertNotNull(toolGroups, "toolGroups 不应为 null");
    }

    /**
     * 取 AgentMetadata 的 version。
     *
     * <p>{@code version} 的类型 Semver 是 Kotlin value class，编译后属性 getter 被改名为
     * {@code getVersion-Id9oKnY()}，Java 源码里写不出这个方法名（含 '-'），只能反射调用。</p>
     */
    private static String versionOf(AgentMetadata meta) {
        try {
            return String.valueOf(AgentMetadata.class.getMethod("getVersion-Id9oKnY").invoke(meta));
        } catch (ReflectiveOperationException ex) {
            return "unknown";
        }
    }
}
