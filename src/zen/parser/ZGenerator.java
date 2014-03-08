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

//ifdef JAVA
package zen.parser;

import zen.ast.ZAsmMacroNode;
import zen.ast.ZAsmNode;
import zen.ast.ZDefaultValueNode;
import zen.ast.ZErrorNode;
import zen.ast.ZListNode;
import zen.ast.ZNode;
import zen.ast.ZNullNode;
import zen.ast.ZStringNode;
import zen.ast.sugar.ZDesugarNode;
import zen.ast.sugar.ZSyntaxSugarNode;
import zen.type.ZFunc;
import zen.type.ZFuncType;
import zen.type.ZPrototype;
import zen.type.ZType;
import zen.util.Field;
import zen.util.LibZen;
import zen.util.Nullable;
import zen.util.Var;
import zen.util.ZenMap;
import zen.util.ZenMethod;

public abstract class ZGenerator extends ZVisitor {
	@Field public ZenMap<String>        ImportedLibraryMap = new ZenMap<String>(null);
	@Field private final ZenMap<ZFunc>  DefinedFuncMap = new ZenMap<ZFunc>(null);

	@Field private String            GrammarInfo;
	@Field public final String       LanguageExtention;
	@Field public final String       TargetVersion;

	@Field public final ZNameSpace   RootNameSpace;
	@Field private int UniqueNumber = 0;
	@Field public String             OutputFile;
	@Field public ZLogger            Logger;

	@Field private boolean StoppedVisitor;

	protected ZGenerator(String LanguageExtension, String TargetVersion) {
		this.RootNameSpace = new ZNameSpace(this, null);
		this.GrammarInfo = "";
		this.LanguageExtention = LanguageExtension;
		this.TargetVersion = TargetVersion;

		this.OutputFile = null;
		this.Logger = new ZLogger();
		this.StoppedVisitor = false;
	}

	public abstract ZSourceEngine GetEngine();

	@ZenMethod public void ImportLocalGrammar(ZNameSpace NameSpace) {
		// TODO Auto-generated method stub
	}

	@ZenMethod protected void GenerateImportLibrary(String LibName) {
		//		this.HeaderBuilder.AppendNewLine("require ", LibName, this.LineFeed);
	}

	public final void ImportLibrary(String LibName) {
		@Var String Imported = this.ImportedLibraryMap.GetOrNull(LibName);
		if(Imported == null) {
			this.GenerateImportLibrary(LibName);
			this.ImportedLibraryMap.put(LibName, LibName);
		}
	}

	public final void SetAsmMacro(ZNameSpace NameSpace, String Symbol, ZFuncType MacroType, String MacroText) {
		@Var ZMacroFunc MacroFunc = null;
		@Var int loc = MacroText.indexOf("~");
		if(loc > 0) {
			@Var String LibName = MacroText.substring(0, loc);
			MacroText = MacroText.substring(loc+1);
			MacroFunc = new ZMacroFunc(Symbol, MacroType, MacroText);
			MacroFunc.RequiredLibrary = LibName;
		}
		else {
			MacroFunc = new ZMacroFunc(Symbol, MacroType, MacroText);
		}
		if(Symbol.equals("_")) {
			this.SetConverterFunc(MacroType.GetRecvType(), MacroType.GetReturnType(), MacroFunc);
		}
		else {
			this.SetDefinedFunc(MacroFunc);
		}
	}

	private String NameConverterFunc(ZType FromType, ZType ToType) {
		return FromType.GetUniqueName() + "T" + ToType.GetUniqueName();
	}

	public final void SetConverterFunc(ZType FromType, ZType ToType, ZFunc Func) {
		//System.out.println("set " + this.NameConverterFunc(FromType, ToType));
		this.DefinedFuncMap.put(this.NameConverterFunc(FromType, ToType), Func);
	}

	public final ZFunc LookupConverterFunc(ZType FromType, ZType ToType) {
		while(FromType != null) {
			@Var ZFunc Func = this.DefinedFuncMap.GetOrNull(this.NameConverterFunc(FromType, ToType));
			//System.out.println("get " + this.NameConverterFunc(FromType, ToType) + ", func="+ Func);
			if(Func != null) {
				return Func;
			}
			FromType = FromType.GetSuperType();
		}
		return null;
	}

	public final void SetAsmSymbol(ZNameSpace NameSpace, ZAsmMacroNode Node) {
		@Var String MacroText = Node.GetMacroText();
		@Var int loc = MacroText.indexOf("~");
		if(loc > 0) {
			@Var String LibName = MacroText.substring(0, loc);
			this.ImportLibrary(LibName);
			Node.Set(ZAsmNode._Macro, new ZStringNode(Node, Node.SymbolToken, MacroText.substring(loc+1)));
		}
		@Var ZAsmNode AsmNode = new ZAsmNode(null);
		AsmNode.SourceToken = Node.SymbolToken;
		AsmNode.Set(ZAsmNode._Macro, Node.AST[ZAsmNode._Macro]);
		AsmNode.Type = Node.MacroType;
		NameSpace.SetLocalSymbol(Node.Symbol, AsmNode);
	}

	@ZenMethod public void WriteTo(@Nullable String FileName) {
		// TODO Stub
	}

	@ZenMethod public String GetSourceText() {
		return null;
	}

	@ZenMethod protected String NameOutputFile(String FileName) {
		if(FileName != null) {
			return FileName + "." + this.LanguageExtention;
		}
		return FileName;
	}

	@Override public final void EnableVisitor() {
		this.StoppedVisitor = false;
	}

	@Override public final void StopVisitor() {
		this.StoppedVisitor = true;
	}

	@Override public final boolean IsVisitable() {
		return !this.StoppedVisitor;
	}

	public final String GetGrammarInfo() {
		return this.GrammarInfo;
	}

	public final void AppendGrammarInfo(String GrammarInfo) {
		this.GrammarInfo = this.GrammarInfo + GrammarInfo + " ";
	}

	public final String GetTargetLangInfo() {
		return this.TargetVersion;
	}

	public abstract boolean StartCodeGeneration(ZNode Node, boolean IsInteractive);

	@ZenMethod public ZType GetFieldType(ZType BaseType, String Name) {
		return ZType.VarType;     // undefined
	}

	@ZenMethod public ZType GetSetterType(ZType BaseType, String Name) {
		return ZType.VarType;     // undefined
	}

	@ZenMethod public ZFuncType GetConstructorFuncType(ZType ClassType, ZListNode List) {
		//return null;              // undefined and undefined error
		return ZFuncType._FuncType;    // undefined and no error
	}

	@ZenMethod public ZFuncType GetMethodFuncType(ZType RecvType, String MethodName, ZListNode List) {
		//return null;              // undefined and undefined error
		return ZFuncType._FuncType;     // undefined and no error
	}

	// Naming

	public final int GetUniqueNumber() {
		@Var int UniqueNumber = this.UniqueNumber;
		this.UniqueNumber = this.UniqueNumber + 1;
		return UniqueNumber;
	}

	public final String NameUniqueSymbol(String Symbol) {
		return Symbol + "Z" + this.GetUniqueNumber();
	}

	public final String NameClass(ZType ClassType) {
		return ClassType.ShortName + "" + ClassType.TypeId;
	}

	//
	public final void SetDefinedFunc(ZFunc Func) {
		this.DefinedFuncMap.put(Func.GetSignature(), Func);
	}

	public final ZPrototype SetPrototype(ZNode Node, String FuncName, ZFuncType FuncType) {
		@Var ZFunc Func = this.GetDefinedFunc(FuncName, FuncType);
		if(Func != null) {
			if(!FuncType.Equals(Func.GetFuncType())) {
				ZLogger._LogError(Node.SourceToken, "function has been defined diffrently: " + Func.GetFuncType());
				return null;
			}
			if(Func instanceof ZPrototype) {
				return (ZPrototype)Func;
			}
			ZLogger._LogError(Node.SourceToken, "function has been defined as macro" + Func);
			return null;
		}
		@Var ZPrototype	Proto= new ZPrototype(0, FuncName, FuncType, Node.SourceToken);
		this.DefinedFuncMap.put(Proto.GetSignature(), Proto);
		return Proto;
	}

	public final ZFunc GetDefinedFunc(String GlobalName) {
		@Var ZFunc Func = this.DefinedFuncMap.GetOrNull(GlobalName);
		if(Func == null && LibZen._IsLetter(LibZen._GetChar(GlobalName, 0))) {
			//			System.out.println("AnotherName = " + GlobalName + ", " + LibZen._AnotherName(GlobalName));
			Func = this.DefinedFuncMap.GetOrNull(LibZen._AnotherName(GlobalName));
		}
		//System.out.println("sinature="+GlobalName+", func="+Func);
		return Func;
	}

	public final ZFunc GetDefinedFunc(String FuncName, ZFuncType FuncType) {
		return this.GetDefinedFunc(FuncType.StringfySignature(FuncName));
	}

	public final ZFunc GetDefinedFunc(String FuncName, ZType RecvType, int FuncParamSize) {
		return this.GetDefinedFunc(ZFunc._StringfySignature(FuncName, FuncParamSize, RecvType));
	}

	public final ZFunc LookupFunc(String FuncName, ZType RecvType, int FuncParamSize) {
		@Var ZFunc Func = this.GetDefinedFunc(ZFunc._StringfySignature(FuncName, FuncParamSize, RecvType));
		while(Func == null) {
			RecvType = RecvType.GetSuperType();
			if(RecvType == null) {
				break;
			}
			Func = this.GetDefinedFunc(ZFunc._StringfySignature(FuncName, FuncParamSize, RecvType));
			//			if(RecvType.IsVarType()) {
			//				break;
			//			}
		}
		return Func;
	}

	public final ZMacroFunc GetMacroFunc(String FuncName, ZType RecvType, int FuncParamSize) {
		@Var ZFunc Func = this.GetDefinedFunc(ZFunc._StringfySignature(FuncName, FuncParamSize, RecvType));
		if(Func instanceof ZMacroFunc) {
			return ((ZMacroFunc)Func);
		}
		return null;
	}

	public final void VisitUndefinedNode(ZNode Node) {
		ZErrorNode ErrorNode = new ZErrorNode(Node.ParentNode, Node.SourceToken, "undefined node:" + Node.toString());
		this.VisitErrorNode(ErrorNode);
	}

	@Override public final void VisitDefaultValueNode(ZDefaultValueNode Node) {
		this.VisitNullNode(new ZNullNode(Node.ParentNode, null));
	}

	@Override public void VisitSyntaxSugarNode(ZSyntaxSugarNode Node) {
		@Var ZDesugarNode DeNode = Node.DeSugar(this);
		@Var int i = 0;
		while(i < DeNode.GetAstSize()) {
			DeNode.AST[i].Accept(this);  // FIXME
			i = i + 1;
		}
	}

	public void RequireLibrary(String resourcePath) {
		// TODO Auto-generated method stub

	}

}
