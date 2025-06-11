import { BrowserRouter, Routes, Route } from 'react-router' ; 
import { createRoot } from 'react-dom/client'
import './App.css';
import App from './App.jsx'
import Protection from './components/Protection.jsx';
import Decryptor from './components/Decryptor.jsx' ; 

createRoot(document.getElementById('root')).render(
  <BrowserRouter>
    <Routes>
      <Route path='/' element = {<App />} />
      <Route path = '/protection' element = {<Protection />}  />
      <Route path = '/decryptor' element = {<Decryptor />}  />
    </Routes>
  </BrowserRouter>,
)
