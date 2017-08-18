package jp.mzw.autoput.modifier;

import jp.mzw.autoput.core.TestSuite;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Created by TK on 7/24/17.
 */
public class ParameterizedModifier extends ParameterizedModifierBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(ParameterizedModifier.class);

    private static final Path output = Paths.get("output.csv");

    public ParameterizedModifier(TestSuite testSuite) {
        super(testSuite);
    }

    public void useParametersAnnotation() {
        Map<MethodDeclaration, List<MethodDeclaration>> detected = detect();
        for (MethodDeclaration origin : detected.keySet()) {
            System.out.println(origin.getName());
            if (origin.getName().toString().equals("testParseSimpleWithDecimals")) {
                continue;
            }
            modify(origin);
        }
    }


    public void checkExistenceOfAutoPutTarget() {
        try (BufferedWriter bw = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            Map<MethodDeclaration, List<MethodDeclaration>> detected = detect();
            for (MethodDeclaration key : detected.keySet()) {
                List<MethodDeclaration> similarMethods = detected.get(key);
                if (similarMethods.size() < 3) {
                    continue;
                }
                System.out.println(key.getName().getIdentifier());
                bw.write(String.join(",", testSuite.getTestClassName(), key.getName().getIdentifier(), String.valueOf(similarMethods.size())));
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
