package jp.mzw.tri.ast;

import org.eclipse.jdt.core.dom.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by TK on 7/20/17.
 */
public class AllLiteralFindVisitor extends ASTVisitor {
    private List<Expression> literals;
    private List<StringLiteral> strings;
    public AllLiteralFindVisitor() {
        super();
        this.literals = new ArrayList<>();
        this.strings = new ArrayList<>();
    }
    @Override
    public boolean visit(NullLiteral node) {
        if(!this.literals.contains(node)) {
            this.literals.add(node);
        }
        return false;
    }
    @Override
    public boolean visit(BooleanLiteral node) {
        if(!this.literals.contains(node)) {
            this.literals.add(node);
        }
        return false;
    }
    @Override
    public boolean visit(CharacterLiteral node) {
        if(!this.literals.contains(node)) {
            this.literals.add(node);
        }
        return false;
    }
    @Override
    public boolean visit(NumberLiteral node) {
        if(!this.literals.contains(node)) {
            this.literals.add(node);
        }
        return false;
    }
    @Override
    public boolean visit(StringLiteral node) {
        if(!this.literals.contains(node)) {
            this.literals.add(node);
        }
        if(!this.strings.contains(node)) {
            String value = node.getLiteralValue();
            if (!StringUtils.isAnyBlank(value) && !StringUtils.isNumeric(value) &&
                    StringUtils.isAlphanumeric(value) && (node.getParent() instanceof MethodInvocation)) {
                this.strings.add(node);
            }
        }
        return false;
    }
    @Override
    public boolean visit(TypeLiteral node) {
        if(!this.literals.contains(node)) {
            this.literals.add(node);
        }
        return false;
    }

    public List<Expression> getFoundLiterals() {
        return this.literals;
    }
    public List<StringLiteral> getFoundStringLiterals() {
        return this.strings;
    }
}
