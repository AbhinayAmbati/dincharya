# Dincharya — ProGuard/R8 rules for release builds.
# Room, WorkManager and Compose ship their own consumer rules; nothing extra
# is needed for v1. If a class is stripped incorrectly, add a keep rule here
# with a comment explaining WHY it is required.

# Keep the Room entities via reflection-free KSP codegen (already safe).
# Keep WorkManager workers (instantiated by reflection by the framework).
-keep class com.dincharya.app.notifications.ReminderWorker { *; }
