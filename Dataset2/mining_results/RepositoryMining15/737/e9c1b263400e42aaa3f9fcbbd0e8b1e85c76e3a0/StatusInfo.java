package hudson.plugins.build_publisher;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *  Represents status of build publishing
 */
public class StatusInfo {

    State state;
    Exception exception;
    String text;
    String serverName;

    public StatusInfo(StatusInfo.State state, String text, String serverName,
            Exception exception) {
        this.exception = exception;
        this.text = text;
        this.state = state;
        this.serverName = serverName;
    }

    public State getState() {
        return state;
    }

    public Exception getException() {
        return exception;
    }

    public String getText() {
        return text;
    }

    public String getServerName() {
        return serverName;
    }

    public String getStackTrace() {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static enum State {
        PENDING, INPROGRESS, SUCCESS, FAILURE, FAILURE_PENDING, INTERRUPTED
    }

}