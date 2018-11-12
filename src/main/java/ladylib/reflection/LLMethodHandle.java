package ladylib.reflection;

import ladylib.misc.ReflectionFailedException;

import java.lang.invoke.MethodHandle;

public class LLMethodHandle<R> {
    private MethodHandle methodHandle;
    private String name;

    public LLMethodHandle(MethodHandle methodHandle, String name) {
        this.methodHandle = methodHandle;
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    protected R invoke(Object... args) {
        try {
            return (R) methodHandle.invoke(args);
        } catch (Throwable throwable) {
            throw new ReflectionFailedException("Could not invoke " + name, throwable);
        }
    }

    public static class LLMethodHandle0<R> extends LLMethodHandle<R> {
        public LLMethodHandle0(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        public R invoke() {
            return super.invoke();
        }
    }

    public static class LLMethodHandle1<P1, R> extends LLMethodHandle<R> {
        public LLMethodHandle1(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        public R invoke(P1 arg) {
            return super.invoke(arg);
        }
    }

    public static class LLMethodHandle2<P1, P2, R> extends LLMethodHandle<R> {
        public LLMethodHandle2(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        public R invoke(P1 arg1, P2 arg2) {
            return super.invoke(arg1, arg2);
        }
    }

    public static class LLMethodHandle3<P1, P2, P3, R> extends LLMethodHandle<R> {
        public LLMethodHandle3(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        public R invoke(P1 arg1, P2 arg2, P3 arg3) {
            return super.invoke(arg1, arg2, arg3);
        }
    }

    public static class LLMethodHandle4<P1, P2, P3, P4, R> extends LLMethodHandle<R> {
        public LLMethodHandle4(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        public R invoke(P1 arg1, P2 arg2, P3 arg3, P4 arg4) {
            return super.invoke(arg1, arg2, arg3, arg4);
        }
    }



}
