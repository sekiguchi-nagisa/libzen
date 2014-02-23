package zen.parser;

import zen.deps.Field;
import zen.deps.ZTokenFunction;

public final class ZTokenFunc {
	@Field public ZTokenFunction      Func;
	@Field public ZTokenFunc	ParentFunc;

	ZTokenFunc(ZTokenFunction Func, ZTokenFunc Parent) {
		this.Func = Func;
		this.ParentFunc = Parent;
	}

	@Override public final String toString() {
		return this.Func.toString();
	}

}