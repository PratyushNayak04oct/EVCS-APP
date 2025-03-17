import java.awt.image.BufferedImage;
import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) {
        try {
            // Step 1: Generate RSA key pair for digital signatures
            System.out.println("Generating RSA key pair...");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            // Step 2: Read the input image
            System.out.println("Reading input image...");
            File inputFile = new File("inputC.png");
            if (!inputFile.exists()) {
                throw new IllegalArgumentException("Input file does not exist: " + inputFile.getAbsolutePath());
            }
            BufferedImage originalImage = ImageIO.read(inputFile);

            // Step 3: Encrypt image using SA + ICM
            System.out.println("Encrypting image using SA + ICM...");
            var shares = ImageCryptographerOriginal.encrypt(originalImage);

            // Step 4: Generate digital signature for the encrypted image
            System.out.println("Generating digital signature...");
            byte[] signature = ImageCryptographerEnhanced.generateDigitalSignature(shares.get(shares.size() - 1), keyPair.getPrivate());

            // Step 5: Save encrypted shares
            System.out.println("Saving encrypted shares...");
            for (int i = 0; i < shares.size(); i++) {
                File shareFile = new File("share_" + i + ".png");
                ImageIO.write(shares.get(i), "PNG", shareFile);
            }

            // Step 6: Save signature to file
            System.out.println("Saving digital signature...");
            java.nio.file.Files.write(new File("signature.bin").toPath(), signature);

            // Step 7: Verify signature before decryption
            System.out.println("Verifying digital signature...");
            boolean isValid = ImageCryptographerEnhanced.verifyDigitalSignature(
                shares.get(shares.size() - 1), 
                signature, 
                keyPair.getPublic()
            );

            if (!isValid) {
                throw new SecurityException("Digital signature verification failed!");
            }

            // Step 8: Decrypt the image
            System.out.println("Decrypting image...");
            BufferedImage decryptedImage = ImageCryptographerOriginal.decrypt(shares);

            // Step 9: Password protect the decrypted image
            System.out.println("Adding password protection...");
            String password = "yourSecurePassword123";
            ImageCryptographerWithPasswordProtection.protectWithPassword(
                decryptedImage,
                password,
                "protected_output.png"
            );

            System.out.println("Workflow completed successfully!");
            System.out.println("Protected image saved as: protected_output.png");
            System.out.println("Password for decryption: " + password);

        } catch (Exception e) {
            System.err.println("Error in workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }
}