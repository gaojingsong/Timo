package fm.liu.timo.parser.ast.expression.primary.function.spatial;

import java.util.List;
import fm.liu.timo.parser.ast.expression.Expression;
import fm.liu.timo.parser.ast.expression.primary.function.FunctionExpression;
import fm.liu.timo.parser.visitor.Visitor;

public class MultiPointFromWKB extends FunctionExpression {

    public MultiPointFromWKB(Expression expr) {
        super("MULTIPOINTFROMWKB", wrapList(expr));
    }

    @Override
    public FunctionExpression constructFunction(List<Expression> arguments) {
        throw new UnsupportedOperationException("function of char has special arguments");
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
