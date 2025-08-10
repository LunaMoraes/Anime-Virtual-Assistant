package actions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all available actions for the virtual assistant.
 * Provides registration, execution, and lifecycle management for actions.
 */
public class ActionManager {

    private final Map<String, Action> registeredActions = new ConcurrentHashMap<>();
    private final ActionContext globalContext = new ActionContext();

    /**
     * Registers a new action with the manager.
     */
    public void registerAction(Action action) {
        registeredActions.put(action.getActionId(), action);
        System.out.println("Registered action: " + action.getActionId() + " - " + action.getDescription());
    }

    /**
     * Executes an action by its ID.
     */
    public ActionResult executeAction(String actionId) {
        return executeAction(actionId, new ActionContext());
    }

    /**
     * Executes an action with a specific context.
     */
    public ActionResult executeAction(String actionId, ActionContext context) {
        Action action = registeredActions.get(actionId);
        if (action == null) {
            return ActionResult.failure("Action not found: " + actionId);
        }

        if (!action.canExecute(context)) {
            return ActionResult.skipped("Action cannot be executed: " + actionId);
        }

        try {
            return action.execute(context);
        } catch (Exception e) {
            return ActionResult.failure("Action execution failed: " + e.getMessage());
        }
    }

    /**
     * Gets all available actions that can be executed with the current context.
     */
    public List<String> getAvailableActions() {
        return getAvailableActions(globalContext);
    }

    /**
     * Gets all available actions that can be executed with the given context.
     */
    public List<String> getAvailableActions(ActionContext context) {
        return registeredActions.values().stream()
                .filter(action -> action.canExecute(context))
                .map(Action::getActionId)
                .sorted()
                .toList();
    }

    /**
     * Gets information about a specific action.
     */
    public String getActionInfo(String actionId) {
        Action action = registeredActions.get(actionId);
        if (action == null) {
            return "Action not found: " + actionId;
        }
        return action.getActionId() + ": " + action.getDescription();
    }

    /**
     * Gets information about all registered actions.
     */
    public List<String> getAllActionsInfo() {
        return registeredActions.values().stream()
                .map(action -> action.getActionId() + ": " + action.getDescription())
                .sorted()
                .toList();
    }

    /**
     * Gets the global context that persists across action executions.
     */
    public ActionContext getGlobalContext() {
        return globalContext;
    }

    /**
     * Returns the live collection of registered Action instances.
     */
    public java.util.Collection<Action> getRegisteredActions() {
        return registeredActions.values();
    }

    /**
     * Checks if an action is registered.
     */
    public boolean hasAction(String actionId) {
        return registeredActions.containsKey(actionId);
    }

    /**
     * Unregisters an action.
     */
    public boolean unregisterAction(String actionId) {
        Action removed = registeredActions.remove(actionId);
        if (removed != null) {
            System.out.println("Unregistered action: " + actionId);
            return true;
        }
        return false;
    }
}
