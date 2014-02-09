package zen.parser;

import zen.deps.Field;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.deps.Var;

public final class ZSourceContext extends ZSource {

	@Field int SourcePosition = 0;
	public ZSourceContext(String FileName, int LineNumber, String Source, ZTokenContext TokenContext) {
		super(FileName, LineNumber, Source, TokenContext);
	}

	public final int GetCharCode() {
		return ZUtils.AsciiToTokenMatrixIndex(LibZen.CharAt(this.SourceText, this.SourcePosition));
	}

	public final int GetPosition() {
		return this.SourcePosition;
	}

	public final boolean HasChar() {
		return this.SourceText.length() - this.SourcePosition > 0;
	}

	public final char ParseChar() {
		return this.SourceText.charAt(this.SourcePosition);
	}

	public final char ParseChar(int n) {
		if(this.SourcePosition+n < this.SourceText.length()) {
			return this.SourceText.charAt(this.SourcePosition+n);
		}
		return '\0';
	}

	public final void MoveNext() {
		this.SourcePosition = this.SourcePosition + 1;
	}

	public final void SkipWhiteSpace() {
		while(this.HasChar()) {
			@Var char ch = this.ParseChar();
			if(ch != ' ' && ch != '\t') {
				break;
			}
			this.MoveNext();
		}
	}

	public void FoundIndent(int StartIndex, int EndIndex) {
		@Var ZToken Token = new ZIndentToken(this, StartIndex, EndIndex);
		this.SourcePosition = EndIndex;
		this.TokenContext.TokenList.add(Token);
	}

	public void Tokenize(int StartIndex, int EndIndex) {
		this.SourcePosition = EndIndex;
		if(StartIndex < EndIndex && EndIndex <= this.SourceText.length()) {
			@Var ZToken Token = new ZToken(this, StartIndex, EndIndex);
			this.TokenContext.TokenList.add(Token);
		}
	}

	public void Tokenize(String PatternName, int StartIndex, int EndIndex) {
		this.SourcePosition = EndIndex;
		if(StartIndex < EndIndex && EndIndex <= this.SourceText.length()) {
			ZSyntaxPattern Pattern = this.TokenContext.NameSpace.GetSyntaxPattern(PatternName);
			if(Pattern == null) {
				this.Panic(StartIndex, "unregistered token pattern: " + PatternName);
				@Var ZToken Token = new ZToken(this, StartIndex, EndIndex);
				this.TokenContext.TokenList.add(Token);
			}
			else {
				@Var ZToken Token = new ZPatternToken(this, StartIndex, EndIndex, Pattern);
				this.TokenContext.TokenList.add(Token);
			}
		}
	}

	public boolean IsDefinedSyntax(int StartIndex, int EndIndex) {
		if(EndIndex < this.SourceText.length()) {
			@Var ZNameSpace NameSpace = this.TokenContext.NameSpace;
			@Var String Token = this.SourceText.substring(StartIndex, EndIndex);
			@Var ZSyntaxPattern Pattern = NameSpace.GetRightSyntaxPattern(Token);
			if(Pattern != null) {
				return true;
			}
		}
		return false;
	}

	public final void TokenizeDefinedSymbol(int StartIndex) {
		//		@Var int StartIndex = this.SourcePosition;
		@Var int EndIndex = StartIndex + 2;
		while(this.IsDefinedSyntax(StartIndex, EndIndex)) {
			EndIndex = EndIndex + 1;
		}
		this.Tokenize(StartIndex, EndIndex-1);
	}

	private final void ApplyTokenFunc(ZTokenFunc TokenFunc) {
		@Var int RollbackPosition = this.SourcePosition;
		while(TokenFunc != null) {
			this.SourcePosition = RollbackPosition;
			if(LibNative.ApplyTokenFunc(TokenFunc.Func, this)) {
				return;
			}
			TokenFunc = TokenFunc.ParentFunc;
		}
		this.TokenizeDefinedSymbol(RollbackPosition);
	}

	public boolean DoTokenize() {
		@Var int TokenSize = this.TokenContext.TokenList.size();
		@Var int CheckPosition = this.SourcePosition;
		while(this.HasChar()) {
			@Var int CharCode = this.GetCharCode();
			@Var ZTokenFunc TokenFunc = this.TokenContext.NameSpace.GetTokenFunc(CharCode);
			this.ApplyTokenFunc(TokenFunc);
			if(this.TokenContext.TokenList.size() > TokenSize) {
				break;
			}
			if(this.SourcePosition == CheckPosition) {
				LibNative.println("Buggy TokenFunc: " + TokenFunc);
				this.MoveNext();
			}
		}
		//this.Dump();
		if(this.TokenContext.TokenList.size() > TokenSize) {
			return true;
		}
		return false;
	}


}