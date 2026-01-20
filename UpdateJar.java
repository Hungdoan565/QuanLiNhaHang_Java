import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * Simple utility to update a class file in a JAR
 * Usage: java UpdateJar.java
 */
public class UpdateJar {
    public static void main(String[] args) throws Exception {
        String jarFile = "target\\restaurant-pos-1.0.0.jar";
        String classFile = "target\\classes\\com\\restaurant\\view\\dialogs\\SplitBillDialog.class";
        String entryName = "com/restaurant/view/dialogs/SplitBillDialog.class";
        
        File tempJar = new File(jarFile + ".tmp");
        
        // Read original JAR and write to temp with updated class
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFile));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar), jis.getManifest())) {
            
            JarEntry entry;
            byte[] buffer = new byte[8192];
            
            while ((entry = jis.getNextJarEntry()) != null) {
                // Skip the class we're updating
                if (entry.getName().equals(entryName)) {
                    System.out.println("Skipping old: " + entry.getName());
                    continue;
                }
                
                jos.putNextEntry(new JarEntry(entry.getName()));
                int read;
                while ((read = jis.read(buffer)) != -1) {
                    jos.write(buffer, 0, read);
                }
                jos.closeEntry();
            }
            
            // Add updated class
            System.out.println("Adding new: " + entryName);
            jos.putNextEntry(new JarEntry(entryName));
            Files.copy(Paths.get(classFile), jos);
            jos.closeEntry();
        }
        
        // Replace original JAR with updated one
        Files.delete(Paths.get(jarFile));
        Files.move(tempJar.toPath(), Paths.get(jarFile));
        
        System.out.println("JAR updated successfully!");
    }
}
