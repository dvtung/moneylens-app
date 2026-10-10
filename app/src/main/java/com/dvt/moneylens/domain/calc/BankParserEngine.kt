package com.dvt.moneylens.domain.calc

import com.dvt.moneylens.domain.model.TransactionType

data class RawFinancialMessage(
    val senderOrPackage: String,
    val title: String = "",
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ParsedFinancialTransaction(
    val bankCode: String,
    val amountMinor: Long,
    val type: TransactionType,
    val balanceAfterMinor: Long? = null,
    val accountHint: String? = null,
    val counterparty: String? = null,
    val description: String? = null,
    val confidence: Float = 0.9f,
    val isCreditCard: Boolean = false,
    val cardLast4: String? = null
)

data class SupportedBank(
    val code: String,
    val name: String,
    val shortName: String,
    val colorHex: String,
    val isEnabled: Boolean = true
)

object BankParserEngine {

    val allSupportedBanks: List<SupportedBank> = listOf(
        SupportedBank("VCB", "Vietcombank (Ngoại thương)", "VCB", "#0A6B5C"),
        SupportedBank("TCB", "Techcombank (Kỹ thương)", "TCB", "#E53935"),
        SupportedBank("MB", "MBBank (Quân đội)", "MB", "#1E88E5"),
        SupportedBank("VPB", "VPBank (Thịnh vượng)", "VPB", "#43A047"),
        SupportedBank("BIDV", "BIDV (Đầu tư & Phát triển)", "BIDV", "#00897B"),
        SupportedBank("ACB", "ACB (Á Châu)", "ACB", "#1565C0"),
        SupportedBank("TPB", "TPBank (Tiên phong)", "TPB", "#8E24AA"),
        SupportedBank("VIETINBANK", "VietinBank (Công thương)", "VietinBank", "#003B70"),
        SupportedBank("AGRIBANK", "Agribank (Nông nghiệp & PTNT)", "Agribank", "#A82229"),
        SupportedBank("VIB", "VIB (Quốc tế)", "VIB", "#005BAA"),
        SupportedBank("SHB", "SHB (Sài Gòn - Hà Nội)", "SHB", "#F57C00"),
        SupportedBank("SHINHAN", "Shinhan Bank (Việt Nam)", "Shinhan", "#004685"),
        SupportedBank("HDBANK", "HDBank (Phát triển TP.HCM)", "HDBank", "#E51C24"),
        SupportedBank("MSB", "MSB (Hàng Hải)", "MSB", "#E65100"),
        SupportedBank("SEABANK", "SeABank (Đông Nam Á)", "SeABank", "#D32F2F"),
        SupportedBank("OCB", "OCB (Phương Đông)", "OCB", "#00897B"),
        SupportedBank("EXIMBANK", "Eximbank (Xuất Nhập Khẩu)", "Eximbank", "#0288D1"),
        SupportedBank("LPBANK", "LPBank (Bưu điện Liên Việt)", "LPBank", "#F57C00"),
        SupportedBank("BACABANK", "BAC A BANK (Bắc Á)", "BacABank", "#D84315"),
        SupportedBank("NAMABANK", "Nam A Bank (Nam Á)", "NamABank", "#FBC02D"),
        SupportedBank("NCB", "NCB (Quốc Dân)", "NCB", "#1976D2"),
        SupportedBank("KIENLONGBANK", "KienlongBank (Kiên Long)", "Kienlong", "#E64A19"),
        SupportedBank("BVBANK", "BVBank (Bản Việt / Digimi)", "BVBank", "#00796B"),
        SupportedBank("PVCOMBANK", "PVcomBank (Đại Chúng)", "PVcomBank", "#F57F17"),
        SupportedBank("ABBANK", "ABBank (An Bình)", "ABBank", "#00838F"),
        SupportedBank("VIETABANK", "VietABank (Việt Á)", "VietABank", "#C2185B"),
        SupportedBank("BAOVIET", "BAOVIET Bank (Bảo Việt)", "BaoViet", "#0277BD"),
        SupportedBank("SAIGONBANK", "SAIGONBANK (Sài Gòn)", "Saigonbank", "#2E7D32"),
        SupportedBank("COOPBANK", "Co-opBank (Hợp tác xã)", "Co-opBank", "#388E3C"),
        SupportedBank("HSBC", "HSBC Bank (Việt Nam)", "HSBC", "#DB0011"),
        SupportedBank("TIMO", "Timo Digital Bank", "Timo", "#7B1FA2"),
        SupportedBank("MOMO", "Ví điện tử MoMo", "MoMo", "#D81B60"),
        SupportedBank("ZALOPAY", "Ví điện tử ZaloPay", "ZaloPay", "#0288D1"),
        SupportedBank("VIETTELMONEY", "Viettel Money", "ViettelPay", "#EE0033"),
        SupportedBank("VNPAY", "Ví điện tử VNPAY", "VNPAY", "#005BAA"),
        SupportedBank("SHOPEEPAY", "Ví ShopeePay", "ShopeePay", "#EE4D2D")
    )

    fun parse(message: RawFinancialMessage, enabledBankCodes: Set<String>? = null): ParsedFinancialTransaction? {
        val fullText = "${message.title} ${message.body}".trim()
        if (fullText.isEmpty()) return null

        val bankCode = detectBank(message.senderOrPackage, fullText) ?: return null
        if (enabledBankCodes != null && !enabledBankCodes.contains(bankCode) && bankCode != "NGANHANG") {
            return null // Ngân hàng bị người dùng tắt trong cấu hình
        }

        // 1. Detect Amount and Direction (+ or -)
        val amountAndType = extractAmountAndType(fullText) ?: return null
        if (amountAndType.first < 1000L) return null // Loại bỏ số tiền < 1.000 VNĐ

        // 2. Extract Balance After (if present)
        val balanceAfter = extractBalanceAfter(fullText)

        // 3. Extract Account Hint (e.g. 4 last digits)
        val accountHint = extractAccountHint(fullText)

        // 4. Extract Description / Counterparty
        val counterparty = extractCounterparty(fullText)

        // 5. Detect Credit Card indicators
        val lowerText = fullText.lowercase()
        val isCredit = lowerText.contains("the tin dung") || lowerText.contains("thẻ tín dụng") ||
                lowerText.contains("credit") || lowerText.contains("qua the ") || lowerText.contains("qua thẻ ") ||
                lowerText.contains("the visa") || lowerText.contains("thẻ visa") || lowerText.contains("the master") ||
                lowerText.contains("the jcb") || lowerText.contains("han muc kha dung") || lowerText.contains("hạn mức khả dụng")

        return ParsedFinancialTransaction(
            bankCode = bankCode,
            amountMinor = amountAndType.first,
            type = amountAndType.second,
            balanceAfterMinor = balanceAfter,
            accountHint = accountHint,
            counterparty = counterparty,
            description = fullText.take(120),
            confidence = 0.95f,
            isCreditCard = isCredit,
            cardLast4 = if (isCredit) accountHint else null
        )
    }

    private fun detectBank(sender: String, text: String): String? {
        // TẦNG 1: Tra cứu chính xác theo Android Package ID từ BankPackageRegistry
        val fromPackage = BankPackageRegistry.detectBankByPackage(sender)
        if (fromPackage != null) {
            return fromPackage
        }

        // TẦNG 2: Nếu là ứng dụng tin nhắn SMS mặc định hoặc tin nhắn nhà mạng
        val isSms = BankPackageRegistry.isSmsPackage(sender) || (sender.length in 3..15 && !sender.contains("."))

        val s = (sender + " " + text).lowercase()
        val detectedFromText = when {
            s.contains("vietcombank") || s.contains("vcb") -> "VCB"
            s.contains("techcombank") || s.contains("tcb") -> "TCB"
            s.contains("mbbank") || s.contains("mb bank") || s.contains("mb") -> "MB"
            s.contains("vpbank") || s.contains("vpb") || s.contains("cake") -> "VPB"
            s.contains("bidv") -> "BIDV"
            s.contains("acb") -> "ACB"
            s.contains("tpbank") || s.contains("tpb") -> "TPB"
            s.contains("shbbank") || s.contains("shb") -> "SHB"
            s.contains("vibbank") || s.contains("vib") -> "VIB"
            s.contains("shinhan") -> "SHINHAN"
            s.contains("coopbank") || s.contains("co-opbank") || s.contains("co-op bank") -> "COOPBANK"
            s.contains("hsbc") -> "HSBC"
            s.contains("agribank") || s.contains("vba") -> "AGRIBANK"
            s.contains("vietinbank") || s.contains("vietin") || s.contains("ctg") -> "VIETINBANK"
            s.contains("hdbank") -> "HDBANK"
            s.contains("msb") -> "MSB"
            s.contains("seabank") -> "SEABANK"
            s.contains("ocb") -> "OCB"
            s.contains("eximbank") -> "EXIMBANK"
            s.contains("lpbank") || s.contains("lienviet") -> "LPBANK"
            s.contains("bac a bank") || s.contains("baca") -> "BACABANK"
            s.contains("nam a bank") || s.contains("namabank") -> "NAMABANK"
            s.contains("ncb") -> "NCB"
            s.contains("kienlongbank") || s.contains("kienlong") -> "KIENLONGBANK"
            s.contains("bvbank") || s.contains("ban viet") || s.contains("digimi") -> "BVBANK"
            s.contains("pvcombank") || s.contains("pvconnect") -> "PVCOMBANK"
            s.contains("abbank") || s.contains("ditizen") -> "ABBANK"
            s.contains("vietabank") -> "VIETABANK"
            s.contains("baoviet") -> "BAOVIET"
            s.contains("saigonbank") -> "SAIGONBANK"
            s.contains("timo") -> "TIMO"
            s.contains("momo") -> "MOMO"
            s.contains("zalopay") -> "ZALOPAY"
            s.contains("viettel money") || s.contains("viettelpay") -> "VIETTELMONEY"
            s.contains("vnpay") -> "VNPAY"
            s.contains("shopeepay") || s.contains("airpay") -> "SHOPEEPAY"
            else -> null
        }

        if (detectedFromText != null) {
            return detectedFromText
        }

        // TẦNG 3: Nếu là SMS và có từ khóa biến động tài chính rõ ràng -> gán là NGANHANG
        if (isSms && hasExplicitFinancialKeywords(s)) {
            return "NGANHANG"
        }

        return null // Không phải ngân hàng và không phải SMS tài chính -> Bỏ qua lập tức
    }

    private fun hasExplicitFinancialKeywords(s: String): Boolean {
        return (s.contains("biến động số dư") || s.contains("bien dong so du") ||
                s.contains("số dư tk") || s.contains("so du tk") ||
                s.contains("gd: +") || s.contains("gd: -") ||
                s.contains("chuyển tiền thành công") || s.contains("chuyen tien thanh cong") ||
                s.contains("thanh toán thành công") || s.contains("thanh toan thanh cong") ||
                s.contains("nạp tiền thành công") || s.contains("nap tien thanh cong"))
    }

    private fun extractAmountAndType(text: String): Pair<Long, TransactionType>? {
        // Mẫu số tiền chuẩn ngân hàng:
        // 1. Kèm đơn vị tiền tệ bắt buộc: VND, vnd, VNĐ, vnđ, đ, Đ
        val currencyRegex = Regex("""([+-]?\s*[\d]{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?)\s*(?:VND|vnd|VNĐ|vnđ|đ|Đ)""")
        // 2. Kèm tiền tố giao dịch ngân hàng: GD: [+-]..., Số tiền: ...
        val prefixRegex = Regex("""(?:GD|Giao dịch|Số tiền|So tien|ST)\s*[:]?\s*([+-]?\s*[\d]{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?)""", RegexOption.IGNORE_CASE)
        // 3. Kèm dấu cộng/trừ rõ ràng với ít nhất 1 dấu chấm/phẩy phân tách nghìn (+50.000, -100.000)
        val explicitSignRegex = Regex("""([+-]\s*[\d]{1,3}(?:[.,]\d{3})+)""")

        val match = currencyRegex.find(text)
            ?: prefixRegex.find(text)
            ?: explicitSignRegex.find(text)
            ?: return null

        var rawAmountStr = match.groupValues[1].replace(" ", "").trim()
        val isNegativeExplicit = rawAmountStr.startsWith("-") || text.contains("-$rawAmountStr")
        val isPositiveExplicit = rawAmountStr.startsWith("+") || text.contains("+$rawAmountStr")

        rawAmountStr = rawAmountStr.replace("+", "").replace("-", "")
        val cleanNumberStr = rawAmountStr.replace(".", "").replace(",", "")
        val rawNumber = cleanNumberStr.toLongOrNull() ?: return null

        // Chặn đứng các số nhỏ < 1.000đ (như 54đ từ pin, giây, số tin nhắn...)
        if (rawNumber < 1000L) {
            return null
        }

        val lower = text.lowercase()
        val isExpense = isNegativeExplicit || lower.contains("thanh toan") || lower.contains("thanh toán") ||
                lower.contains("tru") || lower.contains("trừ") ||
                lower.contains("chi tieu") || lower.contains("chi tiêu") ||
                lower.contains("rut tien") || lower.contains("rút tiền") ||
                lower.contains("chuyen tien di") || lower.contains("chuyển tiền")

        val type = if (isPositiveExplicit || lower.contains("nhan duoc") || lower.contains("nhận được") ||
                lower.contains("cong") || lower.contains("cộng") ||
                lower.contains("nap tien") || lower.contains("nạp tiền")) {
            TransactionType.INCOME
        } else if (isExpense) {
            TransactionType.EXPENSE
        } else {
            TransactionType.EXPENSE
        }

        return Pair(rawNumber, type)
    }

    private fun extractBalanceAfter(text: String): Long? {
        val balanceRegex = Regex("""(?:So du|SD|so du sau gd|so du:)\s*[:]?\s*([\d]{1,3}(?:[.,]\d{3})*)\s*(?:VND|vnd|đ|Đ)?""", RegexOption.IGNORE_CASE)
        val match = balanceRegex.find(text) ?: return null
        val clean = match.groupValues[1].replace(".", "").replace(",", "").trim()
        return clean.toLongOrNull()
    }

    private fun extractAccountHint(text: String): String? {
        val accRegex = Regex("""(?:TK|tai khoan|tài khoản|the|thẻ|card|credit)\s*(?:VISA|MASTER|JCB|tin dung|tín dụng)*\s*[:]?\s*([0-9xX*]{3,16})""", RegexOption.IGNORE_CASE)
        val match = accRegex.find(text) ?: return null
        val full = match.groupValues[1]
        return if (full.length >= 4) full.takeLast(4) else full
    }

    private fun extractCounterparty(text: String): String? {
        // Look for ND:, Noi dung:, tai:, den:
        val ndRegex = Regex("""(?:ND|Noi dung|tai|Den)\s*[:]\s*([^.,\n]+)""", RegexOption.IGNORE_CASE)
        val match = ndRegex.find(text)
        return match?.groupValues?.get(1)?.trim()?.take(40)
    }
}
