package be.ucclesport.rus

import DUMMY_VCS
import com.helger.commons.datetime.PDTFactory
import com.helger.commons.state.ESuccess

import com.helger.ubl21.UBL21Writer
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AddressType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CountryType

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CustomerPartyType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.InvoiceLineType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ItemType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyIdentificationType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyLegalEntityType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyNameType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyTaxSchemeType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxCategoryType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSchemeType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSubtotalType
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxTotalType
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.CompanyIDType
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.DocumentCurrencyCodeType
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.EndpointIDType
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.IDType
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.InvoiceTypeCodeType
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NameType
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType
import java.io.File
import java.math.BigDecimal


fun List<InvoiceLine>.toUbl(path: String, cbe: String, year: Int): ESuccess {
    val sCurrency = "EUR"

    val ivNumber = this.first { it.vcs != DUMMY_VCS }.vcs!!.split("/").let { "$year${"${it[1]}${it[2].dropLast(2)}".padStart(7, '0')}" }.toLong()
    val ivTotal = this.fold(.0) { acc, it -> acc + (it.amount ?: .0) }

    // Create domain object

    // Create domain object
    val taxCategory = TaxCategoryType().apply {
        id = IDType().apply {
            schemeID = "UNCL5305"
            value = "Z"
        }
        setName("00")
        setPercent(BigDecimal.ZERO)
        taxScheme = TaxSchemeType().apply { id = IDType().apply { value = "VAT" } }
    }

    val aInvoice = InvoiceType().apply {
        setID(ivNumber.toString())
        setIssueDate(PDTFactory.getCurrentXMLOffsetDateUTC())
        documentCurrencyCode = DocumentCurrencyCodeType().apply {
            listID = "ISO4217"
            value = sCurrency
        }
        invoiceTypeCode = InvoiceTypeCodeType().apply {
            listID = "UNCL1001"
            value = "380"
        }

        accountingSupplierParty = SupplierPartyType().apply {
            party = PartyType().apply {
                endpointID = EndpointIDType().apply {
                    schemeID = "BE:CBE"
                    value = cbe
                }
                addPartyName(PartyNameType().apply { name = NameType().apply { value = "Royal Uccle Sport" } })
                addPartyIdentification(PartyIdentificationType().apply { id = IDType().apply { value = cbe } })
                postalAddress = AddressType().apply {
                    setStreetName("Chaussée de Ruisbroek")
                    setBuildingNumber("18")
                    setCityName("Uccle")
                    setPostalZone("1180")
                    setCountry(CountryType().apply { name = NameType().apply { value = "Belgium" } })
                    addPartyTaxScheme(PartyTaxSchemeType().apply {
                        companyID = CompanyIDType().apply {
                            schemeID = "BE:VAT"
                            value = "BE$cbe"
                        }
                        setCompanyID(cbe)
                        taxScheme = TaxSchemeType().apply { id = IDType().apply { value = "VAT" } }
                    })
                    addPartyLegalEntity(PartyLegalEntityType().apply {
                        companyID = CompanyIDType().apply {
                            schemeID = "BE:CBE"
                            value = cbe
                        }
                    })
                }
            }
        }
        accountingCustomerParty = CustomerPartyType().apply {
            party = PartyType().apply {
                addPartyName(PartyNameType().apply { name = NameType().apply {
                    value = this@toUbl.firstNotNullOf { it.name }
                } })
                postalAddress = AddressType().apply {
                    setStreetName(this@toUbl.firstNotNullOf { it.street })
                    setBuildingNumber(this@toUbl.firstNotNullOf { it.houseNumber })
                    setCityName(this@toUbl.firstNotNullOf { it.city })
                    setPostalZone(this@toUbl.firstNotNullOf { it.postalCode })
                }
            }
        }
        legalMonetaryTotal = MonetaryTotalType().apply {
            setPayableAmount(BigDecimal.valueOf(ivTotal)).apply { currencyID = sCurrency }
            setLineExtensionAmount(BigDecimal.valueOf(ivTotal)).apply { currencyID = sCurrency }
            setTaxExclusiveAmount(BigDecimal.valueOf(ivTotal)).apply { currencyID = sCurrency }
            setTaxInclusiveAmount(BigDecimal.valueOf(ivTotal)).apply { currencyID = sCurrency }
        }
        this@toUbl.forEachIndexed { index, invoiceLine ->
            addInvoiceLine(InvoiceLineType().apply {
                setID((index+1).toString())
                item = ItemType().apply {
                    name = NameType().apply { value = invoiceLine.description }
                    addClassifiedTaxCategory(taxCategory)
                }
                setLineExtensionAmount(BigDecimal.valueOf(invoiceLine.amount!!)).apply { currencyID = sCurrency }
            })
        }

        addTaxTotal(TaxTotalType().apply {
            setTaxAmount(BigDecimal.ZERO).currencyID = sCurrency
            addTaxSubtotal(TaxSubtotalType().apply {
                setTaxableAmount(BigDecimal.valueOf(ivTotal)).currencyID = sCurrency
                setTaxAmount(BigDecimal.ZERO).currencyID = sCurrency
                setTaxCategory(taxCategory)
            })
        })
    }

    // Write to disk
    return UBL21Writer.invoice().write(aInvoice, File(path))
}
