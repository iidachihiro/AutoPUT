package jp.mzw.autoput.core;

import jp.mzw.autoput.Main;

import jp.mzw.autoput.util.Utils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by TK on 7/20/17.
 */
public class Project {

    protected static Logger LOGGER = LoggerFactory.getLogger(Project.class);
    /** Unique string representing subject project */
    protected String projectId;
    /** Path to directory containing subjects */
    protected String pathToSubjectsDir = "subjects";
    /** Path to output directory (default: output) */
    protected String pathToOutputDir = "output";
    /** Maven home (default: $M2_HOME) */
    protected String mavenHome = "/usr/local/apache-maven-3.3.9";
    /** Path to subject project */
    protected String pathToProject;

    protected Map<String, CompilationUnit> compilationUnits;
    protected List<TestSuite> testSuites;

    public Project(String projectId) {
        this.projectId = projectId;
    }
    public String getProjectId() {
        return this.projectId;
    }
    public File getProjectDir() {
        return new File(this.pathToSubjectsDir, this.projectId);
    }
    public File getOutputDir() {
        return new File(this.pathToOutputDir);
    }
    public File getMavenHome() {
        return new File(this.mavenHome);
    }
    public List<TestSuite> getTestSuites() {
        return this.testSuites;
    }
    public Project setConfig(String filename) throws IOException {
        // load
        Properties config = new Properties();
        InputStream is = Main.class.getClassLoader().getResourceAsStream(filename);
        if (is != null) {
            config.load(is);
        }
        // read
        this.pathToOutputDir = config.getProperty("path_to_output_dir") != null ? config.getProperty("path_to_output_dir") : "output";
        this.pathToSubjectsDir = config.getProperty("path_to_subjects_dir") != null ? config.getProperty("path_to_subjects_dir") : "subjects";
        this.mavenHome = config.getProperty("maven_home") != null ? config.getProperty("maven_home") : "/usr/local/apache-maven-3.3.9";
        prepare();
        // return
        return this;
    }

    private static Map<String, CompilationUnit> getFileUnitMap(File subjectDir) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setEnvironment(null, null, null, true);
        final Map<String, CompilationUnit> units = new HashMap<>();
        FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                units.put(sourceFilePath, ast);
            }
        };
        parser.createASTs(getSources(subjectDir), null, new String[] {}, requestor, new NullProgressMonitor());
        return units;
    }

    private static String[] getSources(File subjectDir) {
        List<File> files = new ArrayList<>();
        files.addAll(Utils.getFiles(new File(subjectDir, "src/main/java")));
        files.addAll(Utils.getFiles(new File(subjectDir, "src/test/java")));
        // For JDOM
        files.addAll(Utils.getFiles(new File(subjectDir, "core/src/java")));
        files.addAll(Utils.getFiles(new File(subjectDir, "test/src/java")));
        String[] sources = new String[files.size()];
        try {
            for (int i = 0; i < files.size(); i++) {
                sources[i] = files.get(i).getCanonicalPath();
            }
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage());
        }
        return sources;
    }

    public static String[] getTestFiles(File subjectDir) {
        List<File> files = new ArrayList<File>();
        files.addAll(Utils.getFiles(new File(subjectDir, "src/test/java")));
        // For JDOM
        files.addAll(Utils.getFiles(new File(subjectDir, "test/src/java")));
        String[] sources = new String[files.size()];
        try {
            for (int i = 0; i < files.size(); i++) {
                sources[i] = files.get(i).getCanonicalPath();
            }
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage());
        }
        return sources;
    }

    public void prepare() {
        String pathToSubjectDir = String.join("/", pathToSubjectsDir, projectId);
        File subjectDir = new File(pathToSubjectDir);
        this.compilationUnits = getFileUnitMap(subjectDir);
        this.testSuites = new ArrayList<>();
        String[] testFiles = getTestFiles(subjectDir);
        for (String testFile : testFiles) {
            CompilationUnit cu = compilationUnits.get(testFile);
            if (cu != null) {
                TestSuite testSuite = new TestSuite(new File(testFile), cu);
                testSuites.add(testSuite);
            }
        }
    }
}
