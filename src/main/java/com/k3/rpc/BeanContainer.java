package com.k3.rpc;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 2019-09-09 11:41
 * Author : Michael.
 */
public class BeanContainer {

    private static final Logger logger = LoggerFactory.getLogger(BeanContainer.class);

    private static volatile BeanContainer beanContainer;
    private final Map<String, Object> beans;

    private BeanContainer() {
        this.beans = new ConcurrentHashMap<>();
        scanAnnotation(".*");
        printBeans();
    }

    private void printBeans() {
        for (String cls : beans.keySet()) {
            logger.info("Service [ {} ] add to bean container.", cls);
        }
    }

    private void scanAnnotation(String pkg) {
        logger.info("Scanning path [ {} ] RpcService annotation...", pkg);
        try {
            Reflections reflections = new Reflections(pkg);
            Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RpcService.class);
            for (Class<?> clazz : classes) {
                Object bean = clazz.newInstance();
                RpcService annotation = clazz.getAnnotation(RpcService.class);
                this.beans.put(annotation.value().getName(), bean);
            }
        } catch (Exception e) {
            throw new RuntimeException("Scan custom annotation [ " + RpcService.class.getName() + " ] failed.", e);
        }
    }

    public Map<String, Object> getAllServiceBeans() {
        return Collections.unmodifiableMap(this.beans);
    }

    public static BeanContainer getInstance() {
        if (beanContainer == null) {
            synchronized (BeanContainer.class) {
                if (beanContainer == null) {
                    beanContainer = new BeanContainer();
                }
            }
        }
        return beanContainer;
    }
}
