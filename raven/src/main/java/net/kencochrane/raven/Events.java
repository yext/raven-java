package net.kencochrane.raven;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.Collections;

/**
 * Collection of builtin Raven/Sentry events.
 */
public abstract class Events {

    public enum LogLevel {
        ERROR(5);

        public int intValue;

        LogLevel(int intValue) {
            this.intValue = intValue;
        }

    }

    public static JSONObject message(String message, Object... params) {
        return message(new JSONObject(), message, params);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject message(JSONObject json, String message, Object... params) {
        JSONObject messageJson = new JSONObject();
        messageJson.put("message", message);
        JSONArray paramArray = new JSONArray();
        if (params != null) {
            paramArray.addAll(Arrays.asList(params));
        }
        messageJson.put("params", paramArray);
        json.put("sentry.interfaces.Message", messageJson);
        return json;
    }

    public static JSONObject query(String query, String engine) {
        return query(new JSONObject(), query, engine);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject query(JSONObject json, String query, String engine) {
        JSONObject content = new JSONObject();
        content.put("query", query);
        content.put("engine", engine);
        json.put("sentry.interfaces.Query", content);
        return json;
    }

    public static JSONObject exception(Throwable exception) {
        return exception(new JSONObject(), exception);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject exception(JSONObject json, Throwable exception) {
        json.put("level", LogLevel.ERROR.intValue);
        json.put("culprit", determineCulprit(exception));
        json.put("sentry.interfaces.Exception", buildException(exception));
        json.put("sentry.interfaces.Stacktrace", buildStacktrace(exception));
        return json;
    }

    /**
     * Determines the class and method name where the root cause exception occurred.
     *
     * @param exception exception
     * @return the culprit
     */
    public static String determineCulprit(Throwable exception) {
        Throwable cause = exception;
        String culprit = null;
        while (cause != null) {
            StackTraceElement[] elements = cause.getStackTrace();
            if (elements.length > 0) {
                StackTraceElement trace = elements[0];
                culprit = trace.getClassName() + "." + trace.getMethodName();
            }
            cause = cause.getCause();
        }
        return culprit;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject buildException(Throwable exception) {
        JSONObject json = new JSONObject();
        json.put("type", exception.getClass().getName());
        json.put("value", exception.getMessage());
        json.put("module", exception.getClass().getPackage().getName());
        return json;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject buildStacktrace(Throwable exception) {
        JSONArray array = new JSONArray();
        StackTraceElement[] causedTrace = null;
        Throwable cause = exception;
        while (cause != null) {
            JSONObject causedByFrame = new JSONObject();
            if (array.isEmpty()) {
                causedByFrame.put("filename", cause.toString());
            } else {
                causedByFrame.put("filename", "Caused by: " + cause.toString());
            }
            array.add(causedByFrame);

            StackTraceElement[] trace = cause.getStackTrace();
            int framesInCommon = 0;
            if (causedTrace != null) {
                int m = trace.length-1, n = causedTrace.length-1;
                while (m >= 0 && n >=0 && trace[m].equals(causedTrace[n])) {
                    m--; n--;
                }
                framesInCommon = trace.length - 1 - m;
            }

            for (int i = 0; i < trace.length; i++) {
                StackTraceElement element = trace[i];
                JSONObject frame = new JSONObject();
                frame.put("filename", "...." + element.getFileName());
                frame.put("function", element.getClassName() + "." + element.getMethodName());
                if (element.getLineNumber() > 0) {
                    frame.put("lineno", element.getLineNumber());
                }
                if (i > trace.length - 1 - framesInCommon) {
                    frame.put("in_app", false);
                } else {
                    frame.put("in_app", true);
                }
                array.add(frame);
            }

            causedTrace = trace;
            cause = cause.getCause();
        }
        Collections.reverse(array);

        JSONObject stacktrace = new JSONObject();
        stacktrace.put("frames", array);
        return stacktrace;
    }

}
