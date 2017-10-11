package jp.mzw.autoput.detect;

import jp.mzw.autoput.Const;
import jp.mzw.autoput.core.Project;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public abstract class DetectorBase extends Detector {
    abstract protected List<String> _detect(Project project);

    public DetectorBase() {
        super();
    }

    @Override
    public void detect(Project project) {
        List<String> contents = _detect(project);
        output(project, contents);
    }

    @Override
    public void output(Project project, List<String> contents) {
        if (!Files.exists(Paths.get(_getOutputPath(project)))) {
            try {
                Files.createDirectories(Paths.get(_getOutputDir(project)));
                Files.createFile(Paths.get(_getOutputPath(project)));
            } catch (IOException e) {
                LOGGER.error("can't make the directory or file for detecting output.");
            }
        }
        _output(project, contents);
    }

    /** Private Method **/
    private String _getOutputPath(Project project) {
        return String.join("/", "detect", project.getProjectId(), Const.DETECT_OUTPUT);
    }
    private String _getOutputDir(Project project) {
        return String.join("/", "detect", project.getProjectId());
    }
    private void _output(Project project, List<String> contents) {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(_getOutputPath(project)), StandardCharsets.UTF_8)) {
            for (String content : contents) {
                bw.write(content);
            }
        } catch (IOException e) {
            LOGGER.error("can't write detect output in {}.", _getOutputPath(project));
        }
    }
}
