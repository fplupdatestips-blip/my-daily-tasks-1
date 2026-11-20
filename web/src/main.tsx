import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { FirebaseBackend, firebaseConfigFromEnv } from './data/firebaseBackend';
import { LocalBackend } from './data/localBackend';
import { AppStore } from './data/store';
import { StoreContext } from './ui/hooks';
import './styles.css';

const config = firebaseConfigFromEnv();
const store = new AppStore(config ? new FirebaseBackend(config) : new LocalBackend());

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <StoreContext.Provider value={store}>
      <App />
    </StoreContext.Provider>
  </StrictMode>,
);
