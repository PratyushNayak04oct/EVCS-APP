function Navbar() {
  return (
    <nav className = "bg-blue-600 shadow-lg">
      <div className = "max-w-7xl mx-auto px-4">
        <div className = "flex justify-between items-center h-16">
          {/* Logo/Brand */}
          <div className = "flex-shrink-0">
            <a 
              href="/" 
              className = "text-white text-xl font-bold hover:text-blue-200 transition-colors"
            >
              EVCS - Enhanced Visual Cryptography
            </a>
          </div>

          {/* Navigation Links */}
          <div className = "flex space-x-8">
            <a
              href="/"
              className = "text-white hover:text-blue-200 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-200"
            >
              Encryptor
            </a>
            <a
              href="/decryptor"
              className = "text-white hover:text-blue-200 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-200"
            >
              Decryptor
            </a>
          </div>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;