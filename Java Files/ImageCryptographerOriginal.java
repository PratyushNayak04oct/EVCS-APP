import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class ImageCryptographerOriginal {
    private static final int NUM_SHARES = 3;
    private static final int THRESHOLD = 2;
    private static final double INITIAL_TEMPERATURE = 100.0;
    private static final double COOLING_RATE = 0.01;
    private static final int ICM_ITERATIONS = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

    public static void main(String[] args) {
        try {
            // Verify input file exists
            File inputFile = new File("download.png");
            if (!inputFile.exists()) {
                throw new IllegalArgumentException("Input file does not exist: " + inputFile.getAbsolutePath());
            }

            BufferedImage originalImage = ImageIO.read(inputFile);
            if (originalImage == null) {
                throw new IllegalArgumentException("Failed to read input image");
            }
            
            System.out.println("Encrypting image...");
            List<BufferedImage> shares = encrypt(originalImage);
            
            // Save shares
            for (int i = 0; i < shares.size(); i++) {
                File outputFile = new File("share_" + i + ".png");
                if (!ImageIO.write(shares.get(i), "PNG", outputFile)) {
                    throw new IOException("Failed to write share " + i);
                }
            }
            
            System.out.println("Decrypting image...");
            BufferedImage decryptedImage = decrypt(shares);
            
            File decryptedFile = new File("decrypted.png");
            if (!ImageIO.write(decryptedImage, "PNG", decryptedFile)) {
                throw new IOException("Failed to write decrypted image");
            }
            
            System.out.println("Process completed successfully");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}