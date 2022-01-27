package asia.groovelab.blesample;

public class Functions {
	static interface FuncBase<R> {}

	public static interface Func0<R> extends FuncBase<R> {
		public abstract R invoke();
	}

	public static interface Func1<P1, R> extends FuncBase<R> {
		public abstract R invoke(P1 p1);
	}

	public static interface Func2<P1, P2,R> extends FuncBase<R> {
		public abstract R invoke(P1 p1, P2 p2);
	}
}
