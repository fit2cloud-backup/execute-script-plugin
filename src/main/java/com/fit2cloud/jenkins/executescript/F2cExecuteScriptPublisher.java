package com.fit2cloud.jenkins.executescript;

import com.fit2cloud.sdk.Fit2CloudClient;
import com.fit2cloud.sdk.model.*;
import com.google.gson.Gson;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by zhangbohan on 15/10/23.
 */
public class F2cExecuteScriptPublisher extends Publisher{
    private final String f2cApiKey;
    private final String f2cApiSecret;
    private final String f2cRestApiEndpoint;
    private final String targetCluster;
    private final String targetClusterRole;
    private final String targetVm;
    private final String scriptName;
    private final String executeStrategy;

    private PrintStream logger;

    @DataBoundConstructor
    public F2cExecuteScriptPublisher(
                                  String targetCluster,
                                  String targetClusterRole,
                                  String targetVm,
                                  String executeStrategy,
                                  String scriptName,
                                  String f2cApiKey,
                                  String f2cApiSecret,
                                  String f2cRestApiEndpoint) {
        this.f2cApiKey = f2cApiKey;
        this.f2cApiSecret = f2cApiSecret;
        this.f2cRestApiEndpoint = f2cRestApiEndpoint;
        this.targetCluster = targetCluster;
        if(com.fit2cloud.jenkins.executescript.Utils.isNullOrEmpty(targetClusterRole)){
            this.targetClusterRole = null;
        }else{
            this.targetClusterRole = targetClusterRole;
        }
        if(Utils.isNullOrEmpty(targetVm)){
            this.targetVm = null;
        }else{
            this.targetVm = targetVm;
        }
        this.executeStrategy = executeStrategy;
        this.scriptName = scriptName;

    }


    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        this.logger = listener.getLogger();
        final boolean buildFailed = build.getResult() == Result.FAILURE;
        if (buildFailed) {
            logger.println("Job构建失败,无法执行FIT2CLOUD中的脚本.");
            return true;
        }



        if(scriptName==null){
            logger.println("脚本名称无效,无法注册到FIT2CLOUD.");
            return false;
        }

        boolean success = true;

        try {
            logger.println("开始请求在FIT2CLOUD中执行脚本...");
            Fit2CloudClient fit2CloudClient = new Fit2CloudClient(this.f2cApiKey,
                    this.f2cApiSecret,
                    this.f2cRestApiEndpoint);

            List<Script> scripts = fit2CloudClient.getScripts(null, null);
            Script script = null;
            for(Script sc:scripts){
                if(sc.getName().equals(scriptName)){
                    script = sc;
                }
            }
            if(script == null){
                this.logger.println("脚本不存在，请确认所填写的脚本名称正确.");
                return false;
            }else{
                List<Server> servers = new ArrayList<Server>();
                if(!Utils.isNullOrEmpty(targetVm)){
                    Server server = fit2CloudClient.getServer(Long.parseLong(targetVm));
                    if(server!=null){
                        servers.add(server);
                    }
                }else{
                    List<Cluster> clusters = fit2CloudClient.getClusters();
                    Cluster cluster = null;
                    for(Cluster c:clusters){
                        if(c.getName().equals(targetCluster)){
                            cluster = c;
                        }
                    }
                    if(cluster == null){
                        this.logger.println("找不到目标集群，请确认所填写的集群名称正确");
                        return false;
                    }else{
                        if(!Utils.isNullOrEmpty(targetClusterRole)){
                            List<ClusterRole> clusterRoles = fit2CloudClient.getClusterRoles(cluster.getId());
                            ClusterRole clusterRole = null;
                            for(ClusterRole cr:clusterRoles){
                                if(cr.getName().equals(targetClusterRole)){
                                    clusterRole = cr;
                                }
                            }
                            if(cluster == null){
                                this.logger.println("找不到目标集群，请确认所填写的集群名称正确");
                                return false;
                            }else{
                                servers = fit2CloudClient.getServers(null,clusterRole.getId(),"id","asc",100,1,false);
                            }


                        }else if(!Utils.isNullOrEmpty(targetCluster)){
                            servers = fit2CloudClient.getServers(cluster.getId(),null,"id","asc",100,1,false);

                        }

                    }

                }

                if(servers.size()==0){
                    this.logger.println("找不到可以执行脚本的目标虚机，无法执行脚本");
                    success = false;
                }else{
                    List<Map> events = new ArrayList<Map>();
                    if(executeStrategy.equals(Utils.ALL_AT_ONCE)){
                        this.logger.println("开始全部目标虚机上执行脚本...");
                        for(Server server:servers){
                            this.logger.println("开始在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行脚本:"+script.getName()+"!");
                            Long eventId = fit2CloudClient.executeScript(server.getId(),script.getScriptText());

                            HashMap map = new HashMap();
                            map.put("eventId",eventId);
                            map.put("server",server);
                            events.add(map);
                        }

                        int finishedEvent = events.size();
                        while(finishedEvent>0){
                            for(Map eventObj:events){
                                Event event = fit2CloudClient.getEvent((Long) eventObj.get("eventId"));
                                if (!Utils.ScriptExecutionStatus.EXPIRED.equals(event.getStatus())&&
                                        !Utils.ScriptExecutionStatus.FAILED.equals(event.getStatus())&&
                                        !Utils.ScriptExecutionStatus.SUCCESS.equals(event.getStatus())){
                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException e) {
                                    }
                                    continue;
                                }
                                Server server = (Server) eventObj.get("server");
                                this.logger.println("脚本在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行脚本的结果:");
                                List<com.fit2cloud.sdk.model.Logging> loggings =  fit2CloudClient.getLoggingsByEventId((Long) eventObj.get("eventId"));
                                for(com.fit2cloud.sdk.model.Logging log:loggings){
                                    this.logger.println(log.getMsg());
                                }
                                if(Utils.ScriptExecutionStatus.SUCCESS.equals(event.getStatus())){
                                    this.logger.println("脚本在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行成功！");
                                    finishedEvent--;
                                }
                                if(Utils.ScriptExecutionStatus.FAILED.equals(event.getStatus())){
                                    this.logger.println("脚本在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行失败！");

                                    success = false;
                                    finishedEvent--;
                                }
                                if(Utils.ScriptExecutionStatus.EXPIRED.equals(event.getStatus())){
                                    this.logger.println("脚本在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行超时！");
                                    success = false;
                                    finishedEvent--;
                                }

                            }
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                            }
                        }
                    }else if(executeStrategy.equals(Utils.ONE_AT_A_TIME)){
                        this.logger.println("开始单台依次执行脚本...");

                        for(Server server:servers){
                            Long eventId = fit2CloudClient.executeScript(server.getId(),script.getScriptText());
                            Event event = fit2CloudClient.getEvent(eventId);
                            this.logger.println("开始在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行脚本:"+script.getName()+"!");
                            while (!Utils.ScriptExecutionStatus.EXPIRED.equals(event.getStatus())&&
                                    !Utils.ScriptExecutionStatus.FAILED.equals(event.getStatus())&&
                                    !Utils.ScriptExecutionStatus.SUCCESS.equals(event.getStatus())){
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                }
                                event = fit2CloudClient.getEvent(eventId);
                            }
                            this.logger.println("脚本在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行脚本的结果:");

                            List<com.fit2cloud.sdk.model.Logging> loggings =  fit2CloudClient.getLoggingsByEventId(eventId);
                            for(com.fit2cloud.sdk.model.Logging log:loggings){
                                this.logger.println(log.getMsg());
                            }
                            if(event.getStatus().equals(Utils.ScriptExecutionStatus.SUCCESS)){
                                this.logger.println("脚本在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行成功！");

                            }
                            if(event.getStatus().equals(Utils.ScriptExecutionStatus.FAILED)){
                                this.logger.println("脚本在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行失败！");
                                this.logger.println("终止后续脚本执行");

                                success = false;
                                break;
                            }
                            if(event.getStatus().equals(Utils.ScriptExecutionStatus.EXPIRED)){
                                this.logger.println("脚本在虚机："+server.getName()+"(虚机ID为："+server.getId()+")上执行超时！");
                                this.logger.println("终止后续脚本执行");

                                success = false;
                                break;
                            }
                        }
                    }

                }

            }

        }catch (Exception e){
            this.logger.println("执行脚本过程中出现错误，错误消息如下:");
            this.logger.println(e.getMessage());
            e.printStackTrace(this.logger);
            success = false;
        }

        return success;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(F2cExecuteScriptPublisher.class);
            load();
        }



        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "FIT2CLOUD执行脚本";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindParameters(this);
            save();
            return super.configure(req, formData);
        }

        public FormValidation doCheckAccount(
                @QueryParameter String f2cApiKey,
                @QueryParameter String f2cApiSecret,
                @QueryParameter String f2cRestApiEndpoint) {
            if (StringUtils.isEmpty(f2cApiKey)) {
                return FormValidation.error("FIT2CLOUD ConsumerKey不能为空！");
            }
            if (StringUtils.isEmpty(f2cApiSecret)) {
                return FormValidation.error("FIT2CLOUD SecretKey不能为空！");
            }
            if (StringUtils.isEmpty(f2cRestApiEndpoint)) {
                return FormValidation.error("FIT2CLOUD EndPoint不能为空！");
            }
            try {
                Fit2CloudClient fit2CloudClient = new Fit2CloudClient(f2cApiKey,f2cApiSecret,f2cRestApiEndpoint);
                fit2CloudClient.getClusters();
            } catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok("验证FIT2CLOUD帐号成功！");
        }

        public FormValidation doCheckConfiguration(
                @QueryParameter String f2cApiKey,
                @QueryParameter String f2cApiSecret,
                @QueryParameter String f2cRestApiEndpoint,
                @QueryParameter String targetCluster,
                @QueryParameter String targetClusterRole,
                @QueryParameter String targetVm,
                @QueryParameter String scriptName
        ) {


            try {
                if (StringUtils.isEmpty(f2cApiKey)) {
                    return FormValidation.error("FIT2CLOUD ConsumerKey不能为空！");
                }
                if (StringUtils.isEmpty(f2cApiSecret)) {
                    return FormValidation.error("FIT2CLOUD SecretKey不能为空！");
                }
                if (StringUtils.isEmpty(f2cRestApiEndpoint)) {
                    return FormValidation.error("FIT2CLOUD EndPoint不能为空！");
                }

                Fit2CloudClient fit2CloudClient = new Fit2CloudClient(f2cApiKey,f2cApiSecret,f2cRestApiEndpoint);
                if (!StringUtils.isEmpty(targetCluster)) {
                    List<Cluster> clusters = fit2CloudClient.getClusters();
                    Cluster cluster = null;
                    for(Cluster c:clusters){
                        if(c.getName().equals(targetCluster)){
                            cluster = c;
                        }
                    }
                    if(cluster == null){
                        return FormValidation.error("目标集群不存在，请重新设置！");
                    }else{
                        if (!StringUtils.isEmpty(targetClusterRole)) {
                            List<ClusterRole> clusterRoles = fit2CloudClient.getClusterRoles(cluster.getId());
                            ClusterRole clusterRole = null;
                            for(ClusterRole cr:clusterRoles){
                                if(cr.getName().equals(targetClusterRole)){
                                    clusterRole = cr;
                                }
                            }

                            if(clusterRole == null){
                                return FormValidation.error("目标虚机组不存在，请重新设置！");
                            }
                        }
                        if (!StringUtils.isEmpty(targetVm)) {
                            Server server = fit2CloudClient.getServer(Long.parseLong(targetVm));
                            if(server == null){
                                return FormValidation.error("目标虚机不存在，请重新设置！");
                            }
                        }
                    }

                }else{
                    return FormValidation.error("目标集群不能为空！");
                }
                if (!StringUtils.isEmpty(scriptName)) {
                    List<Script> scripts = fit2CloudClient.getScripts(null, null);
                    Script script = null;
                    for(Script sc:scripts){
                        if(sc.getName().equals(scriptName)){
                            script = sc;
                        }
                    }
                    if(script == null){
                        return FormValidation.error("脚本不存在，请重新设置！");
                    }
                }

            } catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok("验证注册应用版本设置成功！");
        }



        public FormValidation doCheckTargetCluster(@QueryParameter String val)
                throws IOException, ServletException {
            if (com.fit2cloud.jenkins.executescript.Utils.isNullOrEmpty(val)){
                return FormValidation.error("目标集群不能为空！");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckScriptId(@QueryParameter String val)
                throws IOException, ServletException {
            if (com.fit2cloud.jenkins.executescript.Utils.isNullOrEmpty(val)){
                return FormValidation.error("脚本名不能为空！");
            }
            return FormValidation.ok();
        }




        public ListBoxModel doFillExecuteStrategyItems() {
            ListBoxModel items = new ListBoxModel();

            List<Map<String,String>> supportRepoList = com.fit2cloud.jenkins.executescript.Utils.getStrategyList();
            for(Map<String,String> repoType : supportRepoList){
                items.add(repoType.get("label"),repoType.get("value"));
            }
            return items;
        }


    }

    public String getF2cApiKey() {
        return f2cApiKey;
    }

    public String getF2cApiSecret() {
        return f2cApiSecret;
    }

    public String getF2cRestApiEndpoint() {
        return f2cRestApiEndpoint;
    }


    public String getTargetVm() {
        return targetVm;
    }


    public String getTargetCluster() {
        return targetCluster;
    }

    public String getTargetClusterRole() {
        return targetClusterRole;
    }

    public String getScriptName() {
        return scriptName;
    }

    public String getExecuteStrategy() {
        return executeStrategy;
    }


}
