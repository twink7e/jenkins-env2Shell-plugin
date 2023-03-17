package io.jenkins.plugins;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.NonNull;
import groovy.lang.GString;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive;
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace;
import org.boozallen.plugins.jte.init.primitives.injectors.Keyword;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class env2Shell extends Step implements Serializable {
    private Object data;
    private String scriptText;
    private MyExecution execution;

    @DataBoundConstructor
    public env2Shell(Object data){
        this.data = data;
    }

    @Override
    public StepDescriptor getDescriptor() {
        return new DescriptorImpl();
    }

    public String getResult() throws IOException, InterruptedException {
        if (data instanceof EnvActionImpl){
            EnvActionImpl envAction = (EnvActionImpl) data;
            if (envAction != null) {
                EnvVars envVars = envAction.getEnvironment();
                Map map = (Map)envVars;
                data = map;
            } else {
                return "";
            }
        }
        return ObjectToShellEnv("", data);
    }

    public static String escapeForShell(String s) {
        // Escape special characters in the string
        String escaped = s.replace("\\", "\\\\").replace("\n", "\\n");
//                .replace("\"", "\\\"")
//                .replace("$", "\\$")
//                .replace("`", "\\`");
//        escaped = escaped.replaceAll("\n", "\\\\n");
        // Wrap the string in single quotes to avoid shell expansion
        return "'" + escaped.replace("'", "'\\''") + "'";
    }

    public String ObjectToShellEnv(String prefix, Object raw)  {
        StringBuilder envScript = new StringBuilder();
        if (raw instanceof Map){
            Map<Object, Object> obj = (Map<Object, Object>) raw;
            for (Map.Entry<Object, Object> entry : obj.entrySet()) {
                if (!(entry.getKey() instanceof String)){
                    continue;
                }
                String newPrefix =
                        prefix.length() > 0 ? String.format("%s_%s", prefix, entry.getKey()): (String)entry.getKey();
                envScript.append(ObjectToShellEnv(newPrefix, entry.getValue()));
            }
        }else if (raw instanceof String ||
                raw instanceof GString ||
                raw instanceof Number ||
                raw.getClass().isPrimitive()) {
            var tmp = escapeForShell(raw.toString());
            envScript.append(String.format("export %s=%s\n", prefix.toUpperCase(), tmp));

            // templating-engine-plugin.
        }else if (raw instanceof TemplatePrimitiveNamespace){
            List<TemplatePrimitive> primitives = ((TemplatePrimitiveNamespace) raw).getPrimitives();
            for (int i=0; i<primitives.size(); i++) {
                envScript.append(ObjectToShellEnv(prefix, primitives.get(i)));
            }
        }else if (raw instanceof GlobalVariable){
            // like string.
            if (raw instanceof Keyword){
                Keyword kw = (Keyword) raw;
                String newPrefix = prefix.length() > 0 ? String.format("%s_%s", prefix, kw.getName()): kw.getName();
                envScript.append(ObjectToShellEnv(newPrefix, kw.getValue()));
            }
        }

        return envScript.toString();
    }


    @Override
    public StepExecution start(StepContext context) throws Exception {
        this.execution =  new MyExecution(this, context);
        return this.execution;
    }

    private static class MyExecution extends SynchronousStepExecution<String> implements Serializable {
        private env2Shell converter;
        protected MyExecution(@NonNull env2Shell converter, @NonNull StepContext context) {
            super(context);
            this.converter = converter;
        }


        @Override
        protected String run() throws Exception {
            return this.converter.getResult();
        }

    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "env2shell";
        }
    }
}
