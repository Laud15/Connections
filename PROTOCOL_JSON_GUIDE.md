# 🔹 Protocollo JSON - Connections Game

## 📋 Overview

Il sistema Connections usa un protocollo di comunicazione **Request/Response** basato su JSON.

- **Client → Server**: Invia **Request** (campo `"operation"`)
- **Server → Client (UDP)**: Invia notifiche asincrone
- **Server → Client (TCP)**: Invia **Response** (campo `"status"`)

---

## ✉️ FORMATO RICHIESTA (Request - Client → Server)

Tutte le richieste hanno il seguente formato:
```json
{
  "operation": "OPERATION_NAME",
  "field1": "value1",
  "field2": "value2"
}
```

### Operazioni Disponibili:

#### 1. **register**
```json
{
  "operation": "register",
  "name": "username",
  "psw": "password"
}
```

#### 2. **updateCredentials**
```json
{
  "operation": "updateCredentials",
  "oldName": "username",
  "oldPsw": "password",
  "newName": "newUsername",      // opzionale
  "newPsw": "newPassword"        // opzionale
}
```

#### 3. **login**
```json
{
  "operation": "login",
  "username": "username",
  "psw": "password"
}
```

#### 4. **logout**
```json
{
  "operation": "logout"
}
```

#### 5. **submitProposal**
```json
{
  "operation": "submitProposal",
  "words": ["WORD1", "WORD2", "WORD3", "WORD4"]
}
```

#### 6. **requestGameInfo**
```json
{
  "operation": "requestGameInfo",
  "gameId": -1  // -1 = gioco corrente
}
```

#### 7. **requestGameStats**
```json
{
  "operation": "requestGameStats",
  "gameId": -1  // -1 = gioco corrente
}
```

#### 8. **requestLeaderboard**
```json
{
  "operation": "requestLeaderboard",
  "playerName": "username",     // opzionale: ranking specifico giocatore
  "topPlayers": 10              // opzionale: top-K (0 = tutti)
}
```

#### 9. **requestPlayerStats**
```json
{
  "operation": "requestPlayerStats"
}
```

---

## ✉️ FORMATO RISPOSTA (Response - Server → Client)

Tutte le risposte hanno il seguente formato:

### Caso di Successo (status="ok")
```json
{
  "status": "ok",
  "operation": "OPERATION_RESPONSE_NAME",
  "data": { ... specifico della risposta ... }
}
```

### Caso di Errore (status="error")
```json
{
  "status": "error",
  "errorCode": 400,
  "errorMessage": "Descrizione errore"
}
```

### Codici di Errore Comuni:
- **400** - Bad Request (dati malformati)
- **401** - Unauthorized (non autenticato, credenziali sbagliate)
- **403** - Forbidden (non permesso, es. già loggato)
- **404** - Not Found (risorsa non trovata)
- **409** - Conflict (es. username già registrato)
- **500** - Internal Server Error

---

## 📨 ESEMPI DI FLUSSI

### Flow: Registrazione + Login + Gioco

```
Client ──────────────────────► Server

┌─ Request: register
│  {
│    "operation": "register",
│    "name": "alice",
│    "psw": "secret123"
│  }
│
└──► Response: ok
     {
       "status": "ok",
       "operation": "registerResponse",
       "message": "Registration successful"
     }


┌─ Request: login
│  {
│    "operation": "login",
│    "username": "alice",
│    "psw": "secret123"
│  }
│
└──► Response: ok
     {
       "status": "ok",
       "operation": "loginResponse",
       "gameId": 42,
       "remainingSeconds": 300,
       "words": ["SNOW", "RAIN", "HAIL", ...],
       "correctGroups": [],
       "mistakes": 0,
       "currentScore": 0
     }


┌─ Request: submitProposal
│  {
│    "operation": "submitProposal",
│    "words": ["SNOW", "RAIN", "HAIL", "SLEET"]
│  }
│
└──► Response (se corretto): ok
     {
       "status": "ok",
       "operation": "submitProposalResponse",
       "theme": "WET WEATHER",
       "words": ["SNOW", "RAIN", "HAIL", "SLEET"],
       "currentScore": 6,
       "gameOver": false,
       "won": null
     }


┌─ Request: requestGameStats
│  {
│    "operation": "requestGameStats",
│    "gameId": -1
│  }
│
└──► Response: ok
     {
       "status": "ok",
       "operation": "responseGameStats",
       "gameId": 42,
       "gameStatus": "inProgress",
       "remainingSeconds": 250,
       "playersPlaying": 5,
       "playersFinished": 2,
       "playersWon": 1
     }
```

---

## 🔹 NOTIFICHE ASINCRONE (UDP)

Quando il gioco finisce, il server invia una notifica UDP a tutti i client:

```json
{
  "type": "gameOver",
  "gameId": 42,
  "remainingSeconds": 0
}
```

Il client riceve questa notifica via UDP (porta registrata al login) e sa che deve richiedere le statistiche finali del gioco.

---

## 🔒 INVARIANTI DEL PROTOCOLLO

1. **Ogni request DEVE avere il campo "operation"**
   ❌ Sbagliato:
   ```json
   { "name": "alice", "psw": "secret" }
   ```
   ✅ Corretto:
   ```json
   { "operation": "register", "name": "alice", "psw": "secret" }
   ```

2. **Ogni response DEVE avere il campo "status"** (`"ok"` o `"error"`)
   ❌ Sbagliato:
   ```json
   { "message": "Registration successful" }
   ```
   ✅ Corretto:
   ```json
   { "status": "ok", "operation": "registerResponse", "message": "..." }
   ```

3. **Response di errore DEVE avere "errorCode" e "errorMessage"**
   ❌ Sbagliato:
   ```json
   { "status": "error", "message": "User not found" }
   ```
   ✅ Corretto:
   ```json
   { "status": "error", "errorCode": 404, "errorMessage": "User not found" }
   ```

4. **Tutti i messaggi sono terminati da newline `\n`**
   - Questo è il delimitatore dei messaggi nel protocollo NIO del server
   - Ogni messaggio JSON è seguito da un `\n` per indicare la fine

---

## 🐛 ERRORI COMUNI TROVATI E CORRETTI

### Errore #1: RequestDeserializer confondeva Request e Response
**Prima**: Cercava un campo "status" (Response), ma i client mandano "operation" (Request)
**Dopo**: Cerca solo "operation", che è l'unica cosa che i client mandano

### Errore #2: Nomi di classe incoerenti
**Prima**: `RequestLeaderboardRequest` (doppio "Request")
**Dopo**: `LeaderBoardRequest` (nome corretto)

### Errore #3: Type mismatch nella GameStatsResponse
**Prima**: Passava `long` quando il costruttore voleva `int`
**Dopo**: Cast esplicito `(int)` per convertire long → int

---

## 📖 COME TESTARE IL PROTOCOLLO

### Via Postman / Insomnia:
1. Connettiti al server TCP (es. localhost:12345)
2. Invia JSON terminato con newline:
   ```
   {"operation": "register", "name": "test", "psw": "123"}\n
   ```
3. Ricevi risposta JSON:
   ```
   {"status": "ok", "operation": "registerResponse", ...}\n
   ```

### Via telnet:
```bash
telnet localhost 12345
{"operation": "register", "name": "alice", "psw": "secret"}
[Enter]
# Ricevi risposta JSON
```

### Via Java Client (incluso nel progetto):
```bash
java -jar client/target/client-1.0-jar-with-dependencies.jar
```

