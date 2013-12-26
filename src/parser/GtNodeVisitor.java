package parser;

import java.util.ArrayList;

import parser.ast.GtAllocateNode;
import parser.ast.GtAndNode;
import parser.ast.GtApplyFunctionObjectNode;
import parser.ast.GtApplyOverridedMethodNode;
import parser.ast.GtApplySymbolNode;
import parser.ast.GtArrayLiteralNode;
import parser.ast.GtBinaryNode;
import parser.ast.GtBooleanNode;
import parser.ast.GtBreakNode;
import parser.ast.GtCaseNode;
import parser.ast.GtCastNode;
import parser.ast.GtCatchNode;
import parser.ast.GtClassDeclNode;
import parser.ast.GtCommandNode;
import parser.ast.GtConstPoolNode;
import parser.ast.GtConstructorNode;
import parser.ast.GtContinueNode;
import parser.ast.GtDoWhileNode;
import parser.ast.GtEmptyNode;
import parser.ast.GtErrorNode;
import parser.ast.GtFloatNode;
import parser.ast.GtForEachNode;
import parser.ast.GtForNode;
import parser.ast.GtFuncDeclNode;
import parser.ast.GtFunctionLiteralNode;
import parser.ast.GtGetCapturedNode;
import parser.ast.GtGetIndexNode;
import parser.ast.GtGetLocalNode;
import parser.ast.GtGetterNode;
import parser.ast.GtIfNode;
import parser.ast.GtInstanceOfNode;
import parser.ast.GtIntNode;
import parser.ast.GtMapLiteralNode;
import parser.ast.GtNewArrayNode;
import parser.ast.GtNode;
import parser.ast.GtNullNode;
import parser.ast.GtOrNode;
import parser.ast.GtParamNode;
import parser.ast.GtPrefixDeclNode;
import parser.ast.GtPrefixInclNode;
import parser.ast.GtRegexNode;
import parser.ast.GtReturnNode;
import parser.ast.GtSetCapturedNode;
import parser.ast.GtSetIndexNode;
import parser.ast.GtSetLocalNode;
import parser.ast.GtSetterNode;
import parser.ast.GtSliceNode;
import parser.ast.GtStatementNode;
import parser.ast.GtStringNode;
import parser.ast.GtSuffixDeclNode;
import parser.ast.GtSuffixInclNode;
import parser.ast.GtSwitchNode;
import parser.ast.GtThrowNode;
import parser.ast.GtTrinaryNode;
import parser.ast.GtTryNode;
import parser.ast.GtUnaryNode;
import parser.ast.GtUsingNode;
import parser.ast.GtVarDeclNode;
import parser.ast.GtWhileNode;
import parser.ast.GtYieldNode;

public abstract class GtNodeVisitor {
	/*field*/public ArrayList<String>  ReportedErrorList;

	public GtNodeVisitor/*constructor*/() {
		this.ReportedErrorList = new ArrayList<String>();
	}

	public final String ReportError(int Level, GtToken Token, String Message) {
		if(Level == GreenTeaConsts.ErrorLevel) {
			Message = "(error) " + GtStaticTable.FormatFileLineNumber(Token.FileLine) + " " + Message;
		}
		else if(Level == GreenTeaConsts.TypeErrorLevel) {
			Message = "(error) " + GtStaticTable.FormatFileLineNumber(Token.FileLine) + " " + Message;
		}
		else if(Level == GreenTeaConsts.WarningLevel) {
			Message = "(warning) " + GtStaticTable.FormatFileLineNumber(Token.FileLine) + " " + Message;
		}
		else if(Level == GreenTeaConsts.InfoLevel) {
			Message = "(info) " + GtStaticTable.FormatFileLineNumber(Token.FileLine) + " " + Message;
		}
		this.ReportedErrorList.add(Message);
		return Message;
	}

	public abstract void VisitEmptyNode(GtEmptyNode Node);
	public abstract void VisitNullNode(GtNullNode Node);
	public abstract void VisitBooleanNode(GtBooleanNode Node);
	public abstract void VisitIntNode(GtIntNode Node);
	public abstract void VisitFloatNode(GtFloatNode Node);
	public abstract void VisitStringNode(GtStringNode Node);
	public abstract void VisitRegexNode(GtRegexNode Node);
	public abstract void VisitConstPoolNode(GtConstPoolNode Node);
	public abstract void VisitArrayLiteralNode(GtArrayLiteralNode Node);
	public abstract void VisitMapLiteralNode(GtMapLiteralNode Node);
	public abstract void VisitParamNode(GtParamNode Node);
	public abstract void VisitFunctionLiteralNode(GtFunctionLiteralNode Node);
	public abstract void VisitGetLocalNode(GtGetLocalNode Node);
	public abstract void VisitSetLocalNode(GtSetLocalNode Node);
	public abstract void VisitGetCapturedNode(GtGetCapturedNode Node);
	public abstract void VisitSetCapturedNode(GtSetCapturedNode Node);
	public abstract void VisitGetterNode(GtGetterNode Node);
	public abstract void VisitSetterNode(GtSetterNode Node);
	public abstract void VisitApplySymbolNode(GtApplySymbolNode Node);
	public abstract void VisitApplyFunctionObjectNode(GtApplyFunctionObjectNode Node);
	public abstract void VisitApplyOverridedMethodNode(GtApplyOverridedMethodNode Node);
	public abstract void VisitGetIndexNode(GtGetIndexNode Node);
	public abstract void VisitSetIndexNode(GtSetIndexNode Node);
	public abstract void VisitSliceNode(GtSliceNode Node);
	public abstract void VisitAndNode(GtAndNode Node);
	public abstract void VisitOrNode(GtOrNode Node);
	public abstract void VisitUnaryNode(GtUnaryNode Node);
	public abstract void VisitPrefixInclNode(GtPrefixInclNode Node);
	public abstract void VisitPrefixDeclNode(GtPrefixDeclNode Node);
	public abstract void VisitSuffixInclNode(GtSuffixInclNode Node);
	public abstract void VisitSuffixDeclNode(GtSuffixDeclNode Node);
	public abstract void VisitBinaryNode(GtBinaryNode Node);
	public abstract void VisitTrinaryNode(GtTrinaryNode Node);
	public abstract void VisitConstructorNode(GtConstructorNode Node);
	public abstract void VisitAllocateNode(GtAllocateNode Node);
	public abstract void VisitNewArrayNode(GtNewArrayNode Node);
	public abstract void VisitInstanceOfNode(GtInstanceOfNode Node);
	public abstract void VisitCastNode(GtCastNode Node);
	public abstract void VisitVarDeclNode(GtVarDeclNode Node);
	public abstract void VisitUsingNode(GtUsingNode Node);
	public abstract void VisitIfNode(GtIfNode Node);
	public abstract void VisitWhileNode(GtWhileNode Node);
	public abstract void VisitDoWhileNode(GtDoWhileNode Node);
	public abstract void VisitForNode(GtForNode Node);
	public abstract void VisitForEachNode(GtForEachNode Node);
	public abstract void VisitContinueNode(GtContinueNode Node);
	public abstract void VisitBreakNode(GtBreakNode Node);
	public abstract void VisitStatementNode(GtStatementNode Node);
	public abstract void VisitReturnNode(GtReturnNode Node);
	public abstract void VisitYieldNode(GtYieldNode Node);
	public abstract void VisitThrowNode(GtThrowNode Node);
	public abstract void VisitTryNode(GtTryNode Node);
	public abstract void VisitCatchNode(GtCatchNode Node);
	public abstract void VisitSwitchNode(GtSwitchNode Node);
	public abstract void VisitCaseNode(GtCaseNode Node);
	public abstract void VisitCommandNode(GtCommandNode Node);
	public abstract void VisitErrorNode(GtErrorNode Node);
	public abstract void VisitClassDeclNode(GtClassDeclNode ClassDeclNode);
	public abstract void VisitFuncDeclNode(GtFuncDeclNode FuncDeclNode);
	public abstract void VisitBlock(GtNode Node);
}