package com.N3looG.thingtracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.N3looG.thingtracker.R
import com.N3looG.thingtracker.model.LoanEntity
import com.N3looG.thingtracker.model.LoanType
import com.N3looG.thingtracker.ui.theme.*
import com.N3looG.thingtracker.utils.ImageUtils
import com.N3looG.thingtracker.viewmodel.LoanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    viewModel: LoanViewModel,
    initialType: LoanType = LoanType.LENT,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var personName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var loanType by remember { mutableStateOf(initialType) }
    var notes by remember { mutableStateOf("") }
    var imagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    BackHandler {
        onNavigateBack()
    }

    // Camera launcher (takes new photo directly with system camera)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null && imagePaths.size < 3) {
            val savedPath = ImageUtils.saveBitmapToInternalStorage(context, bitmap, 1800)
            if (savedPath != null) {
                imagePaths = imagePaths + savedPath
            }
        }
    }

    // Gallery launcher (picks image from phone gallery)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && imagePaths.size < 3) {
            val savedPath = ImageUtils.saveResizedImage(context, uri, 1800)
            if (savedPath != null) {
                imagePaths = imagePaths + savedPath
            }
        }
    }

    // Contact Picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) personName = name
                        }

                        if (idIndex != -1 && hasPhoneIndex != -1) {
                            val id = cursor.getString(idIndex)
                            val hasPhone = cursor.getString(hasPhoneIndex)
                            if (hasPhone == "1") {
                                try {
                                    context.contentResolver.query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        null,
                                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                        arrayOf(id),
                                        null
                                    )?.use { phoneCursor ->
                                        if (phoneCursor.moveToFirst()) {
                                            val pIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                            if (pIndex != -1) {
                                                val number = phoneCursor.getString(pIndex)
                                                if (!number.isNullOrBlank()) phoneNumber = number
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("AddLoanScreen", "Error reading phone number", e)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AddLoanScreen", "Error reading contact details", e)
            }
        }
    }

    // Permission launcher for READ_CONTACTS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        }
    }

    // Future due date selection (default 7 days in future)
    var dueDateMillis by remember { mutableStateOf(System.currentTimeMillis() + 86400000L * 7) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDateMillis
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            dueDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = TechPrimary, fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color.Gray, fontSize = 16.sp)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Photo Source Selection Dialog (Camera vs Gallery - Redesigned)
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = TechSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.dialog_photo_source_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TechTextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Camera Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceDialog = false
                                cameraLauncher.launch(null)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = TechSurface),
                        border = BorderStroke(1.5.dp, TechPrimary.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TechPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = TechPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(id = R.string.btn_take_photo),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechTextPrimary
                            )
                        }
                    }

                    // Option 2: Gallery Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceDialog = false
                                galleryLauncher.launch("image/*")
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = TechSurface),
                        border = BorderStroke(1.5.dp, TechSecondary.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TechSecondary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = TechSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(id = R.string.btn_pick_gallery),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechTextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text(
                        text = stringResource(id = R.string.btn_cancel),
                        color = TechTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = TechSurfaceCard,
            titleContentColor = TechTextPrimary,
            textContentColor = TechTextPrimary
        )
    }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.screen_add_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Înapoi"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector (Lent vs Borrowed)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = loanType == LoanType.LENT,
                    onClick = { loanType = LoanType.LENT },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(stringResource(id = R.string.tab_lent), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = loanType == LoanType.BORROWED,
                    onClick = { loanType = LoanType.BORROWED },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(stringResource(id = R.string.tab_borrowed), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(id = R.string.label_title), fontSize = 15.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Person Name Field with Contact Picker Button
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = {
                    Text(
                        if (loanType == LoanType.LENT)
                            stringResource(id = R.string.label_person_lent)
                        else
                            stringResource(id = R.string.label_person_borrowed),
                        fontSize = 15.sp
                    )
                },
                trailingIcon = {
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            contactPickerLauncher.launch(null)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = stringResource(id = R.string.btn_pick_contact),
                            tint = TechPrimary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Phone Number Field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text(stringResource(id = R.string.label_phone_number), fontSize = 15.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = TechPrimary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Due Date Picker Field
            OutlinedTextField(
                value = dateFormatter.format(Date(dueDateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(id = R.string.label_due_date), fontSize = 15.sp) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date", tint = TechPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp)
            )

            // Notes / Details Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(id = R.string.label_notes), fontSize = 15.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            // Multi-Photo Section (Max 3 photos)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (imagePaths.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        imagePaths.forEachIndexed { index, path ->
                            Box(
                                modifier = Modifier
                                    .size(95.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                val bitmap = remember(path) {
                                    try {
                                        BitmapFactory.decodeFile(path)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        imagePaths = imagePaths.filterIndexed { i, _ -> i != index }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(26.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Șterge",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (imagePaths.size < 3) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPhotoSourceDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TechSurfaceCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = TechSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (imagePaths.isEmpty())
                                    stringResource(id = R.string.label_add_photo)
                                else
                                    "Adaugă încă o poză (${imagePaths.size}/3)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TechTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank() && personName.isNotBlank()) {
                        val finalImagePath = imagePaths.joinToString("|").ifBlank { null }
                        viewModel.insertLoan(
                            LoanEntity(
                                title = title,
                                personName = personName,
                                phoneNumber = phoneNumber.ifBlank { null },
                                loanType = loanType,
                                startDate = System.currentTimeMillis(),
                                dueDate = dueDateMillis,
                                notes = notes.ifBlank { null },
                                imagePath = finalImagePath
                            )
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TechPrimary)
            ) {
                Text(
                    text = stringResource(id = R.string.btn_save),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}
