package com.example.wms.shared.application;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T required(Supplier<T> operation);
}
