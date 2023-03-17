package io.jenkins.plugins;

import hudson.model.Label;
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace;
import org.boozallen.plugins.jte.init.primitives.injectors.Keyword;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

public class env2ShellTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  echo env2shell(env)"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = "JOB_BASE_NAME='test-scripted-pipeline'";
        jenkins.assertLogContains(expectedString, completedBuild);
    }

    @Test
    public void testObjectToShellEnv()throws Exception{
        Map<String, Object> mc = new HashMap<>();
        mc.put("name", "sean");
        mc.put("age", 18);

        Map<String, Object> m = new HashMap<>();
        m.put("name", "xiaoshuo");
        m.put("age", 18);
        m.put("subMap", mc);
        // create keywords.
        TemplatePrimitiveNamespace keywords = new TemplatePrimitiveNamespace();
        keywords.setName("keywords");
        Keyword kw = new Keyword();
        kw.setName("data");
        kw.setValue(m);

        Keyword kw1 = new Keyword();
        kw1.setName("type");
        kw1.setValue("test");
        Keyword kw2 = new Keyword();
        kw2.setName("times");
        kw2.setValue(101);
        Keyword kw3 = new Keyword();
        kw3.setName("rate");
        kw3.setValue(10.111);
        Keyword kw4 = new Keyword();
        kw4.setName("text");
        kw4.setValue("nihao\nxiaoshuo");
        keywords.add(kw);
        keywords.add(kw1);
        keywords.add(kw2);
        keywords.add(kw3);
        keywords.add(kw4);

        env2Shell envInj = new env2Shell(keywords);
        String result = envInj.ObjectToShellEnv("", keywords);
        assertEquals(true, result.contains("DATA_SUBMAP_NAME='sean'"));
        assertEquals(true, result.contains("DATA_NAME='xiaoshuo'"));
        assertEquals(true, result.contains("DATA_AGE='18'"));
        assertEquals(true, result.contains("TYPE='test'"));
        assertEquals(true, result.contains("TIMES='101'"));
        assertEquals(true, result.contains("RATE='10.111'"));
        assertEquals(true, result.contains("TEXT='nihao\\nxiaoshuo'"));
    }
}
