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

        List<Package> myPackages = new ArrayList<>();
        Package[] listPackages = Package.getPackages();

        for (Package package1 : listPackages) {
            if (!package1.getName().startsWith("java") && !package1.getName().startsWith("sun") && !package1.getName().startsWith("jdk")) {
                // System.out.println(package1.getName());
                // classes.add(package1.getClass());
                myPackages.add(package1);
            } 
        } 
        // Pour chaque package, on doit récupérer les classes
        for (Package p : myPackages) {
            try {
                List<Class<?>> classesInPackage = getClasses(p);
                for (Class<?> cls : classesInPackage) {
                    if (cls.isAnnotationPresent((Class<? extends Annotation>) annotation)) {
                        classes.add(cls);
                    }
                }
            } catch (ClassNotFoundException e) {
                // Classe non trouvée dans ce package, ignorer
            }
        }
        return classes;
    }

    public List<Class<?>> getClasses(Package p) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = p.getName().replace('.', '/');

        // Récupérer les fichiers .class dans le répertoire correspondant
        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            if (directory.exists()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".class")) {
                            String className = p.getName() + "." + file.getName().replace(".class", "");
                            try {
                                Class<?> cls = Class.forName(className);
                                classes.add(cls);
                            } catch (ClassNotFoundException e) {
                                // Classe non trouvée, ignorer
                            }
                        }
                    }
                }
            }
        }
        return classes;
    }
}
