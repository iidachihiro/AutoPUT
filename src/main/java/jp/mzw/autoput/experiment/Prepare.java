package jp.mzw.autoput.experiment;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 9/1/17.
 */
public class Prepare {

    static final int SIMILAR_METHODS_NUM = 1;
    protected static final String DETECT_RESULT = "detect_result.csv";
    protected static final String CONVERTER_RESULT = "converter_results.csv";
    protected static final String[] PROJECTS =
//            {"commons-codec"};
            {"commons-codec", "commons-collections", "commons-math", "joda-time", "jdom"};

    public static void main(String[] args) throws IOException {
        List<CSVRecord> targets = new ArrayList<>();
        for (String project : PROJECTS) {
            List<CSVRecord> candidates = new ArrayList<>();
            List<CSVRecord> targetsPerProject = new ArrayList<>();
            List<CSVRecord> detectResults = getDetectResults(project);
            for (CSVRecord record : detectResults) {
                if (SIMILAR_METHODS_NUM < record.size()) {
                    candidates.add(record);
                }
            }
            List<CSVRecord> convertResults = getConvertResults(project);
            for (CSVRecord record : convertResults) {
                if (!(record.get(2).equals("1") && record.get(2).equals("1"))) {
                    // Incorrect input or expected
                    continue;
                }
                for (CSVRecord candidate : candidates) {
                    if (candidate.get(0).equals(record.get(0))
                            && candidate.get(1).equals(record.get(1))) {
                        targets.add(candidate);
                        targetsPerProject.add(candidate);
                    }
                }
            }
            System.out.println("The num of experimental targets in " + project
                    + " is " + targetsPerProject.size());
        }
        for (CSVRecord target : targets) {
            System.out.println("Class: " + target.get(0) + " , Original: " + target.get(1));
        }
        System.out.println("The num of experimental targets is " + targets.size());
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

}
