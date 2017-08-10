package jp.mzw.autoput.modifier;

import jp.mzw.autoput.core.TestSuite;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by TK on 7/24/17.
 */
public class UseParametersAnnotation extends ParameterizedModifierBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(UseParametersAnnotation.class);

    public UseParametersAnnotation(TestSuite testSuite) {
        super(testSuite);
    }

    public void useParametersAnnotation() {
        Map<MethodDeclaration, List<MethodDeclaration>> detected = detect();
        for (MethodDeclaration origin : detected.keySet()) {
            System.out.println(origin.getName());
            if (!origin.getName().toString().equals("testParseSimpleWithDecimals")) {
//                continue;
            }
            modify(origin);
        }
    }

}
