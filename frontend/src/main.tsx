import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { RealtimeProvider } from "./realtime/RealtimeProvider";
import { AuthProvider } from "./auth/AuthContext";
import { CartProvider } from "./cart/CartContext";
import { ToastProvider } from "./components/ui/Toast";
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <RealtimeProvider>
      <ToastProvider>
        <CartProvider>
          <App />
        </CartProvider>
      </ToastProvider>
      </RealtimeProvider>
    </AuthProvider>
  </StrictMode>,
)
