package actions;

/**
 * Interface for all assistant actions.
 * Each action represents a specific capability of the virtual assistant.
 */
public interface Action {

    /**
     * Executes the action.
     * @param context The execution context containing relevant data
     * @return ActionResult indicating success/failure and any output data
     */
    ActionResult execute(ActionContext context);

    /**
     * Gets the unique identifier for this action.
     */
    String getActionId();

    /**
     * Gets a human-readable description of what this action does.
     */
    String getDescription();

    /**
     * Checks if this action can be executed given the current context.
     */
    boolean canExecute(ActionContext context);
}
