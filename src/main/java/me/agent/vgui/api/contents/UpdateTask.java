package me.agent.vgui.api.contents;

/**
 * Handle for a repeating update task registered with
 * {@link ViewContents#schedule}. Tasks are cancelled automatically when the
 * view closes; use this to cancel earlier.
 */
public interface UpdateTask {

    /** Stops the task. Safe to call multiple times. */
    void cancel();

    /** Whether the task has been cancelled (explicitly or by the view closing). */
    boolean isCancelled();
}
