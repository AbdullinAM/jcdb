package org.utbot.jcdb.impl.fields;

public class FieldB {

    int c;

    FieldB(int c) {
        this.c = c;
    }

    private void useCPrivate() {
        c = 10;
    }

    private void dumpCPrivate() {
        System.out.println(c);
    }


}
