package jp.mzw.autoput.experiment;

import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestSuite;
import jp.mzw.autoput.maven.MavenUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by TK on 9/1/17.
 */
public class Prepare {

    public static final String CONFIG_FILENAME = "config.properties";

    static final int SIMILAR_METHODS_NUM = 1;
    protected static final String DETECT_RESULT = "detect_result.csv";
    protected static final String CONVERTER_RESULT = "converter_results.csv";
    protected static final String EXPERIMENTAL_SETTING = "experimental_setting.csv";
    protected static final String[] PROJECTS =
            {"commons-codec"};
//            {"commons-codec", "commons-collections", "commons-math", "joda-time", "jdom"};

    public static void main(String[] args) throws IOException {
        String command = args[0];
        if (command.equals("random")) {
            getTargetsRandomly();
        } else if (command.equals("all")) {
            getAllTargets();
        } else if (command.equals("prepare")) {
            prepareExperimentalTargets();
        } else if (command.equals("test-compile")) {
            compileCheck();
        }
    }

    private static String getExperimentalSubjectFile(String project) {
        return String.join("/", "experiment", "subject", project, EXPERIMENTAL_SETTING);
    }

    private static void getTargetsRandomly() {
        for (String project : PROJECTS) {
            System.out.println("Project: " + project);
            List<CSVRecord> convertResults = getConvertResults(project);
            int count = 0;
            while (count < 10) {
                Collections.shuffle(convertResults);
                CSVRecord record = convertResults.get(0);
                convertResults.remove(0);
                if (record.get(2).equals("1") && record.get(3).equals("1")) {
                    System.out.println("Class: " + record.get(0) + " , Original: " + record.get(1));
                    count++;
                }
            }
        }
    }

    private static void getAllTargets() {
        int allSize = 0;
        for (String project : PROJECTS) {
            System.out.println("Project: " + project);
            List<CSVRecord> convertResults = getConvertResults(project);
            int size = 0;
            for (CSVRecord record : convertResults) {
                if (record.get(2).equals("1") && record.get(3).equals("1")) {
//                    System.out.println("Class: " + record.get(0) + " , Original: " + record.get(1));
                    size++;
                }
            }
            allSize += size;
            System.out.println("Size: " + size);
        }
        System.out.println("All Size: " + allSize);
    }

    private static void prepareExperimentalTargets() throws IOException {
        for (String projectId : PROJECTS) {
            Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
            System.out.println("Project: " + project.getProjectId());
            List<String> contents = new ArrayList<>();
            List<CSVRecord> convertResults = getConvertResults(projectId);
            for (int i = 1; i <= 2; i++) {
                contents.add("Subject" + i + "\n");
                int count = 0;
                while (count < 10) {
                    if (convertResults.size() <= 0) {
                        break;
                    }
                    Collections.shuffle(convertResults);
                    CSVRecord record = convertResults.get(0);
                    convertResults.remove(0);
                    if (record.get(2).equals("1") && record.get(3).equals("1")) {
                        String className = record.get(0);
                        String originName = record.get(1);
                        contents.add(String.join(",", className, originName));
                        for (TestSuite testSuite : project.getTestSuites()) {
                            if (testSuite.getTestClassName().equals(className)) {
                                PackageDeclaration packageDeclaration = testSuite.getCu().getPackage();
                                contents.add("," + packageDeclaration.getName());
                            }
                        }
                        contents.add("\n");
                        count++;
                    }
                }
                contents.add("\n");
            }
            _outputExperimentalSubjects(projectId, contents);
        }
    }


    public static List<CSVRecord> getDetectResults(String project) {
        List<CSVRecord> records = new ArrayList<>();
        Path path = Paths.get(String.join("/", "output", project, "detect", DETECT_RESULT));
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            records = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    public static List<CSVRecord> getConvertResults(String project) {
        List<CSVRecord> records = new ArrayList<>();
        Path path = Paths.get(String.join("/", "experiment", "answer", project, CONVERTER_RESULT));
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            records = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    private static void _outputExperimentalSubjects(String project, List<String> contents) {
        if (!Files.exists(Paths.get(getExperimentalSubjectFile(project)))) {
            try {
                Files.createDirectories(Paths.get(String.join("/", "experiment", "subject", project)));
                Files.createFile(Paths.get(getExperimentalSubjectFile(project)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Subjects are already decided!");
            return;
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(getExperimentalSubjectFile(project)), StandardCharsets.UTF_8)) {
            for (String content : contents) {
                bw.write(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void compileCheck() {
        for (String projectId : PROJECTS) {
            System.out.println("Project: " + projectId);
            try {
                Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
                List<CSVRecord> records = getExperimentalSubjects(projectId);
                for (CSVRecord record : records) {
                    String className = record.get(0);
                    String originName = record.get(1);
                    String packageName = record.get(2);
                    String content = getConvertedTest(projectId, className, originName);
                    Path path = getPathToFile(projectId, packageName);
                    createFile(path, content);
                    File subject = project.getProjectDir();
                    File mavenHome = project.getMavenHome();
                    try {
                        MavenUtils.testCompile(subject, mavenHome, className + "_" + originName);
                    } catch (MavenInvocationException e) {
                        e.printStackTrace();
                    }  finally {
                        deleteFile(path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static Path getPathToFile(String project, String packageName) {
        String path;
        if (project.equals("jdom")) {
            path = String.join("/", "subjects", project, "test", "src", "java",
                    packageName.replace(".", "/"), "AutoPutTest.java");
        } else {
            path = String.join("/", "subjects", project, "src", "test", "java",
                    packageName.replace(".", "/"), "AutoPutTest.java");
        }
        return Paths.get(path);
    }

    private static List<CSVRecord> getExperimentalSubjects(String project) {
        List<CSVRecord> records = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(getExperimentalSubjectFile(project)), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            records = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    private static String getConvertedTest(String project, String className, String originName) {
        String content = "";
        try {
            content =  Files.lines(Paths.get(String.join("/", "output", project, "convert", className, originName + ".txt"))
                    , Charset.forName("UTF-8")).collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private static void createFile(Path path, String content) {
        if (Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
