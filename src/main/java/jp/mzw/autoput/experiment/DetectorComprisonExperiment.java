package jp.mzw.autoput.experiment;

import jp.mzw.autoput.Main;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.detect.Detector;
import jp.mzw.autoput.detect.NaiveDetector;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ClassPathResource;


import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DetectorComprisonExperiment {
    protected static final String[] PROJECTS = {
            "commons-bcel",
            "commons-codec",
            "commons-compress",
            "commons-csv",
            "commons-digester",
            "commons-fileupload",
            "commons-math",
    };


    public static void experiment() throws IOException {
        for (String project : PROJECTS) {
            System.out.println("=============== " + project + " ==================");
            List<Pair<String, String>> groundTruths = _getGroundTruth(project);
            // Detector
            System.out.println("--------------- Detector --------------------");
            List<Pair<String, String>> detectorResults = _getDetectorResult(project);
            calculate(groundTruths, detectorResults);
            // Naive Detector
            System.out.println("--------------- Naive Detector --------------------");
            List<Pair<String, String>> naiveDetectorResults = _getNaiveDetectorResult(project);
            calculate(groundTruths, naiveDetectorResults);
        }
    }

    private static List<Pair<String, String>> _getGroundTruth(String project) throws IOException {
        List<CSVRecord> records = new ArrayList<>();
        ClassPathResource resoruce = new ClassPathResource(project + ".csv");
        Path path = Paths.get(resoruce.getURI());
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            records = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Pair<String, String>> ret = new ArrayList<>();
        for (CSVRecord record : records) {
            for (int i = 2; i < record.size(); i++) {
                if (record.get(i).equals("")) {
                    break;
                }
                String method1 = record.get(i);
                for (int j = i + 1; j < record.size(); j++) {
                    if (record.get(j).equals("")) {
                        break;
                    }
                    String method2 = record.get(j);
                    ret.add(new MutablePair<>(method1, method2));
                }
            }
        }
        return ret;
    }

    private static List<Pair<String, String>> _getDetectorResult(String projectId) throws IOException {
        List<Pair<String, String>> ret = new ArrayList<>();
        Project project = new Project(projectId).setConfig(Main.CONFIG_FILENAME);
        Detector detector = new Detector(project);
        List<CSVRecord> records = detector.getDetectResults();
        for (CSVRecord record : records) {
            for (int i = 2; i < record.size(); i++) {
                String method1 = record.get(i);
                for (int j = i + 1; j < record.size(); j++) {
                    String method2 = record.get(j);
                    ret.add(new MutablePair<>(method1, method2));
                }
            }
        }
        return ret;
    }

    private static List<Pair<String, String>> _getNaiveDetectorResult(String projectId) throws IOException {
        List<Pair<String, String>> ret = new ArrayList<>();
        Project project = new Project(projectId).setConfig(Main.CONFIG_FILENAME);
        NaiveDetector naiveDetector = new NaiveDetector(project);
        List<CSVRecord> records = naiveDetector.getNaiveDetectResults();
        for (CSVRecord record : records) {
            for (int i = 2; i < record.size(); i++) {
                String method1 = record.get(i);
                for (int j = i + 1; j < record.size(); j++) {
                    String method2 = record.get(j);
                    ret.add(new MutablePair<>(method1, method2));
                }
            }
        }
        return ret;
    }

    private static void calculate(List<Pair<String, String>> answers, List<Pair<String, String>> records) {
        int truePositive = 0;
        for (Pair<String, String> answer : answers) {
            String answer1 = answer.getLeft();
            String answer2 = answer.getRight();
            for (Pair<String, String> record : records) {
                String id1 = record.getLeft();
                String id2 = record.getRight();
                if ((answer1.equals(id1) && answer2.equals(id2))
                        || (answer1.equals(id2) && answer2.equals(id1))) {
                    truePositive++;
                    break;
                }
            }
        }
        System.out.println("True Positive: " + truePositive);
        System.out.println("False Positive: " + (records.size() - truePositive));
        System.out.println("False Negative: " + (answers.size() - truePositive));
        System.out.println("Recall: " + ((double) truePositive / answers.size()) * 100);
        System.out.println("Precision: " + ((double) truePositive / records.size() * 100));
    }
}
