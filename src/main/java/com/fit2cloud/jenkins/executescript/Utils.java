package com.fit2cloud.jenkins.executescript;

import com.fit2cloud.sdk.model.ApplicationDeployPolicyType;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhangbohan on 15/9/25.
 */
public class Utils {
    public static final String FWD_SLASH = "/";
    public static final String ALL_AT_ONCE = "allatonce";
    public static final String ONE_AT_A_TIME = "oneatatime";

    public class ScriptExecutionStatus {
        public static final String PENDING = "pending";
        public static final String ONGOING = "ongoing";
        public static final String SUCCESS = "success";
        public static final String FAILED = "failed";
        public static final String EXPIRED = "expired";
    }


    public static boolean isNullOrEmpty(final String name) {
        boolean isValid = false;
        if (name == null || name.matches("\\s*")) {
            isValid = true;
        }
        return isValid;
    }

    public static boolean isNumber(final String name) {
        boolean isNumber = false;
        if (name == null || name.matches("[0-9]+")) {
            isNumber = true;
        }
        return isNumber;
    }

    public static String replaceTokens(AbstractBuild<?, ?> build,
                                       BuildListener listener, String text) throws IOException,
            InterruptedException {
        String newText = null;
        if (!isNullOrEmpty(text)) {
            Map<String, String> envVars = build.getEnvironment(listener);
            newText = Util.replaceMacro(text, envVars);
        }
        return newText;
    }


    public static List<Map<String,String>> getStrategyList(){
        List<Map<String,String>> repoList = new ArrayList<Map<String,String>>();
        Map repo = new HashMap();
        repo.put("label","全部同时执行");
        repo.put("value", Utils.ALL_AT_ONCE);
        Map repo1 = new HashMap();
        repo1.put("label","单台依次执行");
        repo1.put("value", Utils.ONE_AT_A_TIME);
        repoList.add(repo);
        repoList.add(repo1);
        return repoList;
    }

}
