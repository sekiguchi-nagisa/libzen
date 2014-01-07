// ***************************************************************************
// Copyright (c) 2013-2014, Konoha project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// *  Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// *  Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// **************************************************************************

package zen.ast;

import zen.parser.ZenSyntaxPattern;
import zen.parser.GtToken;
import zen.parser.ZenVisitor;

public class GtBinaryNode extends GtNode {
	/*field*/public GtNode   LeftNode;
	/*field*/public GtNode	 RightNode;
	/*field*/public ZenSyntaxPattern Pattern;
	public GtBinaryNode/*constructor*/(GtToken SourceToken, GtNode Left, ZenSyntaxPattern Pattern) {
		super();
		this.SourceToken = SourceToken;
		this.LeftNode  = this.SetChild(Left);
		this.RightNode = null;
		this.Pattern = Pattern;
	}
	@Override public final void Append(GtNode Node) {
		this.RightNode = this.SetChild(this.RightNode);
	}
	@Override public boolean Accept(ZenVisitor Visitor) {
		return Visitor.VisitBinaryNode(this);
	}
//	@Override public Object ToConstValue(GtParserContext Context, boolean EnforceConst)  {
//		/*local*/Object LeftValue = this.LeftNode.ToConstValue(Context, EnforceConst) ;
//		if(LeftValue != null) {
//			/*local*/Object RightValue = this.RightNode.ToConstValue(Context, EnforceConst) ;
//			if(RightValue != null) {
//				return LibZen.EvalBinary(this.Type, LeftValue, this.Token.ParsedText, RightValue);
//			}
//		}
//		return null;
//	}
}