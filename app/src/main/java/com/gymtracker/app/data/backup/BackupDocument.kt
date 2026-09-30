package com.gymtracker.app.data.backup

import com.google.gson.JsonParser
import com.gymtracker.app.data.model.BodyMetricsState
import com.gymtracker.app.data.model.CustomExercise
import com.gymtracker.app.data.model.MachineSettings
import com.gymtracker.app.data.model.SteroidCycle
import com.gymtracker.app.data.model.Workout
import com.gymtracker.app.util.GsonProvider

/** Verzija formata kombinovanog backup dokumenta. */
const val BACKUP_SCHEMA_VERSION = 1

/**
 * JEDAN backup dokument sa svim podacima aplikacije. Ranije je svaka kategorija
 * išla u poseban Drive fajl, pa je prekinut backup/restore ostavljao "pola podataka".
 *
 * Sekcija koja je `null` znači "nije prisutna u izvoru" i pri vraćanju se NE dira
 * (koristi se samo za stari format: pet odvojenih fajlova / izvoz samo treninga).
 * U kombinovanom formatu (schemaVersion >= 1) sve sekcije su obavezne.
 */
data class BackupDocument(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: Long = 0L,
    val workouts: List<Workout>? = null,
    val bodyMetrics: BodyMetricsState? = null,
    val cycles: List<SteroidCycle>? = null,
    val customExercises: List<CustomExercise>? = null,
    val machineSettings: List<MachineSettings>? = null
) {
    /** Ukupan broj zapisa (profil se ne broji — uvek postoji bar podrazumevani). */
    val recordCount: Int
        get() = (workouts?.size ?: 0) +
            (bodyMetrics?.weightEntries?.size ?: 0) +
            (cycles?.size ?: 0) +
            (customExercises?.size ?: 0) +
            (machineSettings?.size ?: 0)

    val isEmpty: Boolean
        get() = recordCount == 0

    /** Rezime po kategorijama za prikaz korisniku; sekcije kojih nema se preskaču. */
    fun summary(): String {
        val parts = mutableListOf<String>()
        workouts?.let { parts.add("treninga: ${it.size}") }
        bodyMetrics?.let { parts.add("merenja težine: ${it.weightEntries.size}") }
        cycles?.let { parts.add("ciklusa: ${it.size}") }
        customExercises?.let { parts.add("mojih vežbi: ${it.size}") }
        machineSettings?.let { parts.add("podešavanja mašina: ${it.size}") }
        return parts.joinToString(", ")
    }

    /** Nazivi sekcija kojih nema u izvoru (samo stari format). */
    fun missingSections(): List<String> {
        val missing = mutableListOf<String>()
        if (workouts == null) missing.add("treninzi")
        if (bodyMetrics == null) missing.add("telesne metrike")
        if (cycles == null) missing.add("ciklusi")
        if (customExercises == null) missing.add("moje vežbe")
        if (machineSettings == null) missing.add("podešavanja mašina")
        return missing
    }
}

/** Backup ne može da se pročita ili sadrži neispravan zapis. Poruka je za korisnika. */
class BackupFormatException(message: String) : Exception(message)

/**
 * Parsiranje i validacija backup dokumenta. Čist Kotlin/Gson (bez Androida),
 * da može da se testira na JVM-u.
 *
 * Pravilo: SVE se parsira i proverava PRE prvog upisa u bazu. Bilo koji
 * neispravan zapis je greška celog vraćanja — nikad delimičan "uspeh".
 */
object BackupCodec {
    private val gson = GsonProvider.instance

    fun encode(document: BackupDocument): String = gson.toJson(document)

    /**
     * Čita kombinovani dokument ILI stari lokalni izvoz (JSON niz treninga).
     * Baca [BackupFormatException] sa porukom na srpskom.
     */
    fun decode(json: String): BackupDocument {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) throw BackupFormatException("Backup fajl je prazan.")

        val document = try {
            if (trimmed.startsWith("[")) {
                // Stari izvoz: samo niz treninga. Ostale kategorije se ne diraju.
                BackupDocument(
                    schemaVersion = 0,
                    workouts = gson.fromJson(trimmed, Array<Workout>::class.java)?.toList()
                        ?: throw BackupFormatException("Backup fajl je prazan.")
                )
            } else {
                val root = JsonParser.parseString(trimmed)
                if (!root.isJsonObject || !root.asJsonObject.has("schemaVersion")) {
                    throw BackupFormatException("Fajl nije GymTracker backup.")
                }
                val parsed = gson.fromJson(root, BackupDocument::class.java)
                    ?: throw BackupFormatException("Backup fajl je prazan.")
                if (parsed.schemaVersion > BACKUP_SCHEMA_VERSION) {
                    throw BackupFormatException(
                        "Backup je napravljen novijom verzijom aplikacije. Ažuriraj aplikaciju pa pokušaj ponovo."
                    )
                }
                val missing = parsed.missingSections()
                if (missing.isNotEmpty()) {
                    throw BackupFormatException("Backup je nepotpun — nedostaje: ${missing.joinToString(", ")}.")
                }
                parsed
            }
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: Exception) {
            throw BackupFormatException("Backup je oštećen ili nije u ispravnom formatu (${e.message}).")
        }

        validate(document)
        return document
    }

    /**
     * Stari format: pet odvojenih Drive fajlova. `null`/prazan tekst = fajl ne postoji,
     * ta kategorija se pri vraćanju ne dira.
     */
    fun decodeLegacy(
        workoutsJson: String?,
        bodyMetricsJson: String?,
        cyclesJson: String?,
        customExercisesJson: String?,
        machineSettingsJson: String?
    ): BackupDocument {
        fun <T> section(name: String, json: String?, parse: (String) -> T?): T? {
            if (json.isNullOrBlank()) return null
            return try {
                parse(json) ?: throw BackupFormatException("Backup je oštećen: sekcija „$name“ je prazna.")
            } catch (e: BackupFormatException) {
                throw e
            } catch (e: Exception) {
                throw BackupFormatException("Backup je oštećen: sekcija „$name“ ne može da se pročita (${e.message}).")
            }
        }

        val document = BackupDocument(
            schemaVersion = 0,
            workouts = section("treninzi", workoutsJson) {
                gson.fromJson(it, Array<Workout>::class.java)?.toList()
            },
            bodyMetrics = section("telesne metrike", bodyMetricsJson) {
                gson.fromJson(it, BodyMetricsState::class.java)
            },
            cycles = section("ciklusi", cyclesJson) {
                gson.fromJson(it, Array<SteroidCycle>::class.java)?.toList()
            },
            customExercises = section("moje vežbe", customExercisesJson) {
                gson.fromJson(it, Array<CustomExercise>::class.java)?.toList()
            },
            machineSettings = section("podešavanja mašina", machineSettingsJson) {
                gson.fromJson(it, Array<MachineSettings>::class.java)?.toList()
            }
        )
        validate(document)
        return document
    }

    // Gson pravi objekte bez konstruktora, pa i "non-null" Kotlin polja mogu biti null.
    // Provera ide kroz Any? parametar da je kompajler ne bi izbacio kao "uvek false".
    private fun missing(value: Any?): Boolean = value == null

    private fun fail(section: String, index: Int, what: String): Nothing =
        throw BackupFormatException("Backup je oštećen: $section, stavka ${index + 1} — $what.")

    private fun requireUnique(section: String, keys: List<String>) {
        val seen = HashSet<String>()
        keys.forEachIndexed { index, key ->
            if (!seen.add(key)) fail(section, index, "dupli identifikator")
        }
    }

    fun validate(document: BackupDocument) {
        document.workouts?.let { workouts ->
            val exerciseIds = mutableListOf<String>()
            val setIds = mutableListOf<String>()
            workouts.forEachIndexed { i, w ->
                if (missing(w)) fail("treninzi", i, "prazan zapis")
                if (missing(w.id) || missing(w.date) || missing(w.time) || missing(w.createdAt)) {
                    fail("treninzi", i, "nedostaje id, datum ili vreme")
                }
                if (missing(w.exercises)) fail("treninzi", i, "nedostaje lista vežbi")
                w.exercises.forEach { ex ->
                    if (missing(ex) || missing(ex.id) || missing(ex.exerciseName) || missing(ex.sets)) {
                        fail("treninzi", i, "neispravna vežba")
                    }
                    if (missing(ex.category)) fail("treninzi", i, "nepoznata kategorija vežbe „${ex.exerciseName}“")
                    exerciseIds.add(ex.id)
                    ex.sets.forEach { set ->
                        if (missing(set) || missing(set.id)) fail("treninzi", i, "neispravna serija")
                        setIds.add(set.id)
                    }
                }
            }
            requireUnique("treninzi", workouts.map { it.id })
            requireUnique("vežbe u treninzima", exerciseIds)
            requireUnique("serije u treninzima", setIds)
        }

        document.bodyMetrics?.let { body ->
            if (missing(body.profile) || missing(body.weightEntries)) {
                throw BackupFormatException("Backup je oštećen: telesne metrike nemaju profil ili listu merenja.")
            }
            if (missing(body.profile.gender) || missing(body.profile.activityLevel)) {
                throw BackupFormatException("Backup je oštećen: profil ima nepoznat pol ili nivo aktivnosti.")
            }
            body.weightEntries.forEachIndexed { i, entry ->
                if (missing(entry) || missing(entry.id) || missing(entry.date)) {
                    fail("merenja težine", i, "nedostaje id ili datum")
                }
            }
            requireUnique("merenja težine", body.weightEntries.map { it.id })
        }

        document.cycles?.let { cycles ->
            cycles.forEachIndexed { i, c ->
                if (missing(c) || missing(c.id) || missing(c.startDate) || missing(c.endDate)) {
                    fail("ciklusi", i, "nedostaje id ili datum")
                }
            }
            requireUnique("ciklusi", cycles.map { it.id })
        }

        document.customExercises?.let { exercises ->
            exercises.forEachIndexed { i, e ->
                if (missing(e) || missing(e.id) || missing(e.name)) fail("moje vežbe", i, "nedostaje id ili naziv")
                if (missing(e.category)) fail("moje vežbe", i, "nepoznata kategorija")
            }
            requireUnique("moje vežbe", exercises.map { it.id })
        }

        document.machineSettings?.let { settings ->
            settings.forEachIndexed { i, m ->
                if (missing(m) || missing(m.exerciseName) || missing(m.settings)) {
                    fail("podešavanja mašina", i, "nedostaje naziv vežbe ili lista podešavanja")
                }
                m.settings.forEach { s ->
                    if (missing(s) || missing(s.name) || missing(s.value)) {
                        fail("podešavanja mašina", i, "neispravno podešavanje")
                    }
                }
            }
            requireUnique("podešavanja mašina", settings.map { it.exerciseName })
        }
    }
}
