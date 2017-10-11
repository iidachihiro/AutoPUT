package jp.mzw.autoput.detect;

import jp.mzw.autoput.core.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class Detector {
    protected static Logger LOGGER = LoggerFactory.getLogger(Detector.class);

    public Detector() {}

    abstract public void detect(Project project);
    abstract public void output(Project project, List<String> contents);
}
