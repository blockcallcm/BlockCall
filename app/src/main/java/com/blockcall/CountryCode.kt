package com.blockcall

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Indicativo telefónico del país de la SIM (p. ej. "57" para Colombia), sin permisos ni internet.
 * Limitación conocida: los países que comparten el +1 (EE. UU., Canadá, Caribe) se ven como uno solo.
 */
object CountryCode {

    /** Indicativo del país de la SIM; si no hay SIM, el de la red o el del idioma del teléfono. */
    fun of(context: Context): String? {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val iso = listOf(tm?.simCountryIso, tm?.networkCountryIso, Locale.getDefault().country)
            .firstOrNull { !it.isNullOrBlank() && CODES.containsKey(it.lowercase()) }
        return iso?.let { CODES[it.lowercase()] }
    }

    /** ¿El número viene de otro país? Solo los que traen prefijo internacional (+ o 00); los nacionales no. */
    fun isForeign(number: String, own: String): Boolean {
        val n = number.filter { it.isDigit() || it == '+' }
        val international = when {
            n.startsWith("+") -> n.drop(1)
            n.startsWith("00") -> n.drop(2)
            else -> return false
        }
        return international.isNotEmpty() && !international.startsWith(own)
    }

    // ISO 3166 → indicativo (UIT-T E.164).
    private val CODES: Map<String, String> = buildMap {
        fun add(code: String, vararg isos: String) = isos.forEach { put(it, code) }
        add("1", "us", "ca", "ag", "ai", "as", "bb", "bm", "bs", "dm", "do", "gd", "gu", "jm", "kn", "ky", "lc", "mp", "ms", "pr", "sx", "tc", "tt", "vc", "vg", "vi")
        add("7", "ru", "kz")
        add("20", "eg"); add("27", "za"); add("30", "gr"); add("31", "nl"); add("32", "be"); add("33", "fr")
        add("34", "es"); add("36", "hu"); add("39", "it", "va"); add("40", "ro"); add("41", "ch"); add("43", "at")
        add("44", "gb", "gg", "im", "je"); add("45", "dk"); add("46", "se"); add("47", "no", "sj"); add("48", "pl")
        add("49", "de"); add("51", "pe"); add("52", "mx"); add("53", "cu"); add("54", "ar"); add("55", "br")
        add("56", "cl"); add("57", "co"); add("58", "ve"); add("60", "my"); add("61", "au", "cx", "cc"); add("62", "id")
        add("63", "ph"); add("64", "nz"); add("65", "sg"); add("66", "th"); add("81", "jp"); add("82", "kr")
        add("84", "vn"); add("86", "cn"); add("90", "tr"); add("91", "in"); add("92", "pk"); add("93", "af")
        add("94", "lk"); add("95", "mm"); add("98", "ir")
        add("211", "ss"); add("212", "ma", "eh"); add("213", "dz"); add("216", "tn"); add("218", "ly"); add("220", "gm")
        add("221", "sn"); add("222", "mr"); add("223", "ml"); add("224", "gn"); add("225", "ci"); add("226", "bf")
        add("227", "ne"); add("228", "tg"); add("229", "bj"); add("230", "mu"); add("231", "lr"); add("232", "sl")
        add("233", "gh"); add("234", "ng"); add("235", "td"); add("236", "cf"); add("237", "cm"); add("238", "cv")
        add("239", "st"); add("240", "gq"); add("241", "ga"); add("242", "cg"); add("243", "cd"); add("244", "ao")
        add("245", "gw"); add("246", "io"); add("248", "sc"); add("249", "sd"); add("250", "rw"); add("251", "et")
        add("252", "so"); add("253", "dj"); add("254", "ke"); add("255", "tz"); add("256", "ug"); add("257", "bi")
        add("258", "mz"); add("260", "zm"); add("261", "mg"); add("262", "re", "yt"); add("263", "zw"); add("264", "na")
        add("265", "mw"); add("266", "ls"); add("267", "bw"); add("268", "sz"); add("269", "km"); add("290", "sh")
        add("291", "er"); add("297", "aw"); add("298", "fo"); add("299", "gl")
        add("350", "gi"); add("351", "pt"); add("352", "lu"); add("353", "ie"); add("354", "is"); add("355", "al")
        add("356", "mt"); add("357", "cy"); add("358", "fi", "ax"); add("359", "bg"); add("370", "lt"); add("371", "lv")
        add("372", "ee"); add("373", "md"); add("374", "am"); add("375", "by"); add("376", "ad"); add("377", "mc")
        add("378", "sm"); add("380", "ua"); add("381", "rs"); add("382", "me"); add("383", "xk"); add("385", "hr")
        add("386", "si"); add("387", "ba"); add("389", "mk"); add("420", "cz"); add("421", "sk"); add("423", "li")
        add("500", "fk"); add("501", "bz"); add("502", "gt"); add("503", "sv"); add("504", "hn"); add("505", "ni")
        add("506", "cr"); add("507", "pa"); add("508", "pm"); add("509", "ht"); add("590", "gp", "bl", "mf"); add("591", "bo")
        add("592", "gy"); add("593", "ec"); add("594", "gf"); add("595", "py"); add("596", "mq"); add("597", "sr")
        add("598", "uy"); add("599", "cw", "bq")
        add("670", "tl"); add("672", "nf"); add("673", "bn"); add("674", "nr"); add("675", "pg"); add("676", "to")
        add("677", "sb"); add("678", "vu"); add("679", "fj"); add("680", "pw"); add("681", "wf"); add("682", "ck")
        add("683", "nu"); add("685", "ws"); add("686", "ki"); add("687", "nc"); add("688", "tv"); add("689", "pf")
        add("690", "tk"); add("691", "fm"); add("692", "mh")
        add("850", "kp"); add("852", "hk"); add("853", "mo"); add("855", "kh"); add("856", "la"); add("880", "bd"); add("886", "tw")
        add("960", "mv"); add("961", "lb"); add("962", "jo"); add("963", "sy"); add("964", "iq"); add("965", "kw")
        add("966", "sa"); add("967", "ye"); add("968", "om"); add("970", "ps"); add("971", "ae"); add("972", "il")
        add("973", "bh"); add("974", "qa"); add("975", "bt"); add("976", "mn"); add("977", "np"); add("992", "tj")
        add("993", "tm"); add("994", "az"); add("995", "ge"); add("996", "kg"); add("998", "uz")
    }
}
