package com.example.core.net;

import java.util.function.Supplier;

public class SupplierHandler<T> implements Handler {
    private final Supplier<T> supplier;

    public SupplierHandler(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        return supplier.get();
    }
}