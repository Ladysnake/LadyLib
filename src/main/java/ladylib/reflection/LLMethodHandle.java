package ladylib.reflection;

import ladylib.misc.ReflectionFailedException;

import java.lang.invoke.MethodHandle;

public class LLMethodHandle {
    MethodHandle methodHandle;
    protected String name;

    LLMethodHandle(MethodHandle methodHandle, String name) {
        this.methodHandle = methodHandle;
        this.name = name;
    }

    public static class LLMethodHandle0<T, R> extends LLMethodHandle {
        LLMethodHandle0(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        @SuppressWarnings("unchecked")
        public R invoke(T thisRef) {
            try {
                return (R) methodHandle.invoke(thisRef);
            } catch (Throwable throwable) {
                throw new ReflectionFailedException(String.format("Could not invoke %s [%s] on %s", name, methodHandle, thisRef), throwable);
            }
        }
    }

    public static class LLMethodHandle1<T, P1, R> extends LLMethodHandle {
        LLMethodHandle1(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        @SuppressWarnings("unchecked")
        public R invoke(T thisRef, P1 arg) {
            try {
                return (R) methodHandle.invoke(thisRef, arg);
            } catch (Throwable throwable) {
                throw new ReflectionFailedException(String.format("Could not invoke %s [%s] on %s with arg %s", name, methodHandle, thisRef, arg), throwable);
            }
        }
    }

    public static class LLMethodHandle2<T, P1, P2, R> extends LLMethodHandle {
        LLMethodHandle2(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        @SuppressWarnings("unchecked")
        public R invoke(T thisRef, P1 arg1, P2 arg2) {
            try {
                return (R) methodHandle.invoke(thisRef, arg1, arg2);
            } catch (Throwable throwable) {
                throw new ReflectionFailedException(String.format("Could not invoke %s [%s] on %s with args %s, and %s", name, methodHandle, thisRef, arg1, arg2), throwable);
            }
        }
    }

    public static class LLMethodHandle3<T, P1, P2, P3, R> extends LLMethodHandle {
        LLMethodHandle3(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        @SuppressWarnings("unchecked")
        public R invoke(T thisRef, P1 arg1, P2 arg2, P3 arg3) {
            try {
                return (R) methodHandle.invoke(thisRef, arg1, arg2, arg3);
            } catch (Throwable throwable) {
                throw new ReflectionFailedException(String.format("Could not invoke %s [%s] on %s with args %s, %s and %s", name, methodHandle, thisRef, arg1, arg2, arg3), throwable);
            }
        }
    }

    public static class LLMethodHandle4<T, P1, P2, P3, P4, R> extends LLMethodHandle {
        LLMethodHandle4(MethodHandle methodHandle, String name) {
            super(methodHandle, name);
        }

        @SuppressWarnings("unchecked")
        public R invoke(T thisRef, P1 arg1, P2 arg2, P3 arg3, P4 arg4) {
            try {
                return (R) methodHandle.invoke(thisRef, arg1, arg2, arg3, arg4);
            } catch (Throwable throwable) {
                throw new ReflectionFailedException(String.format("Could not invoke %s [%s] on %s with args %s, %s, %s and %s", name, methodHandle, thisRef, arg1, arg2, arg3, arg4), throwable);
            }
        }
    }
}
