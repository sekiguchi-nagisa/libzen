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

import java.util.ArrayList;

import zen.deps.Field;
import zen.deps.Init;
import zen.parser.ZNameSpace;
import zen.parser.ZToken;
import zen.parser.ZVisitor;

public class ZBlockNode extends ZNode {
	@Field public ArrayList<ZNode> StmtList = new ArrayList<ZNode>();
	@Field @Init public ZNameSpace NameSpace;
	public ZBlockNode(ZToken SourceToken, ZNameSpace NameSpace) {
		this.SourceToken = SourceToken;
		this.NameSpace = NameSpace;
	}
	@Override public void Append(ZNode Node) {
		this.StmtList.add(this.SetChild(Node));
	}

	@Override public void Accept(ZVisitor Visitor) {
		Visitor.VisitBlockNode(this);
	}

	@Override public ZReturnNode ToReturnNode() {
		if(this.StmtList.size() == 1) {
			return this.StmtList.get(0).ToReturnNode();
		}
		return null;
	}

}