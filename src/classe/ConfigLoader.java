package src.classe;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class ConfigLoader {
    private static Properties properties;

    static {
        try {
            properties = new Properties();
            String projectPath = System.getProperty("user.dir");
            File configFile = new File(projectPath, "application.properties");
            
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    System.out.println("application.properties chargé depuis: " + configFile.getAbsolutePath());
                }
            } else {
                System.out.println("application.properties non trouvé à: " + configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}