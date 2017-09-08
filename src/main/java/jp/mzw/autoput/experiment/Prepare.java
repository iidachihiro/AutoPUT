package jp.mzw.autoput.experiment;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestCase;
import jp.mzw.autoput.core.TestSuite;
import jp.mzw.autoput.maven.MavenUtils;
import jp.mzw.autoput.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by TK on 9/1/17.
 */
public class Prepare {

    public static final String CONFIG_FILENAME = "config.properties";

    protected static final String DETECT_RESULT = "detect_result.csv";
    protected static final String CONVERTER_RESULT = "converter_results.csv";
    protected static final String EXPERIMENTAL_SETTING = "experimental_setting.csv";
    protected static final String[] PROJECTS =
//            {"commons-codec"};
            {"commons-codec", "commons-collections", "commons-math", "joda-time", "jdom"};

    public static void main(String[] args) throws IOException {
        String command = args[0];
        String projectId = args[1];
        if (command.equals("random")) {
            getTargetsRandomly(projectId);
        } else if (command.equals("all")) {
            getAllTargets();
        } else if (command.equals("prepare")) {
            prepareExperimentalTargets();
        } else if (command.equals("test-compile")) {
            compileCheck(projectId);
        } else if (command.equals("morethan")) {
            int threshold = new Integer(args[1]);
            detectMoreThanTreshold(threshold);
        }
    }

    private static String getExperimentalSubjectFile(String project) {
        return String.join("/", "experiment", "subject", project, EXPERIMENTAL_SETTING);
    }

    private static void getTargetsRandomly(String project) {
        System.out.println("Project: " + project);
        List<CSVRecord> detectResults = getDetectResults(project);
        for (int i = 1; i <= 2; i++) {
            int count = 1;
            while (count <= 10) {
                Collections.shuffle(detectResults);
                CSVRecord record = detectResults.get(0);
                detectResults.remove(0);
                System.out.println(String.join(",",
                        String.valueOf(i), String.valueOf(count), record.get(1), record.get(2), record.get(0)));
                count++;
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

    private static void detectMoreThanTreshold(int threshold) {
        int allSize = 0;
        for (String project : PROJECTS) {
            System.out.println("Project: " + project);
            List<Pair<String, String>> manySimilarMethods = new ArrayList<>();
            int size = 0;
            List<CSVRecord> detectResults = getDetectResults(project);
            for (CSVRecord record : detectResults) {
                if (record.size() > threshold) {
                    String className = record.get(0);
                    String originName = record.get(1);
                    manySimilarMethods.add(new ImmutablePair<>(className, originName));
                }
            }
            List<CSVRecord> convertResults = getConvertResults(project);
            for (CSVRecord record : convertResults) {
                String className = record.get(0);
                String originName = record.get(1);
                for (Pair<String, String> pair : manySimilarMethods) {
                    if (className.equals(pair.getLeft()) && originName.equals(pair.getRight())
                            && record.get(2).equals("1") && record.get(3).equals("1")) {
                        size++;
                    }
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

    private static void compileCheck(String projectId) {
        System.out.println("Project: " + projectId);
        try {
            Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
            List<CSVRecord> records = getDetectResults(projectId);
            for (CSVRecord record : records) {
                String packageName = record.get(0);
                String className = record.get(1);
                String originName = record.get(2);
                Path path = getPathOfAutoPut(projectId, packageName);
                String content = getConvertedTest(projectId, packageName, className, originName);
                deleteFile(path);
                createFile(path, content);
                File subject = project.getProjectDir();
                File mavenHome = project.getMavenHome();
                try {
                    System.out.println("Compile: " + className + "_" + originName);
                    List<String> goal = Arrays.asList("test-compile");
                    MavenUtils.maven(projectId, subject, goal, mavenHome, packageName + "/" + className + "/" + originName + ".txt");
                } catch (MavenInvocationException e) {
                    e.printStackTrace();
                }
                deleteFile(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void prepareForAutoPut(String projectId) {
        System.out.println("Project: " + projectId);
        try {
            Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
            List<CSVRecord> records = getExperimentalSubjects(projectId);
            Path beforeFilePath = null;
            for (CSVRecord record : records) {
                if (beforeFilePath != null) {
                    deleteFile(beforeFilePath);
                }
                String className = record.get(0);
                String originName = record.get(1);
                String packageName = record.get(2);
                Path path = getPathOfAutoPut(projectId, packageName);
                String content = getConvertedTest(projectId, packageName, className, originName);
                deleteFile(path);
                createFile(path, content);
                File subject = project.getProjectDir();
                File mavenHome = project.getMavenHome();
                deleteFile(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path getPathOfAutoPut(String project, String packageName) {
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

    private static Path getPathOfOriginal(String project, String packageName) {
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

    public static List<CSVRecord> getExperimentalSubjects(String project) {
        List<CSVRecord> records = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(getExperimentalSubjectFile(project)), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            records = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    private static String getConvertedTest(String project, String packageName, String className, String originName) {
        String content = "";
        try {
            content =  Files.lines(Paths.get(String.join("/", "output", project, "convert", packageName, className, originName + ".txt"))
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

    private static void createZip(Project project, int subjectNum, int testNum, String mode) {
        List<File> files = new ArrayList<File>();
        files.addAll(Utils.getFiles(project.getProjectDir()));
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(
                    new File(project.getProjectId() + "_subject" + subjectNum + "-" + testNum + "_" + mode + ".zip"))));
            createZip(zos, files);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createZip(ZipOutputStream zos, List<File> files) throws IOException {
        byte[] buf = new byte[1024];
        InputStream is = null;
        try {
            for (File file : files) {
                ZipEntry entry = new ZipEntry(file.getName());
                zos.putNextEntry(entry);
                is = new BufferedInputStream(new FileInputStream(file));
                int len = 0;
                while((len = is.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
                org.apache.commons.io.IOUtils.closeQuietly(is);
            }
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(is);
        }
    }

    private static void preapreOriginal(String projectId) throws IOException {
        List<CSVRecord> settings = getExperimentalSubjects(projectId);
        for (CSVRecord setting : settings) {
            List<CSVRecord> records = getDetectResults(projectId);
            for (int i = records.size() - 1; 0 <= i; i--) {
                String className = setting.get(0);
                String originName = setting.get(1);
                String packageName = setting.get(2);
                CSVRecord record = records.get(i);
                if (record.get(0).equals(className) && record.get(1).equals(originName)) {
                    List<String> similarMethods = new ArrayList<>();
                    for (int j = 2; i < record.size(); j++) {
                        similarMethods.add(record.get(j));
                    }
                    Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
                    for (TestSuite testSuite : project.getTestSuites()) {
                        if (!testSuite.getTestClassName().equals(className)) {
                            continue;
                        }
                        if (!testSuite.getCu().getPackage().getName().toString().equals(packageName)) {
                            continue;
                        }
                        String content = extractOriginalMethods(testSuite, similarMethods);
                        Path path = getPathOfOriginal(projectId, packageName);
                        createFile(path, content);
                    }
                }
            }
        }
    }

    private static String extractOriginalMethods(TestSuite testSuite, List<String> similarMethods) {
        CompilationUnit cu = testSuite.getCu();
        AST ast = cu.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);
        // deleteする
        TypeDeclaration clazz = null;
        for (AbstractTypeDeclaration abstType : (List<AbstractTypeDeclaration>) cu.types()) {
            if (abstType instanceof TypeDeclaration) {
                TypeDeclaration type = (TypeDeclaration) abstType;
                if (type.getName().getIdentifier().equals(testSuite.getTestClassName())) {
                    clazz = type;
                }
            }
        }
        String typeName = clazz.getName().getIdentifier();
        List<Name> names = ASTUtils.getAllNames(clazz);
        for (Name name : names) {
            if (name.toString().equals(typeName)) {
                Name replace = ast.newName("OriginalTest");
                rewrite.replace(name, replace, null);
            }
        }
        ListRewrite listRewrite = rewrite.getListRewrite(clazz, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        for (TestCase testCase : testSuite.getTestCases()) {
            MethodDeclaration methodDeclaration = testCase.getMethodDeclaration();
            if (ASTUtils.hasPrivateModifier(methodDeclaration.modifiers())) {
                continue;
            }
            for (String similarMethod : similarMethods) {
                if (methodDeclaration.getName().getIdentifier().equals(similarMethod)) {
                    continue;
                }
            }
            listRewrite.remove(methodDeclaration, null);
        }
        // modify
        try {
            Document document = new Document(testSuite.getTestSources());
            TextEdit edit = rewrite.rewriteAST(document, null);
            edit.apply(document);
            return document.get();
        } catch (IOException | BadLocationException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return "";
    }
}
