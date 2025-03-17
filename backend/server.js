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
    const dirs = ['uploads', 'output'];
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

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
    console.log(`Working directory: ${__dirname}`);
});