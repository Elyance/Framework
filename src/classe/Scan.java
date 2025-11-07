package src.classe;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Scan {
    Class<?> annotation;

    public Scan(Class<?> annotation) {
        this.annotation = annotation;
    }

    @SuppressWarnings("unchecked")
    public List<Class<?>> getClassesAnnotatedWith() throws IOException {
        List<Class<?>> classes = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources("");

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            findClasses(directory, "", classes);
        }
        // Pour chaque package, on doit récupérer les classes
        return classes;
    }

    @SuppressWarnings("unchecked")
    private void findClasses(File directory, String packageName, List<Class<?>> classes) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findClasses(file, packageName + file.getName() + ".", classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + file.getName().replace(".class", "");
                try {
                    Class<?> cls = Class.forName(className);
                    if (cls.isAnnotationPresent((Class<? extends Annotation>) annotation)) {
                        classes.add(cls);
                    }
                } catch (Throwable e) {
                    // Ignorer les erreurs de chargement
                }
            }
        }
    }

}
