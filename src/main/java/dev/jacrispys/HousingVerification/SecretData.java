package dev.jacrispys.HousingVerification;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SecretData {
    private static Yaml yaml = new Yaml();
    private static Map<String, Object> loginInfo;

    public static void initLoginInfo() throws IOException {
        yaml = new Yaml();
        loginInfo = yaml.load(generateSecretData());
    }

    protected static InputStream generateSecretData() throws IOException {
        if (SecretData.class.getClassLoader().getResourceAsStream("loginInfo.yml") == null) {
            File file = new File("src/main/resources/loginInfo.yml");
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            if (file.createNewFile()) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("DATA_BASE_PASS", " ");
                fileInfo.put("DB_HOST", "localhost");
                fileInfo.put("TOKEN", " ");
                FileWriter writer = new FileWriter(file.getPath());
                fileInfo.keySet().forEach(key -> {
                    try {
                        writer.write(key + ": " + fileInfo.get(key) + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                writer.flush();
                writer.close();
                return new FileInputStream(file);
            } else throw new FileNotFoundException("Could not create required config file!");

        } else return SecretData.class.getClassLoader().getResourceAsStream("loginInfo.yml");
    }

    public static String getDataBasePass() {
        return (String) loginInfo.get("DATA_BASE_PASS");
    }

    public static String getDBHost() {
        return (String) loginInfo.get("DB_HOST");
    }

    public static String getToken() {
        return (String) loginInfo.get("TOKEN");
    }
}
