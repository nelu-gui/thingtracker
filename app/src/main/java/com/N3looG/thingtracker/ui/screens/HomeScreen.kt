package com.N3looG.thingtracker.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import com.N3looG.thingtracker.R
import com.N3looG.thingtracker.model.LoanEntity
import com.N3looG.thingtracker.model.LoanType
import com.N3looG.thingtracker.model.getImageList
import com.N3looG.thingtracker.ui.theme.*
import com.N3looG.thingtracker.utils.NotificationHelper
import com.N3looG.thingtracker.viewmodel.LoanViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LoanViewModel,
    onNavigateToAdd: (LoanType) -> Unit,
    onExitApp: () -> Unit
) {
    var selectedNavItem by remember { mutableIntStateOf(0) } // 0 = Home, 1 = History, 2 = Settings
    var selectedTab by remember { mutableStateOf(LoanType.LENT) }
    var searchQuery by remember { mutableStateOf("") }
    var filterUrgentOnly by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var photoViewerData by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    val lentLoansFlow = remember { viewModel.getActiveLoans(LoanType.LENT) }
    val borrowedLoansFlow = remember { viewModel.getActiveLoans(LoanType.BORROWED) }

    val lentLoans by lentLoansFlow.collectAsState(initial = emptyList())
    val borrowedLoans by borrowedLoansFlow.collectAsState(initial = emptyList())

    val currentTime = System.currentTimeMillis()
    val allLoans = lentLoans + borrowedLoans

    // Filter loans based on active view (Lent, Borrowed, or Overdue Scadente mode)
    val currentLoans = remember(allLoans, lentLoans, borrowedLoans, selectedTab, searchQuery, filterUrgentOnly, currentTime) {
        val baseList = if (filterUrgentOnly) {
            allLoans.filter { loan ->
                loan.dueDate != null && loan.dueDate < currentTime && !loan.isReturned
            }
        } else {
            if (selectedTab == LoanType.LENT) lentLoans else borrowedLoans
        }

        baseList.filter { loan ->
            loan.title.contains(searchQuery, ignoreCase = true) ||
                    loan.personName.contains(searchQuery, ignoreCase = true)
        }
    }

    // Calculate statistics (active only)
    val activeLentCount = lentLoans.count { !it.isReturned }
    val activeBorrowedCount = borrowedLoans.count { !it.isReturned }
    val overdueCount = allLoans.count { loan ->
        loan.dueDate != null && loan.dueDate < currentTime && !loan.isReturned
    }

    // Context for notifications
    val context = LocalContext.current

    // Trigger local reminder notifications for items due today or overdue
    LaunchedEffect(allLoans) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = todayStart + 86400000L - 1

        allLoans.filter { !it.isReturned && it.dueDate != null }.forEachIndexed { index, loan ->
            val due = loan.dueDate!!
            if (due <= todayEnd) {
                NotificationHelper.sendOverdueNotificationIfNeeded(
                    context = context,
                    loanId = loan.id,
                    itemTitle = loan.title,
                    personName = loan.personName,
                    notificationId = loan.id.toInt() + index
                )
            }
        }
    }

    // Intercept Back Press: If filtering urgent, clear filter; if on History/Settings, return to Home tab; if on Home, show Exit Confirmation
    BackHandler {
        if (filterUrgentOnly) {
            filterUrgentOnly = false
        } else if (selectedNavItem != 0) {
            selectedNavItem = 0
        } else {
            showExitDialog = true
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(id = R.string.dialog_exit_title), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.dialog_exit_message), fontSize = 16.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onExitApp()
                    }
                ) {
                    Text(stringResource(id = R.string.btn_confirm), color = TechDanger, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(id = R.string.btn_cancel), color = TechTextSecondary, fontSize = 16.sp)
                }
            },
            containerColor = TechSurfaceCard,
            titleContentColor = TechTextPrimary,
            textContentColor = TechTextSecondary
        )
    }

    // Confirmation dialog states
    var loanToDelete by remember { mutableStateOf<LoanEntity?>(null) }
    var loanToReturn by remember { mutableStateOf<LoanEntity?>(null) }

    // Full-Screen Photo Viewer Dialog
    if (photoViewerData != null) {
        PhotoViewerDialog(
            photoList = photoViewerData!!.first,
            initialIndex = photoViewerData!!.second,
            onDismiss = { photoViewerData = null }
        )
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }

    // User Manual Dialog
    if (showManualDialog) {
        UserManualDialog(
            onDismiss = { showManualDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (loanToDelete != null) {
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            title = { Text(stringResource(id = R.string.dialog_delete_title), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.dialog_delete_message), fontSize = 16.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        loanToDelete?.let { viewModel.deleteLoan(it) }
                        loanToDelete = null
                    }
                ) {
                    Text(stringResource(id = R.string.btn_delete), color = TechDanger, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text(stringResource(id = R.string.btn_cancel), color = TechTextSecondary, fontSize = 16.sp)
                }
            },
            containerColor = TechSurfaceCard,
            titleContentColor = TechTextPrimary,
            textContentColor = TechTextSecondary
        )
    }

    // Return Confirmation Dialog
    if (loanToReturn != null) {
        AlertDialog(
            onDismissRequest = { loanToReturn = null },
            title = { Text(stringResource(id = R.string.dialog_return_title), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.dialog_return_message), fontSize = 16.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        loanToReturn?.let {
                            val updated = it.copy(isReturned = true)
                            viewModel.updateLoan(updated)
                        }
                        loanToReturn = null
                    }
                ) {
                    Text(stringResource(id = R.string.btn_confirm), color = TechPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToReturn = null }) {
                    Text(stringResource(id = R.string.btn_cancel), color = TechTextSecondary, fontSize = 16.sp)
                }
            },
            containerColor = TechSurfaceCard,
            titleContentColor = TechTextPrimary,
            textContentColor = TechTextSecondary
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = TechTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechBackground,
                    titleContentColor = TechTextPrimary
                )
            )
        },
        floatingActionButton = {
            if (selectedNavItem == 0 && !filterUrgentOnly && currentLoans.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToAdd(selectedTab) },
                    containerColor = TechPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 12.dp, end = 16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.action_add),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                NavigationBar(
                    containerColor = TechSurfaceCard,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedNavItem == 0,
                        onClick = { selectedNavItem = 0 },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.dashboard_title)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TechPrimary,
                            selectedTextColor = TechPrimary,
                            unselectedIconColor = TechTextSecondary,
                            unselectedTextColor = TechTextSecondary,
                            indicatorColor = TechSurface
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNavItem == 1,
                        onClick = { selectedNavItem = 1 },
                        icon = { Icon(imageVector = Icons.Default.History, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_archive)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TechPrimary,
                            selectedTextColor = TechPrimary,
                            unselectedIconColor = TechTextSecondary,
                            unselectedTextColor = TechTextSecondary,
                            indicatorColor = TechSurface
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNavItem == 2,
                        onClick = { selectedNavItem = 2 },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_settings)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TechPrimary,
                            selectedTextColor = TechPrimary,
                            unselectedIconColor = TechTextSecondary,
                            unselectedTextColor = TechTextSecondary,
                            indicatorColor = TechSurface
                        )
                    )
                }
                AdBannerContainer()
            }
        },
        containerColor = TechBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedNavItem) {
                0 -> {
                    // Dashboard Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Spacer(modifier = Modifier.height(2.dp))

                        // Dashboard Statistics Cards Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = stringResource(id = R.string.stat_lent),
                                count = activeLentCount.toString(),
                                color = TechPrimary,
                                isSelected = (!filterUrgentOnly && selectedTab == LoanType.LENT),
                                onClick = {
                                    filterUrgentOnly = false
                                    selectedTab = LoanType.LENT
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = stringResource(id = R.string.stat_borrowed),
                                count = activeBorrowedCount.toString(),
                                color = TechSecondary,
                                isSelected = (!filterUrgentOnly && selectedTab == LoanType.BORROWED),
                                onClick = {
                                    filterUrgentOnly = false
                                    selectedTab = LoanType.BORROWED
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = stringResource(id = R.string.stat_overdue),
                                count = overdueCount.toString(),
                                color = TechDanger,
                                isSelected = filterUrgentOnly,
                                onClick = {
                                    filterUrgentOnly = !filterUrgentOnly
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(id = R.string.placeholder_search), color = TechTextSecondary, fontSize = 16.sp) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TechSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechSecondary,
                                unfocusedBorderColor = TechSurfaceCard,
                                focusedContainerColor = TechSurface,
                                unfocusedContainerColor = TechSurface,
                                focusedTextColor = TechTextPrimary,
                                unfocusedTextColor = TechTextPrimary
                            )
                        )

                        // Tabs for switching between LENT, BORROWED, and OVERDUE
                        val selectedTabIndex = when {
                            filterUrgentOnly -> 2
                            selectedTab == LoanType.LENT -> 0
                            else -> 1
                        }

                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            contentColor = when (selectedTabIndex) {
                                0 -> TechPrimary
                                1 -> TechSecondary
                                else -> TechDanger
                            },
                            indicator = { tabPositions ->
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(
                                            when (selectedTabIndex) {
                                                0 -> TechPrimary
                                                1 -> TechSecondary
                                                else -> TechDanger
                                            }
                                        )
                                )
                            },
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = {
                                    filterUrgentOnly = false
                                    selectedTab = LoanType.LENT
                                },
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.tab_lent),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (selectedTabIndex == 0) TechPrimary else TechTextSecondary
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = {
                                    filterUrgentOnly = false
                                    selectedTab = LoanType.BORROWED
                                },
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.tab_borrowed),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (selectedTabIndex == 1) TechSecondary else TechTextSecondary
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTabIndex == 2,
                                onClick = {
                                    filterUrgentOnly = true
                                },
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.stat_overdue),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (selectedTabIndex == 2) TechDanger else TechTextSecondary
                                    )
                                }
                            )
                        }

                        if (currentLoans.isEmpty()) {
                            EmptyStateView(
                                modifier = Modifier.fillMaxWidth(),
                                showAddButtonInside = !filterUrgentOnly,
                                onAddClick = { onNavigateToAdd(selectedTab) }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 110.dp)
                            ) {
                                items(currentLoans, key = { it.id }) { loan ->
                                    LoanCard(
                                        loan = loan,
                                        onReturnClick = {
                                            loanToReturn = loan
                                        },
                                        onDeleteClick = {
                                            loanToDelete = loan
                                        },
                                        onCallClick = {
                                            val uriString = if (!loan.phoneNumber.isNullOrBlank()) {
                                                "tel:${loan.phoneNumber}"
                                            } else {
                                                "tel:"
                                            }
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = uriString.toUri()
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        onPhotoClick = { list, index ->
                                            photoViewerData = Pair(list, index)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onDeleteClick = { loan -> loanToDelete = loan },
                        onPhotoClick = { list, index -> photoViewerData = Pair(list, index) }
                    )
                }
                2 -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onOpenLanguageDialog = { showLanguageDialog = true },
                        onOpenPrivacyDialog = { showPrivacyDialog = true },
                        onOpenManualDialog = { showManualDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoViewerDialog(
    photoList: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val currentPath = photoList.getOrNull(currentIndex)
            if (currentPath != null) {
                val bitmap = remember(currentPath) {
                    try {
                        BitmapFactory.decodeFile(currentPath)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 56.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentIndex + 1} / ${photoList.size}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Închide",
                        tint = Color.White
                    )
                }
            }

            // Navigation Arrows if multiple photos
            if (photoList.size > 1) {
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { currentIndex-- },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Anterior",
                            tint = Color.White
                        )
                    }
                }

                if (currentIndex < photoList.size - 1) {
                    IconButton(
                        onClick = { currentIndex++ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(16.dp)
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Următor",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdBannerContainer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            factory = { ctx ->
                com.google.android.gms.ads.AdView(ctx).apply {
                    setAdSize(com.google.android.gms.ads.AdSize.BANNER)
                    // Official Google AdMob Test Ad Unit ID for banners
                    adUnitId = "ca-app-pub-3940256099942544/6300978111"
                    loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                }
            }
        )
    }
}

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "" to (stringResource(id = R.string.lang_system_default) to ""),
        "cs" to (stringResource(id = R.string.lang_cs) to "🇨🇿"),
        "da" to (stringResource(id = R.string.lang_da) to "🇩🇰"),
        "de" to (stringResource(id = R.string.lang_de) to "🇩🇪"),
        "en" to (stringResource(id = R.string.lang_en) to "🇬🇧"),
        "es" to (stringResource(id = R.string.lang_es) to "🇪🇸"),
        "fi" to (stringResource(id = R.string.lang_fi) to "🇫🇮"),
        "fr" to (stringResource(id = R.string.lang_fr) to "🇫🇷"),
        "it" to (stringResource(id = R.string.lang_it) to "🇮🇹"),
        "nl" to (stringResource(id = R.string.lang_nl) to "🇳🇱"),
        "no" to (stringResource(id = R.string.lang_no) to "🇳🇴"),
        "pl" to (stringResource(id = R.string.lang_pl) to "🇵🇱"),
        "pt" to (stringResource(id = R.string.lang_pt) to "🇵🇹"),
        "ro" to (stringResource(id = R.string.lang_ro) to "🇷🇴"),
        "sl" to (stringResource(id = R.string.lang_sl) to "🇸🇮"),
        "sv" to (stringResource(id = R.string.lang_sv) to "🇸🇪")
    )

    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.dialog_language_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                languages.forEach { (code, item) ->
                    val (name, flag) = item
                    val labelText = if (flag.isNotEmpty()) "$name $flag" else name
                    val isSelected = if (code.isEmpty()) currentLocale.isEmpty() else currentLocale.startsWith(code)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val locales = if (code.isEmpty()) {
                                    LocaleListCompat.getEmptyLocaleList()
                                } else {
                                    LocaleListCompat.forLanguageTags(code)
                                }
                                AppCompatDelegate.setApplicationLocales(locales)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = TechPrimary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = labelText,
                            fontSize = 16.sp,
                            color = if (isSelected) TechPrimary else TechTextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.btn_cancel), color = TechTextSecondary, fontSize = 16.sp)
            }
        },
        containerColor = TechSurfaceCard,
        titleContentColor = TechTextPrimary,
        textContentColor = TechTextPrimary
    )
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.privacy_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.privacy_body),
                    fontSize = 15.sp,
                    color = TechTextSecondary,
                    lineHeight = 22.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.btn_cancel), color = TechPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        containerColor = TechSurfaceCard,
        titleContentColor = TechTextPrimary,
        textContentColor = TechTextPrimary
    )
}

@Composable
fun UserManualDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.manual_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.manual_body),
                    fontSize = 15.sp,
                    color = TechTextSecondary,
                    lineHeight = 22.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.btn_cancel), color = TechPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        containerColor = TechSurfaceCard,
        titleContentColor = TechTextPrimary,
        textContentColor = TechTextPrimary
    )
}

@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.empty_list),
    showAddButtonInside: Boolean = false,
    onAddClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TechSurfaceCard),
        border = BorderStroke(1.2.dp, TechBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = TechSurface,
                border = BorderStroke(1.5.dp, TechSecondary.copy(alpha = 0.5f)),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = TechSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TechTextPrimary,
                textAlign = TextAlign.Center
            )

            if (showAddButtonInside) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechPrimary),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.action_add),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TechSurfaceCard),
        border = if (isSelected) BorderStroke(2.dp, color) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = count,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = if (isSelected) TechTextPrimary else TechTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LoanCard(
    loan: LoanEntity,
    onReturnClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCallClick: () -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit = { _, _ -> }
) {
    val currentTime = System.currentTimeMillis()
    val dueDate = loan.dueDate

    // Calculate time status and badge
    val statusInfo = remember(dueDate, currentTime, loan.isReturned) {
        if (loan.isReturned) {
            Pair(TechPrimary, R.string.badge_returned)
        } else if (dueDate == null) {
            Pair(TechTextSecondary, R.string.no_due_date)
        } else {
            val diffMs = dueDate - currentTime
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
            if (diffMs < 0) {
                Pair(TechDanger, R.string.overdue_by_days)
            } else if (diffDays == 0L) {
                Pair(TechWarning, R.string.due_today)
            } else {
                Pair(TechPrimary, R.string.due_in_days)
            }
        }
    }

    val badgeColor = statusInfo.first
    val stringResId = statusInfo.second

    val badgeText = when (stringResId) {
        R.string.badge_returned -> stringResource(id = R.string.badge_returned)
        R.string.no_due_date -> stringResource(id = R.string.no_due_date)
        R.string.due_today -> stringResource(id = R.string.due_today)
        R.string.overdue_by_days -> {
            val diffDays = kotlin.math.abs(TimeUnit.MILLISECONDS.toDays(dueDate!! - currentTime))
            String.format(Locale.getDefault(), "Depășit cu %d zile", diffDays)
        }
        R.string.due_in_days -> {
            val diffDays = TimeUnit.MILLISECONDS.toDays(dueDate!! - currentTime)
            String.format(Locale.getDefault(), "Scadent în %d zile", diffDays)
        }
        else -> ""
    }

    // Calculate progress
    val progress = remember(loan.startDate, dueDate) {
        if (dueDate == null || dueDate <= loan.startDate) 1f
        else {
            val totalDuration = (dueDate - loan.startDate).toFloat()
            val elapsed = (currentTime - loan.startDate).toFloat()
            (elapsed / totalDuration).coerceIn(0f, 1f)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (loan.isReturned) Color(0xFF0D1322) else TechSurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (loan.isReturned) TechTextSecondary else TechTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TechSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (!loan.phoneNumber.isNullOrBlank()) "${loan.personName} (${loan.phoneNumber})" else loan.personName,
                            fontSize = 16.sp,
                            color = TechTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!loan.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = loan.notes,
                            fontSize = 15.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action buttons column
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Call/Contact Action
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier
                            .size(38.dp)
                            .background(TechSurface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Contact",
                            tint = TechSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Mark as Returned Button
                    if (!loan.isReturned) {
                        IconButton(
                            onClick = onReturnClick,
                            modifier = Modifier
                                .size(38.dp)
                                .background(TechSurface, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(id = R.string.btn_mark_returned),
                                tint = TechPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(38.dp)
                            .background(TechSurface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.btn_delete),
                            tint = TechDanger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            val photoList = remember(loan.imagePath) { loan.getImageList() }
            if (photoList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    photoList.forEachIndexed { index, path ->
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPhotoClick(photoList, index) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TechSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (loan.isReturned) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = TechPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = badgeText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor
                        )
                    }
                }

                val formattedDate = loan.dueDate?.let {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                } ?: "N/A"

                Text(
                    text = "Scadență: $formattedDate",
                    fontSize = 13.sp,
                    color = TechTextSecondary
                )
            }

            if (dueDate != null && !loan.isReturned) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = badgeColor,
                    trackColor = TechSurface,
                )
            }
        }
    }
}
