import { useState } from "react";
import Navbar from "./Navbar";

function Decryptor() {
  const [file, setFile] = useState(null);
  const [fileContent, setFileContent] = useState(null);
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [processing, setProcessing] = useState(false);
  const [decryptedImage, setDecryptedImage] = useState(null);
  const [logs, setLogs] = useState("");

  const MAX_FILE_SIZE = 5 * 1024 * 1024;

  const handleFile = (e) => {
    setError(null);
    setFileContent(null);
    setDecryptedImage(null);
    setLogs("");

    const file = e.target.files[0];

    if (file.size > MAX_FILE_SIZE) {
      setError("File size should be less than 5MB");
      return;
    }

    setFile(file);

    const reader = new FileReader();
    reader.onerror = () => {
      setError("Error Reading File");
    };

    reader.onload = (e) => {
      setFileContent(e.target.result);
    };
    reader.readAsDataURL(file);
  };

  const onSubmit = async () => {
    if (!file) {
      setError("Please select an encrypted file first");
      return;
    }

    if (!password.trim()) {
      setError("Please enter the decryption password");
      return;
    }

    setProcessing(true);
    setError(null);
    
    try {
      const formData = new FormData();
      formData.append('encryptedFile', file);
      formData.append('password', password);

      const response = await fetch('http://localhost:3001/decrypt-image', {
        method: 'POST',
        body: formData,
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || 'Decryption failed');
      }

      setDecryptedImage(data.outputPath);
      setLogs(data.logs);
    } catch (err) {
      setError(err.message);
    } finally {
      setProcessing(false);
    }
  };

  const downloadDecryptedImage = () => {
    if (decryptedImage) {
      const link = document.createElement('a');
      link.href = `http://localhost:3001${decryptedImage}`;
      link.download = 'decrypted_image.png';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  const viewDecryptedImage = () => {
    if (decryptedImage) {
      window.open(`http://localhost:3001${decryptedImage}`, '_blank');
    }
  };

  return (
    <div className="min-h-screen bg-[#FDFFFC]">
      <Navbar />
      
      <div className="flex w-full justify-center overflow-x-hidden">
        <div className="container max-w-4xl px-4 py-8">
          <h1 className="mb-4 text-3xl font-bold text-green-600">
            File Decryptor
          </h1>
          
          <div className="mb-8 space-y-4 p-6 bg-green-50 rounded-lg">
            <h2 className="text-xl font-semibold text-green-800 mb-3">What this decryptor does:</h2>
            <p className="text-green-700 mb-4">
              This decryption system reverses the multi-layered encryption process by performing the following operations:
            </p>
            <ol className="list-decimal ml-6 space-y-2 text-green-700">
              <li><strong>Password Verification:</strong> Takes the AES password-protected encrypted image file and validates the provided password</li>
              <li><strong>AES Decryption:</strong> Uses the password to decrypt the AES-encrypted content and extract the original image data</li>
              <li><strong>Data Reconstruction:</strong> Processes the decrypted bytes to reconstruct the original image structure</li>
              <li><strong>Image Recovery:</strong> Converts the recovered data back into a viewable PNG format for download and display</li>
            </ol>
            <div className="mt-4 p-3 bg-amber-100 border-l-4 border-amber-500 rounded">
              <p className="text-amber-800 font-medium">
                ⚠️ <strong>Important:</strong> You need the exact password that was used during the encryption process to successfully decrypt the image.
              </p>
            </div>
          </div>

          <div className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Select Encrypted File (protected_output.png)
              </label>
              <input
                type="file"
                accept=".png,.jpg,.jpeg"
                className="block w-full text-sm text-gray-500
                  file:mr-4 file:py-2 file:px-4
                  file:rounded-md file:border-0
                  file:text-sm file:font-semibold
                  file:bg-green-50 file:text-green-700
                  hover:file:bg-green-100 cursor-pointer"
                onChange={handleFile}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Decryption Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter the password used for encryption"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent"
              />
            </div>
          </div>

          {error && (
            <div className="mt-4 p-4 bg-red-50 border border-red-200 text-red-700 rounded-md">
              <div className="flex items-center">
                <span className="text-red-500 mr-2">❌</span>
                {error}
              </div>
            </div>
          )}

          {file && (
            <div className="mt-6 p-6 bg-gray-50 rounded-lg border">
              <h3 className="font-semibold mb-4 text-gray-800 flex items-center">
                <span className="mr-2">📄</span>
                Encrypted File Details:
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="font-medium text-gray-600">Name:</span>
                    <span className="text-gray-800 truncate ml-2">{file.name}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="font-medium text-gray-600">Type:</span>
                    <span className="text-gray-800">{file.type}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="font-medium text-gray-600">Size:</span>
                    <span className="text-gray-800">{(file.size / 1024).toFixed(2)} KB</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="font-medium text-gray-600">Status:</span>
                    <span className="px-3 py-1 bg-red-100 text-red-700 rounded-full text-sm font-medium">
                      🔒 Encrypted
                    </span>
                  </div>
                </div>
                {fileContent && (
                  <div className="flex justify-center">
                    <div className="relative">
                      <img
                        src={fileContent}
                        alt="Encrypted file preview"
                        className="h-[150px] w-[200px] object-cover rounded-lg shadow-md opacity-60"
                      />
                      <div className="absolute inset-0 flex items-center justify-center bg-black bg-opacity-40 rounded-lg">
                        <div className="text-center">
                          <div className="text-white text-2xl mb-1">🔒</div>
                          <span className="text-white text-sm font-semibold">Encrypted Content</span>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          <button
            className="mt-6 w-full md:w-auto px-8 py-3 bg-green-600 text-white font-semibold rounded-lg
              hover:bg-green-700 focus:ring-4 focus:ring-green-200 transition-all duration-200 
              disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer shadow-md"
            onClick={onSubmit}
            disabled={processing || !file || !password.trim()}
          >
            {processing ? (
              <div className="flex items-center justify-center">
                <div className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent mr-3"></div>
                Decrypting...
              </div>
            ) : (
              <div className="flex items-center justify-center">
                <span className="mr-2">🔓</span>
                Start Decryption Process
              </div>
            )}
          </button>

          {processing && (
            <div className="mt-6 p-4 bg-green-50 border border-green-200 text-green-700 rounded-lg">
              <div className="flex items-center">
                <div className="animate-spin rounded-full h-5 w-5 border-2 border-green-600 border-t-transparent mr-3"></div>
                <div>
                  <p className="font-medium">Decrypting your image...</p>
                  <p className="text-sm">This process may take a few moments depending on the file size.</p>
                </div>
              </div>
            </div>
          )}

          {logs && (
            <div className="mt-6 p-4 bg-gray-50 border rounded-lg">
              <h2 className="font-semibold mb-3 text-gray-800 flex items-center">
                <span className="mr-2">📋</span>
                Decryption Process Logs:
              </h2>
              <div className="bg-gray-900 text-green-400 p-4 rounded-md overflow-x-auto">
                <pre className="whitespace-pre-wrap text-sm font-mono">{logs}</pre>
              </div>
            </div>
          )}

          {decryptedImage && (
            <div className="mt-6 p-6 bg-green-50 border-2 border-green-200 rounded-lg">
              <h2 className="font-bold mb-4 text-green-800 text-xl flex items-center">
                <span className="mr-2">✅</span>
                Decryption Successful!
              </h2>
              
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div>
                  <h3 className="font-semibold mb-3 text-green-700 flex items-center">
                    <span className="mr-2">🖼️</span>
                    Decrypted Image Preview:
                  </h3>
                  <div className="border-2 border-green-300 rounded-lg overflow-hidden shadow-lg">
                    <img
                      src={`http://localhost:3001${decryptedImage}`}
                      alt="Decrypted image"
                      className="w-full h-auto"
                      style={{ maxHeight: '300px', objectFit: 'contain' }}
                    />
                  </div>
                </div>
                
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold mb-3 text-green-700">Image Status:</h3>
                    <div className="flex items-center">
                      <span className="px-4 py-2 bg-green-100 text-green-800 rounded-full text-sm font-medium flex items-center">
                        <span className="mr-2">🔓</span>
                        Successfully Decrypted & Ready
                      </span>
                    </div>
                  </div>
                  
                  <div className="space-y-3">
                    <button
                      onClick={downloadDecryptedImage}
                      className="w-full px-4 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 
                               focus:ring-4 focus:ring-green-200 transition-all duration-200 font-medium
                               flex items-center justify-center shadow-md"
                    >
                      <span className="mr-2">📥</span>
                      Download Decrypted Image
                    </button>
                    
                    <button
                      onClick={viewDecryptedImage}
                      className="w-full px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 
                               focus:ring-4 focus:ring-blue-200 transition-all duration-200 font-medium
                               flex items-center justify-center shadow-md"
                    >
                      <span className="mr-2">👁️</span>
                      View in New Tab
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className="mt-8 p-6 bg-blue-50 border border-blue-200 rounded-lg">
            <h3 className="font-semibold text-blue-800 mb-3 flex items-center">
              <span className="mr-2">💡</span>
              Tips for Successful Decryption:
            </h3>
            <ul className="list-disc ml-6 space-y-2 text-blue-700">
              <li>Ensure you are using the exact same password that was used during the encryption process</li>
              <li>Verify that the encrypted file has not been corrupted, modified, or partially downloaded</li>
              <li>The decryption process may take a few moments for larger images - please be patient</li>
              <li>If decryption fails, double-check your password for any typos and try again</li>
              <li>Make sure the file you selected is the correct encrypted output file (usually named protected_output.png)</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Decryptor;