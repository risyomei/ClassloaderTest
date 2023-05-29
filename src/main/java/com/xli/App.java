package com.xli;

// import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class CustomThreadFactory implements ThreadFactory {
    private final ClassLoader classLoader;

    public CustomThreadFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setContextClassLoader(classLoader);
        return thread;
    }
}
public class App 
{
    public String getPath(Class<?> cls) {
        ProtectionDomain protectionDomain = cls.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL location = codeSource.getLocation();
        return location.getPath();
    }
    static void getClassLoaderChain(ClassLoader currentClassLoader) {
        System.out.println("-------");
        while (currentClassLoader != null) {
            System.out.println(currentClassLoader);
            currentClassLoader = currentClassLoader.getParent();
        }
    }
    public static void main( String[] args ) throws MalformedURLException {

        /*
            java -cp 'target/ClassLoaderTest-1.0.jar:/tmp/TargetClass-1.0.jar'  com.xli.App
            /tmp/TargetClass-1.0.jar is added into the CLASSPATH.
         */
        Target oldInstance = new Target();
        oldInstance.printVersion();

        URL[] url = new URL[1];
        url[0]=new URL("file:///tmp/TargetClass-1.1.jar");
        ClassLoader cl = new ChildFirstClassloader(url, App.class.getClassLoader());
        CustomThreadFactory cf = new CustomThreadFactory(cl);
        ExecutorService executor = Executors.newFixedThreadPool(20, cf);
        // ExecutorService executor = Executors.newFixedThreadPool(20);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 1; i++) {
                int finalI = i;
                Future<?> future = executor.submit(() -> {
                    try {
                        // Working Solution:
                        /*
                            利用が面倒、全部Reflectで書き換えが必要
                                val launcher = new InProcessLauncher()
                                  .setMainClass(mainClass)
                                  .setVerbose(true)
                                  .addSparkArg("--conf", s"${dummyBuilder.convertConfigKey(k)}=$v") などなど
                         */
                        Class<?> newClass = Class.forName("com.xli.Target", true, cl);
                        Object newInstance = newClass.getDeclaredConstructor().newInstance();
                        Method method = newClass.getMethod("printVersion");
                        method.invoke(newInstance);

                        // Working Solution
                        /*
                            利用が面倒、KyuubiでダミーのInterfaceを作成し、InProcessLauncherでImplementする必要がある
                         */
//                        Class<?> newClass = Class.forName("com.xli.Target", true, cl);
//                        DynamicTargetCaller newInstance = (DynamicTargetCaller) newClass.getDeclaredConstructor().newInstance();
//                        newInstance.printVersion();

                        // Not Working 1: Doesn't Archive What we want
                        /*
                            Version 1.0
                            ChildFirstClassloader init
                            Version 1.0
                         */
//                        getClassLoaderChain(Thread.currentThread().getContextClassLoader());
//                        Thread.currentThread().setContextClassLoader(cl);
//                        getClassLoaderChain(Thread.currentThread().getContextClassLoader());
//                        Target newInstance = new Target();
//                        newInstance.printVersion();


                        // Not Working 2: Got Exception
                        /*
                            Caused by: java.lang.RuntimeException: java.lang.ClassCastException: class com.xli.Target cannot be cast to class com.xli.Target (com.xli.Target is in unnamed module of loader com.xli.ChildFirstClassloader @44e81672; com.xli.Target is in unnamed module of loader 'app')
                            at com.xli.App.lambda$main$0(App.java:45)
                            at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:515)
                            at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
                            at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
                            at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
                            at java.base/java.lang.Thread.run(Thread.java:829)
                            Caused by: java.lang.ClassCastException: class com.xli.Target cannot be cast to class com.xli.Target (com.xli.Target is in unnamed module of loader com.xli.ChildFirstClassloader @44e81672; com.xli.Target is in unnamed module of loader 'app')
                            at com.xli.App.lambda$main$0(App.java:42)
                        */
//                        Class<?> newClass = Class.forName("com.xli.Target", true, cl);
//                        Target newInstance = (Target) newClass.getDeclaredConstructor().newInstance();
//                        newInstance.printVersion();

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                futures.add(future);
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }   catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}

