import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Decryptor {
    public static BufferedImage decryptProtectedImage(String inputPath, String password) {
        if (inputPath == null || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
    
        try {
            // Read the encrypted file
            byte[] combined = java.nio.file.Files.readAllBytes(new File(inputPath).toPath());
            
            // Extract IV and encrypted bytes
            byte[] iv = new byte[16];
            byte[] encryptedBytes = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);
    
            // Create key from password
            byte[] key = password.getBytes("UTF-8");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                java.security.MessageDigest.getInstance("SHA-256").digest(key),
                "AES"
            );
    
            // Initialize cipher for decryption
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, new javax.crypto.spec.IvParameterSpec(iv));
    
            // Decrypt the image bytes
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
    
            // Convert bytes back to image
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(decryptedBytes);
            return ImageIO.read(bis);
    
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting protected image: " + e.getMessage(), e);
        }
    }
    public static void main(String[] args) {
        try {
            // Use the same password that was used for encryption
            String password = "yourSecurePassword123";
            
            // Decrypt the protected image
            BufferedImage decryptedImage = decryptProtectedImage(
                "protected_output.png", 
                password
            );
            
            // Save it as a regular viewable PNG
            File outputFile = new File("viewable_image.png");
            ImageIO.write(decryptedImage, "PNG", outputFile);
            
            System.out.println("Image successfully decrypted and saved as 'viewable_image.png'");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}