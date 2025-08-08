package actions;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import levels.LevelManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Contributes the levels-related task (prompt + payload) to the ActionContext so that
 * ScreenAnalysisAction can send a unified request to the LLM covering all tasks.
 */
public class LevelsTaskAction implements Action {
    private static final Gson GSON = new Gson();

    @Override
    public String getActionId() {
        return "levels_task";
    }

    @Override
    public String getDescription() {
        return "Adds the levels system task and payload for the unified LLM call";
    }

    @Override
    public boolean canExecute(ActionContext context) {
        return true; // Always able to contribute the task
    }

    @Override
    public ActionResult execute(ActionContext context) {
        // Build the dynamic payload containing attributes and skills
        Map<String, Object> payload = new HashMap<>();
        payload.put("available_attributes", LevelManager.getAttributes());
        // Provide both a list of names and a detailed map for LLM clarity
        java.util.Map<String, levels.SkillInfo> skillsDetail = LevelManager.getAvailableSkills();
        payload.put("skills_detail", skillsDetail);
        payload.put("current_skills", new java.util.ArrayList<>(skillsDetail.keySet()));
        payload.put("attributes_xp", LevelManager.getAttributeXp());

        // Compose content strictly from configuration files and data; SA will just append this
        StringBuilder other = context.contains("other_task_content") ? context.get("other_task_content", StringBuilder.class) : new StringBuilder();

        // Global tasks instruction from system.json
        String tasksInstruction = config.ConfigurationManager.getTasksInstruction();
        if (tasksInstruction != null && !tasksInstruction.isBlank()) {
            other.append(tasksInstruction).append("\n\n");
        }

        // data/levels/prompts.json sections
        try {
            java.nio.file.Path p = java.nio.file.Path.of("data", "levels", "prompts.json");
            if (java.nio.file.Files.exists(p)) {
                String content = java.nio.file.Files.readString(p);
                JsonObject obj = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
                if (obj.has("basic_prompt")) other.append(obj.get("basic_prompt").getAsString()).append("\n");
                if (obj.has("actions_tools")) other.append(obj.get("actions_tools").getAsString()).append("\n");
            }
        } catch (Exception ignored) {}

        // DATA (JSON) section
        JsonObject data = new JsonObject();
        data.add("levels", com.google.gson.JsonParser.parseString(GSON.toJson(payload)));
        other.append("DATA (JSON): ").append(data.toString()).append("\n");

        context.put("other_task_content", other);
        return ActionResult.success("Levels task content added");
    }
}
