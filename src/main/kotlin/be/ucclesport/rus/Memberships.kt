#!/usr/bin/env kotlinc -script

package be.ucclesport.rus

import DUMMY_VCS
import java.io.File

fun main(args: Array<String>) {
    val invoices = mutableMapOf<String, List<InvoiceLine>>()
    val dangling = mutableListOf<InvoiceLine>()

    val records = if (args.size>2) {
        parseCodaFile(args[2])
    } else null

    (args.takeIf { it.size>1 }?.get(1)?.let { File(it).inputStream() } ?: System.`in`).bufferedReader().use { reader ->
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

            val invoiceLine = InvoiceLine(name = name, phone = phone, email = email, vcs = vcs, amount = amount, paid = paid, description = description, street = street, houseNumber = houseNumber, postalCode = postalCode, city = city)
            if (vcs== DUMMY_VCS) {
                dangling.add(invoiceLine)
            } else {
                invoices[vcs] = (invoices[vcs] ?: emptyList()) + invoiceLine
            }
        }

        dangling.forEach { invoiceLine ->
            val vcs = invoices.keys.find { key -> invoices[key]?.any {
                it.name == invoiceLine.name && it.phone == invoiceLine.phone && it.email == invoiceLine.email
            } ?: false }
            if (vcs!=null) {
                invoices[vcs] = (invoices[vcs] ?: emptyList()) + invoiceLine
            } else {
                println("Dangling invoice: $invoiceLine")
            }
        }

        invoices.forEach { (vcs, invoiceLines) ->
            val path = args.first() + "/${vcs.replace("/","-")}.xml"
            if (records!=null) {
                val record = records.filterIsInstance<CodaTransaction>().find {
                    it.communication.contains(vcs.replace("/", "")) ||
                            true == it.extraCommunication?.contains(vcs.replace("/", ""))
                }
                if (record!=null) {
                    println("Writing $path")
                    invoiceLines.toUbl(path, "0411104311", 2023)
                } else {
                    println("No record found for $vcs")
                }
            } else {
                println("Writing $path")
                invoiceLines.toUbl(path, "0411104311", 2023)
            }
        }
    }
}

data class InvoiceLine(
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
