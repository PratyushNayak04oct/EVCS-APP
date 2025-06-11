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
            // Check if correct number of arguments provided
            if (args.length != 3) {
                System.err.println("Usage: java Decryptor <inputPath> <password> <outputPath>");
                System.err.println("  inputPath: Path to the encrypted image file");
                System.err.println("  password: Password used for encryption");
                System.err.println("  outputPath: Path where decrypted image will be saved");
                System.exit(1);
            }
            
            String inputPath = args[0];
            String password = args[1];
            String outputPath = args[2];
            
            System.out.println("Starting decryption process...");
            System.out.println("Input file: " + inputPath);
            System.out.println("Output file: " + outputPath);
            
            // Verify input file exists
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                throw new RuntimeException("Input file does not exist: " + inputPath);
            }
            
            if (!inputFile.canRead()) {
                throw new RuntimeException("Cannot read input file: " + inputPath);
            }
            
            System.out.println("Input file verified - Size: " + inputFile.length() + " bytes");
            
            // Decrypt the protected image
            BufferedImage decryptedImage = decryptProtectedImage(inputPath, password);
            
            if (decryptedImage == null) {
                throw new RuntimeException("Failed to decrypt image - result is null");
            }
            
            System.out.println("Image decrypted successfully");
            System.out.println("Decrypted image dimensions: " + decryptedImage.getWidth() + "x" + decryptedImage.getHeight());
            
            // Ensure output directory exists
            File outputFile = new File(outputPath);
            File outputDir = outputFile.getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create output directory: " + outputDir.getAbsolutePath());
                }
                System.out.println("Created output directory: " + outputDir.getAbsolutePath());
            }
            
            // Save it as a regular viewable PNG
            boolean success = ImageIO.write(decryptedImage, "PNG", outputFile);
            
            if (!success) {
                throw new RuntimeException("Failed to write decrypted image to file");
            }
            
            System.out.println("Decrypted image saved successfully to: " + outputPath);
            System.out.println("File size: " + outputFile.length() + " bytes");
            
        } catch (Exception e) {
            System.err.println("Decryption failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}