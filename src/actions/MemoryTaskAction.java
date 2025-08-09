package actions;

import com.google.gson.Gson;
import config.ConfigurationManager;
import core.AppState;
import personality.PersonalityManager;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Contributes memory task content and data to the unified prompt or runs standalone when chat is throttled.
 */
public class MemoryTaskAction implements Action {
    private static final String ID = "memory_task";

    @Override
    public String getActionId() {
        return ID;
    }

    @Override
    public String getDescription() {
        return "Prepares memory maintenance instructions and current memory payload for the LLM.";
    }

    @Override
    public boolean canExecute(ActionContext context) {
        return AppState.isRunning; // lightweight
    }

    @Override
    public ActionResult execute(ActionContext context) {
        try {
            String instructions = loadMemoryPrompts();
            if (instructions == null || instructions.isBlank()) {
                return ActionResult.failure("memory prompts not found");
            }

            // Build DATA payload
            Map<String, Object> data = new HashMap<>();
            data.put("last_five_phrases", PersonalityManager.getLastResponses());
            // For now we keep memories in ConfigurationManager (stub), can be moved to a file/store later
            data.put("short_term_memory", config.MemoryStore.getShortTerm());
            data.put("long_term_memory", config.MemoryStore.getLongTerm());

            String tasksInstruction = ConfigurationManager.getTasksInstruction();
            boolean willRunSA = context.contains("will_run_screen_analysis") && Boolean.TRUE.equals(context.get("will_run_screen_analysis", Boolean.class));

            if (willRunSA) {
                // Contribute to the unified prompt
                StringBuilder sb = context.contains("other_task_content") ? context.get("other_task_content", StringBuilder.class) : new StringBuilder();
                if (sb == null) sb = new StringBuilder();
                sb.append(tasksInstruction).append('\n');
                sb.append("\n--- MEMORY TASK ---\n");
                sb.append(instructions).append('\n');
                sb.append("\nDATA=\n");
                sb.append(new Gson().toJson(data));
                sb.append("\n--- END OF MEMORY TASK ---\n");
                context.put("other_task_content", sb);
                return ActionResult.success("memory task prepared");
            } else {
                // Run standalone: call analysis model directly using the instructions and data
                StringBuilder finalPrompt = new StringBuilder();
                finalPrompt.append(tasksInstruction).append('\n');
                finalPrompt.append(instructions).append('\n');
                finalPrompt.append("\nDATA=\n");
                finalPrompt.append(new Gson().toJson(data));

                String raw = api.ApiClient.generateResponse(finalPrompt.toString());
                handleMemorySections(raw);
                return ActionResult.success("memory task executed standalone");
            }
        } catch (Exception e) {
            return ActionResult.failure("error preparing memory task: " + e.getMessage());
        }
    }

    private String loadMemoryPrompts() {
        File f = new File("data/memory/prompts.json");
        if (!f.exists()) return null;
        try (FileReader r = new FileReader(f)) {
            java.util.Map<?,?> m = new Gson().fromJson(r, java.util.Map.class);
            StringBuilder s = new StringBuilder();
            Object basic = m.get("basic_prompt");
            Object tools = m.get("actions_tools");
            if (basic != null) s.append(basic).append('\n');
            if (tools != null) s.append(tools).append('\n');
            return s.length() > 0 ? s.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void handleMemorySections(String raw) {
        if (raw == null) return;
        int idx = 0;
        while ((idx = raw.indexOf('[', idx)) != -1) {
            int end = raw.indexOf(']', idx + 1);
            if (end == -1) break;
            String inside = raw.substring(idx + 1, end).trim();
            String lower = inside.toLowerCase();
            if (!lower.startsWith("memory:")) { idx = end + 1; continue; }
            String cmd = inside.substring("memory:".length()).trim();
            if (cmd.startsWith("write_short_term")) {
                int lp = cmd.indexOf('('), rp = cmd.lastIndexOf(')');
                if (lp != -1 && rp > lp) {
                    String payload = cmd.substring(lp + 1, rp).trim();
                    payload = stripWrappingQuotes(payload);
                    config.MemoryStore.setShortTerm(payload);
                    System.out.println("Updated short-term memory.");
                }
            } else if (cmd.startsWith("write_long_term")) {
                int lp = cmd.indexOf('('), rp = cmd.lastIndexOf(')');
                if (lp != -1 && rp > lp) {
                    String payload = cmd.substring(lp + 1, rp).trim();
                    payload = stripWrappingQuotes(payload);
                    config.MemoryStore.setLongTerm(payload);
                    System.out.println("Updated long-term memory.");
                }
            }
            idx = end + 1;
        }
    }

    private String stripWrappingQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
