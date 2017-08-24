package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 8/24/17.
 */
public class AnnotationVisitor extends ASTVisitor {
    protected List<Annotation> annotations;

    public AnnotationVisitor() {
        super();
        annotations = new ArrayList<>();
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public boolean visit(MarkerAnnotation node) {
        if (!annotations.contains(node)) {
            annotations.add(node);
        }
        return super.visit(node);
    }
    @Override
    public boolean visit(NormalAnnotation node) {
        if (!annotations.contains(node)) {
            annotations.add(node);
        }
        return super.visit(node);
    }
    @Override
    public boolean visit(SingleMemberAnnotation node) {
        if (!annotations.contains(node)) {
            annotations.add(node);
        }
        return super.visit(node);
    }
}
