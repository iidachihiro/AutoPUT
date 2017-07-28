package jp.mzw.autoput.modifier;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * Created by TK on 7/28/17.
 */
public class ModifierUtils {

    public static boolean isAssertionMethod(MethodInvocation method) {
        return method.getName().toString().startsWith("assert");
    }

    public static boolean isInputName(Name name) {
        if (!name.isSimpleName()) {
            return false;
        }
        return ((SimpleName) name).getIdentifier().toString().equals("actual")
                || ((SimpleName) name).getIdentifier().toString().equals("input");
    }

    public static boolean isExpectedName(Name name) {
        if (!name.isSimpleName()) {
            return false;
        }
        return ((SimpleName) name).getIdentifier().toString().equals("expected");
    }
}
