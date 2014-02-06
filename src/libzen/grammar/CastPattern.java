package libzen.grammar;

import zen.ast.ZCastNode;
import zen.ast.ZNode;
import zen.deps.Var;
import zen.deps.ZenMatchFunction;
import zen.parser.ZTokenContext;
import zen.type.ZType;

public class CastPattern extends ZenMatchFunction {

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZNode CastNode = new ZCastNode(ParentNode, ZType.VarType, null);
		CastNode = TokenContext.MatchToken(CastNode, "(", ZTokenContext.Required);
		CastNode = TokenContext.MatchPattern(CastNode, ZNode.TypeInfo, "$Type$", ZTokenContext.Required);
		CastNode = TokenContext.MatchToken(CastNode, ")", ZTokenContext.Required);
		CastNode = TokenContext.MatchPattern(CastNode, ZCastNode.Expr, "$SuffixExpression$", ZTokenContext.Required);
		return CastNode;
	}

}
