import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.List;
import javax.imageio.ImageIO;

public class ImageCryptographerEnhanced extends ImageCryptographerOriginal {
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    // Method to generate digital signature for a decrypted image
    public static byte[] generateDigitalSignature(BufferedImage image, PrivateKey privateKey) throws GeneralSecurityException {
        try {
            // Convert image to byte array
            byte[] imageBytes = imageToByteArray(image);
            
            // Create signature object
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(imageBytes);
            
            // Generate signature
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Error generating digital signature", e);
        }
    }

    // Method to verify digital signature
    public static boolean verifyDigitalSignature(BufferedImage image, byte[] signatureBytes, PublicKey publicKey) throws GeneralSecurityException {
        try {
            // Convert image to byte array
            byte[] imageBytes = imageToByteArray(image);
            
            // Create signature object
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(imageBytes);
            
            // Verify signature
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying digital signature", e);
        }
    }

    // Helper method to convert BufferedImage to byte array
    private static byte[] imageToByteArray(BufferedImage image) throws IOException {
        // Implement image to byte array conversion
        // This is a simplified version and might need more robust implementation
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] imageBytes = new byte[width * height * 4]; // 4 bytes per pixel (ARGB)
        
        int byteIndex = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                
                // Convert pixel to bytes
                imageBytes[byteIndex++] = (byte)((pixel >> 24) & 0xFF); // Alpha
                imageBytes[byteIndex++] = (byte)((pixel >> 16) & 0xFF); // Red
                imageBytes[byteIndex++] = (byte)((pixel >> 8) & 0xFF);  // Green
                imageBytes[byteIndex++] = (byte)(pixel & 0xFF);         // Blue
            }
        }
        
        return imageBytes;
    }

    // Method to get viewable image (can be used for reconstruction)
    public static BufferedImage getViewableImage(List<BufferedImage> shares) {
        // Perform decryption
        BufferedImage decryptedImage = decrypt(shares);
        
        // Additional processing for viewability can be added here
        return decryptedImage;
    }

    // Example usage method demonstrating key generation and signature
    public static void main(String[] args) {
        try {
            // Key pair generation
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            
            // Existing image processing logic
            File inputFile = new File("download.png");
            BufferedImage originalImage = ImageIO.read(inputFile);
            
            // Encryption
            List<BufferedImage> shares = encrypt(originalImage);
            
            // Get viewable image
            BufferedImage viewableImage = getViewableImage(shares);
            
            // Generate digital signature
            byte[] signature = generateDigitalSignature(viewableImage, keyPair.getPrivate());
            
            // Verify signature
            boolean isSignatureValid = verifyDigitalSignature(viewableImage, signature, keyPair.getPublic());
            System.out.println("Signature Validity: " + isSignatureValid);
            
            // Save viewable image
            File viewableFile = new File("viewable_image.png");
            ImageIO.write(viewableImage, "PNG", viewableFile);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}