const express = require('express');
const multer = require('multer');
const { exec } = require('child_process');
const path = require('path');
const fs = require('fs');
const cors = require('cors');

const app = express();
const upload = multer({ dest: 'uploads/' });

app.use(cors());
app.use(express.json());
app.use('/output', express.static('output'));

// Create necessary directories
const ensureDirectories = () => {
    const dirs = ['uploads', 'output', 'decrypted'];
    dirs.forEach(dir => {
        const dirPath = path.join(__dirname, dir);
        if (!fs.existsSync(dirPath)) {
            fs.mkdirSync(dirPath, { recursive: true });
            console.log(`Created directory: ${dirPath}`);
        }
    });
};

// Initialize directories on startup
ensureDirectories();

app.post('/process-image', upload.single('image'), async (req, res) => {
    try {
        console.log('Processing new request...');
        
        if (!req.file) {
            console.log('No file uploaded');
            return res.status(400).json({ error: 'No file uploaded' });
        }

        console.log('File received:', req.file);

        // Set up paths
        const workingDir = __dirname;
        const inputPath = path.join(workingDir, 'uploads', 'inputC.png');
        const outputDir = path.join(workingDir, 'output');

        console.log('Working directory:', workingDir);
        console.log('Input path:', inputPath);
        console.log('Output directory:', outputDir);

        // Rename uploaded file
        fs.renameSync(req.file.path, inputPath);
        console.log('File renamed to:', inputPath);

        // Verify Java installation
        exec('java -version', (error, stdout, stderr) => {
            if (error) {
                console.error('Java not found:', error);
                return res.status(500).json({ error: 'Java not installed or not in PATH' });
            }

            console.log('Java version:', stderr); // Java version is typically output to stderr

            // Compile Java file
            const compileCommand = `javac Main.java`;
            console.log('Executing compile command:', compileCommand);

            exec(compileCommand, { cwd: workingDir }, (compileError, compileStdout, compileStderr) => {
                if (compileError) {
                    console.error('Compilation failed:', compileError);
                    console.error('Compilation stderr:', compileStderr);
                    return res.status(500).json({
                        error: 'Java compilation failed',
                        details: compileError.message,
                        stderr: compileStderr
                    });
                }

                console.log('Compilation successful');

                // Execute Java program
                const executeCommand = `java -cp "${workingDir}" Main`;
                console.log('Executing command:', executeCommand);

                exec(executeCommand, { cwd: workingDir }, (execError, execStdout, execStderr) => {
                    if (execError) {
                        console.error('Execution failed:', execError);
                        console.error('Execution stderr:', execStderr);
                        return res.status(500).json({
                            error: 'Java execution failed',
                            details: execError.message,
                            stderr: execStderr
                        });
                    }

                    console.log('Execution stdout:', execStdout);
                    console.log('Execution stderr:', execStderr);

                    // Check if output files were generated
                    const outputFiles = [];
                    
                    // List all files in output directory
                    console.log('Checking output directory:', outputDir);
                    const files = fs.readdirSync(outputDir);
                    console.log('Files in output directory:', files);

                    // Get shares
                    const shares = files
                        .filter(file => file.startsWith('share_'))
                        .map(file => ({
                            name: file,
                            path: `/output/${file}`
                        }));
                    outputFiles.push(...shares);

                    // Check for protected output
                    const protectedOutputPath = path.join(outputDir, 'protected_output.png');
                    if (fs.existsSync(protectedOutputPath)) {
                        outputFiles.push({
                            name: 'protected_output.png',
                            path: '/output/protected_output.png'
                        });
                    }

                    // Check for signature file
                    const signaturePath = path.join(outputDir, 'signature.bin');
                    if (fs.existsSync(signaturePath)) {
                        outputFiles.push({
                            name: 'signature.bin',
                            path: '/output/signature.bin'
                        });
                    }

                    console.log('Found output files:', outputFiles);

                    res.json({
                        message: 'Processing completed',
                        files: outputFiles,
                        logs: execStdout
                    });
                });
            });
        });
    } catch (error) {
        console.error('Server error:', error);
        res.status(500).json({ error: 'Server error', details: error.message });
    }
});

// New endpoint for decrypting images
app.post('/decrypt-image', upload.single('encryptedFile'), async (req, res) => {
    try {
        console.log('Decryption request received...');
        
        if (!req.file) {
            console.log('No encrypted file uploaded');
            return res.status(400).json({ error: 'No encrypted file uploaded' });
        }

        if (!req.body.password) {
            console.log('No password provided');
            return res.status(400).json({ error: 'Password is required for decryption' });
        }

        console.log('Encrypted file received:', req.file);
        console.log('Password provided for decryption');

        // Set up paths
        const workingDir = __dirname;
        const encryptedFilePath = path.join(workingDir, 'uploads', 'encrypted_input.png');
        const decryptedDir = path.join(workingDir, 'decrypted');
        const decryptedFilePath = path.join(decryptedDir, 'decrypted_image.png');

        // Ensure decrypted directory exists
        if (!fs.existsSync(decryptedDir)) {
            fs.mkdirSync(decryptedDir, { recursive: true });
        }

        // Move uploaded file to expected location
        fs.renameSync(req.file.path, encryptedFilePath);
        console.log('Encrypted file moved to:', encryptedFilePath);

        // Create a temporary Java class for decryption with the provided password
        const decryptorJavaContent = `
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class DecryptorTemp {
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
            if (args.length != 3) {
                System.err.println("Usage: java DecryptorTemp <inputPath> <password> <outputPath>");
                System.exit(1);
            }
            
            String inputPath = args[0];
            String password = args[1];
            String outputPath = args[2];
            
            System.out.println("Starting decryption process...");
            System.out.println("Input file: " + inputPath);
            System.out.println("Output file: " + outputPath);
            
            // Decrypt the protected image
            BufferedImage decryptedImage = decryptProtectedImage(inputPath, password);
            
            if (decryptedImage == null) {
                throw new RuntimeException("Failed to decrypt image - result is null");
            }
            
            System.out.println("Image decrypted successfully");
            System.out.println("Decrypted image dimensions: " + decryptedImage.getWidth() + "x" + decryptedImage.getHeight());
            
            // Save it as a regular viewable PNG
            File outputFile = new File(outputPath);
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
}`;

        // Write the temporary Java file
        const tempJavaFile = path.join(workingDir, 'DecryptorTemp.java');
        fs.writeFileSync(tempJavaFile, decryptorJavaContent);

        // Compile the temporary Java file
        const compileCommand = `javac DecryptorTemp.java`;
        console.log('Compiling decryptor:', compileCommand);

        exec(compileCommand, { cwd: workingDir }, (compileError, compileStdout, compileStderr) => {
            if (compileError) {
                console.error('Decryptor compilation failed:', compileError);
                console.error('Compilation stderr:', compileStderr);
                
                // Clean up
                if (fs.existsSync(tempJavaFile)) fs.unlinkSync(tempJavaFile);
                
                return res.status(500).json({
                    error: 'Decryption compilation failed',
                    details: compileError.message,
                    stderr: compileStderr
                });
            }

            console.log('Decryptor compilation successful');

            // Execute the decryption
            const executeCommand = `java -cp "${workingDir}" DecryptorTemp "${encryptedFilePath}" "${req.body.password}" "${decryptedFilePath}"`;
            console.log('Executing decryption command...');

            exec(executeCommand, { cwd: workingDir }, (execError, execStdout, execStderr) => {
                // Clean up temporary files
                if (fs.existsSync(tempJavaFile)) fs.unlinkSync(tempJavaFile);
                const tempClassFile = path.join(workingDir, 'DecryptorTemp.class');
                if (fs.existsSync(tempClassFile)) fs.unlinkSync(tempClassFile);
                if (fs.existsSync(encryptedFilePath)) fs.unlinkSync(encryptedFilePath);

                if (execError) {
                    console.error('Decryption execution failed:', execError);
                    console.error('Execution stderr:', execStderr);
                    
                    // Check if it's a password error
                    if (execStderr && (execStderr.includes('BadPaddingException') || execStderr.includes('AEADBadTagException'))) {
                        return res.status(400).json({
                            error: 'Invalid password or corrupted file',
                            details: 'The provided password is incorrect or the encrypted file is corrupted.'
                        });
                    }
                    
                    return res.status(500).json({
                        error: 'Decryption failed',
                        details: execError.message,
                        stderr: execStderr
                    });
                }

                console.log('Decryption stdout:', execStdout);
                if (execStderr) console.log('Decryption stderr:', execStderr);

                // Check if decrypted file was created
                if (!fs.existsSync(decryptedFilePath)) {
                    return res.status(500).json({
                        error: 'Decryption failed',
                        details: 'Decrypted file was not created'
                    });
                }

                // Serve the decrypted file statically
                app.use('/decrypted', express.static('decrypted'));

                console.log('Decryption completed successfully');
                
                res.json({
                    message: 'Image decrypted successfully',
                    outputPath: '/decrypted/decrypted_image.png',
                    logs: execStdout + (execStderr ? '\n' + execStderr : '')
                });
            });
        });

    } catch (error) {
        console.error('Decryption server error:', error);
        res.status(500).json({ error: 'Server error during decryption', details: error.message });
    }
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
    console.log(`Working directory: ${__dirname}`);
});