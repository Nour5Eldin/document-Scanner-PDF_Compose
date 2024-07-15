package com.noureldin.dscan.ui.screens.home

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.noureldin.dscan.R
import com.noureldin.dscan.data.models.PdfEntity
import com.noureldin.dscan.ui.screens.common.ErrorScreen
import com.noureldin.dscan.ui.screens.common.LoadingDialog
import com.noureldin.dscan.ui.screens.home.components.PdfLayout
import com.noureldin.dscan.ui.screens.home.components.RenameDeleteDialog
import com.noureldin.dscan.ui.theme.Purple40
import com.noureldin.dscan.utils.copyPdfFileToAppDirectory
import com.noureldin.dscan.utils.getFileSize
import com.noureldin.dscan.utils.showToast
import com.noureldin.dscan.ui.viewmodels.PdfViewModel
import com.noureldin.dscan.utils.Resource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(pdfViewModel: PdfViewModel) {
    LoadingDialog(pdfViewModel = pdfViewModel)
    RenameDeleteDialog(pdfViewModel = pdfViewModel)
    val activity = LocalContext.current as Activity
    val context = LocalContext.current
    val pdfState by pdfViewModel.pdfStateFlow.collectAsState()
    val message = pdfViewModel.message
    LaunchedEffect(Unit){
        message.collect{
            when(it){
                is Resource.Success -> {
                    context.showToast(it.data)
                }
                is Resource.Error -> {
                    context.showToast(it.message)
                }
                Resource.Idle -> {}
                Resource.Loading -> {}

            }
        }
    }
    val scannerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult())
    {result->
        if (result.resultCode == Activity.RESULT_OK){
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pdf?.let { pdf->
                Log.d("pdfName",pdf.uri.lastPathSegment.toString())

                val date = Date()
                val firstName = SimpleDateFormat(
                    "dd-MMM-yy HH:mm:ss",
                    Locale.getDefault()
                ).format(date) + ".pdf"

                copyPdfFileToAppDirectory(context,pdf.uri,firstName)
                val pdfEntity = PdfEntity(UUID.randomUUID().toString(),firstName,
                    getFileSize(context,firstName), lastModifiedTime = date)
                pdfViewModel.insertPdf(
                    pdfEntity
                )
            }
        }

    }
    val scanner = remember {
            GmsDocumentScanning.getClient(
                GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(true)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
            )
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold (
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = {
                Text(text = stringResource(id = R.string.app_name))
            },actions = {
                Switch(checked = pdfViewModel.isDarkMode, onCheckedChange = {
                    pdfViewModel.isDarkMode = it
                })
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
               scanner.getStartScanIntent(activity).addOnSuccessListener {
                   scannerLauncher.launch(
                       IntentSenderRequest.Builder(it).build()
                   )
               }.addOnFailureListener {
                   it.printStackTrace()
                   context.showToast(it.message.toString())
               }
            }, text = {
                Text(text = stringResource(id = R.string.app_name))
            }, icon = {
                Icon(painter = painterResource(id = R.drawable.ic_camera), contentDescription = "camera")
            }, containerColor = Purple40)
        }
    ){ paddingValue->
        pdfState.DisplayResult(onLoading = {
        }, onSuccess = {pdfList->
            if (pdfList.isEmpty()){
                ErrorScreen(message = "No Pdf")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValue)
                ) {
                    items(items = pdfList, key = { pdfEntity ->
                        pdfEntity.id
                    }) { pdfEntity ->
                        PdfLayout(pdfEntity = pdfEntity, pdfViewModel = pdfViewModel)

                    }

                }
            }
        }, onError = {
            ErrorScreen(message = it)
        })

    }
}