package jp.mzw.autoput.requirement;

import jp.mzw.autoput.Main;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.generate.Generator;
import jp.mzw.autoput.maven.MavenUtils;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public class RequirementChecker {
    protected static Logger LOGGER = LoggerFactory.getLogger(RequirementChecker.class);

    private static final String REQUIREMENT_PATH = "requirement";
    private static final String JACOCO_PATH = "jacoco";

    // FIXME: use enum
    protected static final int PARAMETERIZE_SUCCESS = 1;
    protected static final int COMPILE_FAILURE      = 2;
    protected static final int COVERAGE_DECREASE    = 3;

    protected Project project;
    protected Generator generator;

    public RequirementChecker(Project project) {
        this.project = project;
        this.generator = new Generator(project);
    }

    public void check() {
        List<CSVRecord> records = generator.getDetectResult();
        int numOfSuccess           = 0;
        int numOfCompileFailure    = 0;
        int numOfCovereageDecrease = 0;
        for (CSVRecord record : records) {
            String packageName = record.get(0);
            String className = record.get(1);
            String testName = record.get(2);
            int status = _check(packageName, className, testName);
            if (status == PARAMETERIZE_SUCCESS) {
                numOfSuccess++;
            } else if (status == COMPILE_FAILURE) {
                numOfCompileFailure++;
            } else if (status == COVERAGE_DECREASE) {
                numOfCovereageDecrease++;
            }
        }
        System.out.println("Success: " + numOfSuccess);
        System.out.println("Compile Failure: " + numOfCompileFailure);
        System.out.println("Coverage Decrease: " + numOfCovereageDecrease);
    }

    private int  _check(String packageName, String className, String testName) {
        // prepare
        File projectHome = new File(_getPathToBaseDir(project));
        File mavenHome = project.getMavenHome();

        // check PUT
        _deletePUTandOriginalCUTs(packageName);
        String contentPUT = generator.getPUT(packageName, className, testName);
        deployPUT(packageName, contentPUT);
        // R1 (test compile)
        int testCompile = MavenUtils.testCompile(projectHome, mavenHome, testName);
        if (testCompile != 0) {
            LOGGER.info("Fail to compile {} {}", testName, Main.PUT);
            _deletePUTandOriginalCUTs(packageName);
            return COMPILE_FAILURE;
        }
        // R2 (coverage measurement & test execution)
        int coveragePUT = -1;
        _deleteJacocoExec(project);
        int measureCoverage = MavenUtils.measureCoverage(projectHome, mavenHome, testName, Main.PUT);
        if (measureCoverage == 0) {
            _copyCoverageReport(packageName, className, testName, Main.PUT);
            String content = _getCoverageReport(packageName, className, testName, Main.PUT);
            Document document = Jsoup.parse(content);
            Element element = document.select("#coveragetable tfoot tr .ctr2").first();
            coveragePUT = Integer.parseInt(element.text().replace("%", ""));
        } else {
            LOGGER.info("Fail to measure coverage {} {}", testName, Main.PUT);
            _deletePUTandOriginalCUTs(packageName);
            return COVERAGE_DECREASE;
        }


        // check original CUTs
        _deletePUTandOriginalCUTs(packageName);
        String contentOriginalCUTs = generator.getOriginalCUTs(packageName, className, testName);
        deployOriginalCUTs(packageName, contentOriginalCUTs);
        // R1 (test compile)
        testCompile = MavenUtils.testCompile(projectHome, mavenHome, testName);
        if (testCompile != 0) {
            LOGGER.info("Fail to compile {} {}", testName, Main.CUT);
            _deletePUTandOriginalCUTs(packageName);
            return COMPILE_FAILURE;
        }
        // R2 (coverage measurement & test execution)
        int coverageCUTs = -2;
        _deleteJacocoExec(project);
        measureCoverage = MavenUtils.measureCoverage(projectHome, mavenHome, testName, Main.CUT);
        if (measureCoverage == 0) {
            _copyCoverageReport(packageName, className, testName, Main.CUT);
            String content = _getCoverageReport(packageName, className, testName, Main.CUT);
            Document document = Jsoup.parse(content);
            Element element = document.select("#coveragetable tfoot tr .ctr2").first();
            coverageCUTs = Integer.parseInt(element.text().replace("%", ""));
        } else {
            LOGGER.info("Fail to measure coverage {} {}", testName, Main.CUT);
            _deletePUTandOriginalCUTs(packageName);
            return COVERAGE_DECREASE;
        }

        _deleteJacocoExec(project);
        _deletePUTandOriginalCUTs(packageName);
        if (coveragePUT == coverageCUTs) {
            LOGGER.info("Generate PUT corrresponding to {} {} {}", packageName, className, testName);
            outputPUT(packageName, className, testName, contentPUT);
            // output for ease of comparing num of statements contained by unit tests
            outputOriginalCUTs(packageName, className, testName, contentOriginalCUTs);
            return PARAMETERIZE_SUCCESS;
        }
        return COVERAGE_DECREASE;
    }

    private void outputPUT(String packageName, String className, String testName, String content) {
        if (!Files.exists(Paths.get(_getPathToOutput(packageName, className, testName)))) {
            try {
                Files.createDirectories(Paths.get(_getPathToOutput(packageName, className, testName)));
                Files.createFile(Paths.get(String.join("/", _getPathToOutput(packageName, className, testName), Main.PUT + ".java")));
            } catch (IOException e) {
                LOGGER.error("Fail to output PUT: {}", e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(String.join("/", _getPathToOutput(packageName, className, testName), Main.PUT + ".java")))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void outputOriginalCUTs(String packageName, String className, String testName, String content) {
        if (!Files.exists(Paths.get(_getPathToOutput(packageName, className, testName)))) {
            try {
                Files.createDirectories(Paths.get(_getPathToOutput(packageName, className, testName)));
                Files.createFile(Paths.get(String.join("/", _getPathToOutput(packageName, className, testName), Main.CUT + ".java")));
            } catch (IOException e) {
                LOGGER.error("Fail to output original CUTs: {}", e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(String.join("/", _getPathToOutput(packageName, className, testName), Main.CUT + ".java")))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String _getPathToOutput(String packageName, String className, String testName) {
        return String.join("/", project.getOutputDir().getName(), project.getProjectId(), REQUIREMENT_PATH, packageName, className, testName);
    }


    private void _deletePUTandOriginalCUTs(String packageName) {
        try {
            Files.deleteIfExists(Paths.get(getPathToDeployPUT(packageName)));
            Files.deleteIfExists(Paths.get(getPathToDeployOriginalCUTs(packageName)));
        } catch (IOException e) {
            LOGGER.error("Fail to delete PUT and original CUTS.");
        }
    }

    private void _deleteJacocoExec(Project project) {
        try {
            Files.deleteIfExists(Paths.get(
                    String.join("/", _getPathToBaseDir(project), "target", "jacoco.exec")));
        } catch (IOException e) {
            LOGGER.error("Fail to delete jacoco.exec.");
        }
    }

    private void deployPUT(String packageName, String content) {
        if (!Files.exists(Paths.get(getPathToDeployPUT(packageName)))) {
            try {
                Files.createFile(Paths.get(getPathToDeployPUT(packageName)));
            } catch (IOException e) {
                LOGGER.error("Fail to deploy PUT: {}", e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(getPathToDeployPUT(packageName)))){
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deployOriginalCUTs(String packageName, String content) {
        if (!Files.exists(Paths.get(getPathToDeployOriginalCUTs(packageName)))) {
            try {
                Files.createFile(Paths.get(getPathToDeployOriginalCUTs(packageName)));
            } catch (IOException e) {
                LOGGER.error("Fail to deploy original CUTs: {}", e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(getPathToDeployOriginalCUTs(packageName)))){
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getPathToDeployPUT(String packageName) {
        return String.join("/", _getPathToDeployDir(packageName), Main.PUT + ".java");
    }
    private String getPathToDeployOriginalCUTs(String packageName) {
        return String.join("/", _getPathToDeployDir(packageName), Main.CUT + ".java");
    }

    private String _getPathToDeployDir(String packageName) {
        String testPath = "src/test/java/";
        if (project.getProjectId().equals("commons-chain")) {
            testPath = "base/src/test/java/";
        } else if (project.getProjectId().equals("commons-digester")) {
            testPath = "core/src/test/java/";
        }
        return String.join("/", "subjects", project.getProjectId(), testPath, packageName.replace(".", "/"));
    }

    private String _getPathToBaseDir(Project project) {
        if (project.getProjectId().equals("commons-digester")) {
            return String.join("/", project.getProjectDir().getPath(), "core");
        } else if (project.getProjectId().equals("commons-chain")) {
            return String.join("/", project.getProjectDir().getPath(), "base");
        } else {
            return project.getProjectDir().getPath();
        }
    }

    private void _copyCoverageReport(String packageName, String className, String testName, String mode) {
        if (!Files.exists(Paths.get(_getPathToDirForCoverageReport(packageName, className, testName, mode)))) {
            try {
                Files.createDirectories(Paths.get(
                        _getPathToDirForCoverageReport(packageName, className, testName, mode)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Files.copy(
                    Paths.get(String.join("/", _getPathToBaseDir(project), "target", "site", "jacoco", "index.html")),
                    Paths.get(String.join("/", _getPathToDirForCoverageReport(packageName, className, testName, mode), "index.html")),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String _getPathToDirForCoverageReport(String packageName, String className, String testName, String mode) {
        return String.join("/", project.getOutputDir().getName(), project.getProjectId(), JACOCO_PATH, packageName, className, testName, mode);
    }

    private String _getCoverageReport(String packageName, String className, String testName, String mode) {
        String content = "";
        try {
            content = Files.lines(
                    Paths.get(
                            String.join("/", _getPathToDirForCoverageReport(packageName, className, testName, mode), "index.html")),
                    Charset.forName("UTF-8")
            ).collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException e) {
            // do nothing
        }
        return content;
    }
}
