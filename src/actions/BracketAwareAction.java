package actions;

import java.util.List;

/**
 * Optional capability for actions that can handle bracketed commands
 * like [levels:...], [memory:...]. The matching is case-sensitive.
 */
public interface BracketAwareAction extends Action {

    /**
     * Returns the list of prefixes this action can handle, e.g. "levels:", "memory:".
     * Matching must be case-sensitive.
     */
    List<String> getBracketPrefixes();

    /**
     * Handle a single bracket content without the surrounding brackets, e.g.
     * "levels:add_skill('X','Y')". Implementations should ignore content that
     * does not start with one of their prefixes.
     */
    void handleBracket(String content, ActionContext context);
}
