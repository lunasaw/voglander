package io.github.lunasaw.voglander.client.domain.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** Successful Handler result. Domain completion data is consumed only inside the completion transaction. */
public final class TaskExecutionResult {
    private final TaskResultReference resultReference;
    private final JSONObject resultSummary;
    private final JSONObject completionData;
    private final TaskCompensation compensation;

    public TaskExecutionResult(TaskResultReference resultReference, JSONObject resultSummary,
        JSONObject completionData, TaskCompensation compensation) {
        this.resultReference = resultReference;
        this.resultSummary = deepCopy(resultSummary);
        this.completionData = deepCopy(completionData);
        this.compensation = compensation == null ? TaskCompensation.noop() : compensation;
    }

    public static TaskExecutionResult success(TaskResultReference reference, JSONObject summary) {
        return new TaskExecutionResult(reference, summary, null, null);
    }

    public TaskResultReference resultReference() { return resultReference; }
    public JSONObject resultSummary() { return deepCopy(resultSummary); }
    public JSONObject completionData() { return deepCopy(completionData); }
    public TaskCompensation compensation() { return compensation; }

    private static JSONObject deepCopy(JSONObject source) {
        return source == null ? new JSONObject() : JSON.parseObject(JSON.toJSONString(source));
    }
}
