import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class Main {
    private static final int NUM_SHARES = 3;
    private static final int THRESHOLD = 2;
    private static final double INITIAL_TEMPERATURE = 100.0;
    private static final double COOLING_RATE = 0.01;
    private static final int ICM_ITERATIONS = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    // Encryption Method
    public static List<BufferedImage> encrypt(BufferedImage originalImage) {
        if (originalImage == null) {
            throw new IllegalArgumentException("Input image cannot be null");
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid image dimensions");
        }
        
        // Convert image to pixel matrix
        int[][] pixelMatrix = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelMatrix[y][x] = originalImage.getRGB(x, y);
            }
        }

        try {
            // Apply SA transformation
            pixelMatrix = simulatedAnnealing(pixelMatrix);
            
            // Apply ICM transformation
            pixelMatrix = iteratedConditionalModes(pixelMatrix);

            // Generate shares from transformed matrix
            return generateShares(pixelMatrix);
        } catch (Exception e) {
            throw new RuntimeException("Error during encryption: " + e.getMessage(), e);
        }
    }

    private static int[][] simulatedAnnealing(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }

        int height = matrix.length;
        int width = matrix[0].length;
        int[][] result = new int[height][width];
        
        // Create a copy of the original matrix
        for (int y = 0; y < height; y++) {
            System.arraycopy(matrix[y], 0, result[y], 0, width);
        }
        
        double temperature = INITIAL_TEMPERATURE;
        
        while (temperature > 1.0) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (SECURE_RANDOM.nextDouble() < temperature / INITIAL_TEMPERATURE) {
                        int pixel = result[y][x];
                        int red = (pixel >> 16) & 0xFF;
                        int green = (pixel >> 8) & 0xFF;
                        int blue = pixel & 0xFF;

                        int perturbation = (int)(temperature / 10);
                        red = perturb(red, perturbation);
                        green = perturb(green, perturbation);
                        blue = perturb(blue, perturbation);

                        result[y][x] = (red << 16) | (green << 8) | blue;
                    }
                }
            }
            temperature *= (1.0 - COOLING_RATE);
        }
        return result;
    }

    private static int perturb(int value, int amount) {
        int change = SECURE_RANDOM.nextInt(Math.max(1, amount * 2)) - amount;
        return Math.max(0, Math.min(255, value + change));
    }

    private static int[][] iteratedConditionalModes(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }

        int height = matrix.length;
        int width = matrix[0].length;
        int[][] result = new int[height][width];
        
        // Create a copy of the original matrix
        for (int y = 0; y < height; y++) {
            System.arraycopy(matrix[y], 0, result[y], 0, width);
        }
        
        for (int iteration = 0; iteration < ICM_ITERATIONS; iteration++) {
            int[][] newMatrix = new int[height][width];
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int[] neighborhood = getNeighborhood(result, x, y);
                    if (neighborhood.length > 0) {
                        int[] avgColor = computeAverageColor(neighborhood);
                        
                        int pixel = result[y][x];
                        int red = ((pixel >> 16) & 0xFF);
                        int green = ((pixel >> 8) & 0xFF);
                        int blue = (pixel & 0xFF);
                        
                        double mixRatio = 0.7;
                        red = (int)(red * mixRatio + avgColor[0] * (1 - mixRatio));
                        green = (int)(green * mixRatio + avgColor[1] * (1 - mixRatio));
                        blue = (int)(blue * mixRatio + avgColor[2] * (1 - mixRatio));
                        
                        newMatrix[y][x] = (red << 16) | (green << 8) | blue;
                    } else {
                        newMatrix[y][x] = result[y][x];
                    }
                }
            }
            result = newMatrix;
        }
        return result;
    }

    private static int[] getNeighborhood(int[][] matrix, int x, int y) {
        List<Integer> neighbors = new ArrayList<>();
        int height = matrix.length;
        int width = matrix[0].length;
        
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int newX = x + dx;
                int newY = y + dy;
                if (newX >= 0 && newX < width && newY >= 0 && newY < height && !(dx == 0 && dy == 0)) {
                    neighbors.add(matrix[newY][newX]);
                }
            }
        }
        
        int[] result = new int[neighbors.size()];
        for (int i = 0; i < neighbors.size(); i++) {
            result[i] = neighbors.get(i);
        }
        return result;
    }

    private static int[] computeAverageColor(int[] pixels) {
        if (pixels == null || pixels.length == 0) {
            throw new IllegalArgumentException("Invalid pixels array");
        }

        int[] result = new int[3];
        for (int pixel : pixels) {
            result[0] += (pixel >> 16) & 0xFF;
            result[1] += (pixel >> 8) & 0xFF;
            result[2] += pixel & 0xFF;
        }
        for (int i = 0; i < 3; i++) {
            result[i] /= pixels.length;
        }
        return result;
    }

    private static List<BufferedImage> generateShares(int[][] pixelMatrix) {
        if (pixelMatrix == null || pixelMatrix.length == 0 || pixelMatrix[0].length == 0) {
            throw new IllegalArgumentException("Invalid pixel matrix");
        }

        int height = pixelMatrix.length;
        int width = pixelMatrix[0].length;
        List<BufferedImage> shares = new ArrayList<>();

        try {
            // Generate N-1 random shares
            for (int i = 0; i < NUM_SHARES - 1; i++) {
                BufferedImage share = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        share.setRGB(x, y, SECURE_RANDOM.nextInt());
                    }
                }
                shares.add(share);
            }

            // Create final share
            BufferedImage finalShare = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixelMatrix[y][x];
                    for (BufferedImage share : shares) {
                        pixel ^= share.getRGB(x, y);
                    }
                    finalShare.setRGB(x, y, pixel);
                }
            }
            shares.add(finalShare);

            return shares;
        } catch (Exception e) {
            throw new RuntimeException("Error generating shares: " + e.getMessage(), e);
        }
    }

    public static BufferedImage decrypt(List<BufferedImage> shares) {
        if (shares == null || shares.size() < THRESHOLD) {
            throw new IllegalArgumentException("Insufficient shares for decryption");
        }

        BufferedImage firstShare = shares.get(0);
        int width = firstShare.getWidth();
        int height = firstShare.getHeight();
        
        try {
            int[][] reconstructedMatrix = new int[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = 0;
                    for (BufferedImage share : shares) {
                        pixel ^= share.getRGB(x, y);
                    }
                    reconstructedMatrix[y][x] = pixel;
                }
            }

            reconstructedMatrix = reverseICM(reconstructedMatrix);
            reconstructedMatrix = reverseSA(reconstructedMatrix);

            BufferedImage decryptedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    decryptedImage.setRGB(x, y, reconstructedMatrix[y][x]);
                }
            }
            return decryptedImage;
        } catch (Exception e) {
            throw new RuntimeException("Error during decryption: " + e.getMessage(), e);
        }
    }

    private static int[][] reverseICM(int[][] matrix) {
        return iteratedConditionalModes(matrix);
    }

    private static int[][] reverseSA(int[][] matrix) {
        return simulatedAnnealing(matrix);
    }

    public static void protectWithPassword(BufferedImage image, String password, String outputPath) {
        if (image == null || password == null || password.isEmpty() || outputPath == null || outputPath.isEmpty()) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
    
        try {
            // Create a temporary file to store the image
            File tempFile = File.createTempFile("temp_", ".png");
            if (!ImageIO.write(image, "PNG", tempFile)) {
                throw new IOException("Failed to write temporary image file");
            }
    
            // Read the image bytes
            byte[] imageBytes = java.nio.file.Files.readAllBytes(tempFile.toPath());
            
            // Create a key from the password
            byte[] key = password.getBytes("UTF-8");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                java.security.MessageDigest.getInstance("SHA-256").digest(key),
                "AES"
            );
    
            // Initialize cipher
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            SECURE_RANDOM.nextBytes(iv);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, new javax.crypto.spec.IvParameterSpec(iv));
    
            // Encrypt the image bytes
            byte[] encryptedBytes = cipher.doFinal(imageBytes);
    
            // Combine IV and encrypted bytes
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
    
            // Write the encrypted data to file
            java.nio.file.Files.write(new File(outputPath).toPath(), combined);
    
            // Clean up temporary file
            tempFile.delete();
            
        } catch (Exception e) {
            throw new RuntimeException("Error protecting image with password: " + e.getMessage(), e);
        }
    }

    // Digital signature methods
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

    private static byte[] imageToByteArray(BufferedImage image) throws IOException {
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

    public static void main(String[] args) {
        try {
            // Create output directory if it doesn't exist
            String outputDir = new File("output").getAbsolutePath();
            File outputDirectory = new File(outputDir);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
                System.out.println("Created output directory: " + outputDir);
            }
    
            // Step 1: Generate RSA key pair for digital signatures
            System.out.println("Generating RSA key pair...");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
    
            // Step 2: Read the input image
            System.out.println("Reading input image...");
            File inputFile = new File("uploads/inputC.png");
            if (!inputFile.exists()) {
                throw new IllegalArgumentException("Input file does not exist: " + inputFile.getAbsolutePath());
            }
            
            System.out.println("Input file path: " + inputFile.getAbsolutePath());
            System.out.println("Input file size: " + inputFile.length() + " bytes");
            
            BufferedImage originalImage = ImageIO.read(inputFile);
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to read input image. File may be corrupted or in wrong format.");
            }
    
            System.out.println("Image dimensions: " + originalImage.getWidth() + "x" + originalImage.getHeight());
    
            // Step 3: Encrypt image using SA + ICM
            System.out.println("Encrypting image using SA + ICM...");
            List<BufferedImage> shares = encrypt(originalImage);
            System.out.println("Generated " + shares.size() + " shares");
    
            // Step 4: Generate digital signature for the encrypted image
            System.out.println("Generating digital signature...");
            byte[] signature = generateDigitalSignature(shares.get(shares.size() - 1), keyPair.getPrivate());
            System.out.println("Signature generated with length: " + signature.length + " bytes");
    
            // Step 5: Save encrypted shares
            System.out.println("Saving encrypted shares...");
            for (int i = 0; i < shares.size(); i++) {
                File shareFile = new File(outputDir, "share_" + i + ".png");
                boolean success = ImageIO.write(shares.get(i), "PNG", shareFile);
                if (!success) {
                    throw new IOException("Failed to write share " + i);
                }
                System.out.println("Saved share " + i + " to: " + shareFile.getAbsolutePath());
            }
    
            // Step 6: Save signature to file
            System.out.println("Saving digital signature...");
            File signatureFile = new File(outputDir, "signature.bin");
            java.nio.file.Files.write(signatureFile.toPath(), signature);
            System.out.println("Signature saved to: " + signatureFile.getAbsolutePath());
    
            // Step 7: Verify signature before decryption
            System.out.println("Verifying digital signature...");
            boolean isValid = verifyDigitalSignature(
                shares.get(shares.size() - 1), 
                signature, 
                keyPair.getPublic()
            );
    
            if (!isValid) {
                throw new SecurityException("Digital signature verification failed!");
            }
            System.out.println("Signature verification successful");
    
            // Step 8: Decrypt the image
            System.out.println("Decrypting image...");
            BufferedImage decryptedImage = decrypt(shares);
            System.out.println("Decryption completed");
    
            // Step 9: Password protect the decrypted image
            System.out.println("Adding password protection...");
            String password = "password123";
            File protectedFile = new File(outputDir, "protected_output.png");
            protectWithPassword(
                decryptedImage,
                password,
                protectedFile.getAbsolutePath()
            );
            System.out.println("Protected image saved to: " + protectedFile.getAbsolutePath());
    
            // Final status report
            System.out.println("\nWorkflow completed successfully!");
            System.out.println("Output directory: " + outputDir);
            System.out.println("Generated files:");
            for (File file : outputDirectory.listFiles()) {
                System.out.println(" - " + file.getName() + " (" + file.length() + " bytes)");
            }
            System.out.println("Password for decryption: " + password);
    
        } catch (Exception e) {
            System.err.println("Error in workflow: " + e.getMessage());
            e.printStackTrace();
            
            // Additional error details
            if (e instanceof IOException) {
                System.err.println("File operation failed. Check file permissions and paths.");
            } else if (e instanceof SecurityException) {
                System.err.println("Security operation failed. Check encryption/signature settings.");
            } else if (e instanceof IllegalArgumentException) {
                System.err.println("Invalid input parameters or file format.");
            }
            
            // Print the full stack trace to help with debugging
            e.printStackTrace(System.err);
        }
    }
}