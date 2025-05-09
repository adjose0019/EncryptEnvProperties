package com.dizan.plugins.encryptenvproperties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Mojo(name = "encrypt-env-properties", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class EncryptEnvPropertiesMojo extends AbstractMojo {

    @Parameter(property = "project.basedir", readonly = true, required = true)
    private File baseDir;

    @Parameter(property = "secretKey", required = true)
    private String secretKey;

    @Override
    public void execute() throws MojoExecutionException {
        Path inputRoot = Paths.get(baseDir.getAbsolutePath(), "src", "main", "properties-noencript", "properties");
        Path outputRoot = Paths.get(baseDir.getAbsolutePath(), "properties");
        Path exclusionsFile = Paths.get(baseDir.getAbsolutePath(), "src", "main", "properties-noencript", "encrypt-env.properties");

        if (!Files.exists(inputRoot)) {
            throw new MojoExecutionException("No existe la carpeta de entrada: " + inputRoot);
        }

        Set<String> excludedKeys = loadExcludedKeys(exclusionsFile);

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        encryptor.setPassword(secretKey);
        encryptor.setIvGenerator(new RandomIvGenerator());

        try (Stream<Path> files = Files.walk(inputRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".properties"))
                    .forEach(file -> {
                        try {
                            Path relativePath = inputRoot.relativize(file);
                            Path destFile = outputRoot.resolve(relativePath);
                            Files.createDirectories(destFile.getParent());

                            getLog().info("Encriptando: " + file);
                            encryptFile(encryptor, file.toFile(), destFile.toFile(), excludedKeys);
                        } catch (Exception e) {
                            throw new RuntimeException("Error procesando archivo: " + file, e);
                        }
                    });

            getLog().info("Cifrado completado. Archivos generados en: " + outputRoot);

        } catch (IOException e) {
            throw new MojoExecutionException("Error al recorrer archivos de entrada", e);
        }
    }

    private Set<String> loadExcludedKeys(Path configPath) {
        if (!Files.exists(configPath)) {
            getLog().info("No se encontró 'encrypt-env.properties'. No se excluirán claves.");
            return Collections.emptySet();
        }

        try (InputStream in = Files.newInputStream(configPath)) {
            Properties config = new Properties();
            config.load(in);
            return config.stringPropertyNames();
        } catch (IOException e) {
            getLog().warn("No se pudo leer 'encrypt-env.properties'. Se ignorarán exclusiones.");
            return Collections.emptySet();
        }
    }

    private void encryptFile(StandardPBEStringEncryptor encryptor, File inputFile, File outputFile, Set<String> excludedKeys) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || !line.contains("=")) {
                    writer.write(line);
                } else {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (excludedKeys.contains(key)) {
                        writer.write(key + "=" + value);
                    } else {
                        String encrypted = "ENC(" + encryptor.encrypt(value) + ")";
                        writer.write(key + "=" + encrypted);
                    }
                }
                writer.newLine();
            }
        }
    }
}
