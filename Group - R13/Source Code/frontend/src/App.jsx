import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Protection from './components/Protection';
import Decryptor from './components/Decryptor';

function App() {
  return (
    <Router>
      <div className = "App">
        <Navbar />
        <Routes>
          <Route path="/" element={<Protection />} />
          <Route path="/decryptor" element={<Decryptor />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;