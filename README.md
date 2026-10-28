# Booking B&B

Applicazione didattica Spring Boot 4.1.1, Java 25 e Maven.
Si prenotano singole camere, anche piu camere in un'unica prenotazione.
Non e previsto l'affitto esclusivo di un appartamento intero.

## Esercizio Di Sicurezza

Questa versione contiene due vulnerabilita intenzionali, lasciate attive.
Usarla solo in un laboratorio locale isolato, con dati fittizi, mai in produzione.

- **XSS persistente (CWE-79):** la descrizione salvata tramite le API di gestione
  appartamenti viene inserita come HTML nel catalogo senza escaping. Un handler
  HTML puo eseguire JavaScript nel browser di chi consulta il catalogo.
- **Open redirect (CWE-601):** `GET /hello?redirect=https://example.org/training`
  risponde con HTTP 302 verso la destinazione fornita, senza verificarne l'host.
  Senza parametro, `/hello` mantiene la risposta originale.

## Avvio

Da PowerShell, nella cartella del progetto:

```powershell
.\mvnw.cmd spring-boot:run
```

Se BookingApplication e gia in esecuzione in VS Code, arrestarla e riavviarla
per caricare le nuove classi e dipendenze. Avviare una sola istanza per database.
L'applicazione usa la porta 8081 per evitare il conflitto con la porta 8080.
L'interfaccia grafica e http://localhost:8081/.
Le API JSON rimangono disponibili, ad esempio http://localhost:8081/camere.

La pagina usa HTML, CSS e JavaScript nella cartella `src/main/resources/static`.
Permette di consultare gli appartamenti, filtrare le camere, cercare la disponibilita
per date e ospiti e prenotare piu camere. La scheda Calendario mostra l'occupazione
giornaliera in vista mese, settimana o giorno; la scheda Prenotazioni mostra lo storico
e permette di annullare un soggiorno, previa conferma.
I dati e i controlli applicativi continuano a essere gestiti dalle API Spring Boot.
Le fotografie sono illustrative; immagini Unsplash, font Google Fonts e icone Lucide
richiedono una connessione Internet. Le foto non rappresentano gli appartamenti reali.

H2 salva i dati nella cartella locale `data/`, esclusa da Git.
I dati rimangono dopo il riavvio. `ddl-auto=update` e una configurazione
didattica: in produzione si usano migrazioni versionate.

## Organizzazione

```text
it.eng.booking
  model        Entita JPA ed enum
  repository   Interfacce Spring Data JPA
  service      Regole applicative e transazioni
  dto          Richieste validate e risposte JSON
  controller   API HTTP
  exception    Errori HTTP con ProblemDetail
  config       Dati iniziali
```

Il controller riceve la richiesta e chiama il service, che usa i repository.
I DTO evitano di serializzare direttamente le relazioni JPA.
Per studiare il codice: partire da `TipoCamera` e `Camera`, poi seguire
`BookingController.getCamere`, `CameraService` e `CameraRepository`.
Successivamente leggere `PrenotazioneService.crea`.

## Catalogo Iniziale

| Appartamento | Camera | Tipo | Capienza | EUR/notte | In servizio |
|---|---|---|---|---|---|
| Aurora | 101 | SINGOLA | 1 | 55.00 | Si |
| Aurora | 102 | DOPPIA | 2 | 85.00 | Si |
| Aurora | 103 | SINGOLA | 1 | 55.00 | No |
| Giardino | 201 | DOPPIA | 2 | 90.00 | Si |
| Giardino | 202 | FAMILIARE | 4 | 140.00 | Si |

Questi dati vengono inseriti solo quando non esistono appartamenti.
Gli identificativi vengono generati dal database: consultarli con `GET /camere`.

## API

| Metodo | Percorso | Risultato |
|---|---|---|
| GET | `/hello` | Controllo iniziale |
| POST | `/chat` | Risposta informativa basata su RAG |
| GET | `/appartamenti` | Appartamenti con le relative camere |
| GET | `/camere` | Catalogo, comprese le camere fuori servizio |
| GET | `/camere/disponibili` | Camere libere per periodo e capienza |
| GET | `/prenotazioni` | Elenco delle prenotazioni |
| GET | `/prenotazioni/{id}` | Dettaglio di una prenotazione |
| POST | `/prenotazioni` | Creazione, risposta 201 con header Location |
| POST | `/prenotazioni/{id}/annulla` | Annullamento, risposta 200 |
| POST | `/gestione/appartamenti` | Crea un appartamento |
| PUT | `/gestione/appartamenti/{id}` | Modifica un appartamento |
| POST | `/gestione/appartamenti/{id}/camere` | Aggiunge una camera |
| PUT | `/gestione/camere/{id}` | Modifica camera o stato di servizio |
| GET | `/gestione/ospiti` | Elenco contatti ospite |
| POST | `/gestione/ospiti` | Crea un contatto ospite |
| PUT | `/gestione/ospiti/{id}` | Modifica un contatto ospite |

`/camere` accetta i filtri opzionali `appartamentoId` e `tipo`:

```text
GET /camere?tipo=DOPPIA
GET /camere/disponibili?dataArrivo=2027-06-10&dataPartenza=2027-06-12&numeroOspiti=2
```

La disponibilita richiede entrambe le date (formato `yyyy-MM-dd`).
`numeroOspiti`, per singola camera, vale 1 se omesso; si possono aggiungere
`appartamentoId` e `tipo`. Una ricerca non riserva le camere: la disponibilita
viene ricontrollata quando si crea la prenotazione.

Esempio di body per `POST /prenotazioni`, con `Content-Type: application/json`:

```json
{
  "ospite": {
    "nome": "Mario",
    "cognome": "Rossi",
    "email": "mario@example.com",
    "telefono": "+39 3331234567"
  },
  "dataArrivo": "2027-06-10",
  "dataPartenza": "2027-06-12",
  "camere": [
    { "cameraId": 1, "numeroOspiti": 1 },
    { "cameraId": 2, "numeroOspiti": 2 }
  ]
}
```

Usare gli ID restituiti dal catalogo e date non passate.
Ogni richiesta crea il proprio contatto ospite; non viene effettuata una
deduplicazione automatica sulla email.

## Regole

- Arrivo da oggi in poi; partenza strettamente successiva all'arrivo.
- Prezzo per camera, non per persona; totale = somma dei prezzi concordati per notte moltiplicata per le notti.
- Capienza rispettata e nessuna camera ripetuta nella richiesta.
- Camere fuori servizio e soggiorni sovrapposti non prenotabili.
- Il checkout non occupa una notte: un nuovo check-in nello stesso giorno e consentito.
- Tutte le camere vengono prenotate insieme oppure nessuna: creazione transazionale.
- Le camere vengono bloccate in ordine di ID durante la creazione per gestire richieste concorrenti.
- La tariffa concordata viene salvata, indipendentemente dalle future variazioni di listino.
- Le nuove prenotazioni riutilizzano il contatto ospite quando l'email corrisponde, senza distinzione tra maiuscole e minuscole.
- L'annullamento conserva lo storico e non blocca piu le camere; ripeterlo e consentito.

La gestione non elimina fisicamente appartamenti o camere per non invalidare lo storico;
una camera si puo mettere fuori servizio e riattivare. L'anagrafica ospiti non ha
cancellazione per non compromettere prenotazioni esistenti.

## Accesso Amministrativo

Gli endpoint `/gestione/**` non hanno ancora autenticazione o autorizzazione.
Il progetto e didattico e va usato solo in locale: non pubblicare la porta 8081
su una rete accessibile ad altri. Prima di usarlo online, proteggere inoltre
gli endpoint di prenotazione, che restituiscono nome, email e telefono degli ospiti.

## Chatbot informativo (RAG)

Il pulsante "Chiedi al B&B" apre un chatbot che recupera i passaggi pertinenti
da `src/main/resources/knowledge/booking-info.txt`, dal catalogo aggiornato e
dai conteggi aggregati delle prenotazioni. Quando la domanda contiene due date
nel formato `yyyy-MM-dd`, controlla anche la disponibilita effettiva delle camere.
La ricerca supporta un numero di ospiti per camera; se omesso, usa 1.
Il chatbot informa, ma non crea, modifica o annulla prenotazioni.

Per generare le risposte viene usata l'API OpenAI. Configurare la chiave
nell'ambiente PowerShell prima di avviare l'applicazione:

```powershell
$env:OPENAI_API_KEY = "inserire-la-propria-chiave"
.\mvnw.cmd spring-boot:run
```

La variabile resta limitata alla sessione PowerShell corrente. In alternativa,
`OPENAI_MODEL` (predefinito `gpt-4o-mini`) e `OPENAI_BASE_URL` (predefinito
`https://api.openai.com/v1`) permettono di configurare modello e endpoint.
Senza chiave il resto dell'applicazione continua a funzionare e `/chat` risponde
con HTTP 503.

La domanda, la cronologia recente della chat e i passaggi recuperati vengono
inviati a OpenAI per generare la risposta. Il chatbot non legge né invia nomi,
email, telefoni o dettagli individuali delle prenotazioni; usa solo i relativi
conteggi aggregati e il risultato di disponibilita richiesto. Non inserire dati
personali nelle domande. L'endpoint non ha autenticazione o limitazione d'uso:
come il resto dell'applicazione, va usato solo in locale finche non viene protetto.

Le prenotazioni nascono `CONFERMATA` e possono diventare `ANNULLATA`.
`IN_ATTESA` e `COMPLETATA` sono valori previsti dal modello, ma senza un workflow
dedicato in questa versione. Le prenotazioni in attesa bloccano le camere.

Errori: 400 per richieste non valide, 404 per risorse inesistenti,
409 per camere non disponibili o contese. Le risposte usano `ProblemDetail`;
gli errori di validazione includono il campo `errori`.

## Verifiche

```powershell
.\mvnw.cmd -DskipTests test-compile
.\mvnw.cmd test
```

I test usano un database H2 in memoria, separato dai dati dell'applicazione.
Coprono catalogo, disponibilita, validazione, sovrapposizioni, checkout/check-in,
annullamento, prenotazioni multicamera e conservazione della tariffa concordata.

Questa versione non ha autenticazione, autorizzazione, pagamenti o gestione
di tasse e sconti. Non esporre pubblicamente le API degli ospiti e delle
prenotazioni senza prima aggiungere controlli di accesso.