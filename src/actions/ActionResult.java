package actions;

/**
 * Represents the result of an action execution.
 */
public class ActionResult {
    public enum Status {
        SUCCESS,
        FAILURE,
        SKIPPED
    }

    private final Status status;
    private final String message;
    private final Object data;

    private ActionResult(Status status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static ActionResult success() {
        return new ActionResult(Status.SUCCESS, null, null);
    }

    public static ActionResult success(String message) {
        return new ActionResult(Status.SUCCESS, message, null);
    }

    public static ActionResult success(Object data) {
        return new ActionResult(Status.SUCCESS, null, data);
    }

    public static ActionResult success(String message, Object data) {
        return new ActionResult(Status.SUCCESS, message, data);
    }

    public static ActionResult failure(String message) {
        return new ActionResult(Status.FAILURE, message, null);
    }

    public static ActionResult skipped(String reason) {
        return new ActionResult(Status.SKIPPED, reason, null);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    public boolean isSkipped() {
        return status == Status.SKIPPED;
    }
}
