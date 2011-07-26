package com.bazaarvoice.core.util;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts between Java Locale objects and Microsoft Windows LCID and LANGID identifiers.
 *
 * A Microsoft LCID corresponds to a Java Locale containing a Language and Country.
 *
 * A Microsoft LANGID corresponds to a Java Locale containing a Language.
 */
class LocaleLcidTable {

    private static final Map<Integer, Locale> _langidToLocale = new HashMap<Integer, Locale>();
    private static final Map<Integer, Locale> _lcidToLocale = new HashMap<Integer, Locale>();

    static Locale getLocaleFromMicrosoftLCID(int lcid) {
        // try to use the full lcid to lookup the locale
        Locale locale = _lcidToLocale.get(lcid);
        if (locale == null) {
            // no match.  lookup based on the language, ignoring the country information.
            // the bottom 10 bits of the lcid is the langid identifier.
            locale = _langidToLocale.get(lcid & 0x3ff);
        }
        return locale;
    }

    private static void defineLanguage(int langid, String locale) {
        if (StringUtils.substringBefore(locale, "_").length() == 3) {
            return;  // ignore 3-letter ISO 639-2 codes--not supported by java Locale
        }
        if (_langidToLocale.put(langid, Locale.toLocale(locale)) != null) {
            throw new IllegalStateException("Duplicate language mapping: 0x" + Integer.toHexString(langid) + " -> " + locale);
        }
    }

    private static void defineLocale(int lcid, String locale) {
        if (StringUtils.substringBefore(locale, "_").length() == 3) {
            return;  // ignore 3-letter ISO 639-2 codes--not supported by java Locale
        }
        if (_lcidToLocale.put(lcid, Locale.toLocale(locale)) != null) {
            throw new IllegalStateException("Duplicate locale mapping: 0x" + Integer.toHexString(lcid) + " -> " + locale);
        }
    }

    static {
        // Table of Languages and Locales extracted from:
        //  http://msdn2.microsoft.com/en-us/library/ms776294.aspx
        //  http://unicode.org/onlinedat/languages.html
        //  http://www.loc.gov/standards/iso639-2/php/English_list.php
        defineLanguage(0x01, "ar");    // Arabic
        defineLocale(0x3801, "ar_AE"); // Arabic, U.A.E.
        defineLocale(0x3c01, "ar_BH"); // Arabic, Bahrain
        defineLocale(0x1401, "ar_DZ"); // Arabic, Algeria
        defineLocale(0x0c01, "ar_EG"); // Arabic, Egypt
        defineLocale(0x0801, "ar_IQ"); // Arabic, Iraq
        defineLocale(0x2c01, "ar_JO"); // Arabic, Jordan
        defineLocale(0x3401, "ar_KW"); // Arabic, Kuwait
        defineLocale(0x3001, "ar_LB"); // Arabic, Lebanon
        defineLocale(0x1001, "ar_LY"); // Arabic, Libya
        defineLocale(0x1801, "ar_MA"); // Arabic, Morocco
        defineLocale(0x2001, "ar_OM"); // Arabic, Oman
        defineLocale(0x4001, "ar_QA"); // Arabic, Qatar
        defineLocale(0x0401, "ar_SA"); // Arabic, Saudi Arabia
        defineLocale(0x2801, "ar_SY"); // Arabic, Syria
        defineLocale(0x1c01, "ar_TN"); // Arabic, Tunisia
        defineLocale(0x2401, "ar_YE"); // Arabic, Yemen
        defineLanguage(0x02, "bg");    // Bulgarian
        defineLocale(0x0402, "bg_BG"); // Bulgarian, Bulgaria
        defineLanguage(0x03, "ca");    // Catalan
        defineLocale(0x0403, "ca_ES"); // Catalan, Catalan
        defineLanguage(0x04, "zh");    // Chinese (Simplified) (Traditional)
        defineLocale(0x0c04, "zh_HK"); // Chinese, Hong Kong SAR, PRC
        defineLocale(0x1404, "zh_MO"); // Chinese, Macao SAR
        defineLocale(0x1004, "zh_SG"); // Chinese, Singapore
        defineLanguage(0x05, "cs");    // Czech
        defineLocale(0x0405, "cs_CZ"); // Czech, Czech Republic
        defineLanguage(0x06, "da");    // Danish
        defineLocale(0x0406, "da_DK"); // Danish, Denmark
        defineLanguage(0x07, "de");    // German
        defineLocale(0x0c07, "de_AT"); // German, Austria
        defineLocale(0x0807, "de_CH"); // German, Switzerland
        defineLocale(0x0407, "de_DE"); // German, Germany
        defineLocale(0x1407, "de_LI"); // German, Liechtenstein
        defineLocale(0x1007, "de_LU"); // German, Luxembourg
        defineLanguage(0x08, "el");    // Greek
        defineLocale(0x0408, "el_GR"); // Greek, Greece
        defineLanguage(0x09, "en");    // English
        defineLocale(0x0c09, "en_AU"); // English, Australia
        defineLocale(0x2809, "en_BE"); // English, Belize
        defineLocale(0x1009, "en_CA"); // English, Canada
        defineLocale(0x0809, "en_GB"); // English, United Kingdom
        defineLocale(0x1809, "en_IE"); // English, Ireland
        defineLocale(0x4009, "en_IN"); // English, India
        defineLocale(0x2009, "en_JM"); // English, Jamaica
        defineLocale(0x4409, "en_MY"); // English, Malaysia
        defineLocale(0x1409, "en_NZ"); // English, New Zealand
        defineLocale(0x3409, "en_PH"); // English, Philippines
        defineLocale(0x4809, "en_SG"); // English, Singapore
        defineLocale(0x2c09, "en_TT"); // English, Trinidad and Tobago
        defineLocale(0x0409, "en_US"); // English, United States
        defineLocale(0x1c09, "en_ZA"); // English, South Africa
        defineLocale(0x3009, "en_ZW"); // English, Zimbabwe
        defineLanguage(0x0a, "es");    // Spanish
        defineLocale(0x2c0a, "es_AR"); // Spanish, Argentina
        defineLocale(0x400a, "es_BO"); // Spanish, Bolivia
        defineLocale(0x340a, "es_CL"); // Spanish, Chile
        defineLocale(0x240a, "es_CO"); // Spanish, Colombia
        defineLocale(0x140a, "es_CR"); // Spanish, Costa Rica
        defineLocale(0x1c0a, "es_DO"); // Spanish, Dominican Republic
        defineLocale(0x300a, "es_EC"); // Spanish, Ecuador
        defineLocale(0x0c0a, "es_ES"); // Spanish, Spain
        defineLocale(0x100a, "es_GT"); // Spanish, Guatemala
        defineLocale(0x480a, "es_HN"); // Spanish, Honduras
        defineLocale(0x080a, "es_MX"); // Spanish, Mexico
        defineLocale(0x4c0a, "es_NI"); // Spanish, Nicaragua
        defineLocale(0x180a, "es_PA"); // Spanish, Panama
        defineLocale(0x280a, "es_PE"); // Spanish, Peru
        defineLocale(0x500a, "es_PR"); // Spanish, Puerto Rico
        defineLocale(0x3c0a, "es_PY"); // Spanish, Paraguay
        defineLocale(0x440a, "es_SV"); // Spanish, El Salvador
        defineLocale(0x380a, "es_UY"); // Spanish, Uruguay
        defineLocale(0x200a, "es_VE"); // Spanish, Venezuela
        defineLanguage(0x0b, "fi");    // Finnish
        defineLocale(0x040b, "fi_FI"); // Finnish, Finland
        defineLanguage(0x0c, "fr");    // French
        defineLocale(0x080c, "fr_BE"); // French, Belgium
        defineLocale(0x0c0c, "fr_CA"); // French, Canada
        defineLocale(0x100c, "fr_CH"); // French, Switzerland
        defineLocale(0x040c, "fr_FR"); // French, France
        defineLocale(0x140c, "fr_LU"); // French, Luxembourg
        defineLocale(0x180c, "fr_MC"); // French, Monaco
        defineLanguage(0x0d, "he");    // Hebrew
//        defineLanguage(0x0d, "iw");    // Hebrew
        defineLocale(0x040d, "he_IL"); // Hebrew, Israel
        defineLanguage(0x0e, "hu");    // Hungarian
        defineLocale(0x040e, "hu_HU"); // Hungarian, Hungary
        defineLanguage(0x0f, "is");    // Icelandic
        defineLocale(0x040f, "is_IS"); // Icelandic, Iceland
        defineLanguage(0x10, "it");    // Italian
        defineLocale(0x0810, "it_CH"); // Italian, Switzerland
        defineLocale(0x0410, "it_IT"); // Italian, Italy
        defineLanguage(0x11, "ja");    // Japanese
        defineLocale(0x0411, "ja_JP"); // Japanese, Japan
        defineLanguage(0x12, "ko");    // Korean
        defineLocale(0x0412, "ko_KR"); // Korean, Korea
        defineLanguage(0x13, "nl");    // Dutch
        defineLocale(0x0813, "nl_BE"); // Dutch, Belgium
        defineLocale(0x0413, "nl_NL"); // Dutch, Netherlands
        defineLanguage(0x14, "no");    // Norwegian
        defineLocale(0x0414, "no_NO"); // Norwegian, Bokm�l, Norway
        defineLocale(0x0814, "no_NO"); // Norwegian, Nynorsk, Norway
        defineLanguage(0x15, "pl");    // Polish
        defineLocale(0x0415, "pl_PL"); // Polish, Poland
        defineLanguage(0x16, "pt");    // Portuguese
        defineLocale(0x0416, "pt_BR"); // Portuguese, Brazil
        defineLocale(0x0816, "pt_PT"); // Portuguese, Portugal
        defineLanguage(0x17, "rm");    // Romansh
        defineLocale(0x0417, "rm_CH"); // Romansh, Switzerland
        defineLanguage(0x18, "ro");    // Romanian
        defineLocale(0x0418, "ro_RO"); // Romanian, Romania
        defineLanguage(0x19, "ru");    // Russian
        defineLocale(0x0419, "ru_RU"); // Russian, Russia
        // Bosnian, Croation and Serbian have the same language ID.
        // The full locale ID is required to differentiate.
//        defineLanguage(0x1a, "bs");    // Bosnian
        defineLocale(0x201a, "bs_BA"); // Bosnian, Bosnia and Herzegovina, Cyrillic
        defineLocale(0x141a, "bs_BA"); // Bosnian, Bosnia and Herzegovina, Latin
//        defineLanguage(0x1a, "hr");    // Croatian
        defineLocale(0x101a, "hr_BA"); // Croatian, Bosnia and Herzegovina, Latin
        defineLocale(0x041a, "hr_HR"); // Croatian, Croatia
//        defineLanguage(0x1a, "sr");    // Serbian
        defineLocale(0x181a, "sr_BA"); // Serbian, Bosnia and Herzegovina, Latin
        defineLocale(0x0c1a, "sr_CS"); // Serbian, Serbia, Cyrillic
        defineLocale(0x081a, "sr_CS"); // Serbian, Serbia, Latin
        defineLanguage(0x1b, "sk");    // Slovak
        defineLocale(0x041b, "sk_SK"); // Slovak, Slovakia
        defineLanguage(0x1c, "sq");    // Albanian
        defineLocale(0x041c, "sq_AL"); // Albanian, Albania
        defineLanguage(0x1d, "sv");    // Swedish
        defineLocale(0x081d, "sv_FI"); // Swedish, Finland
        defineLocale(0x041d, "sv_SE"); // Swedish, Sweden
        defineLanguage(0x1e, "th");    // Thai
        defineLocale(0x041e, "th_TH"); // Thai, Thailand
        defineLanguage(0x1f, "tr");    // Turkish
        defineLocale(0x041f, "tr_TR"); // Turkish, Turkey
        defineLanguage(0x20, "ur");    // Urdu
        defineLocale(0x0420, "ur_PK"); // Urdu, Pakistan
        defineLanguage(0x21, "id");    // Indonesian
//        defineLanguage(0x21, "in");    // Indonesian
        defineLocale(0x0421, "id_ID"); // Indonesian, Indonesia
        defineLanguage(0x22, "uk");    // Ukrainian
        defineLocale(0x0422, "uk_UA"); // Ukrainian, Ukraine
        defineLanguage(0x23, "be");    // Belarusian
        defineLocale(0x0423, "be_BY"); // Belarusian, Belarus
        defineLanguage(0x24, "sl");    // Slovenian
        defineLocale(0x0424, "sl_SI"); // Slovenian, Slovenia
        defineLanguage(0x25, "et");    // Estonian
        defineLocale(0x0425, "et_EE"); // Estonian, Estonia
        defineLanguage(0x26, "lv");    // Latvian
        defineLocale(0x0426, "lv_LV"); // Latvian, Latvia
        defineLanguage(0x27, "lt");    // Lithuanian
        defineLocale(0x0427, "lt_LT"); // Lithuanian, Lithuanian
        defineLanguage(0x28, "tg");    // Tajik
        defineLocale(0x0428, "tg_TJ"); // Tajik, Tajikistan
        defineLanguage(0x29, "fa");    // Persian
        defineLocale(0x0429, "fa_IR"); // Persian, Iran
        defineLanguage(0x2a, "vi");    // Vietnamese
        defineLocale(0x042a, "vi_VN"); // Vietnamese, Vietnam
        defineLanguage(0x2b, "hy");    // Armenian
        defineLocale(0x042b, "hy_AM"); // Armenian, Armenia
        defineLanguage(0x2c, "az");    // Azeri
        defineLocale(0x082c, "az_AZ"); // Azeri, Azerbaijan, Cyrillic
        defineLocale(0x042c, "az_AZ"); // Azeri, Azerbaijan, Latin
        defineLanguage(0x2d, "eu");    // Basque
        defineLocale(0x042d, "eu_ES"); // Basque, Basque
        defineLanguage(0x2e, "dsb");    // Lower Sorbian
        defineLocale(0x082e, "dsb_DE"); // Lower Sorbian, Germany
        defineLanguage(0x2e, "wen");    // Upper Sorbian
        defineLocale(0x042e, "wen_DE"); // Upper Sorbian, Germany
        defineLanguage(0x2f, "mk");    // Macedonian
        defineLocale(0x042f, "mk_MK"); // Macedonian, Macedonia, FYROM
        defineLanguage(0x31, "ts");    // Tsonga
        defineLanguage(0x32, "tn");    // Setswana/Tswana
        defineLocale(0x0432, "tn_ZA"); // Setswana/Tswana, South Africa
        defineLanguage(0x34, "xh");    // Xhosa
        defineLocale(0x0434, "xh_ZA"); // Xhosa/isiXhosa, South Africa
        defineLanguage(0x35, "zu");    // Zulu
        defineLocale(0x0435, "zu_ZA"); // Zulu/isiZulu, South Africa
        defineLanguage(0x36, "af");    // Afrikaans
        defineLocale(0x0436, "af_ZA"); // Afrikaans, South Africa
        defineLanguage(0x37, "ka");    // Georgian
        defineLocale(0x0437, "ka_GE"); // Georgian, Georgia
        defineLanguage(0x38, "fo");    // Faroese
        defineLocale(0x0438, "fo_FO"); // Faroese, Faroe Islands
        defineLanguage(0x39, "hi");    // Hindi
        defineLocale(0x0439, "hi_IN"); // Hindi, India
        defineLanguage(0x3a, "mt");    // Maltese
        defineLocale(0x043a, "mt_MT"); // Maltese, Malta
        defineLanguage(0x3b, "se");    // Sami
        defineLocale(0x243b, "se_FI"); // Sami, Inari, Finland
        defineLocale(0x0c3b, "se_FI"); // Sami, Northern, Finland
        defineLocale(0x203b, "se_FI"); // Sami, Skolt, Finland
        defineLocale(0x103b, "se_NO"); // Sami, Lule, Norway
        defineLocale(0x043b, "se_NO"); // Sami, Northern, Norway
        defineLocale(0x183b, "se_NO"); // Sami, Southern, Norway
        defineLocale(0x143b, "se_SE"); // Sami, Lule, Sweden
        defineLocale(0x083b, "se_SE"); // Sami, Northern, Sweden
        defineLocale(0x1c3b, "se_SE"); // Sami, Southern, Sweden
        // Irish and Gaelic share the same language ID.  Use the IE country to identify Irish.
//        defineLanguage(0x3c, "ga");    // Irish
        defineLocale(0x083c, "ga_IE"); // Irish, Ireland
        defineLanguage(0x3c, "gd");    // Gaelic (Scottish)
        defineLanguage(0x3d, "yi");    // Yiddish
//        defineLanguage(0x3d, "ji");    // Yiddish
        defineLanguage(0x3e, "ms");    // Malay
        defineLocale(0x083e, "ms_BN"); // Malay, Brunei Darassalam
        defineLocale(0x043e, "ms_MY"); // Malay, Malaysia
        defineLanguage(0x3f, "kk");    // Kazakh
        defineLocale(0x043f, "kk_KZ"); // Kazakh, Kazakhstan
        defineLanguage(0x40, "ky");    // Kirghiz
        defineLocale(0x0440, "ky_KG"); // Kyrgyz, Kyrgyzstan
        defineLanguage(0x41, "sw");    // Swahili
        defineLocale(0x0441, "sw_KE"); // Swahili, Kenya
        defineLanguage(0x42, "tk");    // Turkmen
        defineLocale(0x0442, "tk_TM"); // Turkmen, Turkmenistan
        defineLanguage(0x43, "uz");    // Uzbek
        defineLocale(0x0843, "uz_UZ"); // Uzbek, Uzbekistan, Cyrillic
        defineLocale(0x0443, "uz_UZ"); // Uzbek, Uzbekistan, Latin
        defineLanguage(0x44, "tt");    // Tatar
        defineLocale(0x0444, "tt_RU"); // Tatar, Russia
        defineLanguage(0x45, "bn");    // Bengali
        defineLanguage(0x46, "pa");    // Punjabi
        defineLocale(0x0446, "pa_IN"); // Punjabi, India
        defineLanguage(0x47, "gu");    // Gujarati
        defineLocale(0x0447, "gu_IN"); // Gujarati, India
        defineLanguage(0x48, "or");    // Oriya
        defineLocale(0x0448, "or_IN"); // Oriya, India
        defineLanguage(0x49, "ta");    // Tamil
        defineLocale(0x0449, "ta_IN"); // Tamil, India
        defineLanguage(0x4a, "te");    // Telugu
        defineLocale(0x044a, "te_IN"); // Telugu, India
        defineLanguage(0x4b, "kn");    // Kannada
        defineLocale(0x044b, "kn_IN"); // Kannada, India
        defineLanguage(0x4c, "ml");    // Malayalam
        defineLocale(0x044c, "ml_IN"); // Malayalam, India
        defineLanguage(0x4d, "as");    // Assamese
        defineLocale(0x044d, "as_IN"); // Assamese, India
        defineLanguage(0x4e, "mr");    // Marathi
        defineLocale(0x044e, "mr_IN"); // Marathi, India
        defineLanguage(0x4f, "sa");    // Sanskrit
        defineLocale(0x044f, "sa_IN"); // Sanskrit, India
        defineLanguage(0x50, "mn");    // Mongolian
        defineLocale(0x0850, "mn_CN"); // Mongolian, Mongolia
        defineLocale(0x0450, "mn_MN"); // Mongolian, Mongolia, Cyrillic
        defineLanguage(0x51, "bo");    // Tibetan
        defineLocale(0x0451, "bo_CN"); // Tibetan, PRC
        defineLanguage(0x52, "cy");    // Welsh
        defineLocale(0x0452, "cy_GB"); // Welsh, United Kingdom
        defineLanguage(0x53, "km");    // Khmer
        defineLocale(0x0453, "km_KH"); // Khmer, Cambodia
        defineLanguage(0x54, "lo");    // Laothian
        defineLocale(0x0454, "lo_LA"); // Lao, Lao PDR
        defineLanguage(0x55, "my");    // Burmese
        defineLanguage(0x56, "gl");    // Galician
        defineLocale(0x0456, "gl_ES"); // Galician, Spain
        defineLanguage(0x57, "kok");    // Konkani
        defineLocale(0x0457, "kok_IN"); // Konkani, India
        defineLanguage(0x59, "sd");    // Sindhi
        defineLanguage(0x5a, "syr");    // Syriac
        defineLocale(0x045a, "syr_SY"); // Syriac, Syria
        defineLanguage(0x5b, "si");    // Sinhala
        defineLocale(0x045b, "si_LK"); // Sinhala, Sri Lanka
        defineLanguage(0x5d, "iu");    // Inuktitut
        defineLanguage(0x5e, "am");    // Amharic
        defineLocale(0x045e, "am_ET"); // Amharic, Ethiopia
        defineLanguage(0x5f, "tmz");    // Tamazight
        defineLocale(0x085f, "tmz_DZ"); // Tamazight, Algeria, Latin
        defineLanguage(0x60, "ks");    // Kashmiri
        defineLanguage(0x61, "ne");    // Nepali
        defineLocale(0x0461, "ne_NP"); // Nepali, Nepal
        defineLanguage(0x62, "fy");    // Frisian
        defineLocale(0x0462, "fy_NL"); // Frisian, Netherlands
        defineLanguage(0x63, "ps");    // Pashto
        defineLocale(0x0463, "ps_AF"); // Pashto, Afghanistan
        defineLanguage(0x64, "fil");    // Filipino
        defineLocale(0x0464, "fil_PH"); // Filipino, Philippines
        defineLanguage(0x64, "tl");    // Tagalog
        defineLanguage(0x65, "dv");    // Divehi
        defineLocale(0x0465, "dv_MV"); // Divehi, Maldives
        defineLanguage(0x68, "ha");    // Hausa
        defineLocale(0x0468, "ha_NG"); // Hausa, Nigeria
        defineLanguage(0x6a, "yo");    // Yoruba
        defineLocale(0x046a, "yo_NG"); // Yoruba, Nigeria
        defineLanguage(0x6b, "quz");    // Quechua
        defineLocale(0x046b, "quz_BO"); // Quechua, Bolivia
        defineLocale(0x086b, "quz_EC"); // Quechua, Ecuador
        defineLocale(0x0c6b, "quz_PE"); // Quechua, Peru
        defineLanguage(0x6c, "ns");    // Sesotho sa Leboa/Northern Sotho
        defineLocale(0x046c, "ns_ZA"); // Sesotho sa Leboa/Northern Sotho, South Africa
        defineLanguage(0x6d, "ba");    // Bashkir
        defineLocale(0x046d, "ba_RU"); // Bashkir, Russia
        defineLanguage(0x6e, "lb");    // Luxembourgish
        defineLocale(0x046e, "lb_LU"); // Luxembourgish, Luxembourg
        defineLanguage(0x6f, "kl");    // Greenlandic
        defineLocale(0x046f, "kl_GL"); // Greenlandic, Greenland
        defineLanguage(0x70, "ig");    // Igbo
        defineLocale(0x0470, "ig_NG"); // Igbo, Nigeria
        defineLanguage(0x72, "om");    // Oromo (Afan, Galla)
        defineLanguage(0x73, "ti");    // Tigrinya
        defineLanguage(0x74, "gn");    // Guarani
        defineLanguage(0x76, "la");    // Latin
        defineLanguage(0x77, "so");    // Somali
        defineLanguage(0x78, "ii");    // Yi
        defineLocale(0x0478, "ii_CN"); // Yi, PRC
        defineLanguage(0x7a, "arn");    // Mapudungun
        defineLocale(0x047a, "arn_CL"); // Mapudungun, Chile
        defineLanguage(0x7c, "moh");    // Mohawk
        defineLocale(0x047c, "moh_CA"); // Mohawk, Canada
        defineLanguage(0x7e, "br");    // Breton
        defineLocale(0x047e, "br_FR"); // Breton, France
        defineLanguage(0x80, "ug");    // Uighur
        defineLocale(0x0480, "ug_CN"); // Uighur, PRC
        defineLanguage(0x81, "mi");    // Maori
        defineLocale(0x0481, "mi_NZ"); // Maori, New Zealand
        defineLanguage(0x82, "oc");    // Occitan
        defineLocale(0x0482, "oc_FR"); // Occitan, France
        defineLanguage(0x83, "co");    // Corsican
        defineLocale(0x0483, "co_FR"); // Corsican, France
        defineLanguage(0x84, "gsw");    // Alsatian
        defineLocale(0x0484, "gsw_FR"); // Alsatian, France
        defineLanguage(0x85, "sah");    // Yakut
        defineLocale(0x0485, "sah_RU"); // Yakut, Russia
        defineLanguage(0x86, "qut");    // K'iche
        defineLocale(0x0486, "qut_GT"); // K'iche, Guatemala
        defineLanguage(0x87, "rw");    // Kinyarwanda
        defineLocale(0x0487, "rw_RW"); // Kinyarwanda, Rwanda
        defineLanguage(0x88, "wo");    // Wolof
        defineLocale(0x0488, "wo_SN"); // Wolof, Senegal
        defineLanguage(0x8c, "gbz");    // Dari
        defineLocale(0x048c, "gbz_AF"); // Dari, Afghanistan
    }
}
