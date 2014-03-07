package zen.ast.sugar;

import zen.ast.ZNode;
import zen.parser.ZGenerator;
import zen.util.Field;
import zen.util.Var;

public class ZDesugarNode extends ZSyntaxSugarNode {
	//	public final static int _NewNode = 0;
	@Field ZNode OriginalNode;

	public ZDesugarNode(ZNode OriginalNode, ZNode DesugaredNode) {
		super(OriginalNode.ParentNode, null, 1);
		this.OriginalNode = OriginalNode;
		OriginalNode.ParentNode = this;
		this.Set(0, DesugaredNode);
	}

	public ZDesugarNode(ZNode OriginalNode, ZNode[] Nodes) {
		super(OriginalNode.ParentNode, null, Nodes.length);
		this.OriginalNode = OriginalNode;
		OriginalNode.ParentNode = this;
		@Var int i = 0;
		while(i < Nodes.length) {
			this.Set(i, Nodes[i]);
			i = i + 1;
		}
	}

	@Override public ZDesugarNode DeSugar(ZGenerator Generator) {
		return this;
	}

}
