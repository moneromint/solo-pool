package com.moneromint.solo.utils;

import java.util.Objects;

public class ImmutablePair<T, U> {
    private final T lhs;
    private final U rhs;

    public ImmutablePair(T lhs, U rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public T getLhs() {
        return lhs;
    }

    public U getRhs() {
        return rhs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutablePair<?, ?> that = (ImmutablePair<?, ?>) o;
        return Objects.equals(lhs, that.lhs) &&
                Objects.equals(rhs, that.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs);
    }
}
