package com.softvinax.lantern.data.fake

import com.softvinax.lantern.domain.model.Alert
import com.softvinax.lantern.domain.model.AlertLevel
import com.softvinax.lantern.domain.model.LocalizedText
import com.softvinax.lantern.domain.repository.AlertRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeAlertRepository @Inject constructor() : AlertRepository {

    private val now = System.currentTimeMillis()
    private val alerts = MutableStateFlow(seedAlerts(now))

    override fun observeAlerts(): Flow<List<Alert>> = alerts.asStateFlow()

    override suspend fun refresh() {
        delay(250)
    }

    private fun seedAlerts(now: Long): List<Alert> = listOf(
        Alert(
            id = "alert-flood-1",
            level = AlertLevel.URGENT,
            title = LocalizedText(
                en = "Flood water rising in low-lying areas",
                ur = "نیچے والے علاقوں میں سیلاب کا پانی بڑھ رہا ہے",
                roman = "Neeche walay ilaqon mein flood ka pani barh raha hai",
            ),
            message = LocalizedText(
                en = "Move to higher ground now. Avoid underpasses and river banks. Rescue teams are on the way.",
                ur = "ابھی بلند جگہ پر جائیں۔ زیرِ زمین راستوں اور دریا کے کناروں سے بچیں۔ امدادی ٹیمیں راستے میں ہیں۔",
                roman = "Abhi buland jagah par jayen. Underpass aur darya ke kinare se bachein. Rescue teams raaste mein hain.",
            ),
            region = "Karachi • Malir",
            issuedAt = now - 8 * 60 * 1000L,
        ),
        Alert(
            id = "alert-flood-2",
            level = AlertLevel.WARNING,
            title = LocalizedText(
                en = "Water level high at Malir River",
                ur = "ملیر ندی میں پانی کی سطح بلند",
                roman = "Malir nadi mein pani ki satah buland",
            ),
            message = LocalizedText(
                en = "Keep away from the river bank and bridges. Do not drive through water.",
                ur = "دریا کے کنارے اور پلوں سے دور رہیں۔ پانی میں گاڑی نہ چلائیں۔",
                roman = "Darya ke kinare aur pulon se door rahen. Pani mein gaari na chalayen.",
            ),
            region = "Karachi • Malir",
            issuedAt = now - 45 * 60 * 1000L,
        ),
        Alert(
            id = "alert-relief-1",
            level = AlertLevel.INFO,
            title = LocalizedText(
                en = "Relief camp open at Government School",
                ur = "گورنمنٹ اسکول میں ریلیف کیمپ کھل گیا",
                roman = "Government School mein relief camp khul gaya",
            ),
            message = LocalizedText(
                en = "Drinking water, food and shelter are available. Bring your family and tell volunteers how many people are with you.",
                ur = "پینے کا پانی، کھانا اور پناہ دستیاب ہے۔ اپنے خاندان کو ساتھ لائیں اور رضاکاروں کو لوگوں کی تعداد بتائیں۔",
                roman = "Peene ka pani, khana aur panah dastyab hai. Apne khandan ko saath layen aur volunteers ko logon ki tadaad batayen.",
            ),
            region = "Karachi • Gulshan",
            issuedAt = now - 3 * 60 * 60 * 1000L,
        ),
    )
}
