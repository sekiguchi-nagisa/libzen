package libzen.grammar;

import zen.ast.ZGetNameNode;
import zen.ast.ZNode;
import zen.deps.Var;
import zen.deps.ZenMatchFunction;
import zen.parser.ZToken;
import zen.parser.ZTokenContext;

public class PathPattern extends ZenMatchFunction {

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZToken Token = TokenContext.ParseLargeToken();
		return new ZGetNameNode(ParentNode, Token, Token.GetText());
	}

}
