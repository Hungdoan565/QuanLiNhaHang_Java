import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * Simple utility to update class files in a JAR
 * Usage: java UpdateJar.java
 */
public class UpdateJar {
    public static void main(String[] args) throws Exception {
        String jarFile = "target\\restaurant-pos-1.0.0.jar";
        String[] classFiles = {
            "target\\classes\\com\\restaurant\\view\\dialogs\\SplitBillDialog.class",
            "target\\classes\\com\\restaurant\\view\\dialogs\\MultiQRDialog.class",
            "target\\classes\\com\\restaurant\\view\\dialogs\\TransferQRDialog.class",
            "target\\classes\\com\\restaurant\\util\\QRCodeGenerator.class"
        };
        String[] entryNames = {
            "com/restaurant/view/dialogs/SplitBillDialog.class",
            "com/restaurant/view/dialogs/MultiQRDialog.class",
            "com/restaurant/view/dialogs/TransferQRDialog.class",
            "com/restaurant/util/QRCodeGenerator.class"
        };
        
        File tempJar = new File(jarFile + ".tmp");
        
        // Read original JAR and write to temp with updated class
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFile));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar), jis.getManifest())) {
            
            JarEntry entry;
            byte[] buffer = new byte[8192];
            
            while ((entry = jis.getNextJarEntry()) != null) {
                // Skip the classes we're updating
                boolean skip = false;
                for (String name : entryNames) {
                    if (entry.getName().equals(name)) {
                        System.out.println("Skipping old: " + entry.getName());
                        skip = true;
                        break;
                    }
                }
                if (skip) continue;
                
                jos.putNextEntry(new JarEntry(entry.getName()));
                int read;
                while ((read = jis.read(buffer)) != -1) {
                    jos.write(buffer, 0, read);
                }
                jos.closeEntry();
            }
            
            // Add updated classes
            for (int i = 0; i < classFiles.length; i++) {
                System.out.println("Adding new: " + entryNames[i]);
                jos.putNextEntry(new JarEntry(entryNames[i]));
                Files.copy(Paths.get(classFiles[i]), jos);
                jos.closeEntry();
            }
        }
        
        // Replace original JAR with updated one
        Files.delete(Paths.get(jarFile));
        Files.move(tempJar.toPath(), Paths.get(jarFile));
        
        System.out.println("JAR updated successfully!");
    }
}
