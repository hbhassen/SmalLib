package com.hmiso.examples.demo2;

import jakarta.ws.rs.core.Application;

import java.util.Set;

public class Demo2Application extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(WhoAmIResource.class, MessageResource.class);
    }
}
