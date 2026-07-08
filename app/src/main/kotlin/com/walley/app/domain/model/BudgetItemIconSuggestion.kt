package com.walley.app.domain.model

/**
 * Keywords are matched whole-word, case-insensitively, against the item name. English and
 * Polish terms share the same list per icon so a name in either language resolves the same way,
 * with no language selection required. Order matters where terms could otherwise overlap (e.g.
 * "movie"/"cinema" resolve to [BudgetItemIcon.MOVIE], not the more generic [BudgetItemIcon.ENTERTAINMENT]).
 */
private val iconKeywords: List<Pair<BudgetItemIcon, List<String>>> = listOf(
    BudgetItemIcon.RENT to listOf("rent", "mortgage", "czynsz", "najem", "kredyt hipoteczny"),
    BudgetItemIcon.ELECTRICITY to listOf("electricity", "prąd", "energia elektryczna", "elektryczność"),
    BudgetItemIcon.INTERNET to listOf("internet", "wifi", "broadband"),
    BudgetItemIcon.PHONE to listOf("phone", "mobile", "telefon", "komórka"),
    BudgetItemIcon.TV to listOf("tv", "television", "telewizja", "telewizor"),
    BudgetItemIcon.BILLS to listOf("bill", "bills", "utility", "utilities", "rachunek", "rachunki", "opłaty"),
    BudgetItemIcon.ELECTRONICS to listOf("electronics", "electronic", "laptop", "computer", "elektronika", "komputer"),
    BudgetItemIcon.CURRENCY_EXCHANGE to listOf("currency", "exchange", "waluta", "wymiana walut", "kantor"),
    BudgetItemIcon.INSURANCE to listOf("insurance", "ubezpieczenie", "ubezpieczenia"),
    BudgetItemIcon.GIFT to listOf("gift", "gifts", "present", "presents", "prezent", "prezenty", "podarunek"),
    BudgetItemIcon.TRIP to listOf("trip", "flight", "travel", "podróż", "lot", "wycieczka"),
    BudgetItemIcon.VACATIONS to listOf("vacation", "vacations", "holiday", "holidays", "wakacje", "urlop"),
    BudgetItemIcon.TRANSPORTATION to listOf(
        "transport", "transportation", "bus", "train", "metro", "taxi", "uber",
        "autobus", "pociąg", "tramwaj", "taksówka", "komunikacja"
    ),
    BudgetItemIcon.SUBSCRIPTION to listOf("subscription", "subscriptions", "netflix", "spotify", "subskrypcja", "abonament"),
    BudgetItemIcon.MUSIC to listOf("music", "muzyka"),
    BudgetItemIcon.GAMES to listOf("games", "gaming", "gry", "granie"),
    BudgetItemIcon.MOVIE to listOf("movie", "movies", "cinema", "kino", "film"),
    BudgetItemIcon.ENTERTAINMENT to listOf("entertainment", "rozrywka"),
    BudgetItemIcon.EATING_OUT to listOf("restaurant", "restaurants", "dining", "restauracja", "jedzenie na mieście"),
    BudgetItemIcon.GROCERIES to listOf("groceries", "grocery", "supermarket", "zakupy spożywcze", "spożywcze"),
    BudgetItemIcon.CLOTHES to listOf("clothes", "clothing", "apparel", "ubrania", "odzież"),
    BudgetItemIcon.BARBER to listOf("barber", "haircut", "hairdresser", "fryzjer", "fryzjerka"),
    BudgetItemIcon.SPORT to listOf("sport", "gym", "fitness", "siłownia"),
    BudgetItemIcon.BOOKS to listOf("books", "book", "książki", "książka"),
    BudgetItemIcon.REPAIR to listOf("repair", "repairs", "naprawa", "naprawy", "serwis"),
    BudgetItemIcon.MEDICINE to listOf("medicine", "pharmacy", "lekarstwo", "leki", "apteka"),
    BudgetItemIcon.HEALTH to listOf("health", "doctor", "zdrowie", "lekarz"),
    BudgetItemIcon.PET to listOf("pet", "pets", "vet", "zwierzak", "zwierzę", "weterynarz"),
    BudgetItemIcon.KIDS to listOf("kids", "children", "dzieci", "dziecko"),
    BudgetItemIcon.RETIREMENT to listOf("retirement", "pension", "emerytura"),
    BudgetItemIcon.PIGGY_BANK to listOf("piggy bank", "skarbonka", "oszczędności"),
    BudgetItemIcon.FUEL to listOf("fuel", "petrol", "gas", "paliwo", "benzyna"),
    BudgetItemIcon.CAR to listOf("car", "vehicle", "samochód", "auto"),
    BudgetItemIcon.TAX to listOf("tax", "taxes", "podatek", "podatki")
)

private val polishDiacriticFold = mapOf(
    'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n',
    'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z'
)

/** Maps Polish diacritics to their plain-letter equivalents (e.g. "prąd" -> "prad"), so a name
 * typed without them (common when the user doesn't bother with a Polish keyboard layout) still
 * matches keywords written with proper accents, and vice versa. */
private fun String.foldPolishDiacritics(): String = map { polishDiacriticFold[it] ?: it }.joinToString("")

private val foldedIconKeywords: List<Pair<BudgetItemIcon, List<String>>> =
    iconKeywords.map { (icon, keywords) -> icon to keywords.map { it.foldPolishDiacritics() } }

/** Suggests a [BudgetItemIcon] for a free-text item [name], or null if no keyword matches. */
fun suggestIconForName(name: String): BudgetItemIcon? {
    val padded = " ${name.lowercase().foldPolishDiacritics()} "
    return foldedIconKeywords.firstOrNull { (_, keywords) -> keywords.any { padded.contains(" $it ") } }?.first
}
