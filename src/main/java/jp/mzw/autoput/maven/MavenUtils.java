package jp.mzw.autoput.maven;

import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenUtils {
    static Logger LOGGER = LoggerFactory.getLogger(MavenUtils.class);

    public static final String FILENAME_POM = "pom.xml";

    public static int maven(String project, File subject, List<String> goal, File mavenHome, String autoPutTest) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(subject, FILENAME_POM));
        request.setGoals(goal);
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setOutputHandler(new InvocationOutputHandler() {
            @Override
            public void consumeLine(String s) {
                if (s.contains("[INFO] BUILD FAILURE")) {
                    LOGGER.warn("BUILD FAILURE: {}", autoPutTest);
                } else if (s.contains("[INFO] BUILD SUCCESS")) {
                    LOGGER.debug("BUILD SUCCESS: {}", autoPutTest);
                }
            }
        });
        invoker.setErrorHandler(new InvocationOutputHandler() {
            @Override
            public void consumeLine(String s) {
                if (s.contains("[INFO] BUILD FAILURE")) {
                    LOGGER.warn("BUILD FAILURE: {}", autoPutTest);
                } else if (s.contains("[INFO] BUILD SUCCESS")) {
                    LOGGER.debug("BUILD SUCCESS: {}", autoPutTest);
                }
            }
        });
        InvocationResult result = invoker.execute(request);
        return result.getExitCode();
    }
}
