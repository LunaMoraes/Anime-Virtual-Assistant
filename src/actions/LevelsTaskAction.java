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
public class LevelsTaskAction implements BracketAwareAction {
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

    // BracketAwareAction
    @Override
    public java.util.List<String> getBracketPrefixes() {
        return java.util.List.of("levels:");
    }

    @Override
    public void handleBracket(String content, ActionContext context) {
        try {
            String lower = content; // case-sensitive check required by spec
            if (!lower.startsWith("levels:")) return;
            String cmd = content.substring("levels:".length()).trim();
            // Allow optional wrapping parentheses: [levels:(add_exp_on_skill(...))]
            if (cmd.startsWith("(") && cmd.endsWith(")") && cmd.length() > 2) {
                cmd = cmd.substring(1, cmd.length() - 1).trim();
            }
            if (cmd.startsWith("add_exp_on_skill")) {
                int lp = cmd.indexOf('('), rp = cmd.lastIndexOf(')');
                if (lp != -1 && rp > lp) {
                    String arg = cmd.substring(lp + 1, rp).trim();
                    String skill = stripQuotes(arg);
                    System.out.println("Dispatch: levels.addExpOnSkill(" + skill + ")");
                    levels.LevelManager.addExpOnSkill(skill, 1);
                }
            } else if (cmd.startsWith("add_skill")) {
                int lp = cmd.indexOf('('), rp = cmd.lastIndexOf(')');
                if (lp != -1 && rp > lp) {
                    String args = cmd.substring(lp + 1, rp);
                    String[] parts = args.split(",");
                    String skill = parts.length > 0 ? stripQuotes(parts[0].trim()) : null;
                    String attr = parts.length > 1 ? stripQuotes(parts[1].trim()) : null;
                    System.out.println("Dispatch: levels.addSkill(" + skill + ", " + attr + ")");
                    levels.LevelManager.addSkill(skill, attr);
                }
            }
        } catch (Exception ignored) {}
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
