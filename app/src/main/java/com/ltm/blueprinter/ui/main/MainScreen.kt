package com.ltm.blueprinter.ui.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.provider.ContactsContract.Data
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ltm.blueprinter.R
import com.ltm.blueprinter.ui.theme.BluePrinterTheme

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(factory = MainViewModelFactory())
){
    val context = LocalContext.current as Activity

    MainScreenView(
        printContent = "",
        value = viewModel.value.collectAsState().value,
        onImportFromSources = {sources, values -> viewModel.importFromSource(sources, values) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenView(
    printContent: String,
    value: String? = null,
    onImportFromSources: (Sources, Map<String, String>) -> Unit = { _, _ -> }
){
    val context = LocalContext.current as Activity
    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = {resultUri ->
            Log.d("SELECTED", resultUri?.encodedPath.toString())
            resultUri?.let {uri ->
                val path = uri.path
                val idStr = path!!.substring(path!!.lastIndexOf('/') + 1)
                val id = idStr.toInt()

                var cursor = context.contentResolver.query(
                    Data.CONTENT_URI,
                    MainViewModel.CONTACTS_PROJECTION,
                    "${Data.CONTACT_ID} = ${id}",
                    null,
                    null
                )

                Log.d("SELECTED", "Rows: ${cursor?.count}, Columns: ${cursor?.columnCount}")

                val contactMapKeys = mapOf(
                    Pair("name", "name"),
                    Pair("phone_v2", "phone"),
                    Pair("postal-address_v2", "address"),
                )

                val contactMap = mutableMapOf<String, String>()

                cursor?.moveToFirst()
                val columnCount = cursor?.columnCount?.minus(1) ?: 0
                while (cursor?.isAfterLast != true){
                    var sb = StringBuilder()
                    for (i in 0..columnCount){
                        if("mimetype".equals(cursor?.columnNames?.get(i))){
                            val mimeKey = cursor?.getString(i)?.substring(cursor.getString(i)!!.lastIndexOf('/') + 1)

                            if(contactMapKeys.containsKey(mimeKey)){
                                var dataIndex = cursor?.getColumnIndex("data1")

                                contactMap[contactMapKeys.get(mimeKey) ?: ""] = cursor?.getString(dataIndex ?: 0) ?: ""
                            }
                        }

                        sb.append("${cursor?.columnNames?.get(i)}:")
                        sb.append(cursor?.getString(i))
                        sb.append(" ")
                    }
                    Log.d("SELECTED", sb.toString())
                    cursor?.moveToNext()
                }
                cursor.close()

                onImportFromSources(Sources.CONTACTS, contactMap)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BluePrinter",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    )
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(right = 16.dp),
                modifier = Modifier,
            )
        },
        bottomBar = {
            BottomAppBar(
                windowInsets = WindowInsets(left = 16.dp, right = 16.dp),
                actions = {
                    IconButton(
                        onClick = {
                            /*onImportFromSources(Sources.CONTACTS)*/
                            if (hasContactPermission(context)) {
                                // if permission granted open intent to pick contact/
                                pickContactLauncher.launch()
                            } else {
                                // if permission not granted requesting permission .
                                requestContactPermission(context, context)
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = "Import",

                            )
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {  },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.loweredElevation(),
                    ) {
                        Icon(Icons.Filled.Add, "Print")
                    }
                }
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(it)
        ) {
            value?.let { 
                Text(text = it)
            }
        }
    }
}

fun hasContactPermission(context: Context): Boolean {
    // on below line checking if permission is present or not.
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED;
}

fun requestContactPermission(context: Context, activity: Activity) {
    // on below line if permission is not granted requesting permissions.
    if (!hasContactPermission(context)) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_CONTACTS), 1)
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "LoginScreenLight"
)
@Composable
fun MainScreenPreviewLight() {
    BluePrinterTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreenView(
                printContent = "TEST"
            )
        }
    }
}