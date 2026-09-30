# Project Map
_Generated: 2026-05-12 22:44 | Staleness: timestamps_

## Directory Structure
app/ — Android Compose aplikacija za treninge, vežbe, mašinska podešavanja, telesne metrike i backup.
app/src/main/java/com/gymtracker/app/data/model/ — domen modeli za treninge, vežbe, setove, telo, profile i cikluse.
app/src/main/java/com/gymtracker/app/data/local/ — Room baza, DAO-i, entiteti, migrator i mapper-i.
app/src/main/java/com/gymtracker/app/data/repository/ — repozitorijumi koji povezuju UI/domain sa Room slojem.
app/src/main/java/com/gymtracker/app/ui/screens/ — Compose ekrani za trening, istoriju, statistiku, telo, podešavanja i picker-e.
app/src/main/java/com/gymtracker/app/ui/components/ — kartice, redovi setova, exercise UI i dijalozi.
docs/ — lokalni planovi, uključujući machine settings.

## Key Files
settings.gradle.kts — definiše Gradle root `GymTracker`.
app/build.gradle.kts — Compose, Room/KSP i test konfiguracija.
app/src/main/AndroidManifest.xml — aplikacija, launcher aktivnost i storage/internet permisije.
app/src/main/java/com/gymtracker/app/MainActivity.kt — ulaz u Compose UI.
app/src/main/java/com/gymtracker/app/GymTrackerApplication.kt — Application klasa.
app/src/main/java/com/gymtracker/app/data/local/GymTrackerDatabase.kt — Room database registracija.
app/src/main/java/com/gymtracker/app/data/local/JsonToRoomMigrator.kt — migracija starog JSON stanja u Room.
app/src/main/java/com/gymtracker/app/data/repository/WorkoutRepository.kt — glavni tok za treninge i istoriju.
app/src/main/java/com/gymtracker/app/data/repository/MachineSettingsRepository.kt — persistence za podešavanja sprava.
app/src/main/java/com/gymtracker/app/ui/screens/MainScreen.kt — glavni ekran i navigaciona površina.
docs/plans/2026-02-08-machine-settings.md — postojeći plan za machine settings ponašanje.

## Critical Constraints
- Poštovati parent `AGENTS.md`: lokalno bez Git komandi.
- Room migracije i JSON migrator su load-bearing; promene modela zahtevaju proveru entiteta, DAO-a, mapper-a i backup-a zajedno.
- Podešavanja sprava imaju poseban plan u `docs/plans`; proveriti ga pre menjanja tog toka.
- Google Drive backup i lokalni Room storage moraju ostati usklađeni.

## Hot Files
app/src/main/java/com/gymtracker/app/data/local/GymTrackerDatabase.kt, app/src/main/java/com/gymtracker/app/data/local/JsonToRoomMigrator.kt, app/src/main/java/com/gymtracker/app/data/repository/WorkoutRepository.kt, app/src/main/java/com/gymtracker/app/data/repository/MachineSettingsRepository.kt, app/src/main/java/com/gymtracker/app/ui/screens/MainScreen.kt
