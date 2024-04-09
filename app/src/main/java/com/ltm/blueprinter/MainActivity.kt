package com.ltm.blueprinter

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.ltm.blueprinter.models.PDFBlock
import com.ltm.blueprinter.ui.theme.BluePrinterTheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID))

        val storagePermissionResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult?> {
                if (Environment.isExternalStorageManager())
                {
                    // Permission granted. Now resume your workflow.

                }
            }
        )

        storagePermissionResultLauncher.launch(intent)

        setContent {
            BluePrinterTheme {
                val pickFileLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        uri.let { readPDF(it) }
                    }
                }

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ElevatedButton(
                            onClick = {
                                doPrint()

                                pickFileLauncher.launch("application/pdf")
                            }
                        ) {
                            Text("Press to print")
                        }
                    }
                }
            }
        }
    }

    fun readPDF(uri: Uri): Unit{
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val rawPDFFormat = inputStream?.bufferedReader()?.use {
                it.readText()
            }
            val rawPDFMediaBox = rawPDFFormat?.substring(rawPDFFormat.indexOf("/MediaBox [") + 11, rawPDFFormat.indexOf("]"))
            val rawPDFStream = rawPDFFormat?.substring(rawPDFFormat.indexOf(">>\nstream"), rawPDFFormat.indexOf("endstream"))
            var rawPDFTextBlocks = rawPDFStream?.split("BT")
            rawPDFTextBlocks = rawPDFTextBlocks?.map { block ->
                if (block.contains("ET")){
                    block.substring(0 until block.indexOf("ET"))
                } else {
                    ""
                }
            }
            rawPDFTextBlocks = rawPDFTextBlocks?.filter { !it.isNullOrEmpty() }

            inputStream?.close()
            var pdfBlocks: ArrayList<PDFBlock> = ArrayList()

            rawPDFTextBlocks?.let {
                for(textBlock in rawPDFTextBlocks){
                    val blockAttributes = textBlock.split("\n")
                    var tempPDFBlock: PDFBlock = PDFBlock()

                    blockAttributes.let {

                        for(attribute in it){
                            if(attribute.endsWith("Tf")){
                                if(tempPDFBlock.fonts.isNullOrEmpty()){
                                    tempPDFBlock.fonts = attribute
                                }
                            } else if(attribute.endsWith("TL")){
                                if(tempPDFBlock.leading.isNullOrEmpty()){
                                    tempPDFBlock.leading = attribute
                                }
                            } else if(attribute.endsWith("rg")){
                                if(tempPDFBlock.color.isNullOrEmpty()){
                                    tempPDFBlock.color = attribute
                                }
                            } else if(attribute.endsWith("Td")){
                                var coords = attribute.split(" ")
                                var xCoord = coords[0].toDoubleOrNull()
                                var yCoord = coords[1].toDoubleOrNull()

                                if(xCoord != null && yCoord != null){
                                    tempPDFBlock.coords = doubleArrayOf(xCoord, yCoord)
                                }
                            } else if(attribute.endsWith("Tj")){
                                var tempText: String = attribute
                                tempText = tempText.substring(startIndex = 1, tempText.indexOf(") Tj"))
                                if(tempText.startsWith("\\")){
                                    tempText = tempText.substring(1)
                                }

                                if(tempPDFBlock.text.isNullOrEmpty()){
                                    tempPDFBlock.text = tempText
                                } else {
                                    tempPDFBlock.text = "${tempPDFBlock.text}${tempText}"
                                }
                            }
                        }
                    }

                    pdfBlocks.add(tempPDFBlock)
                }

                val pdfWidth = rawPDFMediaBox?.split(" ")?.get(2)?.toDoubleOrNull()
                val middlePoint = pdfWidth?.div(2)
                val threshold = pdfWidth?.div(5)

                var lastY = Double.MAX_VALUE
                var lastX = Double.NEGATIVE_INFINITY
                var line = ""
                var lines = ArrayList<String>()
                for(block in pdfBlocks){
                    if(block.text != null && !Regex("[[:ascii:]]+").matches(block.text!!)) {
                        continue
                    }

                    var yCoords = block.coords?.get(1)

                    if(lastY != yCoords){
                        if(!line.isEmpty()){
                            lines.add(line)
                        }
                        line = ""
                        lastX = Double.NEGATIVE_INFINITY
                        if (yCoords != null) {
                            lastY = yCoords
                        }
                    }
                    var prefix = ""

                    if(block.text?.equals("Bersih") == true){
                        val test = "uh"
                    }
                    if(block.coords?.get(0)!! - lastX > threshold!!){
                        if(block.coords?.get(0)!! < middlePoint!!){
                            prefix = "[L]"
                        } else {
                            prefix = "[R]"
                        }
                    }
                    lastX = block.coords?.get(0)!!

                    block.text?.let { text ->
                        line = "${line}${prefix}${text}"
                    }
                }

                var printValue = StringBuilder()
                for(text in lines){
                    printValue.append(text)
                    printValue.append("\n")
                    Log.d("LINES", text)
                }

                doPrint(printValue.toString())
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun doPrint(){
        /*val printer: EscPosPrinter = EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 57f, 32)

        printer.printFormattedText(
            "[L]123456789012345678901234[R]123456789012345678901234" +
                    "\n" +
                    "[L]123456789012345678901234[R]123456789012345678901234"
        )*/
    }

    fun doPrint(value: String){
        val printer: EscPosPrinter = EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 57f, 32)

        printer.printFormattedText(
            value
        )
    }
}