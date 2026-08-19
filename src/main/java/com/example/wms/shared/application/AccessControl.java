package com.example.wms.shared.application;

public interface AccessControl {

    void requireAny(Role... roles);
}
