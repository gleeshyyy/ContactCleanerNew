# Contact Cleaner — fresh source

This is a fresh Android project for finding repeated phone numbers. Later occurrences are pre-selected for review; the first occurrence is not listed for deletion. Deletion requires confirmation.

## Build
1. Create a new GitHub repository.
2. Upload/push all files from this folder to its `main` branch.
3. Open Actions and wait for **Build Contact Cleaner APK** to finish.
4. Download the `ContactCleaner-debug-apk` artifact, extract it, and install `app-debug.apk`.

## Notes
- Back up contacts before deleting; deletion cannot be automatically undone.
- Detection compares normalized phone-number digits; formatting differences may affect matching.
- Android does not provide ordinary apps with WhatsApp last-seen or inactivity data.
