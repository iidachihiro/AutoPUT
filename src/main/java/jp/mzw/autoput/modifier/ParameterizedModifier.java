package jp.mzw.autoput.modifier;

import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by TK on 7/24/17.
 */
public class ParameterizedModifier extends ParameterizedModifierBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(ParameterizedModifier.class);



    public ParameterizedModifier(Project project) {
        super(project);
    }
    public ParameterizedModifier(TestSuite testSuite) {
        super(testSuite);
    }
}
