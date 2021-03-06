package org.jenkinsci.plugins.typetalk.webhookaction.executorimpl;

import hudson.model.*;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.typetalk.webhookaction.ResponseParameter;
import org.jenkinsci.plugins.typetalk.webhookaction.WebhookExecutor;
import org.jenkinsci.plugins.typetalk.webhookaction.WebhookRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildExecutor extends WebhookExecutor {

    private String project;

    private List<String> parameters;

    private Map<String, String> parameterMap = new HashMap<>();

    public BuildExecutor(WebhookRequest req, StaplerResponse rsp, String project, List<String> parameters) {
        super(req, rsp, "build");
        this.project = project;
        this.parameters = parameters;

        parseParameters();
    }

    private void parseParameters() {
        for (String parameter : parameters) {
            String[] splitParameter = parameter.split("=");

            if (splitParameter.length == 2) {
                parameterMap.put(splitParameter[0], splitParameter[1]);
            }
        }
    }

    /**
     * @see hudson.model.AbstractProject#doBuild
     * @see hudson.model.AbstractProject#doBuildWithParameters
     */
    @Override
    public void execute() {
        TopLevelItem item = Jenkins.getInstance().getItem(project);
        if (!(item instanceof Job)) {
            outputError(new ResponseParameter("Project [ " + project + " ] is not found"));
            return;
        }
        Job project = ((Job) item);

        if (!project.hasPermission(Item.BUILD)) {
            String name = Jenkins.getAuthentication().getName();
            outputError(new ResponseParameter(String.format("Project [ %s ] cannot be built by '%s'", this.project, name)));
            return;
        }

        if (!(project instanceof ParameterizedJobMixIn.ParameterizedJob)) {
            String name = Jenkins.getAuthentication().getName();
            outputError(new ResponseParameter(String.format("Project [ %s ] cannot be built by '%s'", this.project, name)));
            return;
        }
        ParameterizedJobMixIn.ParameterizedJob job = ((ParameterizedJobMixIn.ParameterizedJob) project);

        Jenkins.getInstance().getQueue().schedule(job, job.getQuietPeriod(), getParametersAction(project), getCauseAction());

        ResponseParameter responseParameter = new ResponseParameter("Project [ " + this.project + " ] has been scheduled");
        responseParameter.setProject(project);
        output(responseParameter);
    }

    private Action getParametersAction(Job project) {
        ParametersDefinitionProperty property = (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);
        if (property == null) {
            return null;
        }

        List<ParameterValue> values = new ArrayList<>();
        List<ParameterDefinition> pds = property.getParameterDefinitions();

        if (isOnlySingleParameter(pds)) {

            ParameterDefinition pd = pds.get(0);
            if (!(pd instanceof SimpleParameterDefinition)) {
                return null;
            }
            SimpleParameterDefinition spd = (SimpleParameterDefinition) pd;
            values.add(spd.createValue(parameters.get(0)));

        } else {

            for (ParameterDefinition pd : pds) {
                if (!(pd instanceof SimpleParameterDefinition)) {
                    continue;
                }
                SimpleParameterDefinition spd = (SimpleParameterDefinition) pd;

                if (parameterMap.containsKey(spd.getName())) {
                    String parameterValue = parameterMap.get(spd.getName());
                    values.add(spd.createValue(parameterValue));
                } else {
                    ParameterValue defaultValue = spd.getDefaultParameterValue();
                    if (defaultValue != null) {
                        values.add(defaultValue);
                    }
                }
            }

        }

        return new ParametersAction(values);
    }

    private boolean isOnlySingleParameter(List<ParameterDefinition> pds) {
        return pds.size() == 1 && parameters.size() == 1 &&
                !parameters.get(0).contains("="); // when parameter contains '=', parameter is handled at the other code
    }

    private Action getCauseAction() {
        return new CauseAction(new Cause.RemoteCause(req.getRemoteAddr(), "by Typetalk Webhook"));
    }

}
