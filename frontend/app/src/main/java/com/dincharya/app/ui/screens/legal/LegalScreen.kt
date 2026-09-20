package com.dincharya.app.ui.screens.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dincharya.app.R

/**
 * A quiet, readable legal page: back arrow + title, then heading/body
 * sections. Used for the Privacy Policy and Terms of Use, both reachable
 * from Settings. These are plain in-app pages (not web links) so they work
 * offline, like everything else in Dincharya.
 */
@Composable
fun LegalScreen(
    title: String,
    intro: String,
    sections: List<Pair<String, String>>,
    navController: NavController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // ---- Header: back arrow + title ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.onboarding_back),
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            intro,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        sections.forEach { (heading, body) ->
            Text(heading, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** The Privacy Policy, backed entirely by string resources. */
@Composable
fun PrivacyScreen(navController: NavController) {
    LegalScreen(
        title = stringResource(R.string.privacy_title),
        intro = stringResource(R.string.privacy_intro),
        sections = listOf(
            stringResource(R.string.privacy_data_title) to
                stringResource(R.string.privacy_data_body),
            stringResource(R.string.privacy_permissions_title) to
                stringResource(R.string.privacy_permissions_body),
            stringResource(R.string.privacy_accounts_title) to
                stringResource(R.string.privacy_accounts_body),
            stringResource(R.string.privacy_deletion_title) to
                stringResource(R.string.privacy_deletion_body),
            stringResource(R.string.privacy_contact_title) to
                stringResource(R.string.privacy_contact_body),
        ),
        navController = navController,
    )
}

/** The Terms of Use, backed entirely by string resources. */
@Composable
fun TermsScreen(navController: NavController) {
    LegalScreen(
        title = stringResource(R.string.terms_title),
        intro = stringResource(R.string.terms_intro),
        sections = listOf(
            stringResource(R.string.terms_app_title) to
                stringResource(R.string.terms_app_body),
            stringResource(R.string.terms_responsibility_title) to
                stringResource(R.string.terms_responsibility_body),
            stringResource(R.string.terms_warranty_title) to
                stringResource(R.string.terms_warranty_body),
            stringResource(R.string.terms_changes_title) to
                stringResource(R.string.terms_changes_body),
        ),
        navController = navController,
    )
}
