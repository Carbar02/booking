const byId = (id) => document.getElementById(id);
const currency = new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' });
const dateFormat = new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'short', year: 'numeric' });
const typeLabels = { SINGOLA: 'Singola', DOPPIA: 'Doppia', FAMILIARE: 'Familiare' };
const statusLabels = { CONFERMATA: 'Confermata', ANNULLATA: 'Annullata', IN_ATTESA: 'In attesa', COMPLETATA: 'Completata' };
const photos = [
    'https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=640&q=85',
    'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=640&q=85'
];
const state = { apartments: [], bookings: [], guests: [], availableIds: null, selected: new Map(), searchVersion: 0, cancelId: null, editingApartmentId: null, editingRoomId: null, editingGuestId: null, submitting: false };
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
const icon = (name) => `<i data-lucide="${name}"></i>`;
const refreshIcons = () => window.lucide?.createIcons();
const formatDate = (date) => dateFormat.format(new Date(`${date}T12:00:00`));

function localDate(offset = 0) {
    const date = new Date();
    date.setDate(date.getDate() + offset);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function nights() {
    const duration = (Date.parse(`${byId('departure').value}T00:00:00Z`) - Date.parse(`${byId('arrival').value}T00:00:00Z`)) / 86400000;
    return Number.isFinite(duration) ? Math.max(0, duration) : 0;
}

function showError(id, message) {
    byId(id).textContent = message;
    byId(id).hidden = !message;
}

function toast(message) {
    byId('toast-message').textContent = message;
    byId('toast').hidden = false;
    refreshIcons();
}

async function api(path, options = {}) {
    let response;
    try {
        response = await fetch(path, { ...options, headers: { 'Accept': 'application/json', ...options.headers } });
    } catch {
        throw new Error('Connessione non disponibile. Verifica che Booking sia in esecuzione.');
    }
    const body = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error(body?.errori?.join(' / ') || body?.detail || `Richiesta non riuscita (${response.status}).`);
    }
    if (body === null) throw new Error('Risposta non valida dal server.');
    return body;
}

function emptyState(title, detail, symbol = 'search') {
    return `<div class="empty-state">${icon(symbol)}<h2>${escapeHtml(title)}</h2><p>${escapeHtml(detail)}</p></div>`;
}

function roomMarkup(room) {
    const selected = state.selected.has(room.id);
    const badge = !room.attiva ? '<span class="badge rose">Fuori servizio</span>'
        : `<span class="badge green">${state.availableIds === null ? 'In servizio' : 'Disponibile'}</span>`;
    return `<article class="room ${selected ? 'selected' : ''} ${room.attiva ? '' : 'inactive'}">
        <div class="room-top"><span class="room-icon">${icon(room.tipo === 'SINGOLA' ? 'bed-single' : 'bed-double')}</span>${badge}</div>
        <h3>${escapeHtml(typeLabels[room.tipo] || room.tipo)}</h3><p class="room-number">Camera ${escapeHtml(room.numero)}</p>
        <p class="room-capacity">${icon('users')} ${room.capienzaMassima} ${room.capienzaMassima === 1 ? 'ospite' : 'ospiti'}</p>
        <p class="room-price"><strong>${currency.format(room.prezzoPerNotte)}</strong><span>/ notte</span></p>
        <button class="button ${selected ? 'primary' : 'secondary'}" data-room-id="${room.id}" aria-pressed="${selected}" ${room.attiva ? '' : 'disabled'} aria-label="${selected ? 'Rimuovi' : 'Seleziona'} camera ${escapeHtml(room.numero)} di ${escapeHtml(room.appartamentoNome)}">${icon(selected ? 'check' : 'plus')} ${!room.attiva ? 'Non prenotabile' : selected ? 'Selezionata' : 'Seleziona'}</button>
    </article>`;
}

function renderCatalog() {
    const apartmentId = byId('apartment-filter').value;
    const type = byId('type-filter').value;
    const guests = Number(byId('guests').value);
    let count = 0;
    const sections = state.apartments.map((apartment, index) => {
        if (apartmentId && String(apartment.id) !== apartmentId) return '';
        const rooms = apartment.camere.filter((room) => (!type || room.tipo === type)
            && room.capienzaMassima >= guests && (state.availableIds === null || state.availableIds.has(room.id)));
        count += rooms.length;
        if (!rooms.length) return '';
        return `<section class="property" aria-labelledby="apartment-${apartment.id}" style="animation-delay:${index * 60}ms">
            <div class="property-header"><figure class="property-photo"><img src="${photos[index % photos.length]}" alt="Interno di una camera, immagine illustrativa" width="640" height="376" ${index === 0 ? 'fetchpriority="high"' : 'loading="lazy"'}><figcaption>Immagine illustrativa</figcaption></figure>
                <div class="property-info"><p class="property-number">APPARTAMENTO ${String(index + 1).padStart(2, '0')}</p><h2 id="apartment-${apartment.id}">${escapeHtml(apartment.nome)}</h2><p class="address">${icon('map-pin')}${escapeHtml(apartment.indirizzo)}</p><p class="property-description">${escapeHtml(apartment.descrizione)}</p></div></div>
            <div class="room-grid">${rooms.map(roomMarkup).join('')}</div></section>`;
    }).join('');
    byId('properties').innerHTML = sections || emptyState('Nessuna camera trovata', 'Nessun risultato per le date e i filtri selezionati.');
    byId('results-label').textContent = `${count} ${count === 1 ? 'camera' : 'camere'} ${state.availableIds === null ? 'nel catalogo' : 'disponibili'}${state.availableIds === null ? '' : ` / ${nights()} notti`}`;
    refreshIcons();
}

function renderSummary() {
    const duration = nights();
    byId('summary-dates').textContent = byId('arrival').value && byId('departure').value
        ? `${formatDate(byId('arrival').value)} - ${formatDate(byId('departure').value)} / ${duration} ${duration === 1 ? 'notte' : 'notti'}`
        : 'Date del soggiorno da definire';
    const selections = [...state.selected.values()];
    byId('selected-rooms').innerHTML = selections.length ? selections.map(({ camera, numeroOspiti }) => `<div class="selection-row">
        <div class="selection-top"><div><strong>${escapeHtml(camera.appartamentoNome)} / ${escapeHtml(camera.numero)}</strong><p>${escapeHtml(typeLabels[camera.tipo])}</p></div><button class="icon-button" data-remove-id="${camera.id}" title="Rimuovi camera" aria-label="Rimuovi camera ${escapeHtml(camera.numero)}">${icon('x')}</button></div>
        <div class="selection-bottom"><select data-guests-id="${camera.id}" aria-label="Ospiti per la camera ${escapeHtml(camera.numero)}">${Array.from({ length: camera.capienzaMassima }, (_, index) => `<option value="${index + 1}" ${numeroOspiti === index + 1 ? 'selected' : ''}>${index + 1} ${index === 0 ? 'ospite' : 'ospiti'}</option>`).join('')}</select><strong>${currency.format(camera.prezzoPerNotte * duration)}</strong></div></div>`).join('') : '<p class="muted empty-selection">Nessuna camera selezionata</p>';
    byId('estimated-total').textContent = currency.format(selections.reduce((total, selection) => total + selection.camera.prezzoPerNotte * duration, 0));
    byId('open-booking').disabled = !selections.length || duration <= 0;
    refreshIcons();
}

function invalidateSearch() {
    state.searchVersion++;
    state.availableIds = null;
    state.selected.clear();
    showError('search-error', '');
    renderCatalog();
    renderSummary();
}

function validDates() {
    const arrival = byId('arrival');
    const departure = byId('departure');
    departure.setCustomValidity(departure.value <= arrival.value ? "La partenza deve essere successiva all'arrivo." : '');
    return byId('search-form').reportValidity();
}

async function searchRooms() {
    if (!validDates()) return false;
    const version = ++state.searchVersion;
    byId('search-button').disabled = true;
    byId('search-form').setAttribute('aria-busy', 'true');
    showError('search-error', '');
    try {
        const params = new URLSearchParams({ dataArrivo: byId('arrival').value, dataPartenza: byId('departure').value, numeroOspiti: byId('guests').value });
        const available = await api(`/camere/disponibili?${params}`);
        if (version !== state.searchVersion) return false;
        state.availableIds = new Set(available.map((room) => room.id));
        for (const id of state.selected.keys()) if (!state.availableIds.has(id)) state.selected.delete(id);
        renderCatalog();
        renderSummary();
        return true;
    } catch (error) {
        if (version === state.searchVersion) showError('search-error', error.message);
        return false;
    } finally {
        byId('search-button').disabled = false;
        byId('search-form').removeAttribute('aria-busy');
    }
}

async function toggleRoom(id) {
    if (state.selected.has(id)) {
        state.selected.delete(id);
    } else {
        if (state.availableIds === null && !await searchRooms()) return;
        if (!state.availableIds?.has(id)) {
            showError('search-error', 'Questa camera non e disponibile per il soggiorno selezionato.');
            return;
        }
        const room = state.apartments.flatMap((apartment) => apartment.camere).find((camera) => camera.id === id);
        state.selected.set(id, { camera: room, numeroOspiti: Number(byId('guests').value) });
    }
    renderCatalog();
    renderSummary();
}

function setTab(view, focus = false) {
    for (const name of ['catalog', 'bookings', 'management']) {
        const active = name === view;
        const tab = byId(`${name}-tab`);
        tab.classList.toggle('active', active);
        tab.setAttribute('aria-selected', String(active));
        tab.tabIndex = active ? 0 : -1;
        byId(`${name}-panel`).hidden = !active;
        if (active && focus) tab.focus();
    }
    if (view === 'management') loadManagement();
}

function renderInventory() {
    byId('inventory-list').innerHTML = state.apartments.length ? state.apartments.map((apartment) => `<section class="inventory-property">
        <div class="inventory-property-heading"><div><h3>${escapeHtml(apartment.nome)}</h3><p>${escapeHtml(apartment.indirizzo)} / ${apartment.camere.length} ${apartment.camere.length === 1 ? 'camera' : 'camere'}</p></div>
            <div class="inventory-actions"><button class="button secondary" data-edit-apartment="${apartment.id}">${icon('pencil')} Modifica</button><button class="button primary" data-add-room="${apartment.id}">${icon('plus')} Aggiungi camera</button></div></div>
        ${apartment.camere.length ? `<div class="inventory-scroll"><table class="inventory-table"><thead><tr><th>CAMERA</th><th>TIPO</th><th>OSPITI</th><th>PREZZO / NOTTE</th><th>STATO</th><th>AZIONI</th></tr></thead><tbody>${apartment.camere.map((room) => `<tr><td><strong>${escapeHtml(room.numero)}</strong></td><td>${escapeHtml(typeLabels[room.tipo])}</td><td>${room.capienzaMassima}</td><td>${currency.format(room.prezzoPerNotte)}</td><td><span class="badge ${room.attiva ? 'green' : 'rose'}">${room.attiva ? 'In servizio' : 'Fuori servizio'}</span></td><td><div class="inventory-row-actions"><button class="button secondary" data-edit-room="${room.id}">${icon('pencil')} Modifica</button><button class="button secondary" data-toggle-room="${room.id}" aria-label="${room.attiva ? 'Metti fuori servizio' : 'Rimetti in servizio'} camera ${escapeHtml(room.numero)}">${icon(room.attiva ? 'power' : 'power')} ${room.attiva ? 'Disattiva' : 'Attiva'}</button></div></td></tr>`).join('')}</tbody></table></div>` : '<p class="muted">Nessuna camera associata.</p>'}
        </section>`).join('') : emptyState('Nessun appartamento', 'Aggiungi il primo appartamento per iniziare.', 'house-plus');
    refreshIcons();
}

function renderGuests() {
    const query = byId('guest-search').value.trim().toLocaleLowerCase('it');
    const guests = state.guests.filter((guest) => `${guest.nome} ${guest.cognome} ${guest.email} ${guest.telefono}`.toLocaleLowerCase('it').includes(query));
    byId('guests-count').textContent = state.guests.length;
    byId('guests-list').innerHTML = guests.length ? `<div class="table-scroll" tabindex="0" role="region" aria-label="Elenco ospiti"><table class="guest-table"><thead><tr><th>OSPITE</th><th>EMAIL</th><th>TELEFONO</th><th>AZIONI</th></tr></thead><tbody>${guests.map((guest) => `<tr><td><strong>${escapeHtml(guest.cognome)} ${escapeHtml(guest.nome)}</strong><small>#${guest.id}</small></td><td>${escapeHtml(guest.email)}</td><td>${escapeHtml(guest.telefono)}</td><td><button class="button secondary" data-edit-guest="${guest.id}">${icon('pencil')} Modifica</button></td></tr>`).join('')}</tbody></table></div>`
        : emptyState(query ? 'Nessun ospite trovato' : 'Nessun ospite registrato', query ? 'Prova con un altro nome, email o telefono.' : 'Gli ospiti verranno aggiunti alla prima prenotazione.', 'users');
    refreshIcons();
}

async function loadManagement() {
    byId('refresh-management').disabled = true;
    showError('management-error', '');
    try {
        const [apartments, guests] = await Promise.all([api('/appartamenti'), api('/gestione/ospiti')]);
        state.apartments = apartments;
        state.guests = guests;
        renderInventory();
        renderGuests();
        renderCatalog();
    } catch (error) {
        showError('management-error', error.message);
    } finally {
        byId('refresh-management').disabled = false;
    }
}

function apartmentPayload(form) {
    const fields = new FormData(form);
    return { nome: String(fields.get('nome')).trim(), indirizzo: String(fields.get('indirizzo')).trim(), descrizione: String(fields.get('descrizione') || '').trim() };
}

function openApartmentForm(apartment = null) {
    state.editingApartmentId = apartment?.id ?? null;
    byId('apartment-form').reset();
    byId('apartment-dialog-title').textContent = apartment ? 'Modifica appartamento' : 'Nuovo appartamento';
    byId('apartment-name').value = apartment?.nome ?? '';
    byId('apartment-address').value = apartment?.indirizzo ?? '';
    byId('apartment-description').value = apartment?.descrizione ?? '';
    showError('apartment-error', '');
    byId('apartment-dialog').showModal();
}

function openRoomForm(room = null, apartmentId = null) {
    state.editingRoomId = room?.id ?? null;
    byId('room-form').reset();
    byId('room-dialog-title').textContent = room ? `Modifica camera ${room.numero}` : 'Nuova camera';
    byId('room-apartment').innerHTML = state.apartments.map((apartment) => `<option value="${apartment.id}">${escapeHtml(apartment.nome)}</option>`).join('');
    byId('room-apartment').disabled = Boolean(room);
    byId('room-apartment').value = room?.appartamentoId ?? apartmentId ?? state.apartments[0]?.id ?? '';
    byId('room-number').value = room?.numero ?? '';
    byId('room-type').value = room?.tipo ?? 'SINGOLA';
    byId('room-capacity').value = room?.capienzaMassima ?? 1;
    byId('room-price').value = room?.prezzoPerNotte ?? '55.00';
    byId('room-active').checked = room?.attiva ?? true;
    showError('room-error', '');
    byId('room-dialog').showModal();
}

function openGuestForm(guest) {
    state.editingGuestId = guest?.id ?? null;
    byId('guest-form').reset();
    byId('guest-dialog-title').textContent = guest ? 'Modifica ospite' : 'Nuovo ospite';
    byId('edit-guest-name').value = guest?.nome ?? '';
    byId('edit-guest-surname').value = guest?.cognome ?? '';
    byId('edit-guest-email').value = guest?.email ?? '';
    byId('edit-guest-phone').value = guest?.telefono ?? '';
    showError('guest-error', '');
    byId('guest-dialog').showModal();
}

async function saveApartment(event) {
    event.preventDefault();
    const id = state.editingApartmentId;
    const method = id ? 'PUT' : 'POST';
    const path = id ? `/gestione/appartamenti/${id}` : '/gestione/appartamenti';
    try {
        await api(path, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(apartmentPayload(event.currentTarget)) });
        byId('apartment-dialog').close();
        await Promise.all([loadCatalog(), loadManagement()]);
        toast(id ? 'Appartamento aggiornato.' : 'Appartamento creato.');
    } catch (error) {
        showError('apartment-error', error.message);
    }
}

async function saveRoom(event) {
    event.preventDefault();
    const id = state.editingRoomId;
    const request = { numero: byId('room-number').value.trim(), tipo: byId('room-type').value,
        capienzaMassima: Number(byId('room-capacity').value), prezzoPerNotte: Number(byId('room-price').value),
        attiva: byId('room-active').checked };
    const path = id ? `/gestione/camere/${id}` : `/gestione/appartamenti/${byId('room-apartment').value}/camere`;
    try {
        await api(path, { method: id ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(request) });
        byId('room-dialog').close();
        state.availableIds = null;
        state.selected.clear();
        await Promise.all([loadCatalog(), loadManagement()]);
        renderSummary();
        toast(id ? 'Camera aggiornata.' : 'Camera aggiunta.');
    } catch (error) {
        showError('room-error', error.message);
    }
}

async function saveGuest(event) {
    event.preventDefault();
    const fields = new FormData(event.currentTarget);
    const request = Object.fromEntries(['nome', 'cognome', 'email', 'telefono']
        .map((name) => [name, String(fields.get(name)).trim()]));
    try {
        const creating = state.editingGuestId === null;
        const guest = await api(creating ? '/gestione/ospiti' : `/gestione/ospiti/${state.editingGuestId}`, {
            method: creating ? 'POST' : 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(request)
        });
        state.guests = creating ? [...state.guests, guest] : state.guests.map((existing) => existing.id === guest.id ? guest : existing);
        byId('guest-dialog').close();
        renderGuests();
        toast(creating ? 'Contatto ospite creato.' : 'Contatto ospite aggiornato.');
    } catch (error) {
        showError('guest-error', error.message);
    }
}

function renderBookings() {
    byId('bookings-count').textContent = state.bookings.length;
    byId('booking-list-label').textContent = `${state.bookings.length} ${state.bookings.length === 1 ? 'soggiorno' : 'soggiorni'} / ${state.bookings.filter((booking) => booking.stato === 'CONFERMATA').length} confermati`;
    if (!state.bookings.length) {
        byId('booking-list').innerHTML = emptyState('Nessuna prenotazione', 'I soggiorni prenotati compariranno qui.', 'calendar-days');
    } else {
        byId('booking-list').innerHTML = `<div class="table-scroll" tabindex="0" role="region" aria-label="Elenco prenotazioni"><table><thead><tr><th scope="col">PRENOTAZIONE</th><th scope="col">OSPITE</th><th scope="col">SOGGIORNO</th><th scope="col">CAMERE</th><th scope="col">TOTALE</th><th scope="col">STATO</th><th scope="col">AZIONI</th></tr></thead><tbody>${[...state.bookings].reverse().map((booking) => `<tr>
            <td><strong class="booking-reference">#${booking.id}</strong><small>${formatDate(booking.dataCreazione.slice(0, 10))}</small></td>
            <td><strong>${escapeHtml(booking.ospite.nome)} ${escapeHtml(booking.ospite.cognome)}</strong><small>${escapeHtml(booking.ospite.email)}</small></td>
            <td>${formatDate(booking.dataArrivo)}<small>al ${formatDate(booking.dataPartenza)}</small></td>
            <td>${booking.camere.map((detail) => `<div>${escapeHtml(detail.camera.appartamentoNome)} / ${escapeHtml(detail.camera.numero)}<small>${detail.numeroOspiti} ${detail.numeroOspiti === 1 ? 'ospite' : 'ospiti'}</small></div>`).join('')}</td>
            <td><strong>${currency.format(booking.totale)}</strong></td><td><span class="badge ${booking.stato === 'CONFERMATA' ? 'green' : booking.stato === 'ANNULLATA' ? 'rose' : 'neutral'}">${escapeHtml(statusLabels[booking.stato] || booking.stato)}</span></td>
            <td>${['CONFERMATA', 'IN_ATTESA'].includes(booking.stato) ? `<button class="button secondary" data-cancel-id="${booking.id}" aria-label="Annulla prenotazione ${booking.id}">${icon('calendar-x')} Annulla</button>` : '<span class="muted">-</span>'}</td></tr>`).join('')}</tbody></table></div>`;
    }
    refreshIcons();
}

async function loadBookings() {
    byId('refresh-bookings').disabled = true;
    showError('bookings-error', '');
    try {
        state.bookings = await api('/prenotazioni');
        renderBookings();
    } catch (error) {
        showError('bookings-error', error.message);
    } finally {
        byId('refresh-bookings').disabled = false;
    }
}

async function loadCatalog() {
    byId('global-error').hidden = true;
    byId('retry-load').disabled = true;
    try {
        state.apartments = await api('/appartamenti');
        const selectedFilter = byId('apartment-filter').value;
        byId('apartment-filter').innerHTML = '<option value="">Tutti gli appartamenti</option>' + state.apartments.map((apartment) => `<option value="${apartment.id}">${escapeHtml(apartment.nome)}</option>`).join('');
        byId('apartment-filter').value = selectedFilter;
        byId('catalog-count').textContent = `${state.apartments.length} appartamenti / ${state.apartments.reduce((total, apartment) => total + apartment.camere.length, 0)} camere`;
        renderCatalog();
    } catch (error) {
        byId('global-error-text').textContent = error.message;
        byId('global-error').hidden = false;
        byId('catalog-count').textContent = 'Catalogo non disponibile';
        byId('properties').innerHTML = emptyState('Appartamenti non disponibili', 'Impossibile caricare il catalogo.', 'wifi-off');
        refreshIcons();
    } finally {
        byId('retry-load').disabled = false;
    }
}

function openCheckout() {
    if (!state.selected.size || !validDates()) return;
    showError('booking-error', '');
    byId('checkout-summary').innerHTML = `<p>${formatDate(byId('arrival').value)} - ${formatDate(byId('departure').value)} / ${nights()} notti</p>`
        + [...state.selected.values()].map(({ camera, numeroOspiti }) => `<div class="checkout-room"><span>${escapeHtml(camera.appartamentoNome)} / Camera ${escapeHtml(camera.numero)} / ${numeroOspiti} ${numeroOspiti === 1 ? 'ospite' : 'ospiti'}</span><strong>${currency.format(camera.prezzoPerNotte * nights())}</strong></div>`).join('')
        + `<div class="checkout-total"><span>Totale stimato</span><strong>${byId('estimated-total').textContent}</strong></div>`;
    byId('booking-dialog').showModal();
}

async function submitBooking(event) {
    event.preventDefault();
    if (state.submitting || !state.selected.size) return;
    const fields = new FormData(event.currentTarget);
    const request = { ospite: Object.fromEntries(['nome', 'cognome', 'email', 'telefono'].map((name) => [name, String(fields.get(name)).trim()])),
        dataArrivo: byId('arrival').value, dataPartenza: byId('departure').value,
        camere: [...state.selected.values()].map(({ camera, numeroOspiti }) => ({ cameraId: camera.id, numeroOspiti })) };
    state.submitting = true;
    byId('submit-booking').disabled = true;
    byId('booking-form').setAttribute('aria-busy', 'true');
    showError('booking-error', '');
    try {
        const booking = await api('/prenotazioni', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(request) });
        state.bookings.push(booking);
        state.selected.clear();
        byId('booking-dialog').close();
        byId('booking-form').reset();
        renderSummary();
        renderBookings();
        setTab('bookings', true);
        toast(`Prenotazione #${booking.id} confermata. Totale: ${currency.format(booking.totale)}.`);
        await searchRooms();
    } catch (error) {
        showError('booking-error', error.message);
    } finally {
        state.submitting = false;
        byId('submit-booking').disabled = false;
        byId('booking-form').removeAttribute('aria-busy');
    }
}

async function cancelBooking() {
    byId('confirm-cancel').disabled = true;
    showError('cancel-error', '');
    try {
        const booking = await api(`/prenotazioni/${state.cancelId}/annulla`, { method: 'POST' });
        state.bookings = state.bookings.map((existing) => existing.id === booking.id ? booking : existing);
        byId('cancel-dialog').close();
        renderBookings();
        toast(`Prenotazione #${booking.id} annullata.`);
        if (state.availableIds !== null) await searchRooms();
    } catch (error) {
        showError('cancel-error', error.message);
    } finally {
        byId('confirm-cancel').disabled = false;
    }
}

byId('arrival').min = localDate();
byId('arrival').value = localDate(7);
byId('departure').min = localDate(8);
byId('departure').value = localDate(9);
byId('arrival').addEventListener('change', () => {
    if (!byId('arrival').value) { invalidateSearch(); return; }
    const nextDay = new Date(`${byId('arrival').value}T12:00:00`);
    nextDay.setDate(nextDay.getDate() + 1);
    const minimum = `${nextDay.getFullYear()}-${String(nextDay.getMonth() + 1).padStart(2, '0')}-${String(nextDay.getDate()).padStart(2, '0')}`;
    byId('departure').min = minimum;
    if (byId('departure').value <= byId('arrival').value) byId('departure').value = minimum;
    byId('departure').setCustomValidity('');
    invalidateSearch();
});
byId('departure').addEventListener('change', () => { byId('departure').setCustomValidity(''); invalidateSearch(); });
byId('guests').addEventListener('change', invalidateSearch);
for (const id of ['apartment-filter', 'type-filter']) byId(id).addEventListener('change', renderCatalog);
byId('search-form').addEventListener('submit', (event) => { event.preventDefault(); searchRooms(); });
byId('reset-filters').addEventListener('click', () => { byId('apartment-filter').value = ''; byId('type-filter').value = ''; byId('guests').value = '1'; invalidateSearch(); });
byId('properties').addEventListener('click', (event) => { const button = event.target.closest('[data-room-id]'); if (button) toggleRoom(Number(button.dataset.roomId)); });
byId('selected-rooms').addEventListener('click', (event) => { const button = event.target.closest('[data-remove-id]'); if (button) toggleRoom(Number(button.dataset.removeId)); });
byId('selected-rooms').addEventListener('change', (event) => { const select = event.target.closest('[data-guests-id]'); if (select) { state.selected.get(Number(select.dataset.guestsId)).numeroOspiti = Number(select.value); renderSummary(); } });
for (const view of ['catalog', 'bookings', 'management']) {
    byId(`${view}-tab`).addEventListener('click', () => setTab(view));
    byId(`${view}-tab`).addEventListener('keydown', (event) => {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
        event.preventDefault();
        const views = ['catalog', 'bookings', 'management'];
        const current = views.indexOf(view);
        const next = event.key === 'Home' ? 0 : event.key === 'End' ? views.length - 1
            : (current + (event.key === 'ArrowRight' ? 1 : views.length - 1)) % views.length;
        setTab(views[next], true);
    });
}
for (const [name, panel] of [['inventory', 'inventory'], ['guests', 'guests']]) {
    byId(`${name}-tab`).addEventListener('click', () => {
        for (const tabName of ['inventory', 'guests']) {
            const active = tabName === name;
            byId(`${tabName}-tab`).classList.toggle('active', active);
            byId(`${tabName}-tab`).setAttribute('aria-selected', String(active));
            byId(`${tabName}-tab`).tabIndex = active ? 0 : -1;
            byId(`${tabName}-panel`).hidden = !active;
        }
    });
}
byId('new-apartment').addEventListener('click', () => openApartmentForm());
byId('new-guest').addEventListener('click', () => openGuestForm());
byId('inventory-list').addEventListener('click', (event) => {
    const editApartment = event.target.closest('[data-edit-apartment]');
    if (editApartment) {
        const apartment = state.apartments.find((item) => item.id === Number(editApartment.dataset.editApartment));
        if (apartment) openApartmentForm(apartment);
        return;
    }
    const addRoom = event.target.closest('[data-add-room]');
    if (addRoom) { openRoomForm(null, Number(addRoom.dataset.addRoom)); return; }
    const editRoom = event.target.closest('[data-edit-room]');
    if (editRoom) {
        const room = state.apartments.flatMap((apartment) => apartment.camere).find((item) => item.id === Number(editRoom.dataset.editRoom));
        if (room) openRoomForm(room);
        return;
    }
    const toggle = event.target.closest('[data-toggle-room]');
    if (toggle) {
        const room = state.apartments.flatMap((apartment) => apartment.camere).find((item) => item.id === Number(toggle.dataset.toggleRoom));
        if (room) {
            byId('room-apartment').value = String(room.appartamentoId);
            state.editingRoomId = room.id;
            byId('room-number').value = room.numero;
            byId('room-type').value = room.tipo;
            byId('room-capacity').value = room.capienzaMassima;
            byId('room-price').value = room.prezzoPerNotte;
            const request = { numero: room.numero, tipo: room.tipo, capienzaMassima: room.capienzaMassima,
                prezzoPerNotte: room.prezzoPerNotte, attiva: !room.attiva };
            api(`/gestione/camere/${room.id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(request) })
                .then(async () => {
                    state.availableIds = null;
                    state.selected.clear();
                    await Promise.all([loadCatalog(), loadManagement()]);
                    renderSummary();
                    toast(request.attiva ? 'Camera rimessa in servizio.' : 'Camera fuori servizio.');
                }).catch((error) => showError('management-error', error.message));
        }
    }
});
byId('guests-list').addEventListener('click', (event) => {
    const button = event.target.closest('[data-edit-guest]');
    const guest = state.guests.find((item) => item.id === Number(button?.dataset.editGuest));
    if (guest) openGuestForm(guest);
});
byId('guest-search').addEventListener('input', renderGuests);
byId('refresh-management').addEventListener('click', () => Promise.all([loadManagement(), loadCatalog()]));
byId('apartment-form').addEventListener('submit', saveApartment);
byId('room-form').addEventListener('submit', saveRoom);
byId('guest-form').addEventListener('submit', saveGuest);
document.querySelectorAll('[data-close-dialog]').forEach((button) => button.addEventListener('click', () => byId(button.dataset.closeDialog).close()));
byId('open-booking').addEventListener('click', openCheckout);
for (const id of ['close-booking', 'back-to-catalog']) byId(id).addEventListener('click', () => { if (!state.submitting) byId('booking-dialog').close(); });
byId('booking-dialog').addEventListener('cancel', (event) => { if (state.submitting) event.preventDefault(); });
byId('booking-form').addEventListener('submit', submitBooking);
byId('booking-list').addEventListener('click', (event) => {
    const button = event.target.closest('[data-cancel-id]');
    if (!button) return;
    state.cancelId = Number(button.dataset.cancelId);
    byId('cancel-description').textContent = `La prenotazione #${state.cancelId} sara annullata e le camere torneranno disponibili.`;
    showError('cancel-error', '');
    byId('cancel-dialog').showModal();
});
for (const id of ['close-cancel', 'keep-booking']) byId(id).addEventListener('click', () => { if (!byId('confirm-cancel').disabled) byId('cancel-dialog').close(); });
byId('cancel-dialog').addEventListener('cancel', (event) => { if (byId('confirm-cancel').disabled) event.preventDefault(); });
byId('confirm-cancel').addEventListener('click', cancelBooking);
byId('refresh-bookings').addEventListener('click', loadBookings);
byId('retry-load').addEventListener('click', loadCatalog);
byId('dismiss-toast').addEventListener('click', () => { byId('toast').hidden = true; });
renderSummary();
refreshIcons();
loadCatalog();
loadBookings();