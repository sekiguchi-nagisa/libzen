// ***************************************************************************
// Copyright (c) 2013, JST/CREST DEOS project authors. All rights reserved.
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

package zen.codegen.jvm;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Stack;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import zen.ast.ZAndNode;
import zen.ast.ZArrayLiteralNode;
import zen.ast.ZAsmNode;
import zen.ast.ZBinaryNode;
import zen.ast.ZBlockNode;
import zen.ast.ZBooleanNode;
import zen.ast.ZBreakNode;
import zen.ast.ZCastNode;
import zen.ast.ZClassNode;
import zen.ast.ZComparatorNode;
import zen.ast.ZErrorNode;
import zen.ast.ZFieldNode;
import zen.ast.ZFloatNode;
import zen.ast.ZFuncCallNode;
import zen.ast.ZFunctionNode;
import zen.ast.ZGetIndexNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZGetterNode;
import zen.ast.ZGlobalNameNode;
import zen.ast.ZGroupNode;
import zen.ast.ZIfNode;
import zen.ast.ZInstanceOfNode;
import zen.ast.ZIntNode;
import zen.ast.ZLetNode;
import zen.ast.ZMacroNode;
import zen.ast.ZMapEntryNode;
import zen.ast.ZMapLiteralNode;
import zen.ast.ZMethodCallNode;
import zen.ast.ZNewObjectNode;
import zen.ast.ZNode;
import zen.ast.ZNotNode;
import zen.ast.ZNullNode;
import zen.ast.ZOrNode;
import zen.ast.ZParamNode;
import zen.ast.ZReturnNode;
import zen.ast.ZSetIndexNode;
import zen.ast.ZSetNameNode;
import zen.ast.ZSetterNode;
import zen.ast.ZStringNode;
import zen.ast.ZThrowNode;
import zen.ast.ZTryNode;
import zen.ast.ZUnaryNode;
import zen.ast.ZVarNode;
import zen.ast.ZWhileNode;
import zen.ast.sugar.ZLocalDefinedNode;
import zen.ast.sugar.ZTopLevelNode;
import zen.parser.ZLogger;
import zen.type.ZClassField;
import zen.type.ZClassType;
import zen.type.ZFuncType;
import zen.type.ZType;
import zen.util.LibZen;
import zen.util.Var;
import zen.util.ZFunction;
import zen.util.ZObject;
import zen.util.ZenMap;

public class JavaAsmGenerator extends JavaGenerator {
	public JavaStaticFieldNode MainFuncNode = null;
	AsmClassLoader AsmLoader = null;
	Stack<TryCatchLabel> TryCatchLabel;
	AsmMethodBuilder AsmBuilder;

	public JavaAsmGenerator() {
		super("jvm", "Java 1.6");
		this.TryCatchLabel = new Stack<TryCatchLabel>();
		this.AsmLoader = new AsmClassLoader(this);
	}

	public final Class<?> GetJavaClass(ZType zType, Class<?> C) {
		if(zType instanceof ZFuncType) {
			return this.LoadFuncClass((ZFuncType)zType);
		}
		else {
			return JavaTypeTable.GetJavaClass(zType, C);
		}
	}

	@Override public final Class<?> GetJavaClass(ZType zType) {
		return this.GetJavaClass(zType, Object.class);
	}

	final Type AsmType(ZType zType) {
		Class<?> jClass = this.GetJavaClass(zType, Object.class);
		return Type.getType(jClass);
	}

	final String GetDescripter(ZType zType) {
		Class<?> jClass = this.GetJavaClass(zType, null);
		if(jClass != null) {
			return Type.getType(jClass).toString();
		}
		else {
			return "L" + zType + ";";
		}
	}

	final String GetTypeDesc(ZType zType) {
		Class<?> JClass = this.GetJavaClass(zType);
		return Type.getDescriptor(JClass);
	}

	final String GetMethodDescriptor(ZFuncType FuncType) {
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		for(int i = 0; i < FuncType.GetFuncParamSize(); i++) {
			ZType ParamType = FuncType.GetFuncParamType(i);
			sb.append(this.GetDescripter(ParamType));
		}
		sb.append(")");
		sb.append(this.GetDescripter(FuncType.GetReturnType()));
		String Desc = sb.toString();
		//		String Desc2 = this.GetMethodDescriptor2(FuncType);
		//		System.out.println(" ** Desc: " + Desc + ", " + Desc2 + ", FuncType: " + FuncType);
		return Desc;
	}

	@Override public void VisitNullNode(ZNullNode Node) {
		this.AsmBuilder.visitInsn(Opcodes.ACONST_NULL);
	}

	@Override public void VisitBooleanNode(ZBooleanNode Node) {
		this.AsmBuilder.PushBoolean(Node.BooleanValue);
	}

	@Override public void VisitIntNode(ZIntNode Node) {
		this.AsmBuilder.PushLong(Node.IntValue);
	}

	@Override public void VisitFloatNode(ZFloatNode Node) {
		this.AsmBuilder.PushDouble(Node.FloatValue);
	}

	@Override public void VisitStringNode(ZStringNode Node) {
		this.AsmBuilder.visitLdcInsn(Node.StringValue);
	}

	@Override public void VisitArrayLiteralNode(ZArrayLiteralNode Node) {
		if(Node.IsUntyped()) {
			ZLogger._LogError(Node.SourceToken, "ambigious array");
			this.AsmBuilder.visitInsn(Opcodes.ACONST_NULL);
		}
		else {
			Class<?> ArrayClass = LibAsm.AsArrayClass(Node.Type);
			String Owner = Type.getInternalName(ArrayClass);
			this.AsmBuilder.visitTypeInsn(NEW, Owner);
			this.AsmBuilder.visitInsn(DUP);
			this.AsmBuilder.PushInt(Node.Type.TypeId);
			this.AsmBuilder.PushNodeListAsArray(LibAsm.AsElementClass(Node.Type), 0, Node);
			this.AsmBuilder.SetLineNumber(Node);
			this.AsmBuilder.visitMethodInsn(INVOKESPECIAL, Owner, "<init>", LibAsm.NewArrayDescriptor(Node.Type));
		}
	}

	@Override public void VisitMapLiteralNode(ZMapLiteralNode Node) {
		if(Node.IsUntyped()) {
			ZLogger._LogError(Node.SourceToken, "ambigious map");
			this.AsmBuilder.visitInsn(Opcodes.ACONST_NULL);
		}
		else {
			String Owner = Type.getInternalName(ZenMap.class);
			this.AsmBuilder.visitTypeInsn(NEW, Owner);
			this.AsmBuilder.visitInsn(DUP);
			this.AsmBuilder.PushInt(Node.Type.TypeId);
			this.AsmBuilder.PushInt(Node.GetListSize());
			this.AsmBuilder.visitTypeInsn(ANEWARRAY, Type.getInternalName(Object.class));
			for(int i = 0; i < Node.GetListSize() ; i++) {
				ZMapEntryNode EntryNode = Node.GetMapEntryNode(i);
				this.AsmBuilder.visitInsn(DUP);
				this.AsmBuilder.PushInt(i * 2);
				this.AsmBuilder.PushNode(String.class, EntryNode.KeyNode());
				this.AsmBuilder.visitInsn(Opcodes.AASTORE);
				this.AsmBuilder.visitInsn(DUP);
				this.AsmBuilder.PushInt(i * 2 + 1);
				this.AsmBuilder.PushNode(Object.class, EntryNode.ValueNode());
				this.AsmBuilder.visitInsn(Opcodes.AASTORE);
				i = i + 1;
			}
			this.AsmBuilder.SetLineNumber(Node);
			String Desc = Type.getMethodDescriptor(Type.getType(void.class), new Type[] { Type.getType(int.class),  Type.getType(Object[].class)});
			this.AsmBuilder.visitMethodInsn(INVOKESPECIAL, Owner, "<init>", Desc);
		}
	}

	//	@Override public void VisitNewArrayNode(ZNewArrayNode Node) {
	//		this.Debug("TODO");
	//		this.AsmBuilder.visitInsn(Opcodes.ACONST_NULL);
	//		//		this.CurrentBuilder.LoadConst(Node.Type);
	//		//		this.CurrentBuilder.LoadNewArray(this, 0, Node.NodeList);
	//		//		this.CurrentBuilder.InvokeMethodCall(Node.Type, JLib.NewArray);
	//	}

	@Override public void VisitNewObjectNode(ZNewObjectNode Node) {
		if(Node.IsUntyped()) {
			ZLogger._LogError(Node.SourceToken, "no class for new operator");
			this.AsmBuilder.visitInsn(Opcodes.ACONST_NULL);
		}
		else {
			String ClassName = Type.getInternalName(this.GetJavaClass(Node.Type));
			this.AsmBuilder.visitTypeInsn(NEW, ClassName);
			this.AsmBuilder.visitInsn(DUP);
			Constructor<?> jMethod = this.GetConstructor(Node.Type, Node);
			if(jMethod != null) {
				Class<?>[] P = jMethod.getParameterTypes();
				for(int i = 0; i < P.length; i++) {
					this.AsmBuilder.PushNode(P[i], Node.GetListAt(i));
				}
				this.AsmBuilder.SetLineNumber(Node);
				this.AsmBuilder.visitMethodInsn(INVOKESPECIAL, ClassName, "<init>", Type.getConstructorDescriptor(jMethod));
			}
			else {
				ZLogger._LogError(Node.SourceToken, "no constructor: " + Node.Type);
				this.AsmBuilder.visitInsn(Opcodes.ACONST_NULL);
			}
		}
	}

	@Override public void VisitVarNode(ZVarNode Node) {
		Class<?> DeclClass = this.GetJavaClass(Node.DeclType());
		this.AsmBuilder.AddLocal(DeclClass, Node.GetName());
		this.AsmBuilder.PushNode(DeclClass, Node.InitValueNode());
		this.AsmBuilder.StoreLocal(Node.GetName());
		this.VisitBlockNode(Node);
		this.AsmBuilder.RemoveLocal(DeclClass, Node.GetName());
	}

	@Override public void VisitGlobalNameNode(ZGlobalNameNode Node) {
		ZLogger._LogError(Node.SourceToken, "undefined symbol: " + Node.GlobalName);
		this.AsmBuilder.visitInsn(Opcodes.ACONST_NULL);
	}

	@Override public void VisitGetNameNode(ZGetNameNode Node) {
		this.AsmBuilder.LoadLocal(Node.VarName);
		this.AsmBuilder.CheckReturnCast(Node, this.AsmBuilder.GetLocalType(Node.VarName));
	}

	@Override public void VisitSetNameNode(ZSetNameNode Node) {
		this.AsmBuilder.PushNode(this.AsmBuilder.GetLocalType(Node.VarName), Node.ExprNode());
		this.AsmBuilder.StoreLocal(Node.VarName);
	}

	@Override public void VisitGroupNode(ZGroupNode Node) {
		Node.ExprNode().Accept(this);
	}

	private Field GetField(Class<?> RecvClass, String Name) {
		try {
			return RecvClass.getField(Name);
		} catch (Exception e) {
			LibZen._FixMe(e);
		}
		return null;  // type checker guarantees field exists
	}

	@Override public void VisitGetterNode(ZGetterNode Node) {
		if(Node.IsUntyped()) {
			Method sMethod = JavaMethodTable.GetStaticMethod("GetField");
			ZNode NameNode = new ZStringNode(Node, null, Node.GetName());
			this.AsmBuilder.ApplyStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode(), NameNode});
		}
		else {
			Class<?> RecvClass = this.GetJavaClass(Node.RecvNode().Type);
			Field jField = this.GetField(RecvClass, Node.GetName());
			String Owner = Type.getType(RecvClass).getInternalName();
			String Desc = Type.getType(jField.getType()).getDescriptor();
			if(Modifier.isStatic(jField.getModifiers())) {
				this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, Owner, Node.GetName(), Desc);
			}
			else {
				this.AsmBuilder.PushNode(null, Node.RecvNode());
				this.AsmBuilder.visitFieldInsn(GETFIELD, Owner, Node.GetName(), Desc);
			}
			this.AsmBuilder.CheckReturnCast(Node, jField.getType());
		}
	}

	@Override public void VisitSetterNode(ZSetterNode Node) {
		if(Node.IsUntyped()) {
			Method sMethod = JavaMethodTable.GetStaticMethod("SetField");
			ZNode NameNode = new ZStringNode(Node, null, Node.GetName());
			this.AsmBuilder.ApplyStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode(), NameNode, Node.ExprNode()});
		}
		else {
			Class<?> RecvClass = this.GetJavaClass(Node.RecvNode().Type);
			Field jField = this.GetField(RecvClass, Node.GetName());
			String Owner = Type.getType(RecvClass).getInternalName();
			String Desc = Type.getType(jField.getType()).getDescriptor();
			if(Modifier.isStatic(jField.getModifiers())) {
				this.AsmBuilder.PushNode(jField.getType(), Node.ExprNode());
				this.AsmBuilder.visitFieldInsn(PUTSTATIC, Owner, Node.GetName(), Desc);
			}
			else {
				this.AsmBuilder.PushNode(null, Node.RecvNode());
				this.AsmBuilder.PushNode(jField.getType(), Node.ExprNode());
				this.AsmBuilder.visitFieldInsn(PUTFIELD, Owner, Node.GetName(), Desc);
			}
		}
	}

	@Override public void VisitGetIndexNode(ZGetIndexNode Node) {
		Method sMethod = JavaMethodTable.GetBinaryStaticMethod(Node.RecvNode().Type, "[]", Node.IndexNode().Type);
		this.AsmBuilder.ApplyStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode(), Node.IndexNode()});
	}

	@Override public void VisitSetIndexNode(ZSetIndexNode Node) {
		Method sMethod = JavaMethodTable.GetBinaryStaticMethod(Node.RecvNode().Type, "[]=", Node.IndexNode().Type);
		this.AsmBuilder.ApplyStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode(), Node.IndexNode(), Node.ExprNode()});
	}

	private int GetInvokeType(Method jMethod) {
		if(Modifier.isStatic(jMethod.getModifiers())) {
			return INVOKESTATIC;
		}
		if(Modifier.isInterface(jMethod.getModifiers())) {
			return INVOKEINTERFACE;
		}
		return INVOKEVIRTUAL;
	}

	@Override public void VisitMethodCallNode(ZMethodCallNode Node) {
		this.AsmBuilder.SetLineNumber(Node);
		Method jMethod = this.GetMethod(Node.RecvNode().Type, Node.MethodName(), Node);
		if(jMethod != null) {
			if(!Modifier.isStatic(jMethod.getModifiers())) {
				this.AsmBuilder.PushNode(null, Node.RecvNode());
			}
			Class<?>[] P = jMethod.getParameterTypes();
			for(int i = 0; i < P.length; i++) {
				this.AsmBuilder.PushNode(P[i], Node.GetListAt(i));
			}
			int inst = this.GetInvokeType(jMethod);
			String owner = Type.getInternalName(jMethod.getDeclaringClass());
			this.AsmBuilder.visitMethodInsn(inst, owner, jMethod.getName(), Type.getMethodDescriptor(jMethod));
			this.AsmBuilder.CheckReturnCast(Node, jMethod.getReturnType());
		}
		else {
			jMethod = JavaMethodTable.GetStaticMethod("InvokeUnresolvedMethod");
			this.AsmBuilder.PushNode(Object.class, Node.RecvNode());
			this.AsmBuilder.PushConst(Node.MethodName());
			this.AsmBuilder.PushNodeListAsArray(Object.class, 0, Node);
			this.AsmBuilder.ApplyStaticMethod(Node, jMethod);
		}
	}

	@Override public void VisitMacroNode(ZMacroNode Node) {
		for(int i = 0; i < Node.GetListSize(); i++) {
			this.AsmBuilder.PushNode(null, Node.GetListAt(i));
		}
		@Var String MacroText = Node.MacroFunc.MacroText;
		@Var int ClassEnd = MacroText.indexOf(".");
		@Var int MethodEnd = MacroText.indexOf("(");
		//System.out.println("MacroText: " + MacroText + " " + ClassEnd + ", " + MethodEnd);
		@Var String ClassName = MacroText.substring(0, ClassEnd);
		@Var String MethodName = MacroText.substring(ClassEnd+1, MethodEnd);
		this.AsmBuilder.SetLineNumber(Node);
		//System.out.println("debug: " + ClassName + ", " + MethodName);
		this.AsmBuilder.visitMethodInsn(INVOKESTATIC, ClassName, MethodName, Node.MacroFunc.FuncType);
	}

	@Override public void VisitFuncCallNode(ZFuncCallNode Node) {
		if(Node.GetAstType(ZFuncCallNode._Func).IsFuncType()) {
			ZFuncType FuncType = (ZFuncType)Node.FuncNameNode().Type;
			if(Node.FuncNameNode() instanceof ZGlobalNameNode) {
				ZGlobalNameNode NameNode = (ZGlobalNameNode)Node.FuncNameNode();
				this.AsmBuilder.ApplyFuncName(NameNode, NameNode.GlobalName, FuncType, Node);
			}
			else {
				Class<?> FuncClass = this.LoadFuncClass(FuncType);
				this.AsmBuilder.ApplyFuncObject(Node, FuncClass, Node.FuncNameNode(), FuncType, Node);
			}
		}
		else {
			ZLogger._LogError(Node.SourceToken, "not function");
			this.AsmBuilder.visitInsn(Opcodes.ACONST_NULL);
		}
	}

	@Override public void VisitUnaryNode(ZUnaryNode Node) {
		Method sMethod = JavaMethodTable.GetUnaryStaticMethod(Node.SourceToken.GetText(), Node.RecvNode().Type);
		this.AsmBuilder.ApplyStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode()});
	}

	@Override public void VisitNotNode(ZNotNode Node) {
		Method sMethod = JavaMethodTable.GetUnaryStaticMethod(Node.SourceToken.GetText(), Node.AST[ZNotNode._Recv].Type);
		this.AsmBuilder.ApplyStaticMethod(Node, sMethod, new ZNode[] {Node.AST[ZNotNode._Recv]});
	}

	@Override public void VisitCastNode(ZCastNode Node) {
		if(Node.Type.IsVoidType()) {
			Node.ExprNode().Accept(this);
			this.AsmBuilder.Pop(Node.ExprNode().Type);
		}
		else {
			Class<?> TargetClass = this.GetJavaClass(Node.Type);
			Class<?> SourceClass = this.GetJavaClass(Node.ExprNode().Type);
			Method sMethod = JavaMethodTable.GetCastMethod(TargetClass, SourceClass);
			if(sMethod != null) {
				this.AsmBuilder.ApplyStaticMethod(Node, sMethod, new ZNode[] {Node.ExprNode()});
			}
			else if(!TargetClass.isAssignableFrom(SourceClass)) {
				this.AsmBuilder.visitTypeInsn(CHECKCAST, TargetClass);
			}
		}
	}

	@Override public void VisitInstanceOfNode(ZInstanceOfNode Node) {
		// TODO Auto-generated method stub

	}

	@Override public void VisitBinaryNode(ZBinaryNode Node) {
		Method sMethod = JavaMethodTable.GetBinaryStaticMethod(Node.LeftNode().Type, Node.SourceToken.GetText(), Node.RightNode().Type);
		this.AsmBuilder.ApplyStaticMethod(Node, sMethod, new ZNode[] {Node.LeftNode(), Node.RightNode()});
	}

	@Override public void VisitComparatorNode(ZComparatorNode Node) {
		Method sMethod = JavaMethodTable.GetBinaryStaticMethod(Node.LeftNode().Type, Node.SourceToken.GetText(), Node.RightNode().Type);
		this.AsmBuilder.ApplyStaticMethod(Node, sMethod, new ZNode[] {Node.LeftNode(), Node.RightNode()});
	}

	@Override public void VisitAndNode(ZAndNode Node) {
		Label elseLabel = new Label();
		Label mergeLabel = new Label();
		this.AsmBuilder.PushNode(boolean.class, Node.LeftNode());
		this.AsmBuilder.visitJumpInsn(IFEQ, elseLabel);

		this.AsmBuilder.PushNode(boolean.class, Node.RightNode());
		this.AsmBuilder.visitJumpInsn(IFEQ, elseLabel);

		this.AsmBuilder.visitLdcInsn(true);
		this.AsmBuilder.visitJumpInsn(GOTO, mergeLabel);

		this.AsmBuilder.visitLabel(elseLabel);
		this.AsmBuilder.visitLdcInsn(false);
		this.AsmBuilder.visitJumpInsn(GOTO, mergeLabel);

		this.AsmBuilder.visitLabel(mergeLabel);
	}

	@Override public void VisitOrNode(ZOrNode Node) {
		Label thenLabel = new Label();
		Label mergeLabel = new Label();
		this.AsmBuilder.PushNode(boolean.class, Node.LeftNode());
		this.AsmBuilder.visitJumpInsn(IFNE, thenLabel);

		this.AsmBuilder.PushNode(boolean.class, Node.RightNode());
		this.AsmBuilder.visitJumpInsn(IFNE, thenLabel);

		this.AsmBuilder.visitLdcInsn(false);
		this.AsmBuilder.visitJumpInsn(GOTO, mergeLabel);

		this.AsmBuilder.visitLabel(thenLabel);
		this.AsmBuilder.visitLdcInsn(true);
		this.AsmBuilder.visitJumpInsn(GOTO, mergeLabel);

		this.AsmBuilder.visitLabel(mergeLabel);
	}

	@Override public void VisitBlockNode(ZBlockNode Node) {
		for (int i = 0; i < Node.GetListSize(); i++) {
			Node.GetListAt(i).Accept(this);

		}
	}

	@Override public void VisitIfNode(ZIfNode Node) {
		Label ElseLabel = new Label();
		Label EndLabel = new Label();
		this.AsmBuilder.PushNode(boolean.class, Node.CondNode());
		this.AsmBuilder.visitJumpInsn(IFEQ, ElseLabel);
		// Then
		Node.ThenNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(GOTO, EndLabel);
		// Else
		this.AsmBuilder.visitLabel(ElseLabel);
		if(Node.ElseNode() != null) {
			Node.ElseNode().Accept(this);
			this.AsmBuilder.visitJumpInsn(GOTO, EndLabel);
		}
		// End
		this.AsmBuilder.visitLabel(EndLabel);
	}

	@Override public void VisitReturnNode(ZReturnNode Node) {
		if(Node.HasReturnExpr()) {
			Node.ExprNode().Accept(this);
			Type type = this.AsmType(Node.ExprNode().Type);
			this.AsmBuilder.visitInsn(type.getOpcode(IRETURN));
		}
		else {
			this.AsmBuilder.visitInsn(RETURN);
		}
	}

	@Override public void VisitWhileNode(ZWhileNode Node) {
		Label continueLabel = new Label();
		Label breakLabel = new Label();
		this.AsmBuilder.BreakLabelStack.push(breakLabel);
		this.AsmBuilder.ContinueLabelStack.push(continueLabel);

		this.AsmBuilder.visitLabel(continueLabel);
		this.AsmBuilder.PushNode(boolean.class, Node.CondNode());
		this.AsmBuilder.visitJumpInsn(IFEQ, breakLabel); // condition
		Node.BlockNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(GOTO, continueLabel);
		this.AsmBuilder.visitLabel(breakLabel);

		this.AsmBuilder.BreakLabelStack.pop();
		this.AsmBuilder.ContinueLabelStack.pop();
	}

	@Override public void VisitBreakNode(ZBreakNode Node) {
		Label l = this.AsmBuilder.BreakLabelStack.peek();
		this.AsmBuilder.visitJumpInsn(GOTO, l);
	}

	@Override public void VisitThrowNode(ZThrowNode Node) {
		// use wrapper
		//String name = Type.getInternalName(ZenThrowableWrapper.class);
		//this.CurrentVisitor.MethodVisitor.visitTypeInsn(NEW, name);
		//this.CurrentVisitor.MethodVisitor.visitInsn(DUP);
		//Node.Expr.Accept(this);
		//this.box();
		//this.CurrentVisitor.typeStack.pop();
		//this.CurrentVisitor.MethodVisitor.visitMethodInsn(INVOKESPECIAL, name, "<init>", "(Ljava/lang/Object;)V");
		//this.CurrentVisitor.MethodVisitor.visitInsn(ATHROW);
	}

	@Override public void VisitTryNode(ZTryNode Node) {
		MethodVisitor mv = this.AsmBuilder;
		TryCatchLabel Label = new TryCatchLabel();
		this.TryCatchLabel.push(Label); // push

		// try block
		mv.visitLabel(Label.beginTryLabel);
		Node.TryBlockNode().Accept(this);
		mv.visitLabel(Label.endTryLabel);
		mv.visitJumpInsn(GOTO, Label.finallyLabel);

		// finally block
		mv.visitLabel(Label.finallyLabel);
		if(Node.FinallyBlockNode() != null) {
			Node.FinallyBlockNode().Accept(this);
		}
		this.TryCatchLabel.pop();
	}

	//	public void VisitCatchNode(ZCatchNode Node) {
	//		MethodVisitor mv = this.AsmBuilder;
	//		Label catchLabel = new Label();
	//		TryCatchLabel Label = this.TryCatchLabel.peek();
	//
	//		// prepare
	//		//TODO: add exception class name
	//		String throwType = this.AsmType(Node.GivenType).getInternalName();
	//		mv.visitTryCatchBlock(Label.beginTryLabel, Label.endTryLabel, catchLabel, throwType);
	//
	//		// catch block
	//		this.AsmBuilder.AddLocal(this.GetJavaClass(Node.GivenType), Node.GivenName);
	//		mv.visitLabel(catchLabel);
	//		this.AsmBuilder.StoreLocal(Node.GivenName);
	//		Node.AST[ZCatchNode._Block].Accept(this);
	//		mv.visitJumpInsn(GOTO, Label.finallyLabel);
	//
	//		this.AsmBuilder.RemoveLocal(this.GetJavaClass(Node.GivenType), Node.GivenName);
	//	}

	@Override public void VisitLetNode(ZLetNode Node) {
		if(Node.HasUntypedNode()) {
			ZLogger._LogError(Node.InitValueNode().SourceToken, "type is ambigious");
			return;
		}
		if(Node.InitValueNode() instanceof ZErrorNode) {
			this.VisitErrorNode((ZErrorNode)Node.InitValueNode());
			return;
		}
		if(!Node.IsConstValue()) {
			String ClassName = "Symbol" + Node.GlobalName;
			@Var AsmClassBuilder ClassBuilder = this.AsmLoader.NewClass(ACC_PUBLIC|ACC_FINAL, Node, ClassName, "java/lang/Object");
			Class<?> ValueClass = this.GetJavaClass(Node.GetAstType(ZLetNode._InitValue));
			ClassBuilder.AddField(ACC_PUBLIC|ACC_STATIC, "_", ValueClass, null);

			AsmMethodBuilder StaticInitMethod = ClassBuilder.NewMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V");
			StaticInitMethod.PushNode(ValueClass, Node.InitValueNode());
			StaticInitMethod.visitFieldInsn(Opcodes.PUTSTATIC, ClassName, "_",  Type.getDescriptor(ValueClass));
			StaticInitMethod.visitInsn(RETURN);
			StaticInitMethod.Finish();

			Class<?> StaticClass = this.AsmLoader.LoadGeneratedClass(ClassName);
			Node.GetNameSpace().SetLocalSymbol(Node.GetName(), new JavaStaticFieldNode(Node, StaticClass, Node.InitValueNode().Type, "_"));
		}
	}

	Class<?> LoadFuncClass(ZFuncType FuncType) {
		String ClassName = NameFuncClass(FuncType);
		Class<?> FuncClass = this.GetGeneratedClass(ClassName, null);
		if(FuncClass == null) {
			@Var String SuperClassName = Type.getInternalName(ZFunction.class);
			@Var AsmClassBuilder ClassBuilder = this.AsmLoader.NewClass(ACC_PUBLIC| ACC_ABSTRACT, null, ClassName, ZFunction.class);
			AsmMethodBuilder InvokeMethod = ClassBuilder.NewMethod(ACC_PUBLIC | ACC_ABSTRACT, "Invoke", FuncType);
			InvokeMethod.Finish();

			AsmMethodBuilder InitMethod = ClassBuilder.NewMethod(ACC_PUBLIC, "<init>", "(ILjava/lang/String;)V");
			InitMethod.visitVarInsn(ALOAD, 0);
			InitMethod.visitVarInsn(ILOAD, 1);
			InitMethod.visitVarInsn(ALOAD, 2);
			InitMethod.visitMethodInsn(INVOKESPECIAL, SuperClassName, "<init>", "(ILjava/lang/String;)V");
			InitMethod.visitInsn(RETURN);
			InitMethod.Finish();

			FuncClass = this.AsmLoader.LoadGeneratedClass(ClassName);
			this.SetGeneratedClass(ClassName, FuncClass);
		}
		return FuncClass;
	}

	@Override public void VisitFunctionNode(ZFunctionNode Node) {
		if(Node.Type.IsVoidType()) {
			assert(Node.FuncName() != null);
			assert(Node.IsTopLevel());  // otherwise, transformed to var f = function ()..
			JavaStaticFieldNode FuncNode = this.GenerateFunctionAsSymbolField(Node.FuncName(), Node);
			if(Node.IsExport) {
				if(Node.FuncName().equals("main")) {
					this.MainFuncNode = FuncNode;
				}
			}
			this.SetMethod(Node.FuncName(), (ZFuncType)FuncNode.Type, FuncNode.StaticClass);
		}
		else {
			JavaStaticFieldNode FuncNode = this.GenerateFunctionAsSymbolField(Node.GetUniqueName(this), Node);
			if(this.AsmBuilder != null) {
				this.VisitStaticFieldNode(FuncNode);
			}
			else {  // ad hoc
				Node.GetNameSpace().SetLocalSymbol("it", FuncNode);
			}
		}
	}


	private JavaStaticFieldNode GenerateFunctionAsSymbolField(String FuncName, ZFunctionNode Node) {
		@Var ZFuncType FuncType = Node.GetFuncType();
		String ClassName = this.NameFunctionClass(FuncName, FuncType);
		Class<?> FuncClass = this.LoadFuncClass(FuncType);
		@Var AsmClassBuilder ClassBuilder = this.AsmLoader.NewClass(ACC_PUBLIC|ACC_FINAL, Node, ClassName, FuncClass);

		AsmMethodBuilder InvokeMethod = ClassBuilder.NewMethod(ACC_PUBLIC | ACC_FINAL, "Invoke", FuncType);
		int index = 1;
		for(int i = 0; i < FuncType.GetFuncParamSize(); i++) {
			Type AsmType = this.AsmType(FuncType.GetFuncParamType(i));
			InvokeMethod.visitVarInsn(AsmType.getOpcode(ILOAD), index);
			index += AsmType.getSize();
		}
		InvokeMethod.visitMethodInsn(INVOKESTATIC, ClassName, "f", FuncType);
		InvokeMethod.visitReturn(FuncType.GetReturnType());
		InvokeMethod.Finish();

		ClassBuilder.AddField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "function", FuncClass, null);

		// static init
		AsmMethodBuilder StaticInitMethod = ClassBuilder.NewMethod(ACC_PUBLIC | ACC_STATIC , "<clinit>", "()V");
		StaticInitMethod.visitTypeInsn(NEW, ClassName);
		StaticInitMethod.visitInsn(DUP);
		StaticInitMethod.visitMethodInsn(INVOKESPECIAL, ClassName, "<init>", "()V");
		StaticInitMethod.visitFieldInsn(PUTSTATIC, ClassName, "function",  FuncClass);
		StaticInitMethod.visitInsn(RETURN);
		StaticInitMethod.Finish();

		AsmMethodBuilder InitMethod = ClassBuilder.NewMethod(ACC_PRIVATE, "<init>", "()V");
		InitMethod.visitVarInsn(ALOAD, 0);
		InitMethod.visitLdcInsn(FuncType.TypeId);
		InitMethod.visitLdcInsn(FuncName);
		InitMethod.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(FuncClass), "<init>", "(ILjava/lang/String;)V");
		InitMethod.visitInsn(RETURN);
		InitMethod.Finish();

		AsmMethodBuilder StaticFuncMethod = ClassBuilder.NewMethod(ACC_PUBLIC | ACC_STATIC, "f", FuncType);
		for(int i = 0; i < Node.GetListSize(); i++) {
			ZParamNode ParamNode = Node.GetParamNode(i);
			Class<?> DeclClass = this.GetJavaClass(ParamNode.DeclType());
			StaticFuncMethod.AddLocal(DeclClass, ParamNode.GetName());
		}
		Node.BlockNode().Accept(this);
		StaticFuncMethod.Finish();

		FuncClass = this.AsmLoader.LoadGeneratedClass(ClassName);
		this.SetGeneratedClass(ClassName, FuncClass);
		return new JavaStaticFieldNode(null, FuncClass, FuncType, "function");
	}

	private ZFunction LoadFunction(Class<?> WrapperClass, Class<?> StaticMethodClass) {
		try {
			Field f = StaticMethodClass.getField("function");
			Object func = f.get(null);
			if(WrapperClass != null) {
				Constructor<?> c = WrapperClass.getConstructor(func.getClass().getSuperclass());
				func = c.newInstance(func);
			}
			return (ZFunction)func;
		}
		catch(Exception e) {
			e.printStackTrace();
			LibZen._Exit(1, "failed: " + e);
		}
		return null;
	}

	private void SetMethod(String FuncName, ZFuncType FuncType, Class<?> FuncClass) {
		ZType RecvType = FuncType.GetRecvType();
		if(RecvType instanceof ZClassType && FuncName != null) {
			ZClassType ClassType = (ZClassType)RecvType;
			ZType FieldType = ClassType.GetFieldType(FuncName, null);
			if(FieldType == null || !FieldType.IsFuncType()) {
				FuncName = LibZen._AnotherName(FuncName);
				FieldType = ClassType.GetFieldType(FuncName, null);
				if(FieldType == null || !FieldType.IsFuncType()) {
					return;
				}
			}
			if(FieldType.Equals(FuncType)) {
				this.SetMethod(ClassType, FuncName, this.LoadFunction(null, FuncClass));
			}
			else if(this.IsMethodFuncType((ZFuncType)FieldType, FuncType)) {
				Class<?> WrapperClass = this.MethodWrapperClass((ZFuncType)FieldType, FuncType);
				this.SetMethod(ClassType, FuncName, this.LoadFunction(WrapperClass, FuncClass));
			}
		}
	}

	private boolean IsMethodFuncType(ZFuncType FieldType, ZFuncType FuncType) {
		if(FuncType.GetFuncParamSize() == FieldType.GetFuncParamSize() && FuncType.GetReturnType().Equals(FieldType.GetReturnType())) {
			for(int i = 1; i < FuncType.GetFuncParamSize(); i++) {
				if(!FuncType.GetFuncParamType(i).Equals(FieldType.GetFuncParamType(i))) {
					return false;
				}
			}
		}
		return true;
	}


	private Class<?> MethodWrapperClass(ZFuncType FuncType, ZFuncType SourceFuncType) {
		String ClassName = "W" + NameFuncClass(FuncType) + "W" + NameFuncClass(SourceFuncType);
		Class<?> WrapperClass = this.GetGeneratedClass(ClassName, null);
		if(WrapperClass == null) {
			Class<?> FuncClass = this.LoadFuncClass(FuncType);
			Class<?> SourceClass = this.LoadFuncClass(SourceFuncType);
			@Var AsmClassBuilder ClassBuilder = this.AsmLoader.NewClass(ACC_PUBLIC|ACC_FINAL, null, ClassName, FuncClass);

			ClassBuilder.AddField(ACC_PUBLIC, "f", SourceClass, null);

			AsmMethodBuilder InitMethod = ClassBuilder.NewMethod(ACC_PUBLIC, "<init>", "(L"+Type.getInternalName(SourceClass)+";)V");
			InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
			LibAsm.PushInt(InitMethod, FuncType.TypeId);
			InitMethod.visitLdcInsn(SourceFuncType.ShortName);
			InitMethod.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(FuncClass), "<init>", "(ILjava/lang/String;)V");
			InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
			InitMethod.visitVarInsn(Opcodes.ALOAD, 1);
			InitMethod.visitFieldInsn(PUTFIELD, ClassName, "f", Type.getDescriptor(SourceClass));
			InitMethod.visitInsn(RETURN);
			InitMethod.Finish();

			AsmMethodBuilder InvokeMethod = ClassBuilder.NewMethod(ACC_PUBLIC | ACC_FINAL, "Invoke", FuncType);
			InvokeMethod.visitVarInsn(ALOAD, 0);
			InvokeMethod.visitFieldInsn(GETFIELD, ClassName, "f", Type.getDescriptor(SourceClass));
			InvokeMethod.visitVarInsn(ALOAD, 1);
			//			System.out.println("CAST: " + Type.getInternalName(this.GetJavaClass(SourceFuncType.GetFuncParamType(0))));
			InvokeMethod.visitTypeInsn(CHECKCAST, this.GetJavaClass(SourceFuncType.GetFuncParamType(0)));
			int index = 2;
			for(int i = 1; i < FuncType.GetFuncParamSize(); i++) {
				Type AsmType = this.AsmType(FuncType.GetFuncParamType(i));
				InvokeMethod.visitVarInsn(AsmType.getOpcode(ILOAD), index);
				index += AsmType.getSize();
			}
			//String owner = "C" + FuncType.StringfySignature(FuncName);
			InvokeMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(SourceClass), "Invoke", this.GetMethodDescriptor(SourceFuncType));
			InvokeMethod.visitReturn(FuncType.GetReturnType());
			InvokeMethod.Finish();

			WrapperClass = this.AsmLoader.LoadGeneratedClass(ClassName);
			this.SetGeneratedClass(ClassName, WrapperClass);
		}
		return WrapperClass;
	}


	// -----------------------------------------------------------------------

	private Class<?> GetSuperClass(ZType SuperType) {
		@Var Class<?> SuperClass = null;
		if(SuperType != null) {
			SuperClass = this.GetJavaClass(SuperType);
		}
		else {
			SuperClass = ZObject.class;
		}
		return SuperClass;
	}

	private final static String NameClassMethod(ZType ClassType, String FieldName) {
		return FieldName + ClassType.TypeId;
	}

	private void SetMethod(ZClassType ClassType, String FuncName, ZFunction FuncObject) {
		try {
			Class<?> StaticClass = this.GetJavaClass(ClassType);
			Field f = StaticClass.getField(NameClassMethod(ClassType, FuncName));
			f.set(null, FuncObject);
		}
		catch (Exception e) {
			e.printStackTrace();
			LibZen._Exit(1, "failed " + e);
		}
	}

	@Override public void VisitClassNode(ZClassNode Node) {
		@Var Class<?> SuperClass = this.GetSuperClass(Node.SuperType());
		@Var AsmClassBuilder ClassBuilder = this.AsmLoader.NewClass(ACC_PUBLIC, Node, Node.ClassName(), SuperClass);
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var ZFieldNode FieldNode = Node.GetFieldNode(i);
			if(FieldNode.ClassType.Equals(Node.ClassType)) {
				ClassBuilder.AddField(ACC_PUBLIC, FieldNode.GetName(), FieldNode.DeclType(), this.GetConstValue(FieldNode.InitValueNode()));
			}
			i = i + 1;
		}
		while(i < Node.ClassType.GetFieldSize()) {
			@Var ZClassField Field = Node.ClassType.GetFieldAt(i);
			if(Field.FieldType.IsFuncType()) {
				ClassBuilder.AddField(ACC_PUBLIC|ACC_STATIC, NameClassMethod(Node.ClassType, Field.FieldName), Field.FieldType, null);
			}
			i = i + 1;
		}
		AsmMethodBuilder InitMethod = ClassBuilder.NewMethod(ACC_PUBLIC, "<init>", "()V");
		InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
		InitMethod.PushInt(Node.ClassType.TypeId);
		InitMethod.visitMethodInsn(INVOKESPECIAL, Node.ClassName(), "<init>", "(I)V");
		InitMethod.visitInsn(RETURN);
		InitMethod.Finish();

		InitMethod = ClassBuilder.NewMethod(ACC_PROTECTED, "<init>", "(I)V");
		InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
		InitMethod.visitVarInsn(Opcodes.ILOAD, 1);
		InitMethod.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(SuperClass), "<init>", "(I)V");
		while(i < Node.GetListSize()) {
			@Var ZFieldNode FieldNode = Node.GetFieldNode(i);
			if(!FieldNode.DeclType().IsFuncType()) {
				InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
				InitMethod.PushNode(this.GetJavaClass(FieldNode.DeclType()), FieldNode.InitValueNode());
				InitMethod.visitFieldInsn(PUTFIELD, Node.ClassName(), FieldNode.GetName(), Type.getDescriptor(this.GetJavaClass(FieldNode.DeclType())));
			}
			i++;
		}
		while(i < Node.ClassType.GetFieldSize()) {
			@Var ZClassField Field = Node.ClassType.GetFieldAt(i);
			if(Field.FieldType.IsFuncType()) {
				//System.out.println("FieldName:" + Field.ClassType + "." + Field.FieldName + ", type=" + Field.FieldType);
				String FieldDesc = Type.getDescriptor(this.GetJavaClass(Field.FieldType));
				Label JumpLabel = new Label();
				InitMethod.visitFieldInsn(Opcodes.GETSTATIC, Node.ClassName(), NameClassMethod(Node.ClassType, Field.FieldName), FieldDesc);
				InitMethod.visitJumpInsn(Opcodes.IFNULL, JumpLabel);
				InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
				InitMethod.visitFieldInsn(Opcodes.GETSTATIC, Node.ClassName(), NameClassMethod(Node.ClassType, Field.FieldName), FieldDesc);
				//System.out.println("************" + Field.ClassType + ", " + Type.getInternalName(this.GetJavaClass(Field.ClassType)));
				InitMethod.visitFieldInsn(Opcodes.PUTFIELD, Field.ClassType.ShortName, Field.FieldName, FieldDesc);
				InitMethod.visitLabel(JumpLabel);
			}
			i++;
		}
		InitMethod.visitInsn(RETURN);
		InitMethod.Finish();

		JavaTypeTable.SetTypeTable(Node.ClassType, this.AsmLoader.LoadGeneratedClass(Node.ClassName()));
	}

	@Override public void VisitErrorNode(ZErrorNode Node) {
		@Var String Message = ZLogger._LogError(Node.SourceToken, Node.ErrorMessage);
		this.AsmBuilder.PushConst(Message);
		@Var Method sMethod = JavaMethodTable.GetStaticMethod("ThrowError");
		this.AsmBuilder.ApplyStaticMethod(Node, sMethod);
	}

	public void VisitStaticFieldNode(JavaStaticFieldNode Node) {
		String FieldDesc = Type.getDescriptor(this.GetJavaClass(Node.Type));
		this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(Node.StaticClass), Node.FieldName, FieldDesc);
	}

	@Override public void VisitAsmNode(ZAsmNode Node) {
		// TODO Auto-generated method stub
	}

	@Override public void VisitTopLevelNode(ZTopLevelNode Node) {
		this.VisitUndefinedNode(Node);
	}

	@Override public void VisitLocalDefinedNode(ZLocalDefinedNode Node) {
		if(Node instanceof JavaStaticFieldNode) {
			this.VisitStaticFieldNode((JavaStaticFieldNode)Node);
		}
		else {
			this.VisitUndefinedNode(Node);
		}

	}

	@Override public void ExecMain() {
		this.Logger.OutputErrorsToStdErr();
		if(this.MainFuncNode != null) {
			@Var JavaStaticFieldNode MainFunc = this.MainFuncNode;
			try {
				Method Method = MainFunc.StaticClass.getMethod("f");
				Method.invoke(null);
			}
			catch(NoSuchMethodException e) {
				System.out.println(e);
			}
			catch(Exception e) {
				System.out.println(e);
			}
		}
	}

}


