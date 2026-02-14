package org.acme.extension;

/**
 * A classloader that demonstrates the class identity issue with JUnit's Kotlin
 * suspend function detection.
 *
 * When kotlin.coroutines.Continuation is loaded by this classloader (child-first),
 * it creates a DIFFERENT class instance than the one JUnit's KotlinReflectionUtils
 * cached during its static initialization. This causes the identity check
 * (method.getParameterTypes()[parameterCount - 1] == kotlinCoroutineContinuation)
 * to fail in JUnit's isKotlinSuspendingFunction() method.
 *
 * This mimics what Quarkus's FacadeClassLoader does, which also loads Kotlin classes
 * in isolation.
*/
public class FacadeClassLoader extends java.net.URLClassLoader {

    public FacadeClassLoader(ClassLoader delegate) {
        // Pass the URLs from the delegate classloader
        super(getClasspathURLs(delegate), delegate);
    }

    private static java.net.URL[] getClasspathURLs(ClassLoader loader) {
        if (loader instanceof java.net.URLClassLoader) {
            return ((java.net.URLClassLoader) loader).getURLs();
        }
        // For the system classloader, parse java.class.path
        String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.isEmpty()) {
            return new java.net.URL[0];
        }
        String[] paths = classpath.split(java.io.File.pathSeparator);
        java.util.List<java.net.URL> urls = new java.util.ArrayList<>();
        for (String path : paths) {
            try {
                urls.add(new java.io.File(path).toURI().toURL());
            } catch (java.net.MalformedURLException e) {
                // Skip invalid paths
            }
        }
        return urls.toArray(new java.net.URL[0]);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                // KEY: Load kotlin.* classes child-first (from our own URLs)
                // This creates a separate class instance for kotlin.coroutines.Continuation
                // that breaks JUnit's identity check in isKotlinSuspendingFunction()
                if (name.startsWith("kotlin.")) {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        c = getParent().loadClass(name);
                    }
                } else {
                    c = getParent().loadClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
