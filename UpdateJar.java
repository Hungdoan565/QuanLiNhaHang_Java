import java.io.*;
import java.nio.file.*;
import java.util.*;
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
            "target\\classes\\com\\restaurant\\util\\QRCodeGenerator.class",
            "target\\classes\\com\\restaurant\\model\\Customer.class",
            "target\\classes\\com\\restaurant\\model\\Customer$CustomerTier.class",
            "target\\classes\\com\\restaurant\\model\\Customer$Gender.class",
            "target\\classes\\com\\restaurant\\model\\Notification.class",
            "target\\classes\\com\\restaurant\\model\\Notification$NotificationType.class",
            "target\\classes\\com\\restaurant\\model\\Reservation.class",
            "target\\classes\\com\\restaurant\\model\\Reservation$Status.class",
            "target\\classes\\com\\restaurant\\view\\panels\\POSPanel.class",
            "target\\classes\\com\\restaurant\\view\\panels\\POSPanel$1.class",
            "target\\classes\\com\\restaurant\\view\\panels\\POSPanel$2.class",
            "target\\classes\\com\\restaurant\\view\\panels\\POSPanel$3.class",
            "target\\classes\\com\\restaurant\\view\\panels\\POSPanel$4.class",
            "target\\classes\\com\\restaurant\\view\\panels\\POSPanel$5.class",
            "target\\classes\\com\\restaurant\\view\\panels\\POSPanel$6.class",
            "target\\classes\\com\\restaurant\\view\\panels\\POSPanel$OrderItem.class"
        };
        String[] entryNames = {
            "com/restaurant/view/dialogs/SplitBillDialog.class",
            "com/restaurant/view/dialogs/MultiQRDialog.class",
            "com/restaurant/view/dialogs/TransferQRDialog.class",
            "com/restaurant/util/QRCodeGenerator.class",
            "com/restaurant/model/Customer.class",
            "com/restaurant/model/Customer$CustomerTier.class",
            "com/restaurant/model/Customer$Gender.class",
            "com/restaurant/model/Notification.class",
            "com/restaurant/model/Notification$NotificationType.class",
            "com/restaurant/model/Reservation.class",
            "com/restaurant/model/Reservation$Status.class",
            "com/restaurant/view/panels/POSPanel.class",
            "com/restaurant/view/panels/POSPanel$1.class",
            "com/restaurant/view/panels/POSPanel$2.class",
            "com/restaurant/view/panels/POSPanel$3.class",
            "com/restaurant/view/panels/POSPanel$4.class",
            "com/restaurant/view/panels/POSPanel$5.class",
            "com/restaurant/view/panels/POSPanel$6.class",
            "com/restaurant/view/panels/POSPanel$OrderItem.class"
        };
        
        List<String> classFileList = new ArrayList<>(Arrays.asList(classFiles));
        List<String> entryNameList = new ArrayList<>(Arrays.asList(entryNames));
        
        addPanelClasses("CustomerPanel", classFileList, entryNameList);
        addPanelClasses("PromotionPanel", classFileList, entryNameList);
        addPanelClasses("WaiterPanel", classFileList, entryNameList);
        addComponentClasses("Sidebar", classFileList, entryNameList);
        
        // Add MainFrame
        classFileList.add("target\\classes\\com\\restaurant\\view\\MainFrame.class");
        entryNameList.add("com/restaurant/view/MainFrame.class");
        classFileList.add("target\\classes\\com\\restaurant\\view\\MainFrame$1.class");
        entryNameList.add("com/restaurant/view/MainFrame$1.class");
        classFileList.add("target\\classes\\com\\restaurant\\view\\MainFrame$2.class");
        entryNameList.add("com/restaurant/view/MainFrame$2.class");
        classFileList.add("target\\classes\\com\\restaurant\\view\\MainFrame$3.class");
        entryNameList.add("com/restaurant/view/MainFrame$3.class");
        classFileList.add("target\\classes\\com\\restaurant\\view\\MainFrame$4.class");
        entryNameList.add("com/restaurant/view/MainFrame$4.class");
        classFileList.add("target\\classes\\com\\restaurant\\view\\MainFrame$5.class");
        entryNameList.add("com/restaurant/view/MainFrame$5.class");
        
        File tempJar = new File(jarFile + ".tmp");
        
        // Read original JAR and write to temp with updated class
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFile));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar), jis.getManifest())) {
            
            JarEntry entry;
            byte[] buffer = new byte[8192];
            
            while ((entry = jis.getNextJarEntry()) != null) {
                // Skip the classes we're updating
                boolean skip = false;
                for (String name : entryNameList) {
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
            for (int i = 0; i < classFileList.size(); i++) {
                System.out.println("Adding new: " + entryNameList.get(i));
                jos.putNextEntry(new JarEntry(entryNameList.get(i)));
                Files.copy(Paths.get(classFileList.get(i)), jos);
                jos.closeEntry();
            }
        }
        
        // Replace original JAR with updated one
        Files.delete(Paths.get(jarFile));
        Files.move(tempJar.toPath(), Paths.get(jarFile));
        
        System.out.println("JAR updated successfully!");
    }
    
    private static void addPanelClasses(String panelPrefix, List<String> classFiles, List<String> entryNames) throws IOException {
        Path panelDir = Paths.get("target", "classes", "com", "restaurant", "view", "panels");
        if (!Files.exists(panelDir)) return;
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(panelDir, panelPrefix + "*.class")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String entryName = "com/restaurant/view/panels/" + fileName;
                String classFile = path.toString();
                if (!classFiles.contains(classFile)) {
                    classFiles.add(classFile);
                    entryNames.add(entryName);
                }
            }
        }
    }
    
    private static void addComponentClasses(String componentPrefix, List<String> classFiles, List<String> entryNames) throws IOException {
        Path componentDir = Paths.get("target", "classes", "com", "restaurant", "view", "components");
        if (!Files.exists(componentDir)) return;
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(componentDir, componentPrefix + "*.class")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String entryName = "com/restaurant/view/components/" + fileName;
                String classFile = path.toString();
                if (!classFiles.contains(classFile)) {
                    classFiles.add(classFile);
                    entryNames.add(entryName);
                }
            }
        }
    }
}
