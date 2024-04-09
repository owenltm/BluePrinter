package com.ltm.blueprinter.models

data class PDFBlock(
    var fonts: String?,
    var leading: String?,
    var color: String?,
    var coords: DoubleArray?,
    var text: String?
) {
    constructor() : this(null, null, null, null, null) {}
}
