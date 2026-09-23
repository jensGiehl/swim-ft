# Schwimmkurs-Monitor Frankenthal

Eine Spring-Boot-CLI-Anwendung ohne Webserver. Sie liest die verfügbaren Termine des konfigurierten Schwimmkurses, vergleicht sie mit dem letzten erfolgreichen Lauf und sendet Änderungen über einen Telegram-Bot.

Beim allerersten Lauf wird nur der Ausgangszustand gespeichert. Es wird dabei keine Änderungsnachricht versendet. Jeden Sonntag wird beim ersten erfolgreichen Lauf ab 18:00 Uhr (Zeitzone `Europe/Berlin`) einmalig ein Health-Check gesendet. Jede Telegram-Nachricht enthält den Link zur Kursseite.

## Voraussetzungen

- Java 25
- Maven 3.9 oder neuer
- Telegram-Bot-Token und Chat-ID

Den Bot legt man über [@BotFather](https://t.me/BotFather) an. Nachdem dem Bot eine Nachricht gesendet wurde, lässt sich die Chat-ID beispielsweise über `https://api.telegram.org/bot<TOKEN>/getUpdates` ermitteln.

## Konfiguration

Alle Einstellungen sind Spring-Boot-Properties und können über eine externe Properties-Datei, Kommandozeilenargumente oder Umgebungsvariablen gesetzt werden.

| Property | Umgebungsvariable | Standardwert / Bedeutung |
|---|---|---|
| `schwimmkurs.source.url` | `SCHWIMMKURS_SOURCE_URL` | Zu überwachende Kursseite |
| `schwimmkurs.source.table-selector` | `SCHWIMMKURS_SOURCE_TABLESELECTOR` | CSS-Selektor der Kurstabelle, Standard `#block_list_154` |
| `schwimmkurs.source.user-agent` | `SCHWIMMKURS_SOURCE_USERAGENT` | Firefox-User-Agent |
| `schwimmkurs.source.connect-timeout` | `SCHWIMMKURS_SOURCE_CONNECTTIMEOUT` | Verbindungs-Timeout, Standard `10s` |
| `schwimmkurs.source.read-timeout` | `SCHWIMMKURS_SOURCE_READTIMEOUT` | Lese-Timeout, Standard `30s` |
| `schwimmkurs.state.file` | `SCHWIMMKURS_STATE_FILE` | Persistenter Snapshot, Standard `./data/schwimmkurs-state.bin` |
| `schwimmkurs.state.lock-file` | `SCHWIMMKURS_STATE_LOCKFILE` | Sperrdatei gegen parallele Läufe |
| `schwimmkurs.telegram.api-base-url` | `SCHWIMMKURS_TELEGRAM_APIBASEURL` | Telegram-API, Standard `https://api.telegram.org` |
| `schwimmkurs.telegram.bot-token` | `TELEGRAM_BOT_TOKEN` oder `SCHWIMMKURS_TELEGRAM_BOTTOKEN` | Erforderlicher Bot-Token |
| `schwimmkurs.telegram.chat-id` | `TELEGRAM_CHAT_ID` oder `SCHWIMMKURS_TELEGRAM_CHATID` | Erforderliche Chat-ID |
| `schwimmkurs.telegram.disable-notification` | `SCHWIMMKURS_TELEGRAM_DISABLENOTIFICATION` | Nachricht lautlos senden, Standard `false` |
| `schwimmkurs.telegram.maximum-message-length` | `SCHWIMMKURS_TELEGRAM_MAXIMUMMESSAGELENGTH` | Maximale Länge eines Nachrichtenteils, Standard `3900` |
| `schwimmkurs.health-check.enabled` | `SCHWIMMKURS_HEALTHCHECK_ENABLED` | Health-Check aktiv, Standard `true` |
| `schwimmkurs.health-check.day-of-week` | `SCHWIMMKURS_HEALTHCHECK_DAYOFWEEK` | Wochentag, Standard `SUNDAY` |
| `schwimmkurs.health-check.earliest-time` | `SCHWIMMKURS_HEALTHCHECK_EARLIESTTIME` | Früheste Uhrzeit, Standard `18:00` |
| `schwimmkurs.health-check.zone-id` | `SCHWIMMKURS_HEALTHCHECK_ZONEID` | Zeitzone, Standard `Europe/Berlin` |
| `schwimmkurs.health-check.message` | `SCHWIMMKURS_HEALTHCHECK_MESSAGE` | Text der Health-Check-Nachricht |

Die kompakten `SCHWIMMKURS_…`-Namen entsprechen den Spring-Boot-Regeln für Environment Binding: Punkte werden zu Unterstrichen, Bindestriche entfallen und Buchstaben werden großgeschrieben. Für Token und Chat-ID existieren zusätzlich die leichter lesbaren Aliase `TELEGRAM_BOT_TOKEN` und `TELEGRAM_CHAT_ID`.

## Lokal bauen und ausführen

```bash
mvn clean verify

java -jar target/schwimmkurs-ft-0.0.1-SNAPSHOT.jar \
  --schwimmkurs.telegram.bot-token="BOT_TOKEN" \
  --schwimmkurs.telegram.chat-id="CHAT_ID"
```

Der Prozess prüft genau einmal und beendet sich anschließend. Ein Fehler führt zu einem von null verschiedenen Exit-Code, sodass Cron ihn erkennen kann.

## Docker Image

Bei jedem Push auf `main` oder `master` baut die GitHub Action ein Multi-Arch-Image und veröffentlicht es unter `ghcr.io/OWNER/REPOSITORY:latest` in der GitHub Container Registry.

Ein beispielhafter manueller Start gemäß dem üblichen Container-Schema:

```bash
docker rm -f schwimmkurs-ft 2>/dev/null

docker run -d \
  --name schwimmkurs-ft \
  --pull=always \
  -p 8089:8080 \
  -v /opt/schwimmkurs-ft:/data \
  -e TELEGRAM_BOT_TOKEN="BOT_TOKEN" \
  -e TELEGRAM_CHAT_ID="CHAT_ID" \
  ghcr.io/OWNER/REPOSITORY:latest
```

`8089` ist dabei der Port auf dem Host. Die Anwendung besitzt absichtlich keinen Webserver und öffnet daher keinen Port; die Portweiterleitung ist technisch nicht erforderlich und kann entfernt werden. Das Verzeichnis `/opt/schwimmkurs-ft` wird nach `/data` eingebunden, damit Snapshot und Datum des letzten Health-Checks Container-Neustarts überleben.

Weitere Properties lassen sich auf die gleiche Weise übergeben, zum Beispiel:

```bash
-e SCHWIMMKURS_HEALTHCHECK_EARLIESTTIME="18:30"
-e SCHWIMMKURS_HEALTHCHECK_ZONEID="Europe/Berlin"
-e SCHWIMMKURS_SOURCE_READTIMEOUT="45s"
```

## Linux-Cron

Der folgende Eintrag startet alle fünf Minuten einen kurzlebigen Container. `flock` verhindert überlappende Containerläufe; die Anwendung verwendet zusätzlich eine Dateisperre im Datenverzeichnis.

```cron
*/5 * * * * flock -n /tmp/schwimmkurs-ft-cron.lock docker run --rm --pull=always --name schwimmkurs-ft -v /opt/schwimmkurs-ft:/data -e TELEGRAM_BOT_TOKEN='BOT_TOKEN' -e TELEGRAM_CHAT_ID='CHAT_ID' ghcr.io/OWNER/REPOSITORY:latest >> /var/log/schwimmkurs-ft.log 2>&1
```

Das persistente Verzeichnis muss vorher existieren und für den Container-Benutzer mit UID `10001` schreibbar sein:

```bash
sudo install -d -o 10001 -g 10001 /opt/schwimmkurs-ft
```

## Vergleichsverhalten

Verglichen werden ausschließlich die normalisierten Datenzeilen der Kurstabelle. Änderungen am Seitenlayout, an Suchfeldern oder Skripten erzeugen dadurch keine Meldung. Neue Zeilen erscheinen im Telegram-Diff mit `+`, entfernte oder inhaltlich geänderte Zeilen mit `-`. Der neue Snapshot wird nach einer erfolgreichen Änderungsnachricht gespeichert; schlägt Telegram fehl, wird die Änderung beim nächsten Cron-Lauf erneut versucht.
