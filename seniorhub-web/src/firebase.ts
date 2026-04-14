import { initializeApp, type FirebaseApp } from 'firebase/app'
import { getAuth, GoogleAuthProvider } from 'firebase/auth'
import { getFirestore } from 'firebase/firestore'

export type FirebaseEnv = {
  apiKey: string
  authDomain: string
  projectId: string
  storageBucket: string
  messagingSenderId: string
  appId: string
}

function readEnv(): FirebaseEnv | null {
  const apiKey = import.meta.env.VITE_FIREBASE_API_KEY
  const authDomain = import.meta.env.VITE_FIREBASE_AUTH_DOMAIN
  const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID
  const storageBucket = import.meta.env.VITE_FIREBASE_STORAGE_BUCKET
  const messagingSenderId = import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID
  const appId = import.meta.env.VITE_FIREBASE_APP_ID

  if (!apiKey || !projectId || !appId) return null

  return {
    apiKey,
    authDomain: authDomain ?? '',
    projectId,
    storageBucket: storageBucket ?? '',
    messagingSenderId: messagingSenderId ?? '',
    appId,
  }
}

export function initFirebase(): { app: FirebaseApp; configured: true } | { app: null; configured: false } {
  const env = readEnv()
  if (!env) return { app: null, configured: false }
  const app = initializeApp(env)
  return { app, configured: true }
}

export const firestore = (app: FirebaseApp) => getFirestore(app)
export const auth = (app: FirebaseApp) => getAuth(app)
export const googleProvider = () => new GoogleAuthProvider()
