package org.utbot.jcdb.impl.usages;

import java.util.Collection;
import java.util.List;

public class Generics<T extends Collection<?>> {

    private T niceField;
    private List<? extends T> niceList;

    public <W extends Collection<T>> void merge(Generics<T> generics) {
    }

    public <W extends Collection<T>> W merge1(Generics<T> generics) {
        return null;
    }

}