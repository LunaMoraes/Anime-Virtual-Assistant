import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import actions.ActionManager;
import actions.ScreenAnalysisAction;
import actions.ThinkingEngine;

/**
 * Simplified AssistantCore - now only orchestrates the thinking engine and action system.
 * All heavy lifting is delegated to the ThinkingEngine and specialized Action classes.
 */
public class AssistantCore {

    private ScheduledExecutorService scheduler;
    private ActionManager actionManager;
    private ThinkingEngine thinkingEngine;

    public AssistantCore() {
        initializeActionSystem();
    }

    /**
     * Initializes the action system with all available actions.
     */
    private void initializeActionSystem() {
        actionManager = new ActionManager();
        thinkingEngine = new ThinkingEngine(actionManager);

        // Register all available actions
        actionManager.registerAction(new ScreenAnalysisAction());

        // Future actions can be registered here:
        // actionManager.registerAction(new NoteTakingAction());
        // actionManager.registerAction(new SchedulingAction());
        // actionManager.registerAction(new MaintenanceAction());

        System.out.println("Action system initialized with " +
                          actionManager.getAllActionsInfo().size() + " actions");
    }

    public void startProcessing() {
        if (AppState.isRunning) return;

        scheduler = Executors.newScheduledThreadPool(1);

        // Single scheduled task that triggers the thinking engine
        // The thinking engine will decide what actions to execute
        scheduler.scheduleAtFixedRate(
            thinkingEngine::think,
            0,
            3, // Think every 3 seconds
            TimeUnit.SECONDS
        );

        AppState.isRunning = true;
        System.out.println("AI Assistant thinking engine started.");
    }

    public void stopProcessing() {
        if (!AppState.isRunning) return;

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        AppState.isRunning = false;
        System.out.println("AI Assistant thinking engine stopped.");
    }

    /**
     * Gets the action manager for external access (e.g., for debugging or manual action execution).
     */
    public ActionManager getActionManager() {
        return actionManager;
    }

    /**
     * Gets the thinking engine for external access.
     */
    public ThinkingEngine getThinkingEngine() {
        return thinkingEngine;
    }
}
