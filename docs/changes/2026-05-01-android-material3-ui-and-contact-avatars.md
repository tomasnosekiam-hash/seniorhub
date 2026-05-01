# Android: Material 3 UI baseline and contact avatars

Date: 2026-05-01

## Summary

- Rebased the senior tablet UI theme on the standard Compose Material 3 dark color scheme, typography scale, and shape scale.
- Replaced the custom left menu styling with Material 3 `NavigationRail` and `NavigationRailItem`.
- Moved content containers toward Material 3 `Card` surfaces using `MaterialTheme.colorScheme` roles instead of custom ink colors.
- Added avatar image support to contacts for the senior tablet MVP.

## Contact avatars

- `Contact` now includes `avatarUri`.
- MVP contact creation stores an optional local image URI.
- The senior add-contact overlay can pick an image with Android's document picker and persists read access.
- Contact avatars render the selected image when available, with the initial-based fallback retained.

## Verification

- `./gradlew :app:assembleDebug` passes.
- Debug APK was installed on the connected tablet with `adb install -r`.
- Screenshot verification was attempted, but the tablet was on/behind the lock screen and the final screencap returned black.

## Notes

- Avatar image storage is currently local-URI based for MVP tablet testing. A later production pass should upload avatar files to shared storage if admins need to manage avatars remotely across devices.
