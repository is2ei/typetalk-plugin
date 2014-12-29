package org.jenkinsci.plugins.typetalk.webhookaction.executorimpl;

import org.jenkinsci.plugins.typetalk.webhookaction.WebhookExecutor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

public class UndefinedExecutor extends WebhookExecutor {

    private static final Logger LOGGER = Logger.getLogger(UndefinedExecutor.class.getName());

    public UndefinedExecutor(StaplerRequest req, StaplerResponse rsp, String command) {
        super(req, rsp, command);
    }

    @Override
    public void execute() {
        String message = "command '" + command + "' is not defined";
        LOGGER.warning(message);

        rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        writer.println(message);
    }
}