package org.acme.extension;

import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Demonstrates the Kotlin suspend function detection issue by setting a custom
 * FacadeClassLoader as the Thread Context ClassLoader (TCCL) during test discovery.
 * <p>
 * This mimics what Quarkus does in its test framework. The FacadeClassLoader loads
 * kotlin.* classes child-first, creating separate class instances that break JUnit's
 * identity check in KotlinReflectionUtils.isKotlinSuspendingFunction().
 */
public class CustomLauncherInterceptor
        implements LauncherDiscoveryListener, LauncherSessionListener, TestExecutionListener {

    private static FacadeClassLoader facadeLoader = null;
    private static ClassLoader origCl = null;

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        if (origCl == null) {
            origCl = Thread.currentThread().getContextClassLoader();
        }
        initializeFacadeClassLoader();
        adjustContextClassLoader();
    }

    private void initializeFacadeClassLoader() {
        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        if (currentCl == null || (currentCl != facadeLoader
                && !currentCl.getClass().getName().equals(FacadeClassLoader.class.getName()))) {
            if (facadeLoader == null) {
                facadeLoader = new FacadeClassLoader(currentCl);
            }
        }
    }

    @Override
    public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
        initializeFacadeClassLoader();
        adjustContextClassLoader();
    }

    private void adjustContextClassLoader() {
        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        if (facadeLoader != null && currentCl != facadeLoader) {
            Thread.currentThread().setContextClassLoader(facadeLoader);
        }
    }

    @Override
    public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
        // Keep the facade classloader set during discovery
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        clearContextClassloader();
    }

    private static void clearContextClassloader() {
        try {
            if (facadeLoader != null) {
                if (Thread.currentThread().getContextClassLoader() == facadeLoader) {
                    Thread.currentThread().setContextClassLoader(origCl);
                }
                facadeLoader.close();
                facadeLoader = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to close custom classloader", e);
        }
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // No-op
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        clearContextClassloader();
    }
}
