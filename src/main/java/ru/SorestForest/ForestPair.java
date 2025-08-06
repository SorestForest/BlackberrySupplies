package ru.SorestForest;

public class ForestPair<A, B> {

    public A l;
    public B r;

    public ForestPair(A left, B right) {
        l = left; r = right;
    }

    public static <A, B> ForestPair<A, B> of(A l, B r) {
        return new ForestPair<>(l, r);
    }
}
