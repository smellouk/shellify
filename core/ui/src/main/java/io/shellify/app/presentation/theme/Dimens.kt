package io.shellify.app.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {

    // Spacing (padding / arrangement)
    val spaceXxxs: Dp = 2.dp
    val spaceXxs: Dp = 4.dp
    val spaceXs: Dp = 6.dp
    val spaceSm: Dp = 8.dp
    val space10: Dp = 10.dp
    val spaceMd: Dp = 12.dp
    val space14: Dp = 14.dp
    val spaceLg: Dp = 16.dp
    val spaceXl: Dp = 24.dp
    val space18: Dp = 18.dp
    val space22: Dp = 22.dp
    val spaceXxl: Dp = 48.dp

    // Sizes (icons and component containers, unified scale)
    val sizeXxs: Dp = 12.dp
    val sizeTagIcon: Dp = 13.dp  // icon inside a feature-tag badge
    val sizeXs: Dp = 16.dp
    val sizeSm: Dp = 18.dp
    val sizeMd: Dp = 20.dp
    val sizeLg: Dp = 22.dp
    val sizeXl: Dp = 24.dp
    val size2xl: Dp = 26.dp
    val size3xl: Dp = 28.dp
    val size4xl: Dp = 32.dp
    val size5xl: Dp = 36.dp
    val sizeCard: Dp = 40.dp  // icon container box
    val sizeApp: Dp = 48.dp  // app card / shortcut icon
    val sizeIconPreview: Dp = 52.dp  // add-screen icon preview box
    val sizeEmptyIcon: Dp = 72.dp  // empty-state icon (shortcuts)
    val sizeEmptyIconLg: Dp = 96.dp  // empty-state icon (home)
    val sizeEmptyBox: Dp = 120.dp // empty-state outer container
    val sizeGridCell: Dp = 160.dp // adaptive grid cell minimum width

    // Corner radii
    val cornerXxs: Dp = 4.dp
    val cornerXs: Dp = 6.dp   // feature-tag badge
    val cornerSm: Dp = 8.dp   // icon selector cells
    val cornerMd: Dp = 10.dp  // text fields / dropdowns
    val cornerLg: Dp = 12.dp  // chip rows, app icon
    val cornerIcon: Dp = 13.dp  // add-screen icon preview
    val cornerXl: Dp = 16.dp  // cards

    // Borders / strokes
    val borderHair: Dp = 0.5.dp
    val borderDefault: Dp = 1.dp
    val borderSelected: Dp = 2.dp
    val strokeSm: Dp = 1.5.dp
    val strokeMd: Dp = 2.dp

    // Empty-state illustration sizes
    val illustrationSize: Dp = 160.dp
    val illustrationSizeMid: Dp = 116.dp
    val illustrationSizeInner: Dp = 72.dp
    val illustrationRadius: Dp = 70.dp

    // Additional sizes
    val sizeIconLarge: Dp = 30.dp  // Icon inside illustration tile
    val sizeIllustrationTile: Dp = 64.dp  // Center tile in illustration
    val sizeIconContainer: Dp = 36.dp  // Settings row icon container
    val sizeIconHero: Dp = 56.dp  // Onboarding hero icon tile (security, backup)
    val sizeIconTile: Dp = 44.dp  // Suggestion / feature card icon tile
    val sizeCheckPill: Dp = 28.dp  // QuickPicks circular check toggle
    val heroHeightSm: Dp = 200.dp // Onboarding short hero (QuickPicks)
    val widthSuggestionsMax: Dp = 300.dp  // max width for quick-suggestions column (HomeScreen)
    val widthChipsMax: Dp = 280.dp        // max width for suggestion-chip rows (CategoryScreen)

    // Additional corners
    val cornerFull: Dp = 100.dp  // Pill shape
    val corner12: Dp = 12.dp   // Icon box corners
    val corner14: Dp = 14.dp   // Suggestion row corners / floating tile
    val corner18: Dp = 18.dp   // Feature/permission/backup card corners
    val corner20: Dp = 20.dp   // Category card / appearance preview card
    val corner24: Dp = 24.dp   // CTA button corners
    val corner28: Dp = 28.dp   // Onboarding brand tile / banner clip

    // Typography sizes
    val textSizeEmptyTitle: TextUnit = 22.sp  // Empty-state headline in all screens
    val textSizeBody: TextUnit = 13.sp  // Compact body / supporting text
    val textSizeCta: TextUnit = 14.sp  // Call-to-action button labels
    val textSizeCaption: TextUnit = 11.sp  // Overline / caption (e.g. QUICK SUGGESTIONS)
    val textSizeSectionLabel: TextUnit = 12.sp  // Settings section label

    // Letter spacing
    val letterSpacingTight: TextUnit = (-0.3).sp  // Empty-state titles
    val letterSpacingOverline: TextUnit = 0.8.sp     // Overline / eyebrow labels
    val letterSpacingCaps: TextUnit = 1.sp        // ALL-CAPS labels

    // Line height
    val lineHeightBody: TextUnit = 19.5.sp  // Paired with textSizeBody
}
