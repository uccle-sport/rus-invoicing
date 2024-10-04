#!/usr/bin/env kotlinc -script

package be.ucclesport.rus

import DUMMY_VCS
import java.io.File
import kotlin.streams.asSequence

fun main(args: Array<String>) {
    val invoices = mutableMapOf<String, List<InvoiceLine>>()
    val dangling = mutableListOf<InvoiceLine>()

    val records = if (args.size > 2) {
        parseCodaFile(args[2])
    } else null

    val linesToKeep = setOf(
        "Affiliation Hockey",
        "Contribution environmentale",
        "Cotisation Club né en 1920-2004 - 2023-24",
        "Cotisation Club né en 2005-2011 - 2023-24",
        "Cotisation Club né en 2012-2019 - 2023-24",
        "Hockey - Adultes",
        "Hockey - Adultes sans entraïnements",
        "Hockey - cotisation arbitre théorique",
        "Hockey - Jeunes Adultes 1998-2004",
        "Hockey - Ladies/Gentlemen",
        "Hockey - Non joueurs",
        "Hockey - Poussins",
        "Hockey - réduction 2è enfant",
        "Hockey - réduction 3é enfant",
        "Hockey - réduction famille",
        "Hockey - Top Hockey",
        "Hockey - U 7-U 8",
        "Hockey - U 9-U12",
        "Hockey - U14-U19",
        "Hockey Together"
    )

    val correspondances = CodaTransaction.javaClass.getResourceAsStream("/signaletique.csv")?.bufferedReader()?.use { reader ->
        reader.lines().skip(1).asSequence().fold(emptyMap<Triple<String, String, String>, Pair<String, String>>()) { acc, v ->
            val fields = v.split(";")
            if (fields.size==5) {
                acc + (Triple(fields[0], fields[1], fields[2]) to Pair(fields[3], fields[4]))
            } else acc
        }
    }?: emptyMap()

    (args.takeIf { it.size > 1 }?.get(1)?.let { File(it).inputStream() } ?: System.`in`).bufferedReader()
        .use { reader ->
            reader.lines().skip(1).forEach {
                val fields = it.split(";")
                val name = fields[1]
                val phone = fields[2]
                val email = fields[3]
                val vcs = fields[32]
                val amount = fields[26].replace(",", ".").toDouble()
                val paid = fields[27].replace(",", ".").toDouble()
                val description = fields[23]
                val street = fields[14]
                val houseNumber = fields[15]
                val postalCode = fields[18]
                val city = fields[19]

                val correspondance = correspondances.filter { name == it.key.first && email == it.key.third }.values.firstOrNull()

                val invoiceLine = InvoiceLine(
                    id = correspondance?.first,
                    name = correspondance?.second ?: name,
                    phone = phone,
                    email = email,
                    vcs = vcs,
                    amount = amount,
                    paid = paid,
                    description = description,
                    street = street,
                    houseNumber = houseNumber,
                    postalCode = postalCode,
                    city = city
                )
                if (linesToKeep.any { line -> description.contains(line) }) {
                    if (vcs == DUMMY_VCS) {
                        dangling.add(invoiceLine)
                    } else {
                        invoices[vcs] = (invoices[vcs] ?: emptyList()) + invoiceLine
                    }
                }
            }

            dangling.forEach { invoiceLine ->
                val vcs = invoices.keys.find { key ->
                    invoices[key]?.any {
                        it.name == invoiceLine.name && it.phone == invoiceLine.phone && it.email == invoiceLine.email
                    } ?: false
                }
                if (vcs != null) {
                    invoices[vcs] = (invoices[vcs] ?: emptyList()) + invoiceLine
                } else {
                    println("Dangling invoice: $invoiceLine")
                }
            }

            var ivNumber = 2024000

            invoices.forEach { (vcs, invoiceLines) ->
                val path = args.first() + "/${vcs.replace("/", "-")}.xml"
                if (records != null) {
                    val record = records.filterIsInstance<CodaTransaction>().find {
                        it.communication.contains(vcs.replace("/", "")) ||
                                true == it.extraCommunication?.contains(vcs.replace("/", ""))
                    }
                    if (record != null) {
                        ivNumber += 1
                        println("Writing $path")
                        invoiceLines.toUbl(path, "0411104311", 2023, ivNumber)
                    } else {
                        println("No record found for $vcs")
                    }
                } else {
                    println("Writing $path")
                    invoiceLines.toUbl(path, "0411104311", 2023, ivNumber)
                }
            }
        }
}

data class InvoiceLine(
    val id: String?,
    val vcs: String?,
    val name: String,
    val phone: String?,
    val email: String?,
    val amount: Double?,
    val paid: Double?,
    val description: String,
    val street: String,
    val houseNumber: String,
    val postalCode: String,
    val city: String,
)
