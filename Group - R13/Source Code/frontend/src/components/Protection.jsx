import { useState } from "react";

function Protection() {
  const [file, setFile] = useState(null);
  const [fileContent, setFileContent] = useState(null);
  const [error, setError] = useState(null);
  const [processing, setProcessing] = useState(false);
  const [outputFiles, setOutputFiles] = useState([]);
  const [logs, setLogs] = useState("");

  const MAX_FILE_SIZE = 5 * 1024 * 1024;

  const handleFile = (e) => {
    setError(null);
    setFileContent(null);
    setOutputFiles([]);
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
      setError("Please select a file first");
      return;
    }

    setProcessing(true);
    setError(null);

    try {
      const formData = new FormData();
      formData.append("image", file);

      const response = await fetch("http://localhost:3001/process-image", {
        method: "POST",
        body: formData,
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || "Processing failed");
      }

      setOutputFiles(data.files);
      setLogs(data.logs);
    } catch (err) {
      setError(err.message);
    } finally {
      setProcessing(false);
    }
  };

  const downloadFile = (path, filename) => {
    const link = document.createElement("a");
    link.href = `http://localhost:3001${path}`;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="flex min-h-screen w-full justify-center bg-[#FDFFFC] overflow-x-hidden">
      <div className="container max-w-4xl px-4 py-8">
        <h1 className="mb-4 text-2xl font-semibold text-blue-600">
          File Encryptor With Password Protection
        </h1>

        <div className="mb-8 space-y-4">
          <p>
            This is an image encryption system that uses a multi-layered
            approach:
          </p>
          <ol className="list-decimal ml-6 space-y-2">
            <li>
              Applies Simulated Annealing (SA) and Iterated Conditional Modes
              (ICM) to transform the image
            </li>
            <li>
              Splits the transformed image into multiple shares using secret
              sharing
            </li>
            <li>Adds RSA digital signatures for authenticity</li>
            <li>
              Includes AES password protection for the final encrypted image
            </li>
          </ol>
          <p>
            The process ensures security through multiple encryption layers and
            verification mechanisms.
          </p>
        </div>

        <input
          type="file"
          accept="image/*"
          className="block w-full text-sm text-gray-500 mb-6
            file:mr-4 file:py-2 file:px-4
            file:rounded-md file:border-0
            file:text-sm file:font-semibold
            file:bg-blue-50 file:text-blue-700
            hover:file:bg-blue-100 cursor-pointer"
          onChange={handleFile}
        />

        {error && (
          <div className="mb-4 p-4 bg-red-50 text-red-700 rounded-md">
            {error}
          </div>
        )}

        {file && (
          <div className="mb-6 flex flex-col md:flex-row gap-6 p-4 bg-gray-50 rounded-md">
            <div className="space-y-2">
              <p>
                <span className="font-semibold">Name:</span> {file.name}
              </p>
              <p>
                <span className="font-semibold">Type:</span> {file.type}
              </p>
              <p>
                <span className="font-semibold">Size:</span>{" "}
                {(file.size / 1024).toFixed(2)} KB
              </p>
            </div>
            {fileContent && (
              <div className="flex-shrink-0">
                <img
                  src={fileContent}
                  alt={file.name}
                  className="h-[150px] w-[200px] object-cover rounded-md"
                />
              </div>
            )}
          </div>
        )}

        <button
          className="w-full md:w-auto px-6 py-3 bg-blue-600 text-white font-semibold rounded-md
            hover:bg-blue-700 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          onClick={onSubmit}
          disabled={processing || !file}
        >
          {processing ? "Processing..." : "Start Encryption Process"}
        </button>

        {processing && (
          <div className="mt-6 p-4 bg-blue-50 text-blue-700 rounded-md">
            Processing your image... Please wait...
          </div>
        )}

        {logs && (
          <div className="mt-6 p-4 bg-gray-50 rounded-md">
            <h2 className="font-semibold mb-2">Processing Logs:</h2>
            <pre className="whitespace-pre-wrap text-sm">{logs}</pre>
          </div>
        )}

        {outputFiles.length > 0 && (
          <div className="mt-6">
            <h2 className="font-semibold mb-4">Output Files:</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {outputFiles.map((file, index) => (
                <div
                  key={index}
                  className="p-4 bg-gray-50 rounded-md flex justify-between items-center"
                >
                  <span className="text-sm truncate">{file.name}</span>
                  <button
                    onClick={() => downloadFile(file.path, file.name)}
                    className="ml-4 px-3 py-1 bg-blue-100 text-blue-700 rounded-md hover:bg-blue-200 transition-colors duration-200"
                  >
                    Download
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default Protection;
