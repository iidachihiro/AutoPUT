package jp.mzw.autoput.modifier;

import jp.mzw.autoput.ast.*;
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
            if (!origin.getName().toString().equals("testParseSimpleWithDecimals")) {
                continue;
            }
            modify(origin);
//            System.out.println("===============");
//            System.out.println("Original");
//            System.out.println(origin);
//            System.out.println("-------------");
//            System.out.println("Modified");
//            System.out.println(modified);
//            System.out.println("===============");
        }
    }

    private MethodDeclaration replaceInputAndExpected(MethodDeclaration origin, MethodDeclaration differentMethod) {
        MethodDeclaration ret = (MethodDeclaration) ASTNode.copySubtree(ast, origin);
        List<ASTNode> replaces = ASTUtils.getDifferentNodes(differentMethod, ret);

        List<VariableDeclarationStatement> variableDeclarationStatements = new ArrayList<>();
        origin.accept(new ASTVisitor() {
            @Override
            public boolean visit(VariableDeclarationStatement node) {
                if (!variableDeclarationStatements.contains(node)) {
                    variableDeclarationStatements.add(node);
                }
                return super.visit(node);
            }
        });
        for (Statement statement : (List<Statement>) ret.getBody().statements()) {
            System.out.println(statement);
            for (ASTNode replace : replaces) {
                System.out.println(replace);
                System.out.println(statement.subtreeMatch(new ASTMatcher(), replace));
            }
        }
//        ASTUtils.getDifferentNodes(differentMethod, ret).forEach(s -> System.out.println(s.getParent() + " " + s.getParent().getClass()));
//        ASTUtils.getDifferentNodes(differentMethod, ret).forEach(s -> System.out.println(s.getParent().getParent() + " " + s.getParent().getParent().getClass()));
//        ASTUtils.getDifferentNodes(differentMethod, ret).forEach(s -> System.out.println(s.getParent().getParent().getParent() + " " + s.getParent().getParent().getParent().getClass()));
        return ret;
    }

    private boolean _isExpectedReplace(VariableDeclarationFragment fragment) {
        return fragment.getName().toString().equals("expected");
    }
}
