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
import java.util.Collections;
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
        String command = args[0];
        if (command.equals("random")) {
            getTargetsRandomly();
        } else if (command.equals("all")) {
            getAllTargets();
        }
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
            System.out.println("Size: " + convertResults.size());
            allSize += convertResults.size();
            for (CSVRecord record : convertResults) {
                if (record.get(2).equals("1") && record.get(3).equals("1")) {
                    System.out.println("Class: " + record.get(0) + " , Original: " + record.get(1));
                }
            }
        }
        System.out.println("All Size: " + allSize);
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
