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

//ifdef  JAVA
import java.io.InputStream;
//endif VAJA

class GtScriptRunner {
	public static String LoadFile(String Path) {
		if(LangDeps.HasFile(Path)) {
			return LangDeps.LoadFile(Path);
		}
		return null;
	}
	public static String ExecuteScript(String Path, String Target) {
		/*local*/String[] cmd = {"java", "-jar", "GreenTeaScript.jar", "--" + Target, Path};
		/*local*/String Result = "";
		//FIXME
//ifdef JAVA
		try {
			/*local*/Process proc = new ProcessBuilder(cmd).start();
			proc.waitFor();
			if(proc.exitValue() != 0) {
				return null;
			}
			/*local*/InputStream stdout = proc.getInputStream();
			/*local*/byte[] buffer = new byte[512];
			/*local*/int read = 0;
			while(read > -1) {
				read = stdout.read(buffer, 0, buffer.length);
				if(read > -1) {
					Result += new String(buffer, 0, read);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
//endif VAJA
		return Result;
	}

	public static void Test(String Target, String ScriptPath, String ResultPath) {
		//LangDeps.println("Testing " + ScriptPath + " (Target:" + Target + ") ... ");
		/*local*/String Expected = GtScriptRunner.LoadFile(ResultPath);
		/*local*/String Actual   = GtScriptRunner.ExecuteScript(ScriptPath, Target);
		LangDeps.Assert(Expected.equals(Actual));
		//LangDeps.println("Testing " + ScriptPath + " (Target:" + Target + ") ... OK");
	}
}

public class GreenTeaScriptTest {
	public static void TestToken(GtContext Context, String Source, String[] TokenTestList) {
		/*local*/GtNameSpace NameSpace = Context.DefaultNameSpace;
		/*local*/GtTokenContext TokenContext = new GtTokenContext(NameSpace, Source, 1);
		/*local*/int i = 0;
		while(i < TokenTestList.length) {
			/*local*/String TokenText = TokenTestList[i];
			LangDeps.Assert(TokenContext.MatchToken(TokenText));
			i = i + 1;
		}
	}

	public static GtContext CreateContext() {
		/*local*/String CodeGeneratorName = "Java";
		/*local*/GtGenerator Generator = LangDeps.CodeGenerator(CodeGeneratorName);
		return new GtContext(new DScriptGrammar(), Generator);
	}

	public static void TokenizeOperator0() {
		GtContext Context = GreenTeaScriptTest.CreateContext();
		/*local*/String[] TokenTestList0 = {"1", "||", "2"};
		GreenTeaScriptTest.TestToken(Context, "1 || 2", TokenTestList0);

		/*local*/String[] TokenTestList1 = {"1", "==", "2"};
		GreenTeaScriptTest.TestToken(Context, "1 == 2", TokenTestList1);

		/*local*/String[] TokenTestList2 = {"1", "!=", "2"};
		GreenTeaScriptTest.TestToken(Context, "1 != 2", TokenTestList2);

		/*local*/String[] TokenTestList3 = {"1", "*", "=", "2"};
		GreenTeaScriptTest.TestToken(Context, "1 *= 2", TokenTestList3);

		/*local*/String[] TokenTestList4 = {"1", "=", "2"};
		GreenTeaScriptTest.TestToken(Context, "1 = 2", TokenTestList4);
	}

	public static void TokenizeStatement() {
		GtContext Context = GreenTeaScriptTest.CreateContext();
		/*local*/String[] TokenTestList0 = {"int", "+", "(", "int", "x", ")", ";"};
		GreenTeaScriptTest.TestToken(Context, "int + (int x);", TokenTestList0);
	}

	public static void main(String[] args) {
		if(args.length != 3) {
			GreenTeaScriptTest.TokenizeOperator0();
			GreenTeaScriptTest.TokenizeStatement();
		}
		else {
			GtScriptRunner.Test(args[0], args[1], args[2]);
		}
	}
}