import './style.css'
import {
  onAuthStateChanged,
  signInWithPopup,
  signOut,
  type User,
} from 'firebase/auth'
import {
  addDoc,
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  where,
  writeBatch,
} from 'firebase/firestore'
import {
  COLLECTION,
  CONFIG_DOC_ID,
  JOIN_COLLECTION,
  KEY_ALERT_MESSAGE,
  KEY_ASSISTANT_NAME,
  KEY_CLAIM_CODE,
  KEY_ADMIN_PIN,
  KEY_DEVICE_ID,
  KEY_DEVICE_LABEL,
  KEY_NAME,
  KEY_PAIRING_CODE,
  KEY_PAIRING_EXPIRES_AT,
  KEY_PAIRED,
  KEY_PHONE,
  KEY_ROLE,
  KEY_SIM_NUMBER,
  KEY_SORT_ORDER,
  KEY_UID,
  KEY_USED_AT,
  KEY_USED_BY_UID,
  KEY_VOLUME_PERCENT,
  PAIRING_COLLECTION,
  SUB_CONFIG,
  SUB_CONTACTS,
} from './constants'
import { auth, firestore, googleProvider, initFirebase } from './firebase'

type DeviceListItem = {
  deviceId: string
  label: string
  role: string
  paired: boolean
}

const root = document.querySelector<HTMLDivElement>('#app')
if (!root) throw new Error('#app missing')

const { app, configured } = initFirebase()

if (!configured || !app) {
  root.innerHTML = `
    <main class="shell">
      <h1>SeniorHub web admin</h1>
      <p class="warn">
        Chybí Firebase konfigurace. Zkopíruj <code>.env.example</code> na <code>.env</code>
        a doplň údaje z Firebase konzole (Project settings → Your apps → Web).
      </p>
    </main>
  `
} else {
  const db = firestore(app)
  const authClient = auth(app)
  const provider = googleProvider()

  let selectedDeviceId: string | null = null
  let stopDevice: (() => void) | null = null
  let stopContacts: (() => void) | null = null
  let stopJoins: (() => void) | null = null
  let stopConfig: (() => void) | null = null

  root.innerHTML = `
    <main class="shell">
      <header class="top">
        <div>
          <h1>SeniorHub admin</h1>
          <p class="sub">Google přihlášení, více správců, více tabletů.</p>
        </div>
        <div class="top-actions">
          <p class="pill" id="conn">Načítám…</p>
          <button id="authBtn" type="button" class="primary">Přihlásit přes Google</button>
        </div>
      </header>

      <section class="card" id="authCard">
        <h2>Přístup</h2>
        <p class="sub" id="authState">Čekám na přihlášení…</p>
      </section>

      <section class="card" id="pairCard" hidden>
        <h2>Přidat tablet</h2>
        <p class="sub">Na tabletu otevři párovací obrazovku a zadej kód.</p>
        <div class="row">
          <input id="pairCode" type="text" autocomplete="off" placeholder="Např. R5K8P2" class="inline-input" />
          <button id="pairBtn" type="button" class="primary">Spárovat</button>
        </div>
        <p class="sub" id="pairStatus"></p>
      </section>

      <section class="card" id="devicesCard" hidden>
        <h2>Moje tablety</h2>
        <div id="deviceList" class="device-list"></div>
      </section>

      <section class="card" id="settingsCard" hidden>
        <h2>Nastavení tabletu</h2>
        <p class="sub" id="selectedDeviceLabel">Vyber tablet ze seznamu.</p>
        <label class="field">
          <span>Název / popisek</span>
          <input id="deviceLabel" type="text" autocomplete="off" />
        </label>
        <label class="field">
          <span>Hlasitost <strong id="volLabel"></strong></span>
          <input id="volume" type="range" min="0" max="100" />
        </label>
        <label class="field">
          <span>Hláška na celou obrazovku</span>
          <textarea id="alert" rows="3"></textarea>
        </label>
        <div class="row">
          <button id="saveSettings" type="button" class="primary">Uložit název a hlášku</button>
          <button id="clearAlert" type="button">Vymazat hlášku</button>
        </div>
        <h3 class="sub">Provoz tabletu (PIN, SIM)</h3>
        <p class="sub">
          Stejný PIN odemkne na tabletu skrytý vstup (5× klepnutí do levého horního rohu) a otevře systémová nastavení.
        </p>
        <label class="field">
          <span>Admin PIN (4 číslice)</span>
          <input id="adminPin" type="password" inputmode="numeric" maxlength="4" autocomplete="off" placeholder="např. 8254" />
        </label>
        <label class="field">
          <span>Číslo SIM v tabletu</span>
          <input id="simNumber" type="text" inputmode="tel" autocomplete="off" placeholder="+420 …" />
        </label>
        <label class="field">
          <span>Jméno hlasového asistenta</span>
          <input id="assistantName" type="text" autocomplete="off" placeholder="Matěj" />
        </label>
        <div class="row">
          <button id="saveConfig" type="button" class="primary">Uložit PIN a SIM</button>
        </div>
      </section>

      <section class="card" id="contactsCard" hidden>
        <h2>Kontakty na tabletu</h2>
        <ul id="contactList" class="list"></ul>
        <div class="grid">
          <label class="field">
            <span>Jméno</span>
            <input id="newName" type="text" autocomplete="off" />
          </label>
          <label class="field">
            <span>Telefon</span>
            <input id="newPhone" type="text" inputmode="tel" autocomplete="off" />
          </label>
        </div>
        <button id="addContact" type="button" class="primary">Přidat kontakt</button>
      </section>
    </main>
  `

  const conn = document.querySelector<HTMLParagraphElement>('#conn')!
  const authBtn = document.querySelector<HTMLButtonElement>('#authBtn')!
  const authState = document.querySelector<HTMLParagraphElement>('#authState')!
  const pairCard = document.querySelector<HTMLElement>('#pairCard')!
  const pairCode = document.querySelector<HTMLInputElement>('#pairCode')!
  const pairBtn = document.querySelector<HTMLButtonElement>('#pairBtn')!
  const pairStatus = document.querySelector<HTMLParagraphElement>('#pairStatus')!
  const devicesCard = document.querySelector<HTMLElement>('#devicesCard')!
  const deviceList = document.querySelector<HTMLDivElement>('#deviceList')!
  const settingsCard = document.querySelector<HTMLElement>('#settingsCard')!
  const contactsCard = document.querySelector<HTMLElement>('#contactsCard')!
  const selectedDeviceLabel = document.querySelector<HTMLParagraphElement>('#selectedDeviceLabel')!
  const deviceLabel = document.querySelector<HTMLInputElement>('#deviceLabel')!
  const volume = document.querySelector<HTMLInputElement>('#volume')!
  const volLabel = document.querySelector<HTMLElement>('#volLabel')!
  const alertBox = document.querySelector<HTMLTextAreaElement>('#alert')!
  const contactList = document.querySelector<HTMLUListElement>('#contactList')!
  const newName = document.querySelector<HTMLInputElement>('#newName')!
  const newPhone = document.querySelector<HTMLInputElement>('#newPhone')!

  volume.addEventListener('input', () => {
    volLabel.textContent = `${volume.value} %`
  })

  authBtn.addEventListener('click', async () => {
    if (authClient.currentUser) {
      await signOut(authClient)
      return
    }
    await signInWithPopup(authClient, provider)
  })

  pairBtn.addEventListener('click', async () => {
    const user = authClient.currentUser
    if (!user) return

    const code = normalizeCode(pairCode.value)
    if (!code) {
      pairStatus.textContent = 'Zadej párovací kód z tabletu.'
      return
    }

    pairStatus.textContent = 'Páruji tablet…'
    try {
      const claimRef = doc(db, PAIRING_COLLECTION, code)
      const claimSnap = await getDoc(claimRef)
      if (!claimSnap.exists()) {
        throw new Error('Kód neexistuje nebo už expiroval.')
      }

      const claim = claimSnap.data()
      const deviceId = String(claim[KEY_DEVICE_ID] ?? '')
      if (!deviceId) {
        throw new Error('Párovací kód neobsahuje ID zařízení.')
      }
      if (claim[KEY_USED_AT]) {
        throw new Error('Kód už byl použit. Na tabletu obnov kód.')
      }

      const joinRef = doc(db, JOIN_COLLECTION, `${deviceId}_${user.uid}`)
      const deviceRef = doc(db, COLLECTION, deviceId)
      const batch = writeBatch(db)
      batch.update(claimRef, {
        [KEY_USED_AT]: serverTimestamp(),
        [KEY_USED_BY_UID]: user.uid,
      })
      batch.set(joinRef, {
        [KEY_DEVICE_ID]: deviceId,
        [KEY_UID]: user.uid,
        [KEY_ROLE]: 'admin',
        [KEY_CLAIM_CODE]: code,
        createdAt: serverTimestamp(),
      })
      await batch.commit()
      await setDoc(
        deviceRef,
        {
          [KEY_PAIRED]: true,
          pairedAt: serverTimestamp(),
        },
        { merge: true },
      )
      pairStatus.textContent = `Tablet ${deviceId} je spárovaný.`
      pairCode.value = ''
    } catch (error) {
      pairStatus.textContent = getErrorMessage(error)
    }
  })

  document.querySelector<HTMLButtonElement>('#saveSettings')!.addEventListener('click', async () => {
    if (!selectedDeviceId) return
    await setDoc(
      doc(db, COLLECTION, selectedDeviceId),
      {
        [KEY_DEVICE_LABEL]: deviceLabel.value.trim(),
        [KEY_VOLUME_PERCENT]: Number(volume.value),
        [KEY_ALERT_MESSAGE]: alertBox.value.trim(),
        updatedAt: serverTimestamp(),
      },
      { merge: true },
    )
  })

  document.querySelector<HTMLButtonElement>('#saveConfig')!.addEventListener('click', async () => {
    if (!selectedDeviceId) return
    const adminPinEl = document.querySelector<HTMLInputElement>('#adminPin')!
    const simNumberEl = document.querySelector<HTMLInputElement>('#simNumber')!
    const assistantNameEl = document.querySelector<HTMLInputElement>('#assistantName')!
    const pin = adminPinEl.value.replace(/\D/g, '').slice(0, 4)
    if (pin.length !== 4) {
      conn.textContent = 'Zadej přesně 4 číslice admin PINu.'
      conn.classList.add('bad')
      return
    }
    conn.classList.remove('bad')
    try {
      await setDoc(
        doc(db, COLLECTION, selectedDeviceId, SUB_CONFIG, CONFIG_DOC_ID),
        {
          [KEY_ADMIN_PIN]: pin,
          [KEY_SIM_NUMBER]: simNumberEl.value.trim(),
          [KEY_ASSISTANT_NAME]: assistantNameEl.value.trim() || 'Matěj',
          updatedAt: serverTimestamp(),
        },
        { merge: true },
      )
      conn.textContent = 'PIN a provozní údaje uloženy.'
    } catch (error) {
      conn.textContent = `Chyba uložení: ${getErrorMessage(error)}`
      conn.classList.add('bad')
    }
  })

  document.querySelector<HTMLButtonElement>('#clearAlert')!.addEventListener('click', async () => {
    if (!selectedDeviceId) return
    alertBox.value = ''
    await setDoc(
      doc(db, COLLECTION, selectedDeviceId),
      { [KEY_ALERT_MESSAGE]: deleteField() },
      { merge: true },
    )
  })

  document.querySelector<HTMLButtonElement>('#addContact')!.addEventListener('click', async () => {
    if (!selectedDeviceId) return
    const name = newName.value.trim()
    const phone = newPhone.value.trim()
    if (!name && !phone) return
    await addDoc(collection(db, COLLECTION, selectedDeviceId, SUB_CONTACTS), {
      [KEY_NAME]: name,
      [KEY_PHONE]: phone,
      [KEY_SORT_ORDER]: Date.now(),
      createdAt: serverTimestamp(),
    })
    newName.value = ''
    newPhone.value = ''
  })

  onAuthStateChanged(authClient, (user) => {
    stopJoins?.()
    stopDevice?.()
    stopContacts?.()
    stopConfig?.()
    stopJoins = null
    stopDevice = null
    stopContacts = null
    stopConfig = null
    selectedDeviceId = null
    renderSignedOut()

    if (!user) {
      authBtn.textContent = 'Přihlásit přes Google'
      authState.textContent = 'Pro správu tabletů se přihlas přes Google.'
      conn.textContent = 'Přihlas se přes Google'
      conn.classList.remove('bad')
      return
    }

    authBtn.textContent = 'Odhlásit'
    authState.textContent = `Přihlášen: ${user.displayName ?? user.email ?? user.uid}`
    conn.textContent = 'Firebase Auth + Firestore · online'
    conn.classList.remove('bad')
    pairCard.hidden = false
    devicesCard.hidden = false

    void setDoc(doc(db, 'users', user.uid), {
      lastSeenAt: serverTimestamp(),
      email: user.email ?? '',
      displayName: user.displayName ?? '',
    }, { merge: true }).catch(() => {})

    const joinsQuery = query(
      collection(db, JOIN_COLLECTION),
      where(KEY_UID, '==', user.uid),
    )

    stopJoins = onSnapshot(
      joinsQuery,
      async (snap) => {
        const items = await Promise.all(
          snap.docs.map(async (joinDoc): Promise<DeviceListItem> => {
            const joinData = joinDoc.data()
            const deviceId = String(joinData[KEY_DEVICE_ID] ?? '')
            const deviceSnap = await getDoc(doc(db, COLLECTION, deviceId))
            const deviceData = deviceSnap.data() ?? {}
            return {
              deviceId,
              label: String(deviceData[KEY_DEVICE_LABEL] ?? deviceId),
              role: String(joinData[KEY_ROLE] ?? 'admin'),
              paired: Boolean(deviceData[KEY_PAIRED] ?? false),
            }
          }),
        )
        renderDeviceList(items, user)
      },
      (err) => {
        conn.textContent = `Chyba seznamu zařízení: ${err.message}`
        conn.classList.add('bad')
      },
    )
  })

  function renderSignedOut() {
    pairCard.hidden = true
    devicesCard.hidden = true
    settingsCard.hidden = true
    contactsCard.hidden = true
    deviceList.innerHTML = ''
    contactList.innerHTML = ''
    selectedDeviceLabel.textContent = 'Vyber tablet ze seznamu.'
    pairStatus.textContent = ''
  }

  function renderDeviceList(items: DeviceListItem[], user: User) {
    deviceList.innerHTML = ''
    if (items.length === 0) {
      deviceList.innerHTML = '<p class="sub">Zatím nespravuješ žádný tablet. Spáruj první přes kód.</p>'
      settingsCard.hidden = true
      contactsCard.hidden = true
      return
    }

    if (!selectedDeviceId || !items.some((item) => item.deviceId === selectedDeviceId)) {
      selectedDeviceId = items[0]?.deviceId ?? null
      if (selectedDeviceId) {
        connectSelectedDevice(selectedDeviceId, user)
      }
    }

    items.forEach((item) => {
      const button = document.createElement('button')
      button.type = 'button'
      button.className = `device-chip${item.deviceId === selectedDeviceId ? ' active' : ''}`
      button.innerHTML = `
        <span>${escapeHtml(item.label)}</span>
        <small>${escapeHtml(item.deviceId)} · ${escapeHtml(item.role)}${item.paired ? '' : ' · čeká na spárování'}</small>
      `
      button.addEventListener('click', () => {
        selectedDeviceId = item.deviceId
        connectSelectedDevice(item.deviceId, user)
        renderDeviceList(items, user)
      })
      deviceList.appendChild(button)
    })
  }

  function connectSelectedDevice(deviceId: string, user: User) {
    stopDevice?.()
    stopContacts?.()
    stopConfig?.()
    settingsCard.hidden = false
    contactsCard.hidden = false
    selectedDeviceLabel.textContent = `Zařízení ${deviceId}`

    stopDevice = onSnapshot(
      doc(db, COLLECTION, deviceId),
      (snap) => {
        if (!snap.exists()) return
        const data = snap.data()
        deviceLabel.value = String(data[KEY_DEVICE_LABEL] ?? '')
        const vol = Number(data[KEY_VOLUME_PERCENT] ?? 50)
        volume.value = String(Math.min(100, Math.max(0, vol)))
        volLabel.textContent = `${volume.value} %`
        alertBox.value = String(data[KEY_ALERT_MESSAGE] ?? '')

        const pairingCode = String(data[KEY_PAIRING_CODE] ?? '')
        const pairingExpires = formatFirestoreDate(data[KEY_PAIRING_EXPIRES_AT])
        const pairingHint = pairingCode
          ? ` · pairing ${pairingCode}${pairingExpires ? ` do ${pairingExpires}` : ''}`
          : ''
        selectedDeviceLabel.textContent =
          `${String(data[KEY_DEVICE_LABEL] ?? deviceId)} · ${deviceId}${pairingHint}`
      },
      (err) => {
        conn.textContent = `Chyba zařízení: ${err.message}`
        conn.classList.add('bad')
      },
    )

    const contactsQuery = query(
      collection(db, COLLECTION, deviceId, SUB_CONTACTS),
      orderBy(KEY_SORT_ORDER, 'asc'),
    )

    const adminPinEl = document.querySelector<HTMLInputElement>('#adminPin')!
    const simNumberEl = document.querySelector<HTMLInputElement>('#simNumber')!
    const assistantNameEl = document.querySelector<HTMLInputElement>('#assistantName')!

    stopConfig = onSnapshot(
      doc(db, COLLECTION, deviceId, SUB_CONFIG, CONFIG_DOC_ID),
      (snap) => {
        if (!snap.exists()) {
          adminPinEl.value = ''
          simNumberEl.value = ''
          assistantNameEl.value = ''
          return
        }
        const c = snap.data()
        adminPinEl.value = String(c[KEY_ADMIN_PIN] ?? '')
        simNumberEl.value = String(c[KEY_SIM_NUMBER] ?? '')
        assistantNameEl.value = String(c[KEY_ASSISTANT_NAME] ?? '')
      },
      (err) => {
        conn.textContent = `Chyba konfigurace: ${err.message}`
        conn.classList.add('bad')
      },
    )

    stopContacts = onSnapshot(
      contactsQuery,
      (snap) => {
        contactList.innerHTML = ''
        snap.forEach((contactDoc) => {
          const data = contactDoc.data()
          const li = document.createElement('li')
          li.innerHTML = `
            <div class="li-main">
              <div class="li-title">${escapeHtml(String(data[KEY_NAME] ?? '(bez jména)'))}</div>
              <div class="li-sub">${escapeHtml(String(data[KEY_PHONE] ?? '—'))}</div>
            </div>
            <button type="button" data-id="${contactDoc.id}" class="danger linkbtn">Smazat</button>
          `
          contactList.appendChild(li)
        })

        contactList.querySelectorAll<HTMLButtonElement>('button[data-id]').forEach((btn) => {
          btn.addEventListener('click', async () => {
            const id = btn.dataset.id
            if (!id || !selectedDeviceId) return
            await deleteDoc(doc(db, COLLECTION, selectedDeviceId, SUB_CONTACTS, id))
          })
        })
      },
      (err) => {
        conn.textContent = `Chyba kontaktů: ${err.message}`
        conn.classList.add('bad')
      },
    )

    void setDoc(doc(db, 'users', user.uid), {
      lastOpenedDeviceId: deviceId,
      lastSeenAt: serverTimestamp(),
    }, { merge: true }).catch(() => {})
  }
}

function normalizeCode(value: string): string {
  return value.trim().toUpperCase()
}

function formatFirestoreDate(value: unknown): string {
  if (!value || typeof value !== 'object' || !('toDate' in value)) return ''
  const date = (value as { toDate(): Date }).toDate()
  return new Intl.DateTimeFormat('cs-CZ', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return String(error)
}

function escapeHtml(s: string): string {
  return s
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}
