package com.dincharya.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.app.SettingsStore
import com.dincharya.app.ui.components.RuleCard
import com.dincharya.app.ui.components.SectionHeader
import com.dincharya.app.ui.navigation.Screen

/** App version shown in About â€” keep in sync with app/build.gradle.kts. */
private const val APP_VERSION = "0.1.0"

/**
 * Settings screen: appearance, reminders, and the privacy promise.
 *
 * Layout language matches the rest of the app: grouped sections opened by
 * a hairline kicker, full-width rows with a leading thin-stroke icon,
 * generous spacing. Every row is a single, self-explanatory control.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    // Plain viewModel(): the default factory constructs AndroidViewModel(Application)
    // subclasses automatically, so no explicit factory is needed.
    val viewModel: SettingsViewModel = viewModel()

    // Local mirrors so the switches feel instant while persisting.
    var themeMode by remember { mutableStateOf(viewModel.themeMode) }
    var notifications by remember { mutableStateOf(viewModel.notificationsEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.settings_header), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        // ---- Appearance ----
        SectionHeader(stringResource(R.string.settings_appearance))
        Spacer(Modifier.height(8.dp))
        ThemeOption(
            icon = Icons.Outlined.Devices,
            label = stringResource(R.string.settings_theme_system),
            selected = themeMode == SettingsStore.THEME_SYSTEM,
        ) { themeMode = SettingsStore.THEME_SYSTEM; viewModel.setTheme(SettingsStore.THEME_SYSTEM) }
        ThemeOption(
            icon = Icons.Outlined.LightMode,
            label = stringResource(R.string.settings_theme_light¤°(€€€€€€€€€€€Í•±•Ñ•€ôÑ¡•µ•5½‘”€ôôM•ÑÑ¥¹ÍMÑ½É”¹Q!5}1%!P°(€€€€€€€€¤ìÑ¡•µ•5½‘”€ôM•ÑÑ¥¹ÍMÑ½É”¹Q!5}1%!PìÙ¥•Ý5½‘•°¹Í•ÑQ¡•µ”¡M•ÑÑ¥¹ÍMÑ½É”¹Q!5}1%!P¤ô(€€€€€€€Q¡•µ•=ÁÑ¥½¸ (€€€€€€€€€€€¥½¸€ô%½¹Ì¹=ÕÑ±¥¹•¹…É­5½‘”°(€€€€€€€€€€€±…‰•°€ôÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Í•ÑÑ¥¹Í}Ñ¡•µ•}‘…É¬¤°(€€€€€€€€€€€Í•±•Ñ•€ôÑ¡•µ•5½‘”€ôôM•ÑÑ¥¹ÍMÑ½É”¹Q!5}I,°(€€€€€€€€¤ìÑ¡•µ•5½‘”€ôM•ÑÑ¥¹ÍMÑ½É”¹Q!5}HììÙ¥•Ý5½‘•°¹Í•ÑQ¡•µ”¡M•ÑÑ¥¹ÍMÑ½É”¹Q!5}I,¤ô(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð ÈÐ¹‘À¤¤((€€€€€€€€¼¼€´´´´I•µ¥¹‘•ÉÌ€´´´´(€€€€€€€M•Ñ¥½¹!•…‘•È¡ÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Í•ÑÑ¥¹Í}¹½Ñ¥™¥…Ñ¥½¹Ì¤¤(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð à¹‘À¤¤(€€€€€€€I½Ü (€€€€€€€€€€€Ù•ÉÑ¥…±±¥¹µ•¹Ð€ô±¥¹µ•¹Ð¹•¹Ñ•ÉY•ÉÑ¥…±±ä°(€€€€€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È(€€€€€€€€€€€€€€€€¹™¥±±5…á]¥‘Ñ  ¤(€€€€€€€€€€€€€€€€¹Á…‘‘¥¹œ¡Ù•ÉÑ¥…°€ô€Ø¹‘À¤°(€€€€€€€€¤ì(€€€€€€€€€€€%½¸ (€€€€€€€€€€€€€€€%½¹Ì¹=ÕÑ±¥¹•¹9½Ñ¥™¥…Ñ¥½¹Ì°(€€€€€€€€€€€€€€€½¹Ñ•¹Ñ•ÍÉ¥ÁÑ¥½¸€ô¹Õ±°°(€€€€€€€€€€€€€€€Ñ¥¹Ð€ô5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…•Y…É¥…¹Ð°(€€€€€€€€€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È¹Í¥é” ÈÈ¹‘À¤°(€€€€€€€€€€€€¤(€€€€€€€€€€€MÁ…•È¡5½‘¥™¥•È¹Í¥é” ÄÐ¹‘À¤¤(€€€€€€€€€€€½±Õµ¸¡5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤¤ì(€€€€€€€€€€€€€€€Q•áÐ¡ÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Í•ÑÑ¥¹Í}¹½Ñ¥™¥…Ñ¥½¹Í}•¹…‰±•¤°(€€€€€€€€€€€€€€€€€€€ÍÑå±”€ô5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹‰½‘å1…É”¤(€€€€€€€€€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð È¹‘À¤¤(€€€€€€€€€€€€€€€Q•áÐ (€€€€€€€€€€€€€€€€€€€ÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Í•ÑÑ¥¹Í}¹½Ñ¥™¥…Ñ¥½¹Í}¡¥¹Ð¤°(€€€€€€€€€€€€€€€€€€€ÍÑå±”€ô5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹‰½‘åMµ…±°°(€€€€€€€€€€€€€€€€€€€½±½È€ô5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…•Y…É¥…¹Ð°(€€€€€€€€€€€€€€€€¤(€€€€€€€€€€€ô(€€€€€€€€€€€MÁ…•È¡5½‘¥™¥•È¹Í¥é” ÄØ¹‘À¤¤(€€€€€€€€€€€MÝ¥Ñ  (€€€€€€€€€€€€€€€¡•­•€ô¹½Ñ¥™¥…Ñ¥½¹Ì°(€€€€€€€€€€€€€€€½¹¡•­•‘¡…¹”€ôì(€€€€€€€€€€€€€€€€€€€¹½Ñ¥™¥…Ñ¥½¹Ì€ô¥Ð(€€€€€€€€€€€€€€€€€€€Ù¥•Ý5½‘•°¹Í•Ñ9½Ñ¥™¥…Ñ¥½¹Í¹…‰±•¡¥Ð¤(€€€€€€€€€€€€€€€ô°(€€€€€€€€€€€€¤(€€€€€€€ô(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð ÄØ¹‘À¤¤(€€€€€€€Q•áÐ (€€€€€€€€€€€ÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Í•ÑÑ¥¹Í}¡É½¹½ÑåÁ”°Ù¥•Ý5½‘•°¹¡É½¹½ÑåÁ”¤°(€€€€€€€€€€€ÍÑå±”€ô5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹‰½‘å5•‘¥Õ´°(€€€€€€€€€€€½±½È€ô5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…•Y…É¥…¹Ð°(€€€€€€€€¤(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð ÈÐ¹‘À¤¤((€€€€€€€€¼¼€´´´´‰½ÕÐ€´´´´(€€€€€€€M•Ñ¥½¹!•…‘•È¡ÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Í•ÑÑ¥¹Í}…‰½ÕÐ¤¤(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð ÄÈ¹‘À¤¤(€€€€€€€IÕ±•…Éì(€€€€€€€€€€€Q•áÐ (€€€€€€€€€€€€€€€€‰¥¹¡…Éå„ƒŠPÑ¡”Í¡•‘Õ±”Ñ¡…Ð±•…É¹Ìå½Ô¸ˆ°(€€€€€€€€€€€€€€€ÍÑå±”€ô5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹Ñ¥Ñ±•5•‘¥Õ´°(€€€€€€€€€€€€¤(€€€€€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð à¹‘À¤¤(€€€€€€€€€€€Q•áÐ (€€€€€€€€€€€€€€€ÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Í•ÑÑ¥¹Í}ÁÉ¥Ù…å}±¥¹”¤°(€€€€€€€€€€€€€€€ÍÑå±”€ô5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹‰½‘å5•‘¥Õ´°(€€€€€€€€€€€€€€€½±½È€ô5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…•Y…É¥…¹Ð°(€€€€€€€€€€€€¤(€€€€€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð à¹‘À¤¤(€€€€€€€€€€€Q•áÐ (€€€€€€€€€€€€€€€ÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Í•ÑÑ¥¹Í}Ù•ÉÍ¥½¸°AA}YIM%=8¤°(€€€€€€€€€€€€€€€ÍÑå±”€ô5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹‰½‘åMµ…±°°(€€€€€€€€€€€€€€€½±½È€ô5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…•Y…É¥…¹Ð°(€€€€€€€€€€€€¤(€€€€€€€ô(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð ÄÈ¹‘À¤¤(€€€€€€€M•ÑÑ¥¹Í1¥¹¬ (€€€€€€€€€€€¥½¸€ô%½¹Ì¹=ÕÑ±¥¹•¹AÉ¥Ù…åQ¥À°(€€€€€€€€€€€±…‰•°€ôÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹ÁÉ¥Ù…å}Ñ¥Ñ±”¤°(€€€€€€€€¤ì¹…Ù½¹ÑÉ½±±•È¹¹…Ù¥…Ñ”¡MÉ••¸¹AÉ¥Ù…ä¹É½ÕÑ”¤ô(€€€€€€€M•ÑÑ¥¹Í1¥¹¬ (€€€€€€€€€€€¥½¸€ô%½¹Ì¹=ÕÑ±¥¹•¹•ÍÉ¥ÁÑ¥½¸°(€€€€€€€€€€€±…‰•°€ôÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Ñ•ÉµÍ}Ñ¥Ñ±”¤°(€€€€€€€€¤ì¹…Ù½¹ÑÉ½±±•È¹¹…Ù¥…Ñ”¡MÉ••¸¹Q•ÉµÌ¹É½ÕÑ”¤ô(€€€€€€€M•ÑÑ¥¹Í1¥¹¬ (€€€€€€€€€€€¥½¸€ô%½¹Ì¹=ÕÑ±¥¹•¹I•Á±…ä°(€€€€€€€€€€€±…‰•°€ôÍÑÉ¥¹I•Í½ÕÉ”¡H¹ÍÑÉ¥¹œ¹Í•ÑÑ¥¹Í}É•Á±…å}½¹‰½…É‘¥¹œ¤°(€€€€€€€€¤ì(€€€€€€€€€€€Ù¥•Ý5½‘•°¹É•Á±…å=¹‰½…É‘¥¹œ ¤(€€€€€€€€€€€¹…Ù½¹ÑÉ½±±•È¹¹…Ù¥…Ñ”¡MÉ••¸¹=¹‰½…É‘¥¹œ¹É½ÕÑ”¤ì(€€€€€€€€€€€€€€€Á½ÁUÁQ¼¡MÉ••¸¹Q½‘…ä¹É½ÕÑ”¤ì¥¹±ÕÍ¥Ù”€ôÑÉÕ”ô(€€€€€€€€€€€ô(€€€€€€€ô(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹¡•¥¡Ð ÌÈ¹‘À¤¤(€€€ô)ô((¼¨¨(€¨=¹”Í•±•Ñ…‰±”Ñ¡•µ”É½Üè±•…‘¥¹œ¥½¸°±…‰•°°µ½¹½¡É½µ”Í•±•Ñ¥½¸‘½Ð¸(€¨™¥±±•‘½Ðµ…É­ÌÑ¡”…Ñ¥Ù”Ñ¡•µ”°…¸½ÕÑ±¥¹•É¥¹œÑ¡”½Ñ¡•ÉÌƒŠP(€¨Í¡…Á”°¹•Ù•È½±½ÕÈ°…ÉÉ¥•ÌÑ¡”ÍÑ…Ñ”¸Õ±°É½Ü¥ÌÑ…ÁÁ…‰±”¸(€¨¼)½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸Q¡•µ•=ÁÑ¥½¸ (€€€¥½¸è%µ…•Y•Ñ½È°(€€€±…‰•°èMÑÉ¥¹œ°(€€€Í•±•Ñ•è	½½±•…¸°(€€€½¹±¥¬è€ ¤€´øU¹¥Ð°(¤ì(€€€I½Ü (€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È(€€€€€€€€€€€€¹™¥±±5…á]¥‘Ñ  ¤(€€€€€€€€€€€€¹±¥À¡I½Õ¹‘•‘½É¹•ÉM¡…Á” à¹‘À¤¤(€€€€€€€€€€€€¹±¥­…‰±”¡½¹±¥¬€ô½¹±¥¬¤(€€€€€€€€€€€€¹Á…‘‘¥¹œ¡¡½É¥é½¹Ñ…°€ô€Ð¹‘À°Ù•ÉÑ¥…°€ô€ÄÐ¹‘À¤°(€€€€€€€Ù•ÉÑ¥…±±¥¹µ•¹Ð€ô±¥¹µ•¹Ð¹•¹Ñ•ÉY•ÉÑ¥…±±ä°(€€€€¤ì(€€€€€€€%½¸ (€€€€€€€€€€€¥½¸°(€€€€€€€€€€€½¹Ñ•¹Ñ•ÍÉ¥ÁÑ¥½¸€ô¹Õ±°°(€€€€€€€€€€€Ñ¥¹Ð€ô¥˜€¡Í•±•Ñ•¤5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…”(€€€€€€€€€€€•±Í”5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…•Y…É¥…¹Ð°(€€€€€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È¹Í¥é” ÈÈ¹‘À¤°(€€€€€€€€¤(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹Í¥é” ÄÐ¹‘À¤¤(€€€€€€€Q•áÐ (€€€€€€€€€€€Ñ•áÐ€ô±…‰•°°(€€€€€€€€€€€ÍÑå±”€ô¥˜€¡Í•±•Ñ•¤5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹Ñ¥Ñ±•5•‘¥Õ´(€€€€€€€€€€€•±Í”5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹‰½‘å1…É”°(€€€€€€€€€€€½±½È€ô¥˜€¡Í•±•Ñ•¤5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…”(€€€€€€€€€€€•±Í”5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…•Y…É¥…¹Ð°(€€€€€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤°(€€€€€€€€¤(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹Í¥é” à¹‘À¤¤(€€€€€€€	½à (€€€€€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È(€€€€€€€€€€€€€€€€¹Í¥é” Äà¹‘À¤(€€€€€€€€€€€€€€€€¹Ñ¡•¸ (€€€€€€€€€€€€€€€€€€€¥˜€¡Í•±•Ñ•¤ì(€€€€€€€€€€€€€€€€€€€€€€€5½‘¥™¥•È¹‰…­É½Õ¹¡5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…”°¥É±•M¡…Á”¤(€€€€€€€€€€€€€€€€€€€ô•±Í”ì(€€€€€€€€€€€€€€€€€€€€€€€5½‘¥™¥•È¹‰½É‘•È Ä¸Ô¹‘À°5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½ÕÑ±¥¹”°¥É±•M¡…Á”¤(€€€€€€€€€€€€€€€€€€€ô(€€€€€€€€€€€€€€€€¤°(€€€€€€€€¤(€€€ô)ô((¼¨¨(€¨¹…Ù¥…Ñ¥½¸É½Üè±•…‘¥¹œÑ¡¥¸µÍÑÉ½­”¥½¸°±…‰•°°ÑÉ…¥±¥¹œ¡•ÙÉ½¸¸(€¨UÍ•™½ÈÑ¡”AÉ¥Ù…äA½±¥ä°Q•ÉµÌ½˜UÍ”…¹É•Á±…äµ¥¹ÑÉ½‘ÕÑ¥½¸±¥¹­Ì¸(€¨¼)½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸M•ÑÑ¥¹Í1¥¹¬ (€€€¥½¸è%µ…•Y•Ñ½È°(€€€±…‰•°èMÑÉ¥¹œ°(€€€½¹±¥¬è€ ¤€´øU¹¥Ð°(¤ì(€€€I½Ü (€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È(€€€€€€€€€€€€¹™¥±±5…á]¥‘Ñ  ¤(€€€€€€€€€€€€¹±¥À¡I½Õ¹‘•‘½É¹•ÉM¡…Á” à¹‘À¤¤(€€€€€€€€€€€€¹±¥­…‰±”¡½¹±¥¬€ô½¹±¥¬¤(€€€€€€€€€€€€¹Á…‘‘¥¹œ¡¡½É¥é½¹Ñ…°€ô€Ð¹‘À°Ù•ÉÑ¥…°€ô€ÄÐ¹‘À¤°(€€€€€€€Ù•ÉÑ¥…±±¥¹µ•¹Ð€ô±¥¹µ•¹Ð¹•¹Ñ•ÉY•ÉÑ¥…±±ä°(€€€€¤ì(€€€€€€€%½¸ (€€€€€€€€€€€¥½¸°(€€€€€€€€€€€½¹Ñ•¹Ñ•ÍÉ¥ÁÑ¥½¸€ô¹Õ±°°(€€€€€€€€€€€Ñ¥¹Ð€ô5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…•Y…É¥…¹Ð°(€€€€€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È¹Í¥é” ÈÈ¹‘À¤°(€€€€€€€€¤(€€€€€€€MÁ…•È¡5½‘¥™¥•È¹Í¥é” ÄÐ¹‘À¤¤(€€€€€€€Q•áÐ (€€€€€€€€€€€Ñ•áÐ€ô±…‰•°°(€€€€€€€€€€€ÍÑå±”€ô5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹‰½‘å1…É”°(€€€€€€€€€€€½±½È€ô5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½¹MÕÉ™…”°(€€€€€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤°(€€€€€€€€¤(€€€€€€€%½¸ (€€€€€€€€€€€%½¹Ì¹=ÕÑ±¥¹•¹-•å‰½…É‘ÉÉ½ÝI¥¡Ð°(€€€€€€€€€€€½¹Ñ•¹Ñ•ÍÉ¥ÁÑ¥½¸€ô¹Õ±°°(€€€€€€€€€€€Ñ¥¹Ð€ô5…Ñ•É¥…±Q¡•µ”¹½±½ÉM¡•µ”¹½ÕÑ±¥¹”°(€€€€€€€€€€€µ½‘¥™¥•È€ô5½‘¥™¥•È¹Í¥é” ÈÀ¹‘À¤°(€€€€€€€€¤(€€€ô)ô