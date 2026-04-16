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
  runTransaction,
  serverTimestamp,
  setDoc,
  updateDoc,
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
  KEY_MESSAGE_BODY,
  KEY_MESSAGE_CREATED_AT,
  KEY_MESSAGE_DELIVERY,
  KEY_MESSAGE_OUTBOUND_NAME,
  KEY_MESSAGE_OUTBOUND_PHONE,
  KEY_MESSAGE_INBOUND_FROM_NAME,
  KEY_MESSAGE_INBOUND_FROM_PHONE,
  KEY_MESSAGE_READ_AT,
  KEY_NAME,
  KEY_PAIRING_CODE,
  KEY_PAIRING_EXPIRES_AT,
  KEY_PAIRED,
  KEY_PHONE,
  KEY_ROLE,
  KEY_SIM_NUMBER,
  KEY_SENDER_DISPLAY_NAME,
  KEY_SENDER_UID,
  KEY_SENIOR_FIRST_NAME,
  KEY_SENIOR_LAST_NAME,
  KEY_ADDRESS_LINE,
  KEY_BATTERY_PERCENT,
  KEY_CHARGING,
  KEY_LAST_HEARTBEAT_AT,
  KEY_NETWORK_LABEL,
  KEY_IS_EMERGENCY,
  KEY_SORT_ORDER,
  KEY_UID,
  KEY_USED_AT,
  KEY_USED_BY_UID,
  KEY_VOLUME_PERCENT,
  PAIRING_COLLECTION,
  STATUS_DOC_ID,
  SUB_CONFIG,
  SUB_CONTACTS,
  SUB_MESSAGES,
  SUB_STATUS,
} from './constants'
import { auth, firestore, googleProvider, initFirebase } from './firebase'

type DeviceListItem = {
  deviceId: string
  label: string
  role: string
  paired: boolean
  batteryPercent?: number
  charging?: boolean
  lastHeartbeatLabel?: string
  networkLabel?: string
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

  function sortOrderToNumber(v: unknown): number {
    if (typeof v === 'number' && Number.isFinite(v)) return v
    if (typeof v === 'string') {
      const n = Number.parseInt(v, 10)
      return Number.isFinite(n) ? n : 0
    }
    return 0
  }

  async function swapContactSortOrders(
    deviceId: string,
    idA: string,
    idB: string,
  ): Promise<void> {
    if (idA === idB) return
    const refA = doc(db, COLLECTION, deviceId, SUB_CONTACTS, idA)
    const refB = doc(db, COLLECTION, deviceId, SUB_CONTACTS, idB)
    await runTransaction(db, async (transaction) => {
      const sa = await transaction.get(refA)
      const sb = await transaction.get(refB)
      if (!sa.exists() || !sb.exists()) {
        throw new Error('Kontakt neexistuje.')
      }
      const orderA = sortOrderToNumber(sa.data()?.[KEY_SORT_ORDER])
      const orderB = sortOrderToNumber(sb.data()?.[KEY_SORT_ORDER])
      transaction.update(refA, { [KEY_SORT_ORDER]: orderB })
      transaction.update(refB, { [KEY_SORT_ORDER]: orderA })
    })
  }

  let selectedDeviceId: string | null = null
  let stopDevice: (() => void) | null = null
  let stopStatus: (() => void) | null = null
  let stopContacts: (() => void) | null = null
  let stopJoins: (() => void) | null = null
  let stopConfig: (() => void) | null = null
  let stopMessages: (() => void) | null = null

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

      <section class="card" id="devicesCard" hidden>
        <h2>Moje tablety <span class="card-kicker">— výběr zařízení pro nastavení pod tímto seznamem</span></h2>
        <div id="deviceList" class="device-list"></div>
      </section>

      <section class="card" id="pairCard" hidden>
        <h2>Přidat nový tablet</h2>
        <div class="pair-hint warn">
          <strong>Nejdřív zkontroluj „Moje tablety“ výše.</strong> Už přidaný uvidíš v seznamu — ten znovu nepáruj.
          Kód z tabletu zadej jen při <strong>prvním</strong> spojení toho zařízení s tímto účtem.
        </div>
        <p class="sub">Na tabletu otevři párovací obrazovku a přepiš kód (bez mezer, velikost písmen nevadí).</p>
        <label class="field">
          <span>Role u tohoto tabletu</span>
          <select id="pairRole" class="inline-select">
            <option value="admin">Správce (PIN, kontakty, vzkazy)</option>
            <option value="viewer">Člen rodiny (jen vzkazy na tablet)</option>
          </select>
        </label>
        <p class="sub">Člen rodiny neuvidí PIN ani kontakty — může posílat vzkazy. Pravidla jsou ve Firestore.</p>
        <div class="row">
          <input id="pairCode" type="text" autocomplete="off" placeholder="Např. R5K8P2" class="inline-input" />
          <button id="pairBtn" type="button" class="primary">Spárovat</button>
        </div>
        <p class="sub" id="pairStatus"></p>
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
        <h3 class="sub">Profil seniora</h3>
        <p class="sub">Zobrazí se na tabletu (jméno, adresa). Nezávislé na PINu — můžeš upravit bez měnit kód.</p>
        <div class="grid">
          <label class="field">
            <span>Jméno</span>
            <input id="seniorFirstName" type="text" autocomplete="off" />
          </label>
          <label class="field">
            <span>Příjmení</span>
            <input id="seniorLastName" type="text" autocomplete="off" />
          </label>
        </div>
        <label class="field">
          <span>Adresa (řádek)</span>
          <input id="addressLine" type="text" autocomplete="off" placeholder="Ulice, město…" />
        </label>
        <div class="row">
          <button id="saveSeniorProfile" type="button" class="primary">Uložit profil seniora</button>
        </div>
      </section>

      <section class="card" id="contactsCard" hidden>
        <h2>Kontakty na tabletu</h2>
        <p class="sub">Šipkami změníš pořadí v seznamu (stejné jako na Androidu u správce). Zaškrtni <strong>Nouze</strong> pro prioritu při hovoru / Matějovi.</p>
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

      <section class="card" id="messagesCard" hidden>
        <h2>Vzkazy na tablet</h2>
        <p class="sub">
          Text se uloží do Firestore a tablet ho uvidí v překryvu; po potvrzení seniora se zapíše přečtení.
          Na pozadí může dorazit i push (Cloud Functions).
        </p>
        <label class="field">
          <span>Text vzkazu</span>
          <textarea id="messageBody" rows="4" placeholder="Krátká zpráva pro seniora…"></textarea>
        </label>
        <div class="row">
          <button id="sendMessage" type="button" class="primary">Odeslat na tablet</button>
        </div>
        <h3 class="sub">Historie</h3>
        <p class="sub" id="messagesViewerHint" hidden></p>
        <ul id="messageList" class="list"></ul>
      </section>
    </main>
  `

  const conn = document.querySelector<HTMLParagraphElement>('#conn')!
  const authBtn = document.querySelector<HTMLButtonElement>('#authBtn')!
  const authState = document.querySelector<HTMLParagraphElement>('#authState')!
  const pairCard = document.querySelector<HTMLElement>('#pairCard')!
  const pairRole = document.querySelector<HTMLSelectElement>('#pairRole')!
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
  const messagesCard = document.querySelector<HTMLElement>('#messagesCard')!
  const messageBody = document.querySelector<HTMLTextAreaElement>('#messageBody')!
  const messageList = document.querySelector<HTMLUListElement>('#messageList')!
  const sendMessageBtn = document.querySelector<HTMLButtonElement>('#sendMessage')!

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
    pairStatus.className = 'sub'
    try {
      const claimRef = doc(db, PAIRING_COLLECTION, code)
      const claimSnap = await getDoc(claimRef)
      if (!claimSnap.exists()) {
        throw new Error('Kód neexistuje nebo už expiroval. Na tabletu vygeneruj nový kód.')
      }

      const claim = claimSnap.data()
      const deviceId = String(claim[KEY_DEVICE_ID] ?? '')
      if (!deviceId) {
        throw new Error('Párovací kód neobsahuje ID zařízení.')
      }

      const joinRef = doc(db, JOIN_COLLECTION, `${deviceId}_${user.uid}`)
      const joinSnap = await getDoc(joinRef)
      if (joinSnap.exists()) {
        pairStatus.textContent =
          `Tento tablet (${deviceId}) už máš u tohoto účtu — je v seznamu „Moje tablety“ nad tímto blokem. Další párování nepotřebuješ.`
        pairStatus.className = 'sub pair-status-info'
        return
      }

      if (claim[KEY_USED_AT]) {
        throw new Error(
          'Tento kód už byl jednou použit. Pokud jsi to byl ty, tablet už najdeš v „Moje tablety“. Jinak na tabletu vygeneruj nový kód.',
        )
      }

      const deviceRef = doc(db, COLLECTION, deviceId)
      const batch = writeBatch(db)
      batch.update(claimRef, {
        [KEY_USED_AT]: serverTimestamp(),
        [KEY_USED_BY_UID]: user.uid,
      })
      const joinRole = pairRole.value === 'viewer' ? 'viewer' : 'admin'
      batch.set(joinRef, {
        [KEY_DEVICE_ID]: deviceId,
        [KEY_UID]: user.uid,
        [KEY_ROLE]: joinRole,
        [KEY_CLAIM_CODE]: code,
        createdAt: serverTimestamp(),
      })
      // Jedním commitem s deviceAdmins — pravidla vyhodnotí spárování atomicky (oddělené setDoc po batchi občas selže).
      batch.set(
        deviceRef,
        {
          [KEY_PAIRED]: true,
          pairedAt: serverTimestamp(),
        },
        { merge: true },
      )
      await batch.commit()
      pairStatus.textContent = `Tablet ${deviceId} je spárovaný — zvol ho v „Moje tablety“ a pokračuj v nastavení níže.`
      pairStatus.className = 'sub pair-status-ok'
      pairCode.value = ''
    } catch (error) {
      const raw = getErrorMessage(error)
      const friendly =
        /permission|insufficient permissions|PERMISSION_DENIED/i.test(raw)
          ? `${raw} — často jde o to, že tablet už je u účtu spárovaný (zkontroluj „Moje tablety“), nebo kód není aktuální.`
          : raw
      pairStatus.textContent = friendly
      pairStatus.className = 'sub pair-status-err'
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

  document.querySelector<HTMLButtonElement>('#saveSeniorProfile')!.addEventListener('click', async () => {
    if (!selectedDeviceId) return
    const first = document.querySelector<HTMLInputElement>('#seniorFirstName')!
    const last = document.querySelector<HTMLInputElement>('#seniorLastName')!
    const addr = document.querySelector<HTMLInputElement>('#addressLine')!
    conn.classList.remove('bad')
    try {
      await setDoc(
        doc(db, COLLECTION, selectedDeviceId, SUB_CONFIG, CONFIG_DOC_ID),
        {
          [KEY_SENIOR_FIRST_NAME]: first.value.trim(),
          [KEY_SENIOR_LAST_NAME]: last.value.trim(),
          [KEY_ADDRESS_LINE]: addr.value.trim(),
          updatedAt: serverTimestamp(),
        },
        { merge: true },
      )
      conn.textContent = 'Profil seniora uložen.'
    } catch (error) {
      conn.textContent = `Chyba uložení profilu: ${getErrorMessage(error)}`
      conn.classList.add('bad')
    }
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

  sendMessageBtn.addEventListener('click', async () => {
    const user = authClient.currentUser
    if (!user || !selectedDeviceId) return
    const body = messageBody.value.trim()
    if (!body) {
      conn.textContent = 'Napiš text vzkazu.'
      conn.classList.add('bad')
      return
    }
    conn.classList.remove('bad')
    try {
      await addDoc(collection(db, COLLECTION, selectedDeviceId, SUB_MESSAGES), {
        [KEY_MESSAGE_BODY]: body,
        [KEY_SENDER_UID]: user.uid,
        [KEY_SENDER_DISPLAY_NAME]: user.displayName ?? user.email ?? '',
        [KEY_MESSAGE_CREATED_AT]: serverTimestamp(),
        [KEY_MESSAGE_READ_AT]: null,
      })
      messageBody.value = ''
      conn.textContent = 'Vzkaz odeslán.'
    } catch (error) {
      conn.textContent = `Chyba odeslání vzkazu: ${getErrorMessage(error)}`
      conn.classList.add('bad')
    }
  })

  document.querySelector<HTMLButtonElement>('#addContact')!.addEventListener('click', async () => {
    if (!selectedDeviceId) return
    const name = newName.value.trim()
    const phone = newPhone.value.trim()
    if (!name && !phone) return
    await addDoc(collection(db, COLLECTION, selectedDeviceId, SUB_CONTACTS), {
      [KEY_NAME]: name,
      [KEY_PHONE]: phone,
      [KEY_IS_EMERGENCY]: false,
      [KEY_SORT_ORDER]: Date.now(),
      createdAt: serverTimestamp(),
    })
    newName.value = ''
    newPhone.value = ''
  })

  onAuthStateChanged(authClient, (user) => {
    stopJoins?.()
    stopDevice?.()
    stopStatus?.()
    stopContacts?.()
    stopConfig?.()
    stopMessages?.()
    stopJoins = null
    stopDevice = null
    stopStatus = null
    stopContacts = null
    stopConfig = null
    stopMessages = null
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
            const bat = deviceData[KEY_BATTERY_PERCENT]
            const batteryPercent =
              typeof bat === 'number'
                ? bat
                : typeof bat === 'string'
                  ? Number.parseInt(bat, 10)
                  : undefined
            const hb = deviceData[KEY_LAST_HEARTBEAT_AT]
            const statusSnap = await getDoc(
              doc(db, COLLECTION, deviceId, SUB_STATUS, STATUS_DOC_ID),
            )
            const networkLabelRaw = statusSnap.exists()
              ? String(statusSnap.data()?.[KEY_NETWORK_LABEL] ?? '').trim()
              : ''
            return {
              deviceId,
              label: String(deviceData[KEY_DEVICE_LABEL] ?? deviceId),
              role: String(joinData[KEY_ROLE] ?? 'admin'),
              paired: Boolean(deviceData[KEY_PAIRED] ?? false),
              batteryPercent:
                Number.isFinite(batteryPercent) ? batteryPercent : undefined,
              charging: Boolean(deviceData[KEY_CHARGING]),
              lastHeartbeatLabel: formatFirestoreDate(hb) ?? undefined,
              networkLabel: networkLabelRaw || undefined,
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
    messagesCard.hidden = true
    deviceList.innerHTML = ''
    contactList.innerHTML = ''
    messageList.innerHTML = ''
    selectedDeviceLabel.textContent = 'Vyber tablet ze seznamu.'
    pairStatus.textContent = ''
  }

  function renderDeviceList(items: DeviceListItem[], user: User) {
    deviceList.innerHTML = ''
    if (items.length === 0) {
      deviceList.innerHTML = '<p class="sub">Zatím nespravuješ žádný tablet. Spáruj první přes kód.</p>'
      settingsCard.hidden = true
      contactsCard.hidden = true
      messagesCard.hidden = true
      return
    }

    if (!selectedDeviceId || !items.some((item) => item.deviceId === selectedDeviceId)) {
      selectedDeviceId = items[0]?.deviceId ?? null
      if (selectedDeviceId) {
        const r =
          items.find((i) => i.deviceId === selectedDeviceId)?.role ?? 'admin'
        connectSelectedDevice(selectedDeviceId, user, r)
      }
    }

    items.forEach((item) => {
      const button = document.createElement('button')
      button.type = 'button'
      button.className = `device-chip${item.deviceId === selectedDeviceId ? ' active' : ''}`
      const statusBits: string[] = []
      if (item.batteryPercent != null && Number.isFinite(item.batteryPercent)) {
        statusBits.push(
          `${Math.round(item.batteryPercent)}%${item.charging ? ' ⚡' : ''}`,
        )
      }
      if (item.lastHeartbeatLabel) {
        statusBits.push(`kontakt ${item.lastHeartbeatLabel}`)
      }
      if (item.networkLabel) {
        statusBits.push(String(item.networkLabel))
      }
      const statusLine =
        statusBits.length > 0 ? ` · ${statusBits.join(' · ')}` : ''
      const roleLabel = item.role === 'viewer' ? 'rodina' : 'správce'
      button.innerHTML = `
        <span>${escapeHtml(item.label)}</span>
        <small>${escapeHtml(item.deviceId)} · ${escapeHtml(roleLabel)}${item.paired ? '' : ' · čeká na spárování'}${escapeHtml(statusLine)}</small>
      `
      button.addEventListener('click', () => {
        selectedDeviceId = item.deviceId
        connectSelectedDevice(item.deviceId, user, item.role)
        renderDeviceList(items, user)
      })
      deviceList.appendChild(button)
    })
  }

  function connectSelectedDevice(
    deviceId: string,
    user: User,
    joinRole: string = 'admin',
  ) {
    stopDevice?.()
    stopStatus?.()
    stopContacts?.()
    stopConfig?.()
    stopMessages?.()
    const isViewer = joinRole === 'viewer'
    settingsCard.hidden = isViewer
    contactsCard.hidden = isViewer
    messagesCard.hidden = false
    const viewerHint = document.querySelector<HTMLParagraphElement>(
      '#messagesViewerHint',
    )!
    if (isViewer) {
      viewerHint.hidden = false
      viewerHint.textContent =
        'Režim člena rodiny: můžeš posílat vzkazy. Úpravy PINu, kontaktů a názvu zařízení dělá hlavní správce.'
    } else {
      viewerHint.hidden = true
      viewerHint.textContent = ''
    }
    selectedDeviceLabel.textContent = `Zařízení ${deviceId}`

    let deviceHeaderBase = `Zařízení ${deviceId}`
    let networkSuffix = ''
    const refreshDeviceHeader = () => {
      selectedDeviceLabel.textContent = deviceHeaderBase + networkSuffix
    }

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
        const bat = data[KEY_BATTERY_PERCENT]
        const batteryNum =
          typeof bat === 'number'
            ? bat
            : typeof bat === 'string'
              ? Number.parseInt(bat, 10)
              : NaN
        const batteryHint =
          Number.isFinite(batteryNum)
            ? ` · baterie ${Math.round(batteryNum)}%${data[KEY_CHARGING] ? ' nabíjení' : ''}`
            : ''
        const hb = formatFirestoreDate(data[KEY_LAST_HEARTBEAT_AT])
        const heartbeatHint = hb ? ` · naposledy z tabletu ${hb}` : ''
        deviceHeaderBase =
          `${String(data[KEY_DEVICE_LABEL] ?? deviceId)} · ${deviceId}${pairingHint}${batteryHint}${heartbeatHint}`
        refreshDeviceHeader()
      },
      (err) => {
        conn.textContent = `Chyba zařízení: ${err.message}`
        conn.classList.add('bad')
      },
    )

    stopStatus = onSnapshot(
      doc(db, COLLECTION, deviceId, SUB_STATUS, STATUS_DOC_ID),
      (snap) => {
        const nl = snap.exists()
          ? String(snap.data()?.[KEY_NETWORK_LABEL] ?? '').trim()
          : ''
        networkSuffix = nl ? ` · síť ${nl}` : ''
        refreshDeviceHeader()
      },
      (err) => {
        conn.textContent = `Chyba stavu zařízení: ${err.message}`
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

    const seniorFirstEl = document.querySelector<HTMLInputElement>('#seniorFirstName')!
    const seniorLastEl = document.querySelector<HTMLInputElement>('#seniorLastName')!
    const addressLineEl = document.querySelector<HTMLInputElement>('#addressLine')!

    stopConfig = onSnapshot(
      doc(db, COLLECTION, deviceId, SUB_CONFIG, CONFIG_DOC_ID),
      (snap) => {
        if (!snap.exists()) {
          adminPinEl.value = ''
          simNumberEl.value = ''
          assistantNameEl.value = ''
          seniorFirstEl.value = ''
          seniorLastEl.value = ''
          addressLineEl.value = ''
          return
        }
        const c = snap.data()
        adminPinEl.value = String(c[KEY_ADMIN_PIN] ?? '')
        simNumberEl.value = String(c[KEY_SIM_NUMBER] ?? '')
        assistantNameEl.value = String(c[KEY_ASSISTANT_NAME] ?? '')
        seniorFirstEl.value = String(c[KEY_SENIOR_FIRST_NAME] ?? '')
        seniorLastEl.value = String(c[KEY_SENIOR_LAST_NAME] ?? '')
        addressLineEl.value = String(c[KEY_ADDRESS_LINE] ?? '')
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
        snap.docs.forEach((contactDoc, index) => {
          const data = contactDoc.data()
          const isEmergency = Boolean(data[KEY_IS_EMERGENCY])
          const prevId = index > 0 ? snap.docs[index - 1].id : ''
          const nextId =
            index < snap.docs.length - 1 ? snap.docs[index + 1].id : ''
          const li = document.createElement('li')
          li.className = isEmergency ? 'contact-row emergency' : 'contact-row'
          li.innerHTML = `
            <div class="contact-sort">
              <button type="button" class="linkbtn sort-arrow" aria-label="Výš" data-swap-a="${escapeHtml(contactDoc.id)}" data-swap-b="${escapeHtml(prevId)}" ${prevId ? '' : 'disabled'}>↑</button>
              <button type="button" class="linkbtn sort-arrow" aria-label="Níž" data-swap-a="${escapeHtml(contactDoc.id)}" data-swap-b="${escapeHtml(nextId)}" ${nextId ? '' : 'disabled'}>↓</button>
            </div>
            <div class="li-main">
              <div class="li-title">${escapeHtml(String(data[KEY_NAME] ?? '(bez jména)'))}</div>
              <div class="li-sub">${escapeHtml(String(data[KEY_PHONE] ?? '—'))}</div>
              <label class="inline-check">
                <input type="checkbox" data-emergency-id="${contactDoc.id}" ${isEmergency ? 'checked' : ''} />
                <span>Nouze</span>
              </label>
            </div>
            <button type="button" data-contact-delete="${contactDoc.id}" class="danger linkbtn">Smazat</button>
          `
          contactList.appendChild(li)
        })

        contactList.querySelectorAll<HTMLInputElement>('input[data-emergency-id]').forEach((box) => {
          box.addEventListener('change', async () => {
            const id = box.dataset.emergencyId
            if (!id || !selectedDeviceId) return
            try {
              await updateDoc(doc(db, COLLECTION, selectedDeviceId, SUB_CONTACTS, id), {
                [KEY_IS_EMERGENCY]: box.checked,
              })
            } catch (error) {
              conn.textContent = `Chyba úpravy kontaktu: ${getErrorMessage(error)}`
              conn.classList.add('bad')
            }
          })
        })

        contactList.querySelectorAll<HTMLButtonElement>('button[data-swap-a]').forEach((btn) => {
          btn.addEventListener('click', async () => {
            const a = btn.dataset.swapA
            const b = btn.dataset.swapB
            if (!a || !b || !selectedDeviceId) return
            try {
              await swapContactSortOrders(selectedDeviceId, a, b)
            } catch (error) {
              conn.textContent = `Chyba řazení kontaktu: ${getErrorMessage(error)}`
              conn.classList.add('bad')
            }
          })
        })

        contactList.querySelectorAll<HTMLButtonElement>('button[data-contact-delete]').forEach((btn) => {
          btn.addEventListener('click', async () => {
            const id = btn.dataset.contactDelete
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

    const messagesQuery = query(
      collection(db, COLLECTION, deviceId, SUB_MESSAGES),
      orderBy(KEY_MESSAGE_CREATED_AT, 'desc'),
    )

    stopMessages = onSnapshot(
      messagesQuery,
      (snap) => {
        messageList.innerHTML = ''
        snap.forEach((messageDoc) => {
          const data = messageDoc.data()
          const body = String(data[KEY_MESSAGE_BODY] ?? '')
          const from = String(data[KEY_SENDER_DISPLAY_NAME] ?? '').trim() || 'Rodina'
          const readAt = data[KEY_MESSAGE_READ_AT]
          const created = formatFirestoreDate(data[KEY_MESSAGE_CREATED_AT])
          const status = readAt ? 'Přečteno' : 'Nepřečteno na tabletu'
          const delivery = data[KEY_MESSAGE_DELIVERY]
          const outboundPhone = String(data[KEY_MESSAGE_OUTBOUND_PHONE] ?? '').trim()
          const outboundName = String(data[KEY_MESSAGE_OUTBOUND_NAME] ?? '').trim()
          let tech = 'Web → tablet (Firestore)'
          if (delivery === 'tablet_firestore') {
            const target =
              outboundName || outboundPhone
                ? `${outboundName ? `${outboundName}` : 'Kontakt'}${outboundPhone ? ` · ${outboundPhone}` : ''}`
                : 'kontakt'
            tech = `Tablet bez SMS · Firestore · záměr na: ${target}`
          } else if (delivery === 'sms_cellular') {
            const target =
              outboundName || outboundPhone
                ? `${outboundName ? `${outboundName}` : 'Kontakt'}${outboundPhone ? ` · ${outboundPhone}` : ''}`
                : 'kontakt'
            tech = `Tablet · klasická SMS (zrcadlo v cloudu) · ${target}`
          } else if (delivery === 'sms_inbound') {
            const inboundPhone = String(data[KEY_MESSAGE_INBOUND_FROM_PHONE] ?? '').trim()
            const inboundName = String(data[KEY_MESSAGE_INBOUND_FROM_NAME] ?? '').trim()
            const target =
              inboundName || inboundPhone
                ? `${inboundName ? `${inboundName}` : 'Kontakt'}${inboundPhone ? ` · ${inboundPhone}` : ''}`
                : 'kontakt'
            tech = `Tablet · příchozí klasická SMS (zrcadlo v cloudu) · ${target}`
          }
          const li = document.createElement('li')
          li.innerHTML = `
            <div class="li-main">
              <div class="li-title">${escapeHtml(body.slice(0, 200))}${body.length > 200 ? '…' : ''}</div>
              <div class="li-sub">${escapeHtml(from)} · ${escapeHtml(created)} · ${escapeHtml(status)}</div>
              <div class="li-tech">${escapeHtml(tech)}</div>
            </div>
          `
          messageList.appendChild(li)
        })
      },
      (err) => {
        conn.textContent = `Chyba vzkazů: ${err.message}`
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
    dateStyle: 'short',
    timeStyle: 'short',
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
