package com.N3looG.thingtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.N3looG.thingtracker.R
import com.N3looG.thingtracker.model.LoanEntity
import com.N3looG.thingtracker.viewmodel.LoanViewModel

@Composable
fun HistoryScreen(
    viewModel: LoanViewModel,
    onDeleteClick: (LoanEntity) -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit = { _, _ -> },
) {
    val returnedLoans by viewModel.returnedLoans.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.nav_archive),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (returnedLoans.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.weight(1f),
                title = stringResource(id = R.string.empty_list)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 125.dp)
            ) {
                items(returnedLoans, key = { it.id }) { loan ->
                    LoanCard(
                        loan = loan,
                        onReturnClick = {},
                        onDeleteClick = { onDeleteClick(loan) },
                        onCallClick = {},
                        onPhotoClick = onPhotoClick
                    )
                }
            }
        }
    }
}
